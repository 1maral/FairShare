package hu.ait.maral.fairshare.ui.screen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import hu.ait.maral.fairshare.data.Group
import hu.ait.maral.fairshare.data.User

class RoomViewModel : ViewModel() {

    var group = mutableStateOf<Group?>(null)
        private set

    var preferredCurrency = mutableStateOf("EUR")
        private set

    var isLoading = mutableStateOf(false)
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    // Map<memberId, amountUserOwesThemInEur>
    var owedPerPerson = mutableStateOf<Map<String, Double>>(emptyMap())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var groupListener: ListenerRegistration? = null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Observe group document in real-time.
     */
    fun observeGroup(groupId: String) {
        if (groupId.isBlank()) {
            errorMessage.value = "Invalid group id."
            return
        }

        // Remove previous listener if any
        groupListener?.remove()

        isLoading.value = true
        errorMessage.value = null

        groupListener = db.collection("groups")
            .document(groupId)
            .addSnapshotListener { doc, e ->
                if (e != null) {
                    errorMessage.value = e.localizedMessage
                    isLoading.value = false
                    return@addSnapshotListener
                }

                if (doc != null && doc.exists()) {
                    val loadedGroup = doc.toObject(Group::class.java)?.copy(groupId = doc.id)
                    group.value = loadedGroup
                    recomputeOwedPerPerson()
                    isLoading.value = false
                } else {
                    errorMessage.value = "Group not found."
                    isLoading.value = false
                }
            }
    }

    fun loadUserPreferredCurrency() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { snap ->
                preferredCurrency.value =
                    snap.toObject(User::class.java)?.preferredCurrency ?: "EUR"
            }
    }

    private fun recomputeOwedPerPerson() {
        val g = group.value ?: return
        val uid = currentUserId ?: return
        owedPerPerson.value = computeOwedPerPerson(g.balances, uid)
    }

    /**
     * Given group-level net balances (in EUR) and the current user,
     * compute how much the user owes each other member.
     *
     * balances: Map<memberId, netBalanceInEur>
     *   > 0  -> group owes this member
     *   < 0  -> this member owes the group
     */
    private fun computeOwedPerPerson(
        balances: Map<String, Double>,
        currentUserId: String
    ): Map<String, Double> {
        val userBalance = balances[currentUserId] ?: 0.0

        // If the user isn't a debtor, they don't owe anyone.
        if (userBalance >= 0.0) return emptyMap()

        var remainingDebt = -userBalance  // positive amount user must pay
        val result = mutableMapOf<String, Double>()

        // Collect all creditors (people the group owes)
        val creditors = balances
            .filter { (memberId, balance) ->
                memberId != currentUserId && balance > 0.0
            }
            .toList()

        // Greedy: pay creditors in order until your debt is exhausted
        for ((creditorId, creditorBalance) in creditors) {
            if (remainingDebt <= 0.0) break

            val pay = minOf(remainingDebt, creditorBalance)
            if (pay > 0.0) {
                result[creditorId] = pay
                remainingDebt -= pay
            }
        }

        return result
    }

    fun getBills(groupId: String): List<Bill> {
        if (groupId.isBlank()) {
            errorMessage.value = "Invalid group ID for bills."
            return emptyList()
        }

        // Remove previous listener
        billsListener?.remove()

        isLoading.value = true
        errorMessage.value = null

        billsListener = db.collection("bills")
            .whereEqualTo("groupId", groupId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    errorMessage.value = "Error loading bills: ${e.message}"
                    isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val bill = doc.toObject(Bill::class.java)
                        bill?.copy(billId = doc.id)
                    }

                    // Sort newest → oldest by billDate
                    bills.value = list.sortedByDescending { it.billDate }
                }

                isLoading.value = false
            }

        return bills.value
    }

    override fun onCleared() {
        super.onCleared()
        groupListener?.remove()
    }

    fun fetchUserAvatar(uid: String, callback: (String?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                val url = snap.getString("profilePictureUrl")
                callback(url)
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}
