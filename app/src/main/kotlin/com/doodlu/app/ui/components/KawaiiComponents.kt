package com.doodlu.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlu.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.sin

// ── Gradient definitions ───────────────────────────────────────────────────
val kawaiiBgGradient = Brush.verticalGradient(
    colors = listOf(KawaiiBlush, KawaiiLavender, KawaiiPeach)
)

val kawaiiPinkGradient = Brush.linearGradient(
    colors = listOf(KawaiiPink, KawaiiCoral),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, 0f)
)

val kawaiiPurpleGradient = Brush.linearGradient(
    colors = listOf(KawaiiPurple, Color(0xFFA07AFF)),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, 0f)
)

// Premium aurora gradient for backgrounds
val premiumAuroraGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0D0221),
        Color(0xFF150734),
        Color(0xFF1A0A3E),
        Color(0xFF21094E),
        Color(0xFF1A0A3E),
        Color(0xFF150734)
    )
)

// ── Doodlu Logo — Premium Gradient Text ───────────────────────────────────
@Composable
fun DoodluLogo(
    modifier: Modifier = Modifier,
    fontSize: Int = 36,
    showTagline: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                1f at 0
                1.2f at 200
                1f at 400
                1.1f at 700
                1f at 900
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "heartScale"
    )

    // Shimmer sweep across the logo
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Gradient text with shimmer
            Text(
                text = "Doodlu",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize.sp,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GradTextStart,
                            ShimmerGold.copy(alpha = 0.8f),
                            GradTextEnd,
                            GradTextStart
                        ),
                        start = Offset(shimmerOffset * 300f, 0f),
                        end = Offset(shimmerOffset * 300f + 400f, 0f)
                    )
                )
            )
            Text(
                text = "❤️",
                fontSize = (fontSize * 0.65f).sp,
                modifier = Modifier.scale(heartScale)
            )
        }
        if (showTagline) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "your screen, their heart 💕",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = KawaiiTextSec
            )
        }
    }
}

// ── Room Code Bubble (single character) — Premium ─────────────────────────
@Composable
fun CodeBubble(
    char: Char,
    modifier: Modifier = Modifier,
    animDelay: Int = 0,
    isEmpty: Boolean = false,
    isFocused: Boolean = false,
) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bubble_scale"
    )
    val rotation by animateFloatAsState(
        targetValue = if (visible) 0f else -15f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bubble_rot"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(animDelay.toLong())
        visible = true
    }

    val bgColor = when {
        isEmpty && isFocused -> KawaiiInputFocus
        isEmpty -> KawaiiInputBg
        else -> KawaiiCodeBg
    }
    val borderColor = if (isFocused) KawaiiPink else KawaiiCodeBorder

    Box(
        modifier = modifier
            .size(width = 44.dp, height = 54.dp)
            .scale(scale)
            .rotate(rotation)
            .shadow(
                elevation = if (isEmpty) 0.dp else 8.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = KawaiiPurple.copy(alpha = 0.2f),
                spotColor = KawaiiPink.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isEmpty) {
            Text(
                text = char.toString(),
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = KawaiiTextPri,
                letterSpacing = 0.sp
            )
        }
    }
}

// ── Kawaii Card ────────────────────────────────────────────────────────────
@Composable
fun KawaiiCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color(0xFF000000).copy(alpha = 0.05f),
                spotColor = Color(0xFF000000).copy(alpha = 0.04f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(KawaiiCardAlpha)
            .padding(24.dp),
        content = content
    )
}

// ── Glassmorphism Card ────────────────────────────────────────────────────
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glass_glow_alpha"
    )

    Column(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = KawaiiPink.copy(alpha = 0.12f),
                spotColor = KawaiiPurple.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = glowAlpha),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(24.dp),
        content = content
    )
}

// ── Pink Gradient Button — Premium ────────────────────────────────────────
@Composable
fun KawaiiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emoji: String = ""
) {
    val scaleAnim = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "btn_shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -0.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "sx"
    )

    Box(
        modifier = modifier
            .scale(scaleAnim.value)
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = RoundedCornerShape(50.dp),
                ambientColor = KawaiiPink.copy(alpha = 0.25f),
                spotColor = KawaiiPink.copy(alpha = 0.30f)
            )
            .clip(RoundedCornerShape(50.dp))
            .background(
                if (enabled) Brush.linearGradient(
                    colors = listOf(KawaiiPink, ShimmerPink, KawaiiCoral, KawaiiPink),
                    start = Offset(shimmerX * 600f, 0f),
                    end = Offset(shimmerX * 600f + 500f, 0f)
                )
                else Brush.linearGradient(
                    listOf(Color(0xFFCCCCCC), Color(0xFFBBBBBB)),
                    Offset(0f, 0f), Offset(Float.POSITIVE_INFINITY, 0f)
                )
            )
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    scope.launch {
                        scaleAnim.animateTo(0.93f, spring(stiffness = Spring.StiffnessHigh))
                        scaleAnim.animateTo(
                            1f, spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                        onClick()
                    }
                } else Modifier
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (emoji.isNotEmpty()) "$text $emoji" else text,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            letterSpacing = 0.5.sp
        )
    }
}

// ── Purple Gradient Button — Premium ──────────────────────────────────────
@Composable
fun KawaiiSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emoji: String = ""
) {
    val scaleAnim = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "s_btn_shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -0.5f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "ssx"
    )

    Box(
        modifier = modifier
            .scale(scaleAnim.value)
            .shadow(
                elevation = if (enabled) 10.dp else 0.dp,
                shape = RoundedCornerShape(50.dp),
                ambientColor = KawaiiPurple.copy(alpha = 0.25f),
                spotColor = KawaiiPurple.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(50.dp))
            .background(
                if (enabled) Brush.linearGradient(
                    colors = listOf(KawaiiPurple, ShimmerLavender, Color(0xFFA07AFF), KawaiiPurple),
                    start = Offset(shimmerX * 600f, 0f),
                    end = Offset(shimmerX * 600f + 500f, 0f)
                )
                else Brush.linearGradient(
                    listOf(Color(0xFFCCCCCC), Color(0xFFBBBBBB)),
                    Offset(0f, 0f), Offset(Float.POSITIVE_INFINITY, 0f)
                )
            )
            .then(
                if (enabled) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    scope.launch {
                        scaleAnim.animateTo(0.93f, spring(stiffness = Spring.StiffnessHigh))
                        scaleAnim.animateTo(
                            1f, spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                        onClick()
                    }
                } else Modifier
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (emoji.isNotEmpty()) "$text $emoji" else text,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            letterSpacing = 0.5.sp
        )
    }
}

// ── Floating Particles — Premium with depth variation ─────────────────────
@Composable
fun FloatingParticles(modifier: Modifier = Modifier) {
    val particles = listOf(
        "❤️" to 18, "✨" to 12, "💕" to 16, "⭐" to 10, "🌸" to 14,
        "💫" to 11, "❤️" to 15, "✨" to 13, "💕" to 17, "⭐" to 9,
        "🌟" to 11, "💖" to 13
    )
    Box(modifier = modifier) {
        particles.forEachIndexed { idx, (emoji, size) ->
            FloatingParticle(
                emoji = emoji,
                startFraction = idx * 0.082f + 0.02f,
                duration = 4000 + idx * 420,
                delay = idx * 480,
                particleSize = size,
                blurAmount = if (idx % 3 == 0) 1.dp else 0.dp
            )
        }
    }
}

@Composable
private fun FloatingParticle(
    emoji: String,
    startFraction: Float,
    duration: Int,
    delay: Int,
    particleSize: Int = 14,
    blurAmount: Dp = 0.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "p_$delay")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, delayMillis = delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "prog_$delay"
    )
    // Alpha: smooth fade in/out
    val alpha = when {
        progress < 0.12f -> progress / 0.12f * 0.12f
        progress > 0.82f -> (1f - progress) / 0.18f * 0.12f
        else -> 0.12f
    }
    // Sine-wave horizontal drift for organic movement
    val driftX = sin(progress * 6.28f * 2f) * 12f

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val xOffset = maxWidth * startFraction
        val yOffset = maxHeight * (1f - progress)
        Text(
            text = emoji,
            fontSize = particleSize.sp,
            modifier = Modifier
                .offset(x = xOffset + driftX.dp, y = yOffset)
                .alpha(alpha)
                .then(if (blurAmount > 0.dp) Modifier.blur(blurAmount) else Modifier)
        )
    }
}

// ── Aurora Orbs — soft animated background blobs ──────────────────────────
@Composable
fun AuroraOrbs(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val drift1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(8000, easing = EaseInOut), RepeatMode.Reverse
        ), label = "d1"
    )
    val drift2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(10000, easing = EaseInOut), RepeatMode.Reverse
        ), label = "d2"
    )
    val drift3 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(7000, easing = EaseInOut), RepeatMode.Reverse
        ), label = "d3"
    )

    BoxWithConstraints(modifier = modifier) {
        // Large pink orb — top-right
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(
                    x = maxWidth * 0.5f + (drift1 * 40f).dp,
                    y = maxHeight * 0.05f + (drift2 * 30f).dp
                )
                .blur(80.dp)
                .clip(CircleShape)
                .background(AuroraDeepPink.copy(alpha = 0.25f))
        )
        // Violet orb — center-left
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(
                    x = maxWidth * -0.1f + (drift2 * 50f).dp,
                    y = maxHeight * 0.3f + (drift3 * 40f).dp
                )
                .blur(70.dp)
                .clip(CircleShape)
                .background(AuroraViolet.copy(alpha = 0.22f))
        )
        // Indigo orb — bottom-center
        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(
                    x = maxWidth * 0.2f + (drift3 * 35f).dp,
                    y = maxHeight * 0.6f + (drift1 * 25f).dp
                )
                .blur(75.dp)
                .clip(CircleShape)
                .background(AuroraIndigo.copy(alpha = 0.18f))
        )
        // Rose orb — top-left subtle
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(
                    x = maxWidth * 0.1f + (drift1 * 20f).dp,
                    y = maxHeight * 0.1f + (drift2 * 35f).dp
                )
                .blur(60.dp)
                .clip(CircleShape)
                .background(AuroraRose.copy(alpha = 0.15f))
        )
    }
}

// ── Connection Badge ───────────────────────────────────────────────────────
@Composable
fun KawaiiConnectionBadge(
    isConnected: Boolean,
    playerCount: Int,
    modifier: Modifier = Modifier
) {
    val dotColor by animateColorAsState(
        targetValue = if (isConnected) KawaiiGreen else KawaiiRed,
        animationSpec = tween(600),
        label = "conn_color"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "conn_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(KawaiiCard.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = if (isConnected) pulseAlpha else 1f))
        )
        Text(
            text = if (isConnected) "$playerCount online" else "connecting...",
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = KawaiiTextSec
        )
    }
}
