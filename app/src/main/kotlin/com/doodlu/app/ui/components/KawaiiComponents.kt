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
import androidx.compose.ui.draw.clip
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

// ── Doodlu Logo ────────────────────────────────────────────────────────────
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

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Doodlu",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize.sp,
                color = KawaiiTextPri
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

// ── Room Code Bubble (single character) ────────────────────────────────────
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
            .shadow(
                elevation = if (isEmpty) 0.dp else 4.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = KawaiiPurple.copy(alpha = 0.15f),
                spotColor = KawaiiPurple.copy(alpha = 0.1f)
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

// ── Pink Gradient Button ───────────────────────────────────────────────────
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

    Box(
        modifier = modifier
            .scale(scaleAnim.value)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(50.dp),
                ambientColor = KawaiiPink.copy(alpha = 0.2f),
                spotColor = KawaiiPink.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(50.dp))
            .background(
                if (enabled) kawaiiPinkGradient
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
                        scaleAnim.animateTo(0.95f, spring(stiffness = Spring.StiffnessHigh))
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
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color.White
        )
    }
}

// ── Purple Gradient Button ─────────────────────────────────────────────────
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

    Box(
        modifier = modifier
            .scale(scaleAnim.value)
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = RoundedCornerShape(50.dp),
                ambientColor = KawaiiPurple.copy(alpha = 0.2f),
                spotColor = KawaiiPurple.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(50.dp))
            .background(
                if (enabled) kawaiiPurpleGradient
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
                        scaleAnim.animateTo(0.95f, spring(stiffness = Spring.StiffnessHigh))
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
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Color.White
        )
    }
}

// ── Floating Particles (hearts & sparkles) ────────────────────────────────
@Composable
fun FloatingParticles(modifier: Modifier = Modifier) {
    val particles = listOf("❤️", "✨", "💕", "⭐", "🌸", "💫", "❤️", "✨", "💕", "⭐")
    Box(modifier = modifier) {
        particles.forEachIndexed { idx, emoji ->
            FloatingParticle(
                emoji = emoji,
                startFraction = idx * 0.1f + 0.05f,
                duration = 3200 + idx * 380,
                delay = idx * 550
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
    // alpha: fade in at start, fade out near top
    val alpha = when {
        progress < 0.1f -> progress / 0.1f * 0.08f
        progress > 0.85f -> (1f - progress) / 0.15f * 0.08f
        else -> 0.08f
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val xOffset = maxWidth * startFraction
        val yOffset = maxHeight * (1f - progress)
        Text(
            text = emoji,
            fontSize = 14.sp,
            modifier = Modifier.offset(x = xOffset, y = yOffset),
            color = Color.Black.copy(alpha = alpha)
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
