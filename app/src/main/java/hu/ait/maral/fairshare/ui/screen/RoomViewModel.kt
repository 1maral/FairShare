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

    var owedPerPerson = mutableStateOf<Map<String, Double>>(emptyMap())
        private set

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var groupListener: ListenerRegistration? = null

    val currentUserId: String?
        get() = auth.currentUser?.uid


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

        if (userBalance >= 0.0) return emptyMap()

        var remainingDebt = -userBalance
        val result = mutableMapOf<String, Double>()

        val creditors = balances
            .filter { (memberId, balance) ->
                memberId != currentUserId && balance > 0.0
            }
            .toList()

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
