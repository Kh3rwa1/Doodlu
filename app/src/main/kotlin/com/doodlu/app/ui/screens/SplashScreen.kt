package com.doodlu.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlu.app.ui.components.*
import com.doodlu.app.ui.theme.*

@Composable
fun SplashScreen(onSplashDone: () -> Unit) {

    // Bounce-in animation for the logo
    var logoVisible by remember { mutableStateOf(false) }
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "logoScale"
    )

    // Loading dots animation
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "d1"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "d2"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "d3"
    )

    LaunchedEffect(Unit) {
        logoVisible = true
        kotlinx.coroutines.delay(1500)
        onSplashDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(kawaiiBgGradient),
        contentAlignment = Alignment.Center
    ) {
        // Floating particles behind
        FloatingParticles(modifier = Modifier.fillMaxSize())

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(logoScale)
        ) {
            DoodluLogo(
                fontSize = 42,
                showTagline = true
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Loading dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(dot1Scale, dot2Scale, dot3Scale).forEach { dotScale ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(KawaiiPink)
                    )
                }
            }
        }
    }
}
