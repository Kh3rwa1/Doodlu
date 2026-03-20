package com.doodlu.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlu.app.ui.components.*
import com.doodlu.app.ui.theme.*

@Composable
fun SplashScreen(onSplashDone: () -> Unit) {

    // ── Entrance stages ───────────────────────────────────────────────
    var stage by remember { mutableIntStateOf(0) }
    val logoScale by animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0f,
        animationSpec = tween(400),
        label = "logoAlpha"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (stage >= 2) 1f else 0f,
        animationSpec = tween(500),
        label = "tagAlpha"
    )
    val dotsAlpha by animateFloatAsState(
        targetValue = if (stage >= 3) 1f else 0f,
        animationSpec = tween(400),
        label = "dotsAlpha"
    )

    // Progress arc using three pulsing dots
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "d1"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 160, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "d2"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 320, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "d3"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100); stage = 1
        kotlinx.coroutines.delay(300); stage = 2
        kotlinx.coroutines.delay(300); stage = 3
        kotlinx.coroutines.delay(800)
        onSplashDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0221),
                        Color(0xFF150734),
                        Color(0xFF1A0A3E),
                        Color(0xFF21094E),
                        Color(0xFF1A0A3E),
                        Color(0xFF0F0628)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Aurora orbs for premium feel
        AuroraOrbs(modifier = Modifier.fillMaxSize())

        // Floating particles (subtle)
        FloatingParticles(modifier = Modifier.fillMaxSize())

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(logoScale)
                .alpha(logoAlpha)
        ) {
            DoodluLogo(
                fontSize = 48,
                showTagline = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline with separate fade-in
            Text(
                text = "your screen, their heart 💕",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.alpha(taglineAlpha)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Premium loading dots — gradient colored
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(dotsAlpha)
            ) {
                listOf(
                    dot1Scale to KawaiiPink,
                    dot2Scale to ShimmerLavender,
                    dot3Scale to KawaiiPurple
                ).forEach { (dotScale, color) ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(dotScale)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}
