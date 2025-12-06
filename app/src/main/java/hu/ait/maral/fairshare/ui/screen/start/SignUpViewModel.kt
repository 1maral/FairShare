package hu.ait.maral.fairshare.ui.screen.start

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.maral.fairshare.data.User
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import hu.ait.maral.fairshare.data.SupabaseProvider
import io.github.jan.supabase.storage.storage

sealed interface SignUpUiState {
    object Init : SignUpUiState
    object Loading : SignUpUiState
    object RegisterSuccess : SignUpUiState
    data class Error(val errorMessage: String?) : SignUpUiState
}

class SignUpViewModel(
    val supabase: SupabaseClient = SupabaseProvider.supabase
) : ViewModel() {

    var signUpUiState: SignUpUiState by mutableStateOf(SignUpUiState.Init)

    private val auth: FirebaseAuth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    /**
     * Registers user, uploads avatar if available, saves to Firestore
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun registerUser(
        contentResolver: ContentResolver,
        avatarUri: Uri?,
        name: String,
        email: String,
        password: String,
        phone: String,
        paymentMethods: Map<String, String>,
        preferredCurrency: String
    ) {
        signUpUiState = SignUpUiState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser == null) {
                    signUpUiState = SignUpUiState.Error("User is null after registration")
                    return@addOnSuccessListener
                }

                viewModelScope.launch {
                    val imageBytes: ByteArray? = avatarUri?.let { uri ->
                        val source = ImageDecoder.createSource(contentResolver, uri)
                        val bitmap = ImageDecoder.decodeBitmap(source)
                        val baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                        baos.toByteArray()
                    }

                    val imageUrl = imageBytes?.let { uploadProfileImage(firebaseUser.uid, it) }

                    saveUserToFirestore(
                        uid = firebaseUser.uid,
                        name = name,
                        email = email,
                        phone = phone,
                        paymentMethods = paymentMethods,
                        profileImageUrl = imageUrl,
                        preferredCurrency = preferredCurrency
                    )
                }
            }
            .addOnFailureListener {
                signUpUiState = SignUpUiState.Error(it.localizedMessage)
            }
    }


    /**
     * Uploads profile image to Supabase
     */
    private suspend fun uploadProfileImage(uid: String, bytes: ByteArray): String? {
        return try {

            // Safe filename without folders
            val fileName = "$uid-${UUID.randomUUID()}.jpg"

            val bucket = supabase.storage.from("profile_pictures")
            bucket.upload(
                path = fileName,
                data = bytes
            )

            // Get public URL
            bucket.publicUrl(fileName)

        } catch (e: Exception) {
            signUpUiState = SignUpUiState.Error("Image upload failed: ${e.message}")
            null
        }
    }


    /**
     * Saves user to Firestore
     */
    private fun saveUserToFirestore(
        uid: String,
        name: String,
        email: String,
        phone: String,
        paymentMethods: Map<String, String>,
        profileImageUrl: String?,
        preferredCurrency: String
    ) {
        val newUser = User(
            uid = uid,
            name = name,
            email = email,
            phoneNumber = phone,
            paymentMethods = paymentMethods,
            preferredCurrency = preferredCurrency,
            profilePictureUrl = profileImageUrl,
            createdAt = System.currentTimeMillis(),
            lastLogin = System.currentTimeMillis()
        )

        db.collection("users")
            .document(uid)
            .set(newUser)
            .addOnSuccessListener {
                signUpUiState = SignUpUiState.RegisterSuccess
            }
            .addOnFailureListener {
                signUpUiState = SignUpUiState.Error(it.localizedMessage)
            }
    }
}
