package hu.ait.maral.fairshare.ui.screen.start

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import hu.ait.maral.fairshare.data.User

sealed interface SignUpUiState {
    object Init : SignUpUiState
    object Loading : SignUpUiState
    object RegisterSuccess : SignUpUiState
    data class Error(val errorMessage: String?) : SignUpUiState
}

class SignUpViewModel : ViewModel() {

    var signUpUiState: SignUpUiState by mutableStateOf(SignUpUiState.Init)

    private val auth: FirebaseAuth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()

    fun registerUser(
        name: String,
        email: String,
        password: String,
        phone: String?,
        paymentMethods: Map<String, String>
    ) {
        signUpUiState = SignUpUiState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val firebaseUser = authResult.user
                if (firebaseUser != null) {

                    val newUser = User(
                        uid = firebaseUser.uid,
                        name = name,
                        email = email,
                        phoneNumber = phone,
                        paymentMethods = paymentMethods,
                        createdAt = System.currentTimeMillis(),
                        lastLogin = System.currentTimeMillis()
                    )

                    // Save user profile to Firestore
                    db.collection("users")
                        .document(firebaseUser.uid)
                        .set(newUser)
                        .addOnSuccessListener {
                            signUpUiState = SignUpUiState.RegisterSuccess
                        }
                        .addOnFailureListener {
                            signUpUiState = SignUpUiState.Error(it.localizedMessage)
                        }
                } else {
                    signUpUiState = SignUpUiState.Error("User is null after registration")
                }
            }
            .addOnFailureListener {
                signUpUiState = SignUpUiState.Error(it.localizedMessage)
            }
    }
}
