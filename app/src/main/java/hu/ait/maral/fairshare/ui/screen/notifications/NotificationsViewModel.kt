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

    /**
     * User accepts an invite:
     * - move uid from pendingMemberIds → memberIds
     * - add their display name to members
     * - ensure balances is a Map<memberId, Double> and add entry for this user
     */
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

            // --- balances: support BOTH old list format and new map format ---
            val rawBalances = groupSnap.get("balances")

            val balancesMap: MutableMap<String, Double> = when (rawBalances) {
                is Map<*, *> -> {
                    // New schema already: Map<String, Number>
                    rawBalances.mapNotNull { (k, v) ->
                        val key = k as? String ?: return@mapNotNull null
                        val value = (v as? Number)?.toDouble() ?: 0.0
                        key to value
                    }.toMap().toMutableMap()
                }

                is List<*> -> {
                    // Old schema: List<Double> aligned with memberIds by index
                    val list = rawBalances.map { (it as? Number)?.toDouble() ?: 0.0 }
                    mutableMapOf<String, Double>().apply {
                        for (i in currentMemberIds.indices) {
                            val id = currentMemberIds[i]
                            val value = list.getOrNull(i) ?: 0.0
                            this[id] = value
                        }
                    }
                }

                else -> mutableMapOf()
            }
            // ----------------------------------------------------------------

            // Display name for the accepting user
            val nameFromDoc = userSnap.getString("name")
            val displayName = nameFromDoc
                ?: user.displayName
                ?: user.email
                ?: "Unknown"

            // 1) Move uid from pending -> members
            val updatedMemberIds = (currentMemberIds + user.uid).distinct()
            val updatedPendingIds = currentPendingIds.filter { it != user.uid }

            // 2) Add name + ensure they have a 0.0 entry in balances
            val updatedMembers = currentMembers + displayName
            if (!balancesMap.containsKey(user.uid)) {
                balancesMap[user.uid] = 0.0
            }

            // 3) Write everything back, always as MAP for balances
            tx.update(
                groupRef,
                mapOf(
                    "memberIds" to updatedMemberIds,
                    "pendingMemberIds" to updatedPendingIds,
                    "members" to updatedMembers,
                    "balances" to balancesMap
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
