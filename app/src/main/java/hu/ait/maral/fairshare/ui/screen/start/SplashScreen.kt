package hu.ait.maral.fairshare.ui.screen.start

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import hu.ait.maral.fairshare.R

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val animationDuration = 3000  // 3 seconds

    // Animate left and right halves
    val leftOffset = remember { Animatable(0f) }
    val rightOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Move left to left, right to right
        leftOffset.animateTo(
            targetValue = -500f,
            animationSpec = tween(durationMillis = animationDuration, easing = LinearOutSlowInEasing)
        )
        rightOffset.animateTo(
            targetValue = 500f,
            animationSpec = tween(durationMillis = animationDuration, easing = LinearOutSlowInEasing)
        )

        // Wait a bit then navigate
        delay(300)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFE4EC)) // baby pink background
    ) {
        // Left panel
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .offset(x = leftOffset.value.dp)
                .background(Color(0xFF98C9A3)) // matcha green
        )

        // Right panel
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.TopEnd)
                .offset(x = rightOffset.value.dp)
                .background(Color(0xFF98C9A3)) // matcha green
        )

        // Logo appears in center
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "FairShare Logo",
            modifier = Modifier
                .align(Alignment.Center)
                .size(150.dp)
        )
    }
}
