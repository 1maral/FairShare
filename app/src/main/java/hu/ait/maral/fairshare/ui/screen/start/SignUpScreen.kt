package hu.ait.maral.fairshare.ui.screen.start

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import hu.ait.maral.fairshare.R
import hu.ait.maral.fairshare.ui.theme.BackgroundPink
import hu.ait.maral.fairshare.ui.theme.ButtonGreen
import hu.ait.maral.fairshare.ui.theme.LogoGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.P)
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

    val currencyOptions = listOf(
        stringResource(hu.ait.maral.fairshare.R.string.usd),
        stringResource(hu.ait.maral.fairshare.R.string.eur),
        stringResource(hu.ait.maral.fairshare.R.string.gbp),
        stringResource(hu.ait.maral.fairshare.R.string.huf),
        stringResource(hu.ait.maral.fairshare.R.string.jpy),
        stringResource(hu.ait.maral.fairshare.R.string.cad),
        stringResource(hu.ait.maral.fairshare.R.string.aud),
        stringResource(hu.ait.maral.fairshare.R.string.chf),
        stringResource(hu.ait.maral.fairshare.R.string.inr),
        stringResource(hu.ait.maral.fairshare.R.string.cny),
        stringResource(hu.ait.maral.fairshare.R.string.sek),
        stringResource(hu.ait.maral.fairshare.R.string.nok),
        stringResource(hu.ait.maral.fairshare.R.string.nzd),
        stringResource(hu.ait.maral.fairshare.R.string.mxn),
        stringResource(R.string.brl)
    )
    var isCurrencyExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedCurrency by rememberSaveable { mutableStateOf(currencyOptions.first()) }

    val paymentMethods = remember { mutableStateMapOf<String, String>() }

    val paymentTypeOptions = listOf(
        stringResource(R.string.zelle),
        stringResource(R.string.venmo),
        stringResource(R.string.paypal),
        stringResource(R.string.cash_app),
        stringResource(R.string.bank_transfer))
    var isPaymentTypeExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedPaymentType by rememberSaveable { mutableStateOf(paymentTypeOptions.first()) }
    var newPaymentValue by rememberSaveable { mutableStateOf("") }

    val context = LocalContext.current
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        avatarUri = uri
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val state = viewModel.signUpUiState

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { pickImageLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUri != null) {
                                AsyncImage(
                                    model = avatarUri,
                                    contentDescription = "avatar",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "select avatar",
                                    tint = LogoGreen,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = stringResource(R.string.create_your_fairshare_account),
                            fontSize = 18.sp,
                            color = LogoGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundPink,
                    titleContentColor = LogoGreen
                )
            )
        },
        containerColor = BackgroundPink,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(BackgroundPink)
        ) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUri != null) {
                                AsyncImage(
                                    model = avatarUri,
                                    contentDescription = "avatar preview",
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "avatar placeholder",
                                    tint = LogoGreen,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedButton(onClick = { pickImageLauncher.launch("image/*") }) {
                            Text(stringResource(R.string.choose_avatar))
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.full_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(stringResource(R.string.phone_number)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.email)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Password
                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.confirm_password)) },
                        singleLine = true,
                        visualTransformation = if (showPasswordConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPasswordConfirm = !showPasswordConfirm }) {
                                Icon(
                                    imageVector = if (showPasswordConfirm) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = isCurrencyExpanded,
                        onExpandedChange = { isCurrencyExpanded = !isCurrencyExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCurrency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Preferred Currency") },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCurrencyExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BackgroundPink,
                                unfocusedContainerColor = BackgroundPink,
                                disabledContainerColor = BackgroundPink
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = isCurrencyExpanded,
                            onDismissRequest = { isCurrencyExpanded = false },
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            currencyOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        selectedCurrency = option
                                        isCurrencyExpanded = false
                                    },
                                    colors = MenuDefaults.itemColors(
                                        textColor = LogoGreen,
                                        leadingIconColor = LogoGreen,
                                        trailingIconColor = LogoGreen,
                                        disabledTextColor = LogoGreen.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }
                }

                item {
                    Text(stringResource(R.string.payment_methods), color = LogoGreen)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = isPaymentTypeExpanded,
                            onExpandedChange = { isPaymentTypeExpanded = !isPaymentTypeExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedPaymentType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.type)) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPaymentTypeExpanded)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = BackgroundPink,
                                    unfocusedContainerColor = BackgroundPink,
                                    disabledContainerColor = BackgroundPink
                                )
                            )

                            ExposedDropdownMenu(
                                expanded = isPaymentTypeExpanded,
                                onDismissRequest = { isPaymentTypeExpanded = false },
                                modifier = Modifier.heightIn(max = 200.dp)
                            ) {
                                paymentTypeOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            selectedPaymentType = option
                                            isPaymentTypeExpanded = false
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = LogoGreen,
                                            leadingIconColor = LogoGreen,
                                            trailingIconColor = LogoGreen,
                                            disabledTextColor = LogoGreen.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = newPaymentValue,
                            onValueChange = { newPaymentValue = it },
                            placeholder = { Text(stringResource(R.string.ex_user)) },
                            singleLine = true,
                            modifier = Modifier.weight(1.5f)
                        )

                        OutlinedButton(onClick = {
                            if (newPaymentValue.isNotBlank()) {
                                paymentMethods[selectedPaymentType] = newPaymentValue
                                newPaymentValue = ""
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Enter a handle/id to add a payment method"
                                    )
                                }
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "add")
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(paymentMethods.entries.toList()) { entry ->
                    val key = entry.key
                    val value = entry.value
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(key, color = LogoGreen)
                            Text(value)
                        }
                        IconButton(onClick = { paymentMethods.remove(key) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ButtonGreen
                            )
                        ) {
                            Text(stringResource(R.string.back), color = MaterialTheme.colorScheme.onPrimary)
                        }
                        OutlinedButton(
                            onClick = {
                                when {
                                    name.isBlank() -> {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Name is required")
                                        }
                                        return@OutlinedButton
                                    }
                                    phone.isBlank() -> {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Phone number is required")
                                        }
                                        return@OutlinedButton
                                    }
                                    email.isBlank() -> {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Email is required")
                                        }
                                        return@OutlinedButton
                                    }
                                    password.isBlank() -> {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Password is required")
                                        }
                                        return@OutlinedButton
                                    }
                                    selectedCurrency.isBlank() -> {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Currency is required")
                                        }
                                        return@OutlinedButton
                                    }
                                }

                                if (password != confirmPassword) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Passwords do not match")
                                    }
                                    return@OutlinedButton
                                }

                                viewModel.registerUser(
                                    contentResolver = context.contentResolver,
                                    avatarUri = avatarUri,
                                    name = name,
                                    email = email,
                                    password = password,
                                    phone = phone,
                                    paymentMethods = paymentMethods.toMap(),
                                    preferredCurrency = selectedCurrency
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ButtonGreen
                            )
                        ) {
                            Text(stringResource(R.string.sign_up), color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            when (state) {
                is SignUpUiState.Error -> {
                    LaunchedEffect(state) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                state.errorMessage ?: "Unknown error"
                            )
                        }
                    }
                }
                is SignUpUiState.RegisterSuccess -> {
                    LaunchedEffect(Unit) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Registered successfully!")
                        }
                        onRegisterSuccess()
                    }
                }
                is SignUpUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = LogoGreen)
                    }
                }
                else -> {}
            }
        }
    }
}
