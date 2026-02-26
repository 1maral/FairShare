package hu.ait.maral.fairshare.ui.screen.start

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import hu.ait.maral.fairshare.R
import hu.ait.maral.fairshare.ui.theme.LogoGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Palette ──────────────────────────────────────────────────────────────────
private val Rose300    = Color(0xFFF48FB1)
private val Rose500    = Color(0xFFE76F8E)
private val Mint300    = Color(0xFFA8D8B0)
private val OffWhite   = Color(0xFFFFFAFC)
private val Stone      = Color(0xFF9E8E95)
private val FieldBg    = Color(0xFFFFF8FA)
private val FieldFocus = Color(0xFFF9E4EC)

// ── Diamond shape ─────────────────────────────────────────────────────────────
private val DiamondShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width / 2f, size.height)
    lineTo(0f, size.height / 2f)
    close()
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: (prefillEmail: String, prefillPassword: String) -> Unit
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var email        by rememberSaveable { mutableStateOf("") }
    var password     by rememberSaveable { mutableStateOf("") }

    val coroutineScope    = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSuccessOverlay by remember { mutableStateOf(false) }

    // ── Breathing title scale ─────────────────────────────────────────────────
    val breathe = rememberInfiniteTransition(label = "breathe")
    val titleScale by breathe.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = EaseInOutCubic),
            RepeatMode.Reverse
        ),
        label = "titleScale"
    )

    // ── Divider diamond slow spin (kept, just for the small center diamond) ───
    val spin = rememberInfiniteTransition(label = "spin")
    val ringRotation by spin.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label         = "ring"
    )

    // ── Card entrance — simpler: just a quick fade, no slide ─────────────────
    val cardAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        cardAlpha.animateTo(1f, tween(400, easing = EaseOutCubic))
    }

    // ── Button press scale ────────────────────────────────────────────────────
    val loginBtnScale    = remember { Animatable(1f) }
    val registerBtnScale = remember { Animatable(1f) }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier  = Modifier.padding(bottom = 24.dp)
            ) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = LogoGreen,
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { _ ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFAECF2), Color(0xFFFFF8FB), Color(0xFFEDF7F0))
                    )
                )
        ) {

            // ── Static soft accent orbs ───────────────────────────────────────
            Box(
                Modifier
                    .size(320.dp)
                    .offset((-130).dp, (-130).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Color(0x20E76F8E), Color.Transparent))
                    )
            )
            Box(
                Modifier
                    .size(260.dp)
                    .align(Alignment.BottomEnd)
                    .offset(100.dp, 100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(Color(0x1AA8D8B0), Color.Transparent))
                    )
            )

            // ── Two static decorative diamonds (no spin) ──────────────────────
            Box(
                Modifier
                    .size(90.dp)
                    .align(Alignment.TopEnd)
                    .offset((-28).dp, 36.dp)
                    .clip(DiamondShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0x18E76F8E), Color(0x0FA8D8B0)))
                    )
            )
            Box(
                Modifier
                    .size(30.dp)
                    .align(Alignment.TopStart)
                    .offset(32.dp, 60.dp)
                    .clip(DiamondShape)
                    .background(Color(0x25E76F8E))
            )

            // ── Main centered content ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Spacer(Modifier.height(48.dp))

                // Breathing title
                Text(
                    text          = "FairShare",
                    fontSize      = 42.sp,
                    fontWeight    = FontWeight.Black,
                    color         = LogoGreen,
                    letterSpacing = (-1).sp,
                    modifier      = Modifier.scale(titleScale)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text          = "Share bills fairly. Keep friendships.",
                    fontSize      = 13.sp,
                    color         = Stone,
                    letterSpacing = 0.6.sp,
                    textAlign     = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                // Decorative divider — only the small center diamond still spins
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth(0.7f)
                ) {
                    HorizontalDivider(Modifier.weight(1f), color = Rose300.copy(alpha = 0.4f))
                    Box(
                        Modifier
                            .padding(horizontal = 10.dp)
                            .size(10.dp)
                            .rotate(ringRotation * 0.4f)
                            .background(Rose500.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                    )
                    HorizontalDivider(Modifier.weight(1f), color = Rose300.copy(alpha = 0.4f))
                }

                Spacer(Modifier.height(10.dp))

                // ── Login card ────────────────────────────────────────────────
                Card(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = cardAlpha.value },
                    shape     = RoundedCornerShape(28.dp),
                    colors    = CardDefaults.cardColors(containerColor = OffWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                Brush.horizontalGradient(listOf(Rose300, Rose500, Mint300))
                            )
                    )

                    Column(
                        modifier            = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text          = "Welcome",
                            fontSize      = 19.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = Rose500,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text     = "Sign in to continue",
                            fontSize = 12.sp,
                            color    = Stone,
                            modifier = Modifier.padding(top = 3.dp)
                        )

                        Spacer(Modifier.height(24.dp))

                        OutlinedTextField(
                            value         = email,
                            onValueChange = { email = it },
                            label         = { Text(stringResource(R.string.e_mail)) },
                            placeholder   = {
                                Text(
                                    stringResource(R.string.email_fairshare_com),
                                    color = Color(0xFFCCBBC2)
                                )
                            },
                            singleLine  = true,
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Rose500) },
                            shape       = RoundedCornerShape(16.dp),
                            colors      = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = Rose500,
                                unfocusedBorderColor    = Color(0xFFE8D5DC),
                                focusedLabelColor       = Rose500,
                                unfocusedLabelColor     = Stone,
                                cursorColor             = Rose500,
                                focusedContainerColor   = FieldFocus,
                                unfocusedContainerColor = FieldBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value                = password,
                            onValueChange        = { password = it },
                            label                = { Text(stringResource(R.string.password)) },
                            singleLine           = true,
                            visualTransformation = if (showPassword) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            leadingIcon  = { Icon(Icons.Default.Lock, null, tint = Rose500) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector        = if (showPassword) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint               = Rose500
                                    )
                                }
                            },
                            shape  = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = Rose500,
                                unfocusedBorderColor    = Color(0xFFE8D5DC),
                                focusedLabelColor       = Rose500,
                                unfocusedLabelColor     = Stone,
                                cursorColor             = Rose500,
                                focusedContainerColor   = FieldFocus,
                                unfocusedContainerColor = FieldBg
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(28.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    loginBtnScale.animateTo(0.95f, tween(80))
                                    loginBtnScale.animateTo(
                                        1f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )
                                    val result = viewModel.loginUser(email, password)
                                    if (result?.user != null) {
                                        showSuccessOverlay = true
                                        delay(1800)
                                        onLoginSuccess()
                                    }
                                }
                            },
                            modifier  = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .scale(loginBtnScale.value),
                            shape     = RoundedCornerShape(16.dp),
                            colors    = ButtonDefaults.buttonColors(
                                containerColor = LogoGreen,
                                contentColor   = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text          = stringResource(R.string.login),
                                fontSize      = 15.sp,
                                fontWeight    = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(Modifier.weight(1f), color = Color(0xFFEDD8E0))
                            Text("  or  ", color = Stone, fontSize = 12.sp)
                            HorizontalDivider(Modifier.weight(1f), color = Color(0xFFEDD8E0))
                        }

                        Spacer(Modifier.height(14.dp))

                        OutlinedButton(
                            onClick  = {
                                coroutineScope.launch {
                                    registerBtnScale.animateTo(0.95f, tween(80))
                                    registerBtnScale.animateTo(
                                        1f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )
                                    onNavigateToRegister(email, password)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .scale(registerBtnScale.value),
                            shape    = RoundedCornerShape(16.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                            border   = androidx.compose.foundation.BorderStroke(1.5.dp, Rose500)
                        ) {
                            Text(
                                text          = stringResource(R.string.register),
                                fontSize      = 15.sp,
                                fontWeight    = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }

            // ── State overlays ────────────────────────────────────────────────
            when (val state = viewModel.loginUiState) {
                is LoginUiState.Error -> {
                    LaunchedEffect(state) {
                        snackbarHostState.showSnackbar(
                            message           = state.errorMessage ?: "Unknown error",
                            withDismissAction = true
                        )
                    }
                }
                is LoginUiState.RegisterSuccess -> {
                    LaunchedEffect(Unit) {
                        snackbarHostState.showSnackbar(
                            message           = "Registered successfully!",
                            withDismissAction = true
                        )
                    }
                }
                is LoginUiState.Loading -> {
                    Box(
                        modifier         = Modifier
                            .fillMaxSize()
                            .background(Color(0x44FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color       = LogoGreen,
                            strokeWidth = 3.dp
                        )
                    }
                }
                else -> {}
            }

            if (showSuccessOverlay) {
                LoginSuccessOverlay()
            }
        }
    }
}

// ── Full-screen success animation ─────────────────────────────────────────────
@Composable
private fun LoginSuccessOverlay() {
    val overlayAlpha = remember { Animatable(0f) }
    val checkScale   = remember { Animatable(0f) }
    val rippleScale  = remember { Animatable(0.4f) }
    val rippleAlpha  = remember { Animatable(0.5f) }
    val textAlpha    = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        overlayAlpha.animateTo(1f, tween(280))
        checkScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
        )
        launch { rippleScale.animateTo(2.4f, tween(650, easing = EaseOutCubic)) }
        launch { rippleAlpha.animateTo(0f,   tween(650, easing = EaseOutCubic)) }
        textAlpha.animateTo(1f, tween(380, delayMillis = 100))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha.value }
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFAECF2), Color(0xFFFFF8FB), Color(0xFFEDF7F0))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(110.dp)
                        .scale(rippleScale.value)
                        .graphicsLayer { alpha = rippleAlpha.value }
                        .clip(CircleShape)
                        .background(LogoGreen.copy(alpha = 0.18f))
                )
                Box(
                    Modifier
                        .size(88.dp)
                        .scale(checkScale.value)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFF6FCF97), LogoGreen))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Check,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text          = "You're in!",
                fontSize      = 26.sp,
                fontWeight    = FontWeight.Bold,
                color         = LogoGreen,
                letterSpacing = (-0.5).sp,
                modifier      = Modifier.graphicsLayer { alpha = textAlpha.value }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text     = "Taking you to your rooms…",
                fontSize = 14.sp,
                color    = Stone,
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value }
            )

            Spacer(Modifier.height(32.dp))

            LoadingDots(
                modifier = Modifier.graphicsLayer { alpha = textAlpha.value }
            )
        }
    }
}

// ── Three bouncing dots ───────────────────────────────────────────────────────
@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "dots")

    val dot1Y by inf.animateFloat(
        initialValue  = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(
            tween(380, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            initialStartOffset = StartOffset(0)
        ), label = "d1"
    )
    val dot2Y by inf.animateFloat(
        initialValue  = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(
            tween(380, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            initialStartOffset = StartOffset(120)
        ), label = "d2"
    )
    val dot3Y by inf.animateFloat(
        initialValue  = 0f, targetValue = -8f,
        animationSpec = infiniteRepeatable(
            tween(380, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            initialStartOffset = StartOffset(240)
        ), label = "d3"
    )

    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        listOf(dot1Y, dot2Y, dot3Y).forEach { offsetY ->
            Box(
                Modifier
                    .size(8.dp)
                    .graphicsLayer { translationY = offsetY }
                    .clip(CircleShape)
                    .background(LogoGreen.copy(alpha = 0.6f))
            )
        }
    }
}
