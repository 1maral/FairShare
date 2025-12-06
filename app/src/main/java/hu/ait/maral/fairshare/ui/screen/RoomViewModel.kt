package hu.ait.maral.fairshare.ui.screen

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.maral.fairshare.data.Group

class RoomViewModel : ViewModel() {

    var group = mutableStateOf<Group?>(null)
        private set

    var isLoading = mutableStateOf(false)
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    private val db = FirebaseFirestore.getInstance()

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
}