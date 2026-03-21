package hu.ait.maral.fairshare.ui.screen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import hu.ait.maral.fairshare.data.Bill
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

    var bills = mutableStateOf<List<Bill>>(emptyList())
        private set


    var owedPerPerson = mutableStateOf<Map<String, Double>>(emptyMap())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var groupListener: ListenerRegistration? = null
    private var billsListener: ListenerRegistration? = null

    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    fun observeGroup(groupId: String) {
        if (groupId.isBlank()) {
            errorMessage.value = "Invalid group id."
            return
        }

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

    private fun computeOwedPerPerson(
        balances: Map<String, Double>,
        currentUserId: String
    ): Map<String, Double> {
        val userBalance = balances[currentUserId] ?: 0.0

        if (userBalance >= -0.01) return emptyMap()

        var remainingDebt = -userBalance
        val result = mutableMapOf<String, Double>()

        val creditors = balances
            .filter { (memberId, balance) ->
                memberId != currentUserId && balance > 0.01
            }
            .toList()

        for ((creditorId, creditorBalance) in creditors) {
            if (remainingDebt <= 0.01) break

            val pay = minOf(remainingDebt, creditorBalance)
            if (pay > 0.01) {
                result[creditorId] = pay
                remainingDebt -= pay
            }
        }
        return result
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


    fun startBillsListener(groupId: String) {
        if (groupId.isBlank()) {
            errorMessage.value = "Invalid group ID for bills."
            return
        }

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

                    val sorted = list.sortedByDescending { it.billDate }
                    bills.value = sorted
                }
                isLoading.value = false
            }
    }

    // Checks balances, removes the user from the group's memberIds/members
    // lists, and adds the groupId to the user's archivedRooms in Firestore —
    // all in a single transaction so nothing is left in a half-state.
    fun leaveGroup(
        groupId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid   = currentUserId
        if (uid.isBlank()) return

        val g        = group.value ?: return
        val balances = g.balances
        val owedMap  = owedPerPerson.value

        // Check 1: you still owe someone
        val youOweNames = owedMap.entries
            .filter { (memberId, amount) -> memberId != uid && amount > 0.01 }
            .mapNotNull { (memberId, _) ->
                val idx = g.memberIds.indexOf(memberId)
                g.members.getOrNull(idx)
            }
        if (youOweNames.isNotEmpty()) {
            onError("You still owe ${youOweNames.joinToString(", ")}. Please settle before leaving.")
            return
        }

        // Check 2: someone still owes you
        val userBal = balances[uid] ?: 0.0
        if (userBal > 0.01) {
            val theyOweNames = g.memberIds
                .filter { it != uid }
                .mapNotNull { memberId ->
                    val otherBal = balances[memberId] ?: 0.0
                    if (otherBal < -0.01) {
                        val idx = g.memberIds.indexOf(memberId)
                        g.members.getOrNull(idx)
                    } else null
                }
            if (theyOweNames.isNotEmpty()) {
                onError("${theyOweNames.joinToString(", ")} still owe you. Ask them to settle first.")
                return
            }
        }

        // All clear, run a transaction that:
        //   1. Removes uid from groups/{groupId}.memberIds + members
        //   2. Adds groupId to users/{uid}.archivedRooms
        val groupRef = db.collection("groups").document(groupId)
        val userRef  = db.collection("users").document(uid)

        db.runTransaction { transaction ->
            // ── Read both docs first (Firestore requires all reads before writes) ──
            val groupSnap = transaction.get(groupRef)
            val userSnap  = transaction.get(userRef)

            // Update group: remove this user
            val memberIds = (groupSnap.get("memberIds") as? MutableList<String> ?: mutableListOf())
                .toMutableList()
            val members   = (groupSnap.get("members")   as? MutableList<String> ?: mutableListOf())
                .toMutableList()
            val idx = memberIds.indexOf(uid)
            if (idx != -1) { memberIds.removeAt(idx); members.removeAt(idx) }
            transaction.update(groupRef, "memberIds", memberIds)
            transaction.update(groupRef, "members",   members)

            // Update user: append groupId to archivedRooms (avoid duplicates)
            @Suppress("UNCHECKED_CAST")
            val existing = (userSnap.get("archivedRooms") as? List<String> ?: emptyList())
                .toMutableList()
            if (!existing.contains(groupId)) existing.add(groupId)
            transaction.update(userRef, "archivedRooms", existing)

        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onError("Failed to leave group: ${e.message}")
        }
    }


    fun settleDebtWithMember(
        groupId: String,
        creditorId: String,
        amountPaid: Double,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val g   = group.value ?: return onError("Group not loaded")
        val uid = currentUserId

        val balances        = g.balances.toMutableMap()
        val userBalance     = balances[uid] ?: 0.0
        val creditorBalance = balances[creditorId] ?: 0.0

        var newUserBalance     = userBalance + amountPaid
        var newCreditorBalance = creditorBalance - amountPaid

        // ── To zero if within 1 cent of zero ─────────────────────────────────
        // This prevents "0.00 owed" balances from floating-point rounding
        if (kotlin.math.abs(newUserBalance) < 0.01)     newUserBalance     = 0.0
        if (kotlin.math.abs(newCreditorBalance) < 0.01) newCreditorBalance = 0.0

        balances[uid]        = newUserBalance
        balances[creditorId] = newCreditorBalance

        FirebaseFirestore.getInstance()
            .collection("groups")
            .document(groupId)
            .update("balances", balances)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.localizedMessage ?: "Unknown error") }
    }
}