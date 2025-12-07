package hu.ait.maral.fairshare.ui.screen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()


    fun loadGroup(groupId: String) {
        if (groupId.isBlank()) {
            errorMessage.value = "Invalid group id."
            return
        }

        isLoading.value = true
        errorMessage.value = null

        db.collection("groups")
            .document(groupId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val loadedGroup = doc.toObject(Group::class.java)?.copy(groupId = doc.id)
                    group.value = loadedGroup
                } else {
                    errorMessage.value = "Group not found."
                }
                isLoading.value = false
            }
            .addOnFailureListener { e ->
                errorMessage.value = e.localizedMessage
                isLoading.value = false
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


}