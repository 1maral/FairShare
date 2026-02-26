package hu.ait.maral.fairshare.ui.screen.profile

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import hu.ait.maral.fairshare.R
import hu.ait.maral.fairshare.ui.theme.LogoGreen
import kotlinx.coroutines.launch

// ── Strawberry-Matcha Palette ─────────────────────────────────────────────────
private val Rose300    = Color(0xFFF48FB1)
private val Rose500    = Color(0xFFE76F8E)
private val Mint300    = Color(0xFFA8D8B0)
private val OffWhite   = Color(0xFFFFFAFC)
private val Stone      = Color(0xFF9E8E95)
private val FieldBg    = Color(0xFFFFF8FA)
private val FieldFocus = Color(0xFFF9E4EC)

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = Rose500,
    unfocusedBorderColor    = Rose300.copy(alpha = 0.5f),
    focusedLabelColor       = Rose500,
    unfocusedLabelColor     = Stone,
    cursorColor             = Rose500,
    focusedContainerColor   = FieldFocus,
    unfocusedContainerColor = FieldBg,
    disabledContainerColor  = FieldBg
)

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = OffWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Brush.verticalGradient(listOf(Rose500, Mint300)))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text          = title,
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = Rose500,
                    letterSpacing = 0.4.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val context           = LocalContext.current
    val scope             = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val preferredCurrency by viewModel.preferredCurrency
    val paymentMethods    by viewModel.paymentMethods
    val avatarUrl         by viewModel.avatarUrl
    val localAvatarUri    by viewModel.localAvatarUri
    val isLoading         by viewModel.isLoading
    val isSaving          by viewModel.isSaving
    val errorMessage      by viewModel.errorMessage

    var newPaymentName  by rememberSaveable { mutableStateOf("") }
    var newPaymentValue by rememberSaveable { mutableStateOf("") }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.setLocalAvatarUri(uri) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage ?: "Unknown error")
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = Mint300,
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFAECF2),   // matches HomeScreen exactly
                            Color(0xFFFFF8FB),
                            Color(0xFFEDF7F0)
                        )
                    )
                )
        ) {
            // ── Soft accent orbs (same as HomeScreen & LoginScreen) ───────────
            Box(
                Modifier
                    .size(300.dp)
                    .offset((-120).dp, (-120).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Color(0x18E76F8E), Color.Transparent))
                    )
            )
            Box(
                Modifier
                    .size(220.dp)
                    .align(Alignment.BottomEnd)
                    .offset(80.dp, 80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Color(0x14A8D8B0), Color.Transparent))
                    )
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.align(Alignment.Center),
                    color       = LogoGreen,
                    strokeWidth = 3.dp
                )
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding      = PaddingValues(
                        top    = 88.dp,   // clears the floating top bar
                        start  = 24.dp,
                        end    = 24.dp,
                        bottom = 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ── Avatar ───────────────────────────────────────────────
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.fillMaxWidth()
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .shadow(12.dp, CircleShape, ambientColor = Rose300)
                                        .clip(CircleShape)
                                        .border(
                                            width = 2.5.dp,
                                            brush = Brush.sweepGradient(
                                                listOf(Rose300, LogoGreen, Rose500, Rose300)
                                            ),
                                            shape = CircleShape
                                        )
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFFFCE4EC), OffWhite)
                                            )
                                        )
                                        .clickable { pickImageLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        localAvatarUri != null -> AsyncImage(
                                            model              = localAvatarUri,
                                            contentDescription = "new avatar",
                                            modifier           = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale       = ContentScale.Crop
                                        )
                                        avatarUrl != null -> AsyncImage(
                                            model              = avatarUrl,
                                            contentDescription = "current avatar",
                                            modifier           = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale       = ContentScale.Crop
                                        )
                                        else -> Icon(
                                            Icons.Default.Person, null,
                                            tint     = Rose500,
                                            modifier = Modifier.size(54.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(LogoGreen)
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt, null,
                                        tint     = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text     = "Tap to change photo",
                                fontSize = 11.sp,
                                color    = Stone
                            )
                        }
                    }

                    // ── Currency ─────────────────────────────────────────────
                    item {
                        SectionCard("Currency Preference") {
                            val currencyOptions = listOf(
                                stringResource(R.string.usd), stringResource(R.string.eur),
                                stringResource(R.string.gbp), stringResource(R.string.huf),
                                stringResource(R.string.jpy), stringResource(R.string.cad),
                                stringResource(R.string.aud), stringResource(R.string.chf),
                                stringResource(R.string.inr), stringResource(R.string.cny),
                                stringResource(R.string.sek), stringResource(R.string.nok),
                                stringResource(R.string.nzd), stringResource(R.string.mxn),
                                stringResource(R.string.brl)
                            )
                            var currencyMenuExpanded by rememberSaveable { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded         = currencyMenuExpanded,
                                onExpandedChange = { currencyMenuExpanded = !currencyMenuExpanded },
                                modifier         = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value         = preferredCurrency,
                                    onValueChange = {},
                                    readOnly      = true,
                                    label         = { Text(stringResource(R.string.base_currency)) },
                                    trailingIcon  = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyMenuExpanded)
                                    },
                                    colors     = fieldColors(),
                                    shape      = RoundedCornerShape(14.dp),
                                    modifier   = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded         = currencyMenuExpanded,
                                    onDismissRequest = { currencyMenuExpanded = false },
                                    modifier         = Modifier.heightIn(max = 240.dp)
                                ) {
                                    currencyOptions.forEach { cur ->
                                        DropdownMenuItem(
                                            text    = { Text(cur, color = LogoGreen) },
                                            onClick = {
                                                viewModel.setPreferredCurrency(cur)
                                                currencyMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Payment methods ──────────────────────────────────────
                    item {
                        SectionCard("Payment Methods") {
                            val paymentTypes = listOf(
                                stringResource(R.string.zelle),        stringResource(R.string.venmo),
                                stringResource(R.string.paypal),       stringResource(R.string.cash_app),
                                stringResource(R.string.bank_transfer)
                            )
                            var typeMenuExpanded by rememberSaveable { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded         = typeMenuExpanded,
                                onExpandedChange = { typeMenuExpanded = !typeMenuExpanded },
                                modifier         = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value         = newPaymentName,
                                    onValueChange = {},
                                    readOnly      = true,
                                    label         = { Text(stringResource(R.string.method)) },
                                    trailingIcon  = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded)
                                    },
                                    colors     = fieldColors(),
                                    shape      = RoundedCornerShape(14.dp),
                                    modifier   = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded         = typeMenuExpanded,
                                    onDismissRequest = { typeMenuExpanded = false },
                                    modifier         = Modifier.fillMaxWidth()
                                ) {
                                    paymentTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text    = { Text(type, color = LogoGreen) },
                                            onClick = {
                                                newPaymentName = type
                                                typeMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value         = newPaymentValue,
                                    onValueChange = { newPaymentValue = it },
                                    label         = { Text("Handle / ID") },
                                    placeholder   = {
                                        Text(
                                            stringResource(R.string.ex_user),
                                            color = Rose300.copy(alpha = 0.5f)
                                        )
                                    },
                                    singleLine = true,
                                    shape      = RoundedCornerShape(14.dp),
                                    colors     = fieldColors(),
                                    modifier   = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(LogoGreen)
                                        .clickable {
                                            if (newPaymentName.isNotBlank() && newPaymentValue.isNotBlank()) {
                                                viewModel.addPaymentMethod(newPaymentName, newPaymentValue)
                                                newPaymentName  = ""
                                                newPaymentValue = ""
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Select a type and enter an ID"
                                                    )
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Color.White)
                                }
                            }
                        }
                    }

                    // ── Existing payment method rows ─────────────────────────
                    items(paymentMethods.entries.toList()) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(FieldFocus, Color(0xFFEAF6EE))
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.key,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 13.sp,
                                    color      = Rose500
                                )
                                Text(
                                    entry.value,
                                    fontSize = 12.sp,
                                    color    = Stone
                                )
                            }
                            IconButton(onClick = { viewModel.removePaymentMethod(entry.key) }) {
                                Icon(Icons.Default.Delete, null, tint = Rose300)
                            }
                        }
                    }

                    // ── Save button ──────────────────────────────────────────
                    item {
                        Spacer(Modifier.height(4.dp))
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
                                    if (success) onSaveClick()
                                }
                            },
                            modifier  = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape     = RoundedCornerShape(16.dp),
                            colors    = ButtonDefaults.buttonColors(
                                containerColor = LogoGreen,
                                contentColor   = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                stringResource(R.string.save_changes),
                                fontWeight    = FontWeight.SemiBold,
                                fontSize      = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }

                // ── Saving overlay ───────────────────────────────────────────
                if (isSaving) {
                    Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .background(Color(0x66FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = LogoGreen, strokeWidth = 3.dp)
                    }
                }

                // ── Floating top bar — identical structure to HomeScreen ──────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .shadow(
                            elevation    = 8.dp,
                            shape        = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                            ambientColor = Rose300.copy(alpha = 0.3f),
                            spotColor    = Rose500.copy(alpha = 0.15f)
                        )
                        .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        .background(
                            Brush.horizontalGradient(listOf(LogoGreen, Color(0xFF5DB88A)))
                        )
                ) {
                    Column {
                        Spacer(Modifier.fillMaxWidth().statusBarsPadding())
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Back button
                            IconButton(
                                onClick  = onBackClick,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint     = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            // Title
                            Text(
                                text          = stringResource(R.string.your_profile),
                                fontSize      = 22.sp,
                                fontWeight    = FontWeight.Black,
                                color         = Color.White,
                                letterSpacing = (-0.5).sp,
                                modifier      = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}