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
import hu.ait.maral.fairshare.ui.theme.BackgroundPink
import hu.ait.maral.fairshare.ui.theme.ButtonGreen

@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    val animationDuration = 3000

    val leftOffset = remember { Animatable(0f) }
    val rightOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        leftOffset.animateTo(
            targetValue = -500f,
            animationSpec = tween(durationMillis = animationDuration, easing = LinearOutSlowInEasing)
        )
        rightOffset.animateTo(
            targetValue = 500f,
            animationSpec = tween(durationMillis = animationDuration, easing = LinearOutSlowInEasing)
        )

        delay(300)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPink)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .offset(x = leftOffset.value.dp)
                .background(ButtonGreen)
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(Alignment.TopEnd)
                .offset(x = rightOffset.value.dp)
                .background(ButtonGreen)
        )

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "FairShare Logo",
            modifier = Modifier
                .align(Alignment.Center)
                .size(150.dp)
        )
    }
}
