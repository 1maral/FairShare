package hu.ait.maral.fairshare.ui.screen.profile

import android.content.ContentResolver
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.maral.fairshare.data.SupabaseProvider
import hu.ait.maral.fairshare.data.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID

class ProfileViewModel(
    private val supabase: SupabaseClient = SupabaseProvider.supabase
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // State exposed to UI
    val preferredCurrency = mutableStateOf("USD")
    val paymentMethods = mutableStateOf<Map<String, String>>(emptyMap())
    val avatarUrl = mutableStateOf<String?>(null)
    val localAvatarUri = mutableStateOf<Uri?>(null)

    val isLoading = mutableStateOf(true)
    val isSaving = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val user = auth.currentUser
        if (user == null) {
            errorMessage.value = "No logged in user."
            isLoading.value = false
            return
        }
        val uid = user.uid

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val profile = doc.toObject(User::class.java)
                if (profile != null) {
                    preferredCurrency.value = profile.preferredCurrency ?: "USD"
                    paymentMethods.value = profile.paymentMethods ?: emptyMap()
                    avatarUrl.value = profile.profilePictureUrl
                }
                isLoading.value = false
            }
            .addOnFailureListener { e ->
                errorMessage.value = e.localizedMessage
                isLoading.value = false
            }
    }

    fun setPreferredCurrency(value: String) {
        preferredCurrency.value = value.uppercase()
    }

    fun addPaymentMethod(name: String, value: String) {
        if (name.isBlank() || value.isBlank()) return
        val current = paymentMethods.value.toMutableMap()
        current[name] = value
        paymentMethods.value = current
    }

    fun removePaymentMethod(name: String) {
        val current = paymentMethods.value.toMutableMap()
        current.remove(name)
        paymentMethods.value = current
    }

    fun setLocalAvatarUri(uri: Uri?) {
        localAvatarUri.value = uri
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun saveProfile(
        contentResolver: ContentResolver,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "No logged in user.")
            return
        }
        val uid = user.uid

        isSaving.value = true
        errorMessage.value = null

        viewModelScope.launch {
            try {
                // If a new avatar is selected, upload it to Supabase first
                val newAvatarUri = localAvatarUri.value
                val newAvatarUrl: String? = if (newAvatarUri != null) {
                    val bytes = uriToJpegBytes(contentResolver, newAvatarUri)
                    if (bytes != null) uploadProfileImage(uid, bytes) else null
                } else {
                    null
                }

                // Build partial updates
                val updates = mutableMapOf<String, Any>(
                    "preferredCurrency" to preferredCurrency.value,
                    "paymentMethods" to paymentMethods.value
                )
                if (newAvatarUrl != null) {
                    updates["profilePictureUrl"] = newAvatarUrl
                }

                db.collection("users").document(uid)
                    .update(updates)
                    .addOnSuccessListener {
                        if (newAvatarUrl != null) {
                            avatarUrl.value = newAvatarUrl
                            localAvatarUri.value = null
                        }
                        isSaving.value = false
                        onResult(true, "Profile updated.")
                    }
                    .addOnFailureListener { e ->
                        isSaving.value = false
                        errorMessage.value = e.localizedMessage
                        onResult(false, e.localizedMessage)
                    }

            } catch (e: Exception) {
                isSaving.value = false
                errorMessage.value = e.localizedMessage
                onResult(false, e.localizedMessage)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun uriToJpegBytes(
        contentResolver: ContentResolver,
        uri: Uri
    ): ByteArray? {
        return try {
            val source = ImageDecoder.createSource(contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source)
            val baos = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, baos)
            baos.toByteArray()
        } catch (e: Exception) {
            errorMessage.value = "Failed to read image: ${e.message}"
            null
        }
    }

    private suspend fun uploadProfileImage(uid: String, bytes: ByteArray): String? {
        return try {
            val fileName = "profile/$uid-${UUID.randomUUID()}.jpg"
            val bucket = supabase.storage.from("profile_pictures")
            bucket.upload(fileName, bytes)
            bucket.publicUrl(fileName)
        } catch (e: Exception) {
            errorMessage.value = "Image upload failed: ${e.message}"
            null
        }
    }
}
