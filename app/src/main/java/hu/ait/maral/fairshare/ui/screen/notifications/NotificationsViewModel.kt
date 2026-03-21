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
    private val db   = FirebaseFirestore.getInstance()

    fun loadPendingGroups() {
        val user = auth.currentUser ?: return

        isLoading.value = true
        errorMessage.value = null

        db.collection("groups")
            .whereArrayContains("pendingMemberIds", user.uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage.value = e.localizedMessage
                    isLoading.value = false
                    return@addSnapshotListener
                }
                val result = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Group::class.java)?.copy(groupId = doc.id)
                } ?: emptyList()
                pendingGroups.value = result
                isLoading.value = false
            }
    }

    fun acceptGroup(group: Group) {
        val user = auth.currentUser ?: return
        val groupRef = db.collection("groups").document(group.groupId)
        val userRef  = db.collection("users").document(user.uid)

        db.runTransaction { tx ->
            val groupSnap = tx.get(groupRef)
            val userSnap  = tx.get(userRef)

            val currentMemberIds  = (groupSnap.get("memberIds")        as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val currentPendingIds = (groupSnap.get("pendingMemberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val currentMembers    = (groupSnap.get("members")          as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            // Safely read balances regardless of how Firestore stored the numbers
            val rawBalances = groupSnap.get("balances")
            val balancesMap: MutableMap<String, Double> = when (rawBalances) {
                is Map<*, *> -> rawBalances.mapNotNull { (k, v) ->
                    val key   = k as? String ?: return@mapNotNull null
                    val value = (v as? Number)?.toDouble() ?: 0.0
                    key to value
                }.toMap().toMutableMap()
                else -> mutableMapOf()
            }

            val displayName = userSnap.getString("name")
                ?: user.displayName
                ?: user.email
                ?: "Unknown"

            val updatedMemberIds  = (currentMemberIds + user.uid).distinct()
            val updatedPendingIds = currentPendingIds.filter { it != user.uid }
            val updatedMembers    = currentMembers + displayName

            if (!balancesMap.containsKey(user.uid)) {
                balancesMap[user.uid] = 0.0
            }

            tx.update(
                groupRef,
                mapOf(
                    "memberIds"        to updatedMemberIds,
                    "pendingMemberIds" to updatedPendingIds,
                    "members"          to updatedMembers,
                    "balances"         to balancesMap
                )
            )
        }.addOnSuccessListener {
            // Snapshot listener above will auto-refresh pendingGroups
        }.addOnFailureListener { e ->
            errorMessage.value = e.localizedMessage
        }
    }

    fun declineGroup(group: Group) {
        val user     = auth.currentUser ?: return
        val groupRef = db.collection("groups").document(group.groupId)

        db.runTransaction { tx ->
            val snap              = tx.get(groupRef)
            val currentPendingIds = (snap.get("pendingMemberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val updatedPendingIds = currentPendingIds.filter { it != user.uid }
            tx.update(groupRef, mapOf("pendingMemberIds" to updatedPendingIds))
        }.addOnFailureListener { e ->
            errorMessage.value = e.localizedMessage
        }
    }
}