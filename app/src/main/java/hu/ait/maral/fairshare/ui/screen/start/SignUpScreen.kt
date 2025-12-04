package hu.ait.maral.fairshare.ui.screen.start

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.ait.maral.fairshare.ui.theme.BackgroundPink
import hu.ait.maral.fairshare.ui.theme.ButtonGreen
import hu.ait.maral.fairshare.ui.theme.LogoGreen
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
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var showPasswordConfirm by rememberSaveable { mutableStateOf(false) }
    var venmo by rememberSaveable { mutableStateOf("") }

    // Validation states
    var nameError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var confirmPasswordError by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPink)
    ) {

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )

        Text(
            text = "Create Your FairShare Account",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp),
            fontSize = 28.sp,
            fontFamily = FontFamily.Cursive,
            color = LogoGreen
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // NAME
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = it.isBlank()
                },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = nameError
            )

            Spacer(Modifier.height(5.dp))

            // PHONE
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(5.dp))

            // EMAIL
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = !it.contains("@")
                },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = emailError
            )

            Spacer(Modifier.height(5.dp))

            // PASSWORD
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = it.length < 6
                },
                label = { Text("Password (min 6 chars)") },
                singleLine = true,
                visualTransformation =
                    if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Clear else Icons.Default.Add,
                            null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isError = passwordError
            )

            Spacer(Modifier.height(5.dp))

            // CONFIRM PASSWORD
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    confirmPasswordError = it != password
                },
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation =
                    if (showPasswordConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPasswordConfirm = !showPasswordConfirm }) {
                        Icon(
                            imageVector = if (showPasswordConfirm) Icons.Default.Clear else Icons.Default.Add,
                            null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isError = confirmPasswordError
            )

            Spacer(Modifier.height(5.dp))

            // VENMO
            OutlinedTextField(
                value = venmo,
                onValueChange = { venmo = it },
                placeholder = { Text("fairshare123") },
                label = { Text("Venmo Username (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                OutlinedButton(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ButtonGreen
                    )
                ) { Text("Back", color = Color.White) }

                OutlinedButton(
                    onClick = {
                        // Validate on click
                        nameError = name.isBlank()
                        emailError = !email.contains("@")
                        passwordError = password.length < 6
                        confirmPasswordError = confirmPassword != password

                        if (nameError || emailError || passwordError || confirmPasswordError) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Please fix the highlighted errors.",
                                    withDismissAction = true
                                )
                            }
                            return@OutlinedButton
                        }

                        scope.launch {
                            val paymentMethods = mutableMapOf<String, String>()
                            if (venmo.isNotBlank()) paymentMethods["Venmo"] = venmo

                            viewModel.registerUser(
                                name = name,
                                email = email,
                                password = password,
                                phone = phone.ifBlank { null },
                                paymentMethods = paymentMethods
                            )
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ButtonGreen
                    )
                ) { Text("Sign Up", color = Color.White) }
            }
        }

        // UI STATE HANDLING
        when (val state = viewModel.signUpUiState) {

            is SignUpUiState.Error -> {
                LaunchedEffect(state) {
                    snackbarHostState.showSnackbar(
                        state.errorMessage ?: "Unknown error.",
                        withDismissAction = true
                    )
                }
            }

            is SignUpUiState.RegisterSuccess -> {
                LaunchedEffect(Unit) {
                    snackbarHostState.showSnackbar(
                        "Account created successfully!",
                        withDismissAction = true
                    )
                }
                onRegisterSuccess()
            }

            is SignUpUiState.Loading -> {
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
