package hu.ait.maral.fairshare.ui.screen.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.maral.fairshare.data.Group
import hu.ait.maral.fairshare.data.User
import kotlin.jvm.java

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
        errorMessage.value = null

        db.collection("groups")
            .whereArrayContains("memberIds", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Group::class.java)
                        ?.copy(groupId = doc.id)
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
        val currentUser = auth.currentUser ?: return

        if (name.isBlank()) {
            errorMessage.value = "Group name cannot be empty."
            return
        }

        val invitedEmails = buildSet<String> {
            memberEmails.forEach { raw ->
                val clean = raw.trim().lowercase()
                if (clean.isNotEmpty()) add(clean)
            }
        }.toList()

        isLoading.value = true
        errorMessage.value = null

        val creatorUid = currentUser.uid
        val usersRef = db.collection("users").document(creatorUid)

        usersRef.get().addOnSuccessListener { userSnap ->
            val creatorName = userSnap.getString("name")
                ?: currentUser.displayName
                ?: currentUser.email
                ?: "Unknown"

            if (invitedEmails.isEmpty()) {
                val groupData = hashMapOf(
                    "name" to name,
                    "members" to listOf(creatorName),
                    "memberIds" to listOf(creatorUid),
                    "pendingMemberIds" to emptyList<String>(),
                    "balances" to listOf(0.0)
                )

                db.collection("groups")
                    .add(groupData)
                    .addOnSuccessListener {
                        isLoading.value = false
                        loadGroupsForUser()
                    }
                    .addOnFailureListener { e ->
                        isLoading.value = false
                        errorMessage.value = e.localizedMessage
                    }

                return@addOnSuccessListener
            }

            val emailToUid = mutableMapOf<String, String>()
            val invalidEmails = mutableListOf<String>()
            var processed = 0
            val total = invitedEmails.size

            fun finishValidation() {
                if (invalidEmails.isNotEmpty()) {
                    isLoading.value = false
                    errorMessage.value =
                        "These emails are not registered users: ${invalidEmails.joinToString(", ")}"
                    return
                }

                val confirmedMemberIds = listOf(creatorUid)
                val confirmedMemberNames = listOf(creatorName)
                val confirmedBalances = listOf(0.0)

                val pendingMemberIds = emailToUid.values
                    .filter { it != creatorUid }
                    .distinct()

                val groupData = hashMapOf(
                    "name" to name,
                    "members" to confirmedMemberNames,
                    "memberIds" to confirmedMemberIds,
                    "pendingMemberIds" to pendingMemberIds,
                    "balances" to confirmedBalances
                )

                db.collection("groups")
                    .add(groupData)
                    .addOnSuccessListener {
                        isLoading.value = false
                        loadGroupsForUser()
                    }
                    .addOnFailureListener { e ->
                        isLoading.value = false
                        errorMessage.value = e.localizedMessage
                    }
            }

            for (email in invitedEmails) {
                db.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        processed++
                        val doc = snapshot.documents.firstOrNull()
                        if (doc != null) {
                            val uid = doc.getString("uid")
                            if (!uid.isNullOrBlank()) {
                                emailToUid[email] = uid
                            } else {
                                invalidEmails.add(email)
                            }
                        } else {
                            invalidEmails.add(email)
                        }

                        if (processed == total) finishValidation()
                    }
                    .addOnFailureListener {
                        processed++
                        invalidEmails.add(email)
                        if (processed == total) finishValidation()
                    }
            }
        }.addOnFailureListener { e ->
            isLoading.value = false
            errorMessage.value = e.localizedMessage
        }
    }

    fun loadUserPreferredCurrency() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { snap ->
                val user = snap.toObject(User::class.java)
                preferredCurrency.value = user?.preferredCurrency ?: "EUR"
            }
    }

}
