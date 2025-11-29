package hu.ait.maral.fairshare.ui.screen.start

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.ait.maral.fairshare.data.User
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    viewModel: SignUpViewModel = viewModel(),
    defaultEmail: String = "",
    defaultPassword: String = "",
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(defaultEmail) }
    var password by rememberSaveable { mutableStateOf(defaultPassword) }
    var confirmPassword by rememberSaveable { mutableStateOf(defaultPassword) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showPasswordConfirm by rememberSaveable { mutableStateOf(false) }

    // For simplicity, let's just allow entering one payment method here
    var venmo by rememberSaveable { mutableStateOf("") }

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

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
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
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
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
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = venmo,
                onValueChange = { venmo = it },
                label = { Text("Venmo Username (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = { onNavigateBack() }) {
                    Text("Back")
                }

                OutlinedButton(onClick = {
                    if (password != confirmPassword) return@OutlinedButton

                    coroutineScope.launch {
                        val paymentMethods = mutableMapOf<String, String>()
                        if (venmo.isNotBlank()) paymentMethods["Venmo"] = venmo

                        viewModel.registerUser(
                            name = name,
                            email = email,
                            password = password,
                            phone = if (phone.isNotBlank()) phone else null,
                            paymentMethods = paymentMethods
                        )
                    }
                }) {
                    Text("Sign Up")
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (viewModel.signUpUiState) {
                is SignUpUiState.Error -> Text(
                    text = "Error: ${(viewModel.signUpUiState as SignUpUiState.Error).errorMessage}"
                )
                is SignUpUiState.Loading -> CircularProgressIndicator()
                is SignUpUiState.RegisterSuccess -> {
                    Text("Account created successfully!")
                    onRegisterSuccess()
                }
                else -> {}
            }
        }
    }
}
