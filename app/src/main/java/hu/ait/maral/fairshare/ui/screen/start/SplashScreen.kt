package hu.ait.maral.fairshare.ui.screen.start

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.ait.maral.fairshare.R
import hu.ait.maral.fairshare.ui.theme.LogoGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BlushWhite = Color(0xFFFFF0F5)
private val DeepRose   = Color(0xFFE76F8E)
private val RosePink   = Color(0xFFF48FB1)
private val SoftMint   = Color(0xFFA8D8B0)

@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    // ── Logo animation state ──────────────────────────────────────────────────
    val logoScale = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }

    // ── Text animation state ──────────────────────────────────────────────────
    val titleAlpha     = remember { Animatable(0f) }
    val titleOffsetY   = remember { Animatable(24f) }
    val taglineAlpha   = remember { Animatable(0f) }
    val taglineOffsetY = remember { Animatable(14f) }

    // ── Exit ──────────────────────────────────────────────────────────────────
    val exitAlpha = remember { Animatable(1f) }

    // ── Breathing: only starts after entrance, driven by infinite transition ──
    var entranceDone by remember { mutableStateOf(false) }
    val breathe = rememberInfiniteTransition(label = "breathe")
    val breatheScale by breathe.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.07f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "breatheScale"
    )

    // ── Blob drift ────────────────────────────────────────────────────────────
    val blob1 = rememberInfiniteTransition(label = "blob1")
    val blob1Y by blob1.animateFloat(
        initialValue  = 0f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(2800, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "blob1Y"
    )
    val blob2 = rememberInfiniteTransition(label = "blob2")
    val blob2X by blob2.animateFloat(
        initialValue  = 0f, targetValue = -14f,
        animationSpec = infiniteRepeatable(tween(3200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "blob2X"
    )

    LaunchedEffect(Unit) {
        // 1 — Logo pops in with spring
        launch { logoAlpha.animateTo(1f, tween(350)) }
        logoScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        entranceDone = true

        delay(150)

        // 2 — Title slides up
        launch { titleOffsetY.animateTo(0f, tween(480, easing = EaseOutCubic)) }
        titleAlpha.animateTo(1f, tween(350))

        delay(160)

        // 3 — Tagline slides up
        launch { taglineOffsetY.animateTo(0f, tween(420, easing = EaseOutCubic)) }
        taglineAlpha.animateTo(1f, tween(350))

        // 4 — Hold
        delay(600)

        // 5 — Fade out
        exitAlpha.animateTo(0f, tween(300, easing = EaseInCubic))

        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exitAlpha.value }
            .background(
                Brush.radialGradient(
                    colors = listOf(BlushWhite, Color(0xFFFCE4EC), Color(0xFFE8F5EE)),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // ── Drifting background blobs ─────────────────────────────────────────
        Box(
            Modifier
                .size(320.dp)
                .offset((-80).dp, (-100).dp + blob1Y.dp)
                .blur(90.dp)
                .background(Color(0x40F48FB1), CircleShape)
        )
        Box(
            Modifier
                .size(260.dp)
                .align(Alignment.BottomEnd)
                .offset(60.dp + blob2X.dp, 80.dp)
                .blur(80.dp)
                .background(Color(0x40A8D8B0), CircleShape)
        )
        Box(
            Modifier
                .size(180.dp)
                .align(Alignment.BottomStart)
                .offset((-40).dp, 40.dp)
                .blur(70.dp)
                .background(Color(0x30E76F8E), CircleShape)
        )

        // ── Center column ─────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logo — square shape, spring entrance, then breathes
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        alpha  = logoAlpha.value
                        val s  = logoScale.value * if (entranceDone) breatheScale else 1f
                        scaleX = s
                        scaleY = s
                    }
                    .shadow(
                        elevation    = 16.dp,
                        shape        = RoundedCornerShape(24.dp),
                        ambientColor = DeepRose,
                        spotColor    = DeepRose
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter            = painterResource(id = R.drawable.logo),
                    contentDescription = "FairShare Logo",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text          = "FairShare",
                fontSize      = 40.sp,
                fontWeight    = FontWeight.Black,
                color         = LogoGreen,
                letterSpacing = (-0.5).sp,
                modifier      = Modifier.graphicsLayer {
                    alpha        = titleAlpha.value
                    translationY = titleOffsetY.value.dp.toPx()
                }
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text          = "Share bills fairly. Keep friendships.",
                fontSize      = 13.sp,
                fontWeight    = FontWeight.Normal,
                color         = Color(0xFF9E8E95),
                letterSpacing = 0.3.sp,
                modifier      = Modifier.graphicsLayer {
                    alpha        = taglineAlpha.value
                    translationY = taglineOffsetY.value.dp.toPx()
                }
            )
        }
    }
}