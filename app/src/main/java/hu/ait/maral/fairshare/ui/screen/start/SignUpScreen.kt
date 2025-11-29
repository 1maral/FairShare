package hu.ait.maral.fairshare.ui.screen.start

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    viewModel: LoginViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showPasswordConfirm by rememberSaveable { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {

        Text(
            text = "Create Your FairShare Account",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp),
            fontSize = 28.sp
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Name field
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                singleLine = true
            )

            // Phone field
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                singleLine = true
            )

            // Email field
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                singleLine = true
            )

            // Password field
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Info, null) },
                singleLine = true,
                visualTransformation =
                    if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Clear else Icons.Default.Add,
                            contentDescription = null
                        )
                    }
                }
            )

            // Confirm Password
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation =
                    if (showPasswordConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPasswordConfirm = !showPasswordConfirm }) {
                        Icon(
                            imageVector = if (showPasswordConfirm) Icons.Default.Clear else Icons.Default.Add,
                            contentDescription = null
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = { onNavigateBack() }) {
                    Text("Back")
                }
                OutlinedButton(onClick = {
                    if (password != confirmPassword) {
                        return@OutlinedButton
                    }
                    coroutineScope.launch {
                        val result = viewModel.registerUser(email, password)
                        if (result?.user != null) {
                            onRegisterSuccess()
                        }
                    }
                }) {
                    Text("Sign Up")
                }
            }
        }

        // Bottom status messages
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (viewModel.loginUiState) {
                is LoginUiState.Error -> Text(
                    text = "Error: ${(viewModel.loginUiState as LoginUiState.Error).errorMessage}"
                )
                is LoginUiState.Loading -> CircularProgressIndicator()
                is LoginUiState.RegisterSuccess -> Text("Account created successfully!")
                else -> {}
            }
        }
    }
}
