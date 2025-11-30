package hu.ait.maral.fairshare.ui.screen.notifications

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.maral.fairshare.data.Group

class NotificationsViewModel : ViewModel() {

    var pendingGroups = mutableStateOf<List<Group>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /** Load all groups where current user is in pendingMemberIds */
    fun loadPendingGroups() {
        val user = auth.currentUser ?: return

        isLoading.value = true
        errorMessage.value = null

        db.collection("groups")
            .whereArrayContains("pendingMemberIds", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Group::class.java)?.copy(groupId = doc.id)
                }
                pendingGroups.value = result
                isLoading.value = false
            }
            .addOnFailureListener { e ->
                errorMessage.value = e.localizedMessage
                isLoading.value = false
            }
    }

    /** User accepts an invite: move uid from pendingMemberIds → memberIds (+ add email to members) */
    fun acceptGroup(group: Group) {
        val user = auth.currentUser ?: return
        val groupRef = db.collection("groups").document(group.groupId)
        val userRef = db.collection("users").document(user.uid)

        db.runTransaction { tx ->
            val groupSnap = tx.get(groupRef)
            val userSnap = tx.get(userRef)

            val currentMemberIds =
                (groupSnap.get("memberIds") as? List<String>) ?: emptyList()
            val currentPendingIds =
                (groupSnap.get("pendingMemberIds") as? List<String>) ?: emptyList()
            val currentMembers =
                (groupSnap.get("members") as? List<String>) ?: emptyList()
            val currentBalancesAny =
                (groupSnap.get("balances") as? List<*>) ?: emptyList<Any>()

            val currentBalances = currentBalancesAny.map {
                (it as? Number)?.toDouble() ?: 0.0
            }

            // Fetch display name for this uid
            val nameFromDoc = userSnap.getString("name")
            val displayName = nameFromDoc
                ?: user.displayName
                ?: user.email
                ?: "Unknown"

            // 1) Move uid from pending -> members
            val updatedMemberIds = (currentMemberIds + user.uid).distinct()
            val updatedPendingIds = currentPendingIds.filter { it != user.uid }

            // 2) Add name + balance 0.0 only NOW (when they accept)
            val updatedMembers = currentMembers + displayName
            val updatedBalances = currentBalances + 0.0

            tx.update(
                groupRef,
                mapOf(
                    "memberIds" to updatedMemberIds,
                    "pendingMemberIds" to updatedPendingIds,
                    "members" to updatedMembers,
                    "balances" to updatedBalances
                )
            )
        }.addOnSuccessListener {
            loadPendingGroups()
        }.addOnFailureListener { e ->
            errorMessage.value = e.localizedMessage
        }
    }

    fun declineGroup(group: Group) {
        val user = auth.currentUser ?: return
        val groupRef = db.collection("groups").document(group.groupId)

        db.runTransaction { transaction ->
            val snap = transaction.get(groupRef)

            val currentPendingIds =
                (snap.get("pendingMemberIds") as? List<String>) ?: emptyList()
            val updatedPendingIds = currentPendingIds.filter { it != user.uid }

            transaction.update(
                groupRef,
                mapOf(
                    "pendingMemberIds" to updatedPendingIds
                )
            )
        }.addOnSuccessListener {
            loadPendingGroups()
        }.addOnFailureListener { e ->
            errorMessage.value = e.localizedMessage
        }
    }
}
