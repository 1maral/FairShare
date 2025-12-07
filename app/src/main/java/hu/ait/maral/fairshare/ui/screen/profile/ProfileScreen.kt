package hu.ait.maral.fairshare.ui.screen.profile

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import hu.ait.maral.fairshare.ui.theme.BackgroundPink
import hu.ait.maral.fairshare.ui.theme.ButtonGreen
import hu.ait.maral.fairshare.ui.theme.LogoGreen
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),  onSaveClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val preferredCurrency by viewModel.preferredCurrency
    val paymentMethods by viewModel.paymentMethods
    val avatarUrl by viewModel.avatarUrl
    val localAvatarUri by viewModel.localAvatarUri
    val isLoading by viewModel.isLoading
    val isSaving by viewModel.isSaving
    val errorMessage by viewModel.errorMessage

    var newPaymentName by rememberSaveable { mutableStateOf("") }
    var newPaymentValue by rememberSaveable { mutableStateOf("") }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setLocalAvatarUri(uri)
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            scope.launch {
                snackbarHostState.showSnackbar(errorMessage ?: "Unknown error")
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Your Profile",
                        fontSize = 20.sp,
                        color = LogoGreen
                    )
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
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LogoGreen)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // Avatar
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clickable { pickImageLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    localAvatarUri != null -> {
                                        AsyncImage(
                                            model = localAvatarUri,
                                            contentDescription = "new avatar",
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    avatarUrl != null -> {
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = "current avatar",
                                            modifier = Modifier
                                                .size(120.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    else -> {
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
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            OutlinedButton(onClick = { pickImageLauncher.launch("image/*") }) {
                                Text("Change Avatar")
                            }
                        }
                    }

                    // Preferred currency (styled dropdown)
                    item {
                        val currencyOptions = listOf(
                            "USD", "EUR", "GBP", "HUF", "JPY",
                            "CAD", "AUD", "CHF", "INR", "CNY",
                            "SEK", "NOK", "NZD", "MXN", "BRL"
                        )

                        var currencyMenuExpanded by rememberSaveable { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = currencyMenuExpanded,
                            onExpandedChange = { currencyMenuExpanded = !currencyMenuExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = preferredCurrency,
                                onValueChange = { /* readOnly */ },
                                readOnly = true,
                                label = { Text("Base Currency", color = LogoGreen) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = BackgroundPink,
                                    unfocusedContainerColor = BackgroundPink,
                                    disabledContainerColor = BackgroundPink
                                ),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                singleLine = true
                            )

                            ExposedDropdownMenu(
                                expanded = currencyMenuExpanded,
                                onDismissRequest = { currencyMenuExpanded = false },
                                modifier = Modifier.heightIn(max = 240.dp),
                                containerColor = BackgroundPink
                            ) {
                                currencyOptions.forEach { code ->
                                    DropdownMenuItem(
                                        text = { Text(code, color = LogoGreen) },
                                        onClick = {
                                            viewModel.setPreferredCurrency(code)
                                            currencyMenuExpanded = false
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

                    // Payment methods header + add row
                    item {
                        Text("Payment methods (type + id)", color = LogoGreen)
                        Spacer(modifier = Modifier.height(8.dp))

                        val paymentTypes = listOf(
                            "Zelle", "Venmo", "PayPal", "Cash App", "Bank Transfer"
                        )
                        var typeMenuExpanded by rememberSaveable { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = typeMenuExpanded,
                                onExpandedChange = { typeMenuExpanded = !typeMenuExpanded },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = newPaymentName,
                                    onValueChange = { /* readOnly – no manual typing */ },
                                    readOnly = true,
                                    label = { Text("Method") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = BackgroundPink,
                                        unfocusedContainerColor = BackgroundPink,
                                        disabledContainerColor = BackgroundPink
                                    ),
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    singleLine = true
                                )

                                ExposedDropdownMenu(
                                    expanded = typeMenuExpanded,
                                    onDismissRequest = { typeMenuExpanded = false },
                                    containerColor = BackgroundPink
                                ) {
                                    paymentTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type, color = LogoGreen) },
                                            onClick = {
                                                newPaymentName = type
                                                typeMenuExpanded = false
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
                                placeholder = { Text("username / email / id") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedButton(onClick = {
                                if (newPaymentName.isNotBlank() && newPaymentValue.isNotBlank()) {
                                    viewModel.addPaymentMethod(newPaymentName, newPaymentValue)
                                    newPaymentName = ""
                                    newPaymentValue = ""
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Select a type and enter an ID")
                                    }
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "add")
                            }
                        }
                    }

                    // Existing payment methods
                    item { Spacer(modifier = Modifier.height(4.dp)) }

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
                            IconButton(onClick = { viewModel.removePaymentMethod(key) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Save button
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.saveProfile(
                                    contentResolver = context.contentResolver
                                ) { success, message ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message ?: if (success) "Saved" else "Error saving profile"
                                        )
                                    }
                                    if (success) {
                                        onSaveClick()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Changes", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }

                if (isSaving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = LogoGreen)
                    }
                }
            }
        }
    }
}
