package hu.ait.maral.fairshare.ui.screen.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.maral.fairshare.data.Group
import hu.ait.maral.fairshare.data.User

class HomeScreenViewModel : ViewModel() {

    var preferredCurrency = mutableStateOf("EUR")
        private set

    var groups = mutableStateOf<List<Group>>(emptyList())
        private set

    var isLoading = mutableStateOf(false)
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun loadGroupsForUser() {
        val user = auth.currentUser ?: return
        isLoading.value = true

        db.collection("groups")
            .whereArrayContains("memberIds", user.uid)
            .get()
            .addOnSuccessListener { snap ->
                val result = snap.documents.mapNotNull { doc ->
                    doc.toObject(Group::class.java)?.copy(groupId = doc.id)
                }
                groups.value = result
                isLoading.value = false
            }
            .addOnFailureListener { e ->
                errorMessage.value = e.localizedMessage
                isLoading.value = false
            }
    }

    fun createGroup(name: String, memberEmails: List<String>) {
        val creator = auth.currentUser ?: return
        val creatorUid = creator.uid

        if (name.isBlank()) {
            errorMessage.value = "Group name cannot be empty."
            return
        }

        isLoading.value = true
        errorMessage.value = null

        db.collection("users").document(creatorUid).get()
            .addOnSuccessListener { userSnap ->
                val creatorName =
                    userSnap.getString("name") ?: creator.email ?: "Unknown"

                // CLEAN invited emails
                val invitedEmails = memberEmails
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .distinct()

                // If no invited members → simple group with creator only
                if (invitedEmails.isEmpty()) {
                    val groupData = mapOf(
                        "name" to name,
                        "members" to listOf(creatorName),
                        "memberIds" to listOf(creatorUid),
                        "pendingMemberIds" to emptyList<String>(),
                        "balances" to mapOf(creatorUid to 0.0) // NEW MAP FORMAT
                    )
                    db.collection("groups").add(groupData)
                        .addOnSuccessListener {
                            isLoading.value = false
                            loadGroupsForUser()
                        }
                        .addOnFailureListener {
                            errorMessage.value = it.localizedMessage
                            isLoading.value = false
                        }
                    return@addOnSuccessListener
                }

                val emailToUid = mutableMapOf<String, String>()
                val invalid = mutableListOf<String>()
                var processed = 0
                val total = invitedEmails.size

                fun finishValidation() {
                    if (invalid.isNotEmpty()) {
                        errorMessage.value =
                            "Invalid users: ${invalid.joinToString()}"
                        isLoading.value = false
                        return
                    }

                    val pendingIds = emailToUid.values
                        .filter { it != creatorUid }
                        .distinct()

                    val groupData = mapOf(
                        "name" to name,
                        "members" to listOf(creatorName),
                        "memberIds" to listOf(creatorUid),
                        "pendingMemberIds" to pendingIds,
                        "balances" to mapOf(creatorUid to 0.0) // ONLY creator has balance
                    )

                    db.collection("groups").add(groupData)
                        .addOnSuccessListener {
                            isLoading.value = false
                            loadGroupsForUser()
                        }
                        .addOnFailureListener {
                            errorMessage.value = it.localizedMessage
                            isLoading.value = false
                        }
                }

                invitedEmails.forEach { email ->
                    db.collection("users")
                        .whereEqualTo("email", email)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { snap ->
                            processed++
                            val doc = snap.documents.firstOrNull()
                            val uid = doc?.getString("uid")

                            if (uid != null) emailToUid[email] = uid
                            else invalid.add(email)

                            if (processed == total) finishValidation()
                        }
                        .addOnFailureListener {
                            processed++
                            invalid.add(email)
                            if (processed == total) finishValidation()
                        }
                }
            }
            .addOnFailureListener {
                isLoading.value = false
                errorMessage.value = it.localizedMessage
            }
    }

    fun loadUserPreferredCurrency() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                preferredCurrency.value =
                    snap.toObject(User::class.java)?.preferredCurrency ?: "EUR"
            }
    }
}
