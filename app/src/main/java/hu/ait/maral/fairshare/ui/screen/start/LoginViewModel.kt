package hu.ait.maral.fairshare.ui.screen.start

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface LoginUiState {
    object Init            : LoginUiState
    object Loading         : LoginUiState
    object RegisterSuccess : LoginUiState
    object LoginSuccess    : LoginUiState
    data class Error(val errorMessage: String?) : LoginUiState
}

class LoginViewModel : ViewModel() {

    // Compose state — must only be written on the Main thread.
    // viewModelScope.launch defaults to Main, and we use withContext(IO)
    // for the Firebase call, then write state after returning to Main.
    var loginUiState: LoginUiState by mutableStateOf(LoginUiState.Init)

    private val auth: FirebaseAuth = Firebase.auth

    // ── Register ──────────────────────────────────────────────────────────────
    fun registerUser(email: String, password: String) {
        viewModelScope.launch {                          // runs on Main
            loginUiState = LoginUiState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    auth.createUserWithEmailAndPassword(email, password).await()
                }                                        // back on Main here
                loginUiState = when {
                    result?.user != null -> LoginUiState.RegisterSuccess
                    else -> LoginUiState.Error("Registration failed. Please try again.")
                }
            } catch (e: Exception) {
                loginUiState = LoginUiState.Error(
                    e.localizedMessage ?: "Registration failed."
                )
            }
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    // Regular fun, NOT suspend — the screen calls this directly, no
    // coroutineScope.launch wrapper needed in the UI layer.
    // This eliminates the threading bug where Animatable + .await() were
    // competing inside the same UI coroutine on ambiguous dispatchers.
    fun loginUser(email: String, password: String) {
        viewModelScope.launch {                          // runs on Main
            loginUiState = LoginUiState.Loading
            try {
                val result = withContext(Dispatchers.IO) {
                    auth.signInWithEmailAndPassword(email, password).await()
                }                                        // back on Main here
                loginUiState = when {
                    result?.user != null -> LoginUiState.LoginSuccess
                    else -> LoginUiState.Error("Login failed. No account found for this email.")
                }
            } catch (e: Exception) {
                loginUiState = LoginUiState.Error(
                    e.localizedMessage ?: "Login failed. Please check your credentials."
                )
            }
        }
    }
}