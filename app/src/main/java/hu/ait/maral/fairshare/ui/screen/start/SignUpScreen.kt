package hu.ait.maral.fairshare.ui.screen.start

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import hu.ait.maral.fairshare.R
import hu.ait.maral.fairshare.ui.theme.LogoGreen
import kotlinx.coroutines.launch

// ── Palette ──────────────────────────────────────────────────────────────────
private val DeepRose   = Color(0xFFE76F8E)
private val SoftRose   = Color(0xFFF9E4EC)
private val CardWhite  = Color(0xFFFFFAFC)
private val SubtleGray = Color(0xFF9E8E95)
private val FieldBg    = Color(0xFFFFF8FA)
private val FieldFocus = Color(0xFFF9E4EC)

@Composable
private fun fairshareFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = DeepRose,
    unfocusedBorderColor    = Color(0xFFE8D5DC),
    focusedLabelColor       = DeepRose,
    unfocusedLabelColor     = SubtleGray,
    cursorColor             = DeepRose,
    focusedContainerColor   = FieldFocus,
    unfocusedContainerColor = FieldBg,
    disabledContainerColor  = FieldBg
)

// Step metadata
private data class StepInfo(val index: Int, val title: String, val subtitle: String)
private val STEPS = listOf(
    StepInfo(0, "Profile Photo",   "Optional — add a photo so friends can recognize you"),
    StepInfo(1, "Personal Info",   "Tell us a little about yourself"),
    StepInfo(2, "Security",        "Keep your account safe with a strong password"),
    StepInfo(3, "Preference",     "Choose your preferred currency for splitting bills"),
    StepInfo(4, "Payment Methods", "Add payment method so friends can repay")
)

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
    // ── All original state names preserved ──────────────────────────────────
    var name                by rememberSaveable { mutableStateOf("") }
    var phone               by rememberSaveable { mutableStateOf("") }
    var email               by rememberSaveable { mutableStateOf(defaultEmail) }
    var password            by rememberSaveable { mutableStateOf(defaultPassword) }
    var confirmPassword     by rememberSaveable { mutableStateOf("") }
    var showPassword        by rememberSaveable { mutableStateOf(false) }
    var showPasswordConfirm by rememberSaveable { mutableStateOf(false) }

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
    var isCurrencyExpanded  by rememberSaveable { mutableStateOf(false) }
    var selectedCurrency    by rememberSaveable { mutableStateOf(currencyOptions.first()) }

    val paymentMethods        = remember { mutableStateMapOf<String, String>() }
    val paymentTypeOptions    = listOf(
        stringResource(R.string.zelle),   stringResource(R.string.venmo),
        stringResource(R.string.paypal),  stringResource(R.string.cash_app),
        stringResource(R.string.bank_transfer)
    )
    var isPaymentTypeExpanded by rememberSaveable { mutableStateOf(false) }
    var selectedPaymentType   by rememberSaveable { mutableStateOf(paymentTypeOptions.first()) }
    var newPaymentValue       by rememberSaveable { mutableStateOf("") }

    val context           = LocalContext.current
    var avatarUri         by remember { mutableStateOf<Uri?>(null) }
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> avatarUri = uri }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()
    val state             = viewModel.signUpUiState

    // ── Step state ───────────────────────────────────────────────────────────
    var currentStep    by rememberSaveable { mutableStateOf(0) }
    var goingForward   by remember { mutableStateOf(true) }
    val totalSteps     = STEPS.size

    fun validateStep(): String? = when (currentStep) {
        1 -> when {
            name.isBlank()  -> "Full name is required"
            phone.isBlank() -> "Phone number is required"
            email.isBlank() -> "Email address is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
                -> "Please enter a valid email address"
            else            -> null
        }
        2 -> when {
            password.isBlank()           -> "Please enter a password"
            password.length < 6          -> "Password must be at least 6 characters"
            password != confirmPassword  -> "Passwords do not match"
            else                         -> null
        }
        3 -> when {
            selectedCurrency.isBlank() -> "Please select a currency"
            else                       -> null
        }
        4 -> when {
            newPaymentValue.isNotBlank() -> "Tap + to add the payment method"
            else                         -> null
        }
        else -> null
    }

    fun goNext() {
        val error = validateStep()
        if (error != null) { scope.launch { snackbarHostState.showSnackbar(error) }; return }
        if (currentStep < totalSteps - 1) { goingForward = true; currentStep++ }
    }

    fun goBack() {
        if (currentStep > 0) { goingForward = false; currentStep-- }
        else onNavigateBack()
    }

    // Blob drift
    val blobAnim = rememberInfiniteTransition(label = "blob")
    val blobY by blobAnim.animateFloat(
        initialValue  = 0f, targetValue  = 18f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "blobY"
    )

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = LogoGreen,
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
                        listOf(Color(0xFFFDE8F0), Color(0xFFFFF5F8), Color(0xFFE8F5EE))
                    )
                )
        ) {
            // Background blobs
            Box(
                Modifier.size(280.dp).offset((-70).dp, (-40).dp + blobY.dp)
                    .blur(90.dp).background(Color(0x40F9A8C0), CircleShape)
            )
            Box(
                Modifier.size(220.dp).align(Alignment.BottomEnd).offset(60.dp, 40.dp)
                    .blur(80.dp).background(Color(0x40A8D8B0), CircleShape)
            )

            Column(modifier = Modifier.fillMaxSize()) {

                // ── Top bar ──────────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFDE8F0))
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    IconButton(onClick = { goBack() }, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Default.ArrowBack, null, tint = LogoGreen)
                    }
                    Text(
                        text       = stringResource(R.string.create_your_fairshare_account),
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = LogoGreen,
                        modifier   = Modifier.align(Alignment.Center)
                    )
                }

                // ── Step progress dots ───────────────────────────────────────
                Row(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    STEPS.forEachIndexed { index, step ->
                        val isActive   = index == currentStep
                        val isComplete = index < currentStep
                        val dotColor   = when {
                            isComplete -> LogoGreen
                            isActive   -> DeepRose
                            else       -> Color(0xFFE8D5DC)
                        }
                        val dotWidth by animateDpAsState(
                            targetValue   = if (isActive) 28.dp else 8.dp,
                            animationSpec = spring(dampingRatio = 0.6f),
                            label         = "dot$index"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(dotColor)
                        ) {
                            if (isComplete) {
                                Icon(
                                    Icons.Default.Check, null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(6.dp).align(Alignment.Center)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text       = "${currentStep + 1} / $totalSteps",
                        fontSize   = 12.sp,
                        color      = SubtleGray,
                        fontWeight = FontWeight.Medium
                    )
                }

                // ── Animated step content ────────────────────────────────────
                AnimatedContent(
                    targetState   = currentStep,
                    transitionSpec = {
                        if (goingForward) {
                            (slideInHorizontally { it } + fadeIn(tween(280))).togetherWith(
                                slideOutHorizontally { -it } + fadeOut(tween(200))
                            )
                        } else {
                            (slideInHorizontally { -it } + fadeIn(tween(280))).togetherWith(
                                slideOutHorizontally { it } + fadeOut(tween(200))
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    label    = "stepContent"
                ) { step ->
                    LazyColumn(
                        modifier            = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding      = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Step header
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text       = STEPS[step].title,
                                    fontSize   = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color      = LogoGreen
                                )
                                Text(
                                    text      = STEPS[step].subtitle,
                                    fontSize  = 13.sp,
                                    color     = SubtleGray,
                                    textAlign = TextAlign.Center,
                                    modifier  = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp)
                                )
                            }
                        }

                        // Step body
                        item {
                            Card(
                                modifier  = Modifier.fillMaxWidth(),
                                shape     = RoundedCornerShape(24.dp),
                                colors    = CardDefaults.cardColors(containerColor = CardWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    when (step) {

                                        // ── Step 0: Avatar ───────────────────
                                        0 -> {
                                            Column(
                                                modifier            = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(140.dp)
                                                        .shadow(12.dp, CircleShape, ambientColor = DeepRose)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.verticalGradient(listOf(Color(0xFFFCE4EC), Color.White))
                                                        )
                                                        .border(2.5.dp, DeepRose, CircleShape)
                                                        .clickable { pickImageLauncher.launch("image/*") },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (avatarUri != null) {
                                                        AsyncImage(
                                                            model              = avatarUri,
                                                            contentDescription = "avatar",
                                                            modifier           = Modifier.fillMaxSize().clip(CircleShape),
                                                            contentScale       = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(Icons.Default.Person, null, tint = DeepRose, modifier = Modifier.size(64.dp))
                                                            Text("Tap to add", fontSize = 11.sp, color = DeepRose, fontWeight = FontWeight.Medium)
                                                        }
                                                    }
                                                }
                                                Spacer(Modifier.height(16.dp))
                                                OutlinedButton(
                                                    onClick = { pickImageLauncher.launch("image/*") },
                                                    shape   = RoundedCornerShape(12.dp),
                                                    border  = androidx.compose.foundation.BorderStroke(1.5.dp, DeepRose),
                                                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = DeepRose)
                                                ) {
                                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Choose from gallery", fontWeight = FontWeight.Medium)
                                                }
                                                if (avatarUri != null) {
                                                    Spacer(Modifier.height(8.dp))
                                                    Text("Photo selected", color = LogoGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }

                                        // ── Step 1: Personal info ────────────
                                        1 -> {
                                            OutlinedTextField(
                                                value = name, onValueChange = { name = it },
                                                label = { Text(stringResource(R.string.full_name)) },
                                                leadingIcon = { Icon(Icons.Default.Person, null, tint = DeepRose) },
                                                singleLine = true, shape = RoundedCornerShape(14.dp),
                                                colors = fairshareFieldColors(), modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            OutlinedTextField(
                                                value = phone, onValueChange = { phone = it },
                                                label = { Text(stringResource(R.string.phone_number)) },
                                                leadingIcon = { Icon(Icons.Default.Phone, null, tint = DeepRose) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = fairshareFieldColors(), modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            OutlinedTextField(
                                                value = email, onValueChange = { email = it },
                                                label = { Text(stringResource(R.string.email)) },
                                                leadingIcon = { Icon(Icons.Default.Email, null, tint = DeepRose) },
                                                singleLine = true, shape = RoundedCornerShape(14.dp),
                                                colors = fairshareFieldColors(), modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        // ── Step 2: Security ─────────────────
                                        2 -> {
                                            OutlinedTextField(
                                                value = password, onValueChange = { password = it },
                                                label = { Text(stringResource(R.string.password)) },
                                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = DeepRose) },
                                                singleLine = true,
                                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                                trailingIcon = {
                                                    IconButton(onClick = { showPassword = !showPassword }) {
                                                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = DeepRose)
                                                    }
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = fairshareFieldColors(), modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            OutlinedTextField(
                                                value = confirmPassword, onValueChange = { confirmPassword = it },
                                                label = { Text(stringResource(R.string.confirm_password)) },
                                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = DeepRose) },
                                                singleLine = true,
                                                visualTransformation = if (showPasswordConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                                                trailingIcon = {
                                                    IconButton(onClick = { showPasswordConfirm = !showPasswordConfirm }) {
                                                        Icon(if (showPasswordConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = DeepRose)
                                                    }
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = fairshareFieldColors(), modifier = Modifier.fillMaxWidth()
                                            )
                                            // Password match indicator
                                            if (confirmPassword.isNotEmpty()) {
                                                Spacer(Modifier.height(8.dp))
                                                val match = password == confirmPassword
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        if (match) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                        null,
                                                        tint     = if (match) LogoGreen else Color(0xFFE57373),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        if (match) "Passwords match" else "Passwords don't match",
                                                        fontSize = 12.sp,
                                                        color    = if (match) LogoGreen else Color(0xFFE57373)
                                                    )
                                                }
                                            }
                                        }

                                        // ── Step 3: Currency ─────────────────
                                        3 -> {
                                            ExposedDropdownMenuBox(
                                                expanded         = isCurrencyExpanded,
                                                onExpandedChange = { isCurrencyExpanded = !isCurrencyExpanded },
                                                modifier         = Modifier.fillMaxWidth()
                                            ) {
                                                OutlinedTextField(
                                                    value = selectedCurrency, onValueChange = {},
                                                    readOnly = true, label = { Text("Preferred Currency") },
                                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCurrencyExpanded) },
                                                    shape = RoundedCornerShape(14.dp), colors = fairshareFieldColors()
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = isCurrencyExpanded,
                                                    onDismissRequest = { isCurrencyExpanded = false },
                                                    modifier = Modifier.heightIn(max = 200.dp)
                                                ) {
                                                    currencyOptions.forEach { option ->
                                                        DropdownMenuItem(
                                                            text    = { Text(option, color = LogoGreen) },
                                                            onClick = { selectedCurrency = option; isCurrencyExpanded = false }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // ── Step 4: Payment methods ──────────
                                        4 -> {
                                            // Dropdown: full width on its own row
                                            ExposedDropdownMenuBox(
                                                expanded         = isPaymentTypeExpanded,
                                                onExpandedChange = { isPaymentTypeExpanded = !isPaymentTypeExpanded },
                                                modifier         = Modifier.fillMaxWidth()
                                            ) {
                                                OutlinedTextField(
                                                    value = selectedPaymentType, onValueChange = {},
                                                    readOnly = true, label = { Text(stringResource(R.string.type)) },
                                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPaymentTypeExpanded) },
                                                    shape = RoundedCornerShape(14.dp), colors = fairshareFieldColors()
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = isPaymentTypeExpanded,
                                                    onDismissRequest = { isPaymentTypeExpanded = false },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    paymentTypeOptions.forEach { option ->
                                                        DropdownMenuItem(
                                                            text = { Text(option, color = LogoGreen) },
                                                            onClick = { selectedPaymentType = option; isPaymentTypeExpanded = false }
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.height(10.dp))

                                            // Handle field + add button on one row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = newPaymentValue, onValueChange = { newPaymentValue = it },
                                                    label = { Text("Handle / ID") },
                                                    placeholder = { Text(stringResource(R.string.ex_user), color = Color(0xFFCCBBC2)) },
                                                    singleLine = true, shape = RoundedCornerShape(14.dp),
                                                    colors = fairshareFieldColors(), modifier = Modifier.weight(1f)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(52.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(DeepRose)
                                                        .clickable {
                                                            if (newPaymentValue.isNotBlank()) {
                                                                paymentMethods[selectedPaymentType] = newPaymentValue
                                                                newPaymentValue = ""
                                                            } else {
                                                                scope.launch { snackbarHostState.showSnackbar("Enter a handle/id first") }
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Add, null, tint = Color.White)
                                                }
                                            }

                                            if (paymentMethods.isNotEmpty()) {
                                                Spacer(Modifier.height(12.dp))
                                                HorizontalDivider(color = Color(0xFFEDD8E0))
                                                Spacer(Modifier.height(8.dp))
                                                paymentMethods.entries.toList().forEach { entry ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(SoftRose)
                                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(entry.key,   color = DeepRose,   fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                            Text(entry.value, color = SubtleGray, fontSize = 12.sp)
                                                        }
                                                        IconButton(onClick = { paymentMethods.remove(entry.key) }) {
                                                            Icon(Icons.Default.Delete, null, tint = Color(0xFFE57373))
                                                        }
                                                    }
                                                }
                                            } else {
                                                Spacer(Modifier.height(12.dp))
                                                Text(
                                                    "No payment methods added yet.",
                                                    color     = SubtleGray,
                                                    fontSize  = 13.sp,
                                                    textAlign = TextAlign.Center,
                                                    modifier  = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Bottom navigation bar ────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFDE8F0))
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Back
                    OutlinedButton(
                        onClick  = { goBack() },
                        shape    = RoundedCornerShape(14.dp),
                        border   = androidx.compose.foundation.BorderStroke(1.5.dp, DeepRose),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = DeepRose),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Back", fontWeight = FontWeight.SemiBold)
                    }

                    // Step label
                    Text(
                        text       = "Step " + currentStep,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = SubtleGray
                    )

                    // Next / Finish
                    if (currentStep < totalSteps - 1) {
                        Button(
                            onClick   = { goNext() },
                            shape     = RoundedCornerShape(14.dp),
                            colors    = ButtonDefaults.buttonColors(containerColor = LogoGreen, contentColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(4.dp),
                            modifier  = Modifier.height(50.dp)
                        ) {
                            Text("Next", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                when {
                                    name.isBlank()              -> scope.launch { snackbarHostState.showSnackbar("Name is required") }
                                    phone.isBlank()             -> scope.launch { snackbarHostState.showSnackbar("Phone number is required") }
                                    email.isBlank()             -> scope.launch { snackbarHostState.showSnackbar("Email is required") }
                                    password.isBlank()          -> scope.launch { snackbarHostState.showSnackbar("Password is required") }
                                    selectedCurrency.isBlank()  -> scope.launch { snackbarHostState.showSnackbar("Currency is required") }
                                    password != confirmPassword -> scope.launch { snackbarHostState.showSnackbar("Passwords do not match") }
                                    else -> viewModel.registerUser(
                                        contentResolver   = context.contentResolver,
                                        avatarUri         = avatarUri,
                                        name              = name,
                                        email             = email,
                                        password          = password,
                                        phone             = phone,
                                        paymentMethods    = paymentMethods.toMap(),
                                        preferredCurrency = selectedCurrency
                                    )
                                }
                            },
                            shape     = RoundedCornerShape(14.dp),
                            colors    = ButtonDefaults.buttonColors(containerColor = LogoGreen, contentColor = Color.White),
                            elevation = ButtonDefaults.buttonElevation(4.dp),
                            modifier  = Modifier.height(50.dp)
                        ) {
                            Text("Create", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── State overlays ───────────────────────────────────────────────
            when (state) {
                is SignUpUiState.Error -> {
                    LaunchedEffect(state) {
                        snackbarHostState.showSnackbar(state.errorMessage ?: "Unknown error")
                    }
                }
                is SignUpUiState.RegisterSuccess -> {
                    LaunchedEffect(Unit) {
                        snackbarHostState.showSnackbar("Registered successfully!")
                        onRegisterSuccess()
                    }
                }
                is SignUpUiState.Loading -> {
                    Box(
                        modifier         = Modifier.fillMaxSize().background(Color(0x66FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = LogoGreen, strokeWidth = 3.dp)
                    }
                }
                else -> {}
            }
        }
    }
}
