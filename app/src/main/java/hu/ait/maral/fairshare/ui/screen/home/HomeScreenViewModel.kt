package hu.ait.maral.fairshare.ui.screen.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.maral.fairshare.data.Group
import hu.ait.maral.fairshare.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HomeScreenViewModel : ViewModel() {

    var groups = mutableStateOf<List<Group>>(emptyList())
        private set

    var archivedGroups = mutableStateOf<List<Group>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    var errorMessage = mutableStateOf<String?>(null)

    var preferredCurrency = mutableStateOf("EUR")
        private set

    var pendingNotificationCount = mutableStateOf(0)
        private set

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Helpers ───────────────────────────────────────────────────────────────
    fun loadUserPreferredCurrency() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                preferredCurrency.value =
                    snap.toObject(User::class.java)?.preferredCurrency ?: "EUR"
            }
    }

    fun loadPendingCount() {
        val uid = auth.currentUser?.uid ?: return
        // Queries pendingMemberIds on groups directly — consistent with
        // NotificationsViewModel which uses the same field.
        db.collection("groups")
            .whereArrayContains("pendingMemberIds", uid)
            .addSnapshotListener { snap, _ ->
                pendingNotificationCount.value = snap?.size() ?: 0
            }
    }

    fun loadGroupsForUser() {
        val uid = auth.currentUser?.uid ?: return
        isLoading.value = true
        db.collection("groups")
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage.value = e.localizedMessage
                    isLoading.value = false
                    return@addSnapshotListener
                }
                groups.value = snapshot?.documents
                    ?.mapNotNull { doc -> doc.toObject(Group::class.java)?.copy(groupId = doc.id) }
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                isLoading.value = false
            }
    }

    fun loadArchivedGroupsForUser() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userSnap ->
                @Suppress("UNCHECKED_CAST")
                val archivedIds = (userSnap.get("archivedRooms") as? List<String>) ?: emptyList()
                if (archivedIds.isEmpty()) {
                    archivedGroups.value = emptyList()
                    return@addOnSuccessListener
                }
                val fetched = mutableListOf<Group>()
                var pending = archivedIds.size
                archivedIds.forEach { groupId ->
                    db.collection("groups").document(groupId).get()
                        .addOnSuccessListener { groupSnap ->
                            groupSnap.toObject(Group::class.java)
                                ?.copy(groupId = groupSnap.id)
                                ?.let { fetched.add(it) }
                            pending--
                            if (pending == 0) archivedGroups.value = fetched.sortedByDescending { it.createdAt }                        }
                        .addOnFailureListener {
                            pending--
                            if (pending == 0) archivedGroups.value = fetched.sortedByDescending { it.createdAt }                        }
                }
            }
    }

    fun fetchUserAvatar(uid: String, callback: (String?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap -> callback(snap.getString("profilePictureUrl")) }
            .addOnFailureListener { callback(null) }
    }

    // ── Create group ──────────────────────────────────────────────────────────
    // Only the creator is added to memberIds immediately.
    // Every invited email goes into pendingMemberIds — they must accept the
    // invite in NotificationsScreen before they actually join.
    fun createGroup(name: String, memberEmails: List<String>) {
        val uid = auth.currentUser?.uid ?: return

        when {
            name.isBlank()   -> { errorMessage.value = "Group name cannot be empty.";             return }
            name.length > 25 -> { errorMessage.value = "Group name cannot exceed 25 characters."; return }
        }

        val trimmedEmails = memberEmails.map { it.trim() }.filter { it.isNotEmpty() }

        viewModelScope.launch(Dispatchers.Main) {
            isLoading.value = true
            try {
                // 1. Fetch creator info
                val creatorSnap = withContext(Dispatchers.IO) {
                    db.collection("users").document(uid).get().await()
                }
                val creatorName  = creatorSnap.getString("name")?.takeIf { it.isNotBlank() } ?: "Me"
                val creatorEmail = creatorSnap.getString("email") ?: ""

                // 2. Resolve every invited email — abort if any is not registered
                val pendingUids = mutableListOf<String>()

                for (email in trimmedEmails) {
                    // Skip silently if the creator typed their own email
                    if (email.equals(creatorEmail, ignoreCase = true)) continue

                    val snap = withContext(Dispatchers.IO) {
                        db.collection("users").whereEqualTo("email", email).get().await()
                    }
                    val doc = snap.documents.firstOrNull()

                    if (doc == null) {
                        errorMessage.value = "No account found for \"$email\". " +
                                "Make sure they have registered in FairShare first."
                        isLoading.value = false
                        return@launch
                    }

                    if (!pendingUids.contains(doc.id)) {
                        pendingUids.add(doc.id)
                    }
                }

                // 3. Write the group:
                //    memberIds        = [creatorUid] only
                //    pendingMemberIds = invited uids (join on accept)
                //    members          = [creatorName] only
                //    balances         = { creatorUid: 0.0 } only
                val groupData = hashMapOf(
                    "name"             to name,
                    "memberIds"        to listOf(uid),
                    "pendingMemberIds" to pendingUids,
                    "members"          to listOf(creatorName),
                    "balances"         to mapOf(uid to 0.0),
                    "createdAt"        to System.currentTimeMillis()
                )
                withContext(Dispatchers.IO) {
                    db.collection("groups").add(groupData).await()
                }

            } catch (e: Exception) {
                errorMessage.value =
                    e.localizedMessage ?: "Failed to create group. Please try again."
            } finally {
                isLoading.value = false
            }
        }
    }

    // ── Add members to existing group ─────────────────────────────────────────
    // Same rule: invited users go into pendingMemberIds, not memberIds.
    fun addMembersToGroup(groupId: String, emails: List<String>) {
        val trimmed = emails.map { it.trim() }.filter { it.isNotEmpty() }
        if (trimmed.isEmpty()) return

        viewModelScope.launch(Dispatchers.Main) {
            isLoading.value = true
            try {
                val groupRef  = db.collection("groups").document(groupId)
                val groupSnap = withContext(Dispatchers.IO) { groupRef.get().await() }

                val memberIds = (groupSnap.get("memberIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.toMutableList()
                    ?: mutableListOf()

                val pendingIds = (groupSnap.get("pendingMemberIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    ?.toMutableList()
                    ?: mutableListOf()

                for (email in trimmed) {
                    val snap = withContext(Dispatchers.IO) {
                        db.collection("users").whereEqualTo("email", email).get().await()
                    }
                    val doc = snap.documents.firstOrNull()

                    if (doc == null) {
                        errorMessage.value = "No account found for \"$email\". " +
                                "Make sure they have registered in FairShare first."
                        return@launch
                    }

                    // Skip if already a full member or already has a pending invite
                    if (!memberIds.contains(doc.id) && !pendingIds.contains(doc.id)) {
                        pendingIds.add(doc.id)
                    }
                }

                // Only update pendingMemberIds — memberIds stays unchanged until they accept
                withContext(Dispatchers.IO) {
                    groupRef.update("pendingMemberIds", pendingIds).await()
                }

            } catch (e: Exception) {
                errorMessage.value =
                    e.localizedMessage ?: "Failed to add members. Please try again."
            } finally {
                isLoading.value = false
            }
        }
    }
}