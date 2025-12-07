package hu.ait.maral.fairshare.ui.screen.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.ait.maral.fairshare.R
import hu.ait.maral.fairshare.ui.theme.BackgroundPink
import hu.ait.maral.fairshare.ui.theme.ButtonGreen
import hu.ait.maral.fairshare.ui.theme.LogoGreen
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: (prefillEmail: String, prefillPassword: String) -> Unit
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPink)
    ) {

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FairShare",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = LogoGreen
            )

            Spacer(modifier = Modifier.height(45.dp))  // space between text and logo

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "FairShare Logo",
                modifier = Modifier.size(150.dp)
            )
        }

        // Main Login Fields
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(0.8f),
                label = { Text("E-mail") },
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("email@fairshare.com") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Email, null) }
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(0.8f),
                label = { Text("Password") },
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("") },
                singleLine = true,
                visualTransformation =
                    if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Info, null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                // LOGIN BUTTON
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val result = viewModel.loginUser(email, password)
                            if (result?.user != null) {
                                snackbarHostState.showSnackbar(
                                    message = "Login successful!",
                                    withDismissAction = true
                                )
                                onLoginSuccess()
                            }
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ButtonGreen
                    )
                ) {
                    Text("Login", color = Color.White)
                }

                OutlinedButton(
                    onClick = { onNavigateToRegister(email, password) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ButtonGreen
                    )
                ) {
                    Text("Register", color = Color.White)
                }
            }
        }

        when (val state = viewModel.loginUiState) {

            is LoginUiState.Error -> {
                LaunchedEffect(state) {
                    snackbarHostState.showSnackbar(
                        message = state.errorMessage ?: "Unknown error",
                        withDismissAction = true
                    )
                }
            }

            is LoginUiState.RegisterSuccess -> {
                LaunchedEffect(Unit) {
                    snackbarHostState.showSnackbar(
                        message = "Registered successfully!",
                        withDismissAction = true
                    )
                }
            }

            is LoginUiState.LoginSuccess -> {
                LaunchedEffect(Unit) {
                    snackbarHostState.showSnackbar(
                        message = "Logged in successfully!",
                        withDismissAction = true
                    )
                }
            }

            is LoginUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x55FFFFFF))
                        .align(Alignment.Center)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = LogoGreen
                    )
                }
            }
            else -> {}
        }
    }
}
