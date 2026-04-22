package com.celestial.spire.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.celestial.spire.data.PreferencesManager
import com.celestial.spire.ui.components.*
import com.celestial.spire.ui.theme.*
import com.celestial.spire.util.buildWallpaperPickerIntent
import kotlinx.coroutines.launch

@Composable
fun WallpaperSetupScreen(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val prefs   = remember { PreferencesManager(context) }

    // Launcher: when user returns from the system wallpaper picker
    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        scope.launch {
            prefs.setWallpaperSetupShown(true)
            onContinue()
        }
    }

    // ── Staggered entrance (5 stages) ────────────────────────────────────
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150);  stage = 1   // phone
        kotlinx.coroutines.delay(200);  stage = 2   // badge
        kotlinx.coroutines.delay(180);  stage = 3   // headline + body
        kotlinx.coroutines.delay(200);  stage = 4   // steps card
        kotlinx.coroutines.delay(180);  stage = 5   // buttons + footer
    }

    fun stageAlpha(s: Int) = if (stage >= s) 1f else 0f
    fun stageSlide(s: Int) = if (stage >= s) 0f else 40f

    val phoneAlpha by animateFloatAsState(stageAlpha(1), tween(500), label = "pa")
    val phoneSlide by animateFloatAsState(stageSlide(1),
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "ps")
    val badgeAlpha by animateFloatAsState(stageAlpha(2), tween(400), label = "ba")
    val badgeSlide by animateFloatAsState(stageSlide(2),
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "bs")
    val headAlpha by animateFloatAsState(stageAlpha(3), tween(500), label = "ha")
    val headSlide by animateFloatAsState(stageSlide(3),
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "hs")
    val cardAlpha by animateFloatAsState(stageAlpha(4), tween(500), label = "ca")
    val cardSlide by animateFloatAsState(stageSlide(4),
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "cs")
    val btnAlpha by animateFloatAsState(stageAlpha(5), tween(500), label = "bta")
    val btnSlide by animateFloatAsState(stageSlide(5),
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "bts")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(kawaiiBgGradient)
    ) {
        // Soft floating particles
        FloatingParticles(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // ── Phone illustration with float + glow ─────────────────────
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = phoneAlpha
                        translationY = phoneSlide
                    },
                contentAlignment = Alignment.Center
            ) {
                PremiumPhoneIllustration()
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Gradient pill badge ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = badgeAlpha
                        translationY = badgeSlide
                    }
            ) {
                PremiumPillBadge(text = "Almost there ✨")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Headline + body ──────────────────────────────────────────
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = headAlpha
                    translationY = headSlide
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "One last thing!",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = KawaiiTextPri,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Set Doodlu as your live wallpaper so you can see each other's doodles on your lock screen and home screen — in real time 💕",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = KawaiiTextSec,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Steps card ───────────────────────────────────────────────
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = cardAlpha
                    translationY = cardSlide
                }
            ) {
                PremiumStepsCard()
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── CTA + Skip ───────────────────────────────────────────────
            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = btnAlpha
                    translationY = btnSlide
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KawaiiPrimaryButton(
                    text = "Set as Wallpaper",
                    emoji = "🎨",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = buildWallpaperPickerIntent(context)
                        wallpaperLauncher.launch(intent)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        scope.launch {
                            prefs.setWallpaperSetupShown(true)
                            onContinue()
                        }
                    }
                ) {
                    Text(
                        text = "I'll do it later ⚙️",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = ShimmerPink
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Footer ───────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(KawaiiPink.copy(alpha = 0.4f), KawaiiPurple.copy(alpha = 0.4f))
                            )
                        )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "made with 💕 for people in love",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = KawaiiTextSec,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ── Gradient pill badge ────────────────────────────────────────────────────
@Composable
private fun PremiumPillBadge(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pill_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "pill_ga"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        KawaiiPink.copy(alpha = glowAlpha),
                        KawaiiPurple.copy(alpha = glowAlpha)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White,
            letterSpacing = 0.3.sp
        )
    }
}

// ── Premium phone illustration with floating animation + glow ─────────────
@Composable
private fun PremiumPhoneIllustration() {
    // Gentle float
    val infiniteTransition = rememberInfiniteTransition(label = "phone_float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "float_y"
    )

    // Heart pulse
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                1f at 0; 1.25f at 200; 1f at 450; 1.12f at 700; 1f at 950
            },
            repeatMode = RepeatMode.Restart
        ), label = "heart_s"
    )

    // Shimmer glow behind phone
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "glow_s"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f, targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "glow_a"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .graphicsLayer { translationY = floatY },
        contentAlignment = Alignment.Center
    ) {
        // Pink glow behind
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(glowScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            KawaiiPink.copy(alpha = glowAlpha),
                            KawaiiPurple.copy(alpha = glowAlpha * 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Phone canvas
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(140.dp)
        ) {
            drawPremiumPhone(heartScale)
        }
    }
}

private fun DrawScope.drawPremiumPhone(heartScale: Float) {
    val w = size.width
    val h = size.height

    // ── Phone shadow ──────────────────────────────────────────────────────
    drawRoundRect(
        color = Color(0xFF000000).copy(alpha = 0.10f),
        topLeft = Offset(w * 0.22f + 5f, h * 0.04f + 7f),
        size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.92f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f)
    )

    // ── Phone body ────────────────────────────────────────────────────────
    drawRoundRect(
        color = Color(0xFF2D2D3A),
        topLeft = Offset(w * 0.22f, h * 0.04f),
        size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.92f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.10f)
    )

    // ── Screen ────────────────────────────────────────────────────────────
    val scrL = w * 0.25f
    val scrT = h * 0.10f
    val scrR = w * 0.75f
    val scrB = h * 0.90f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF0F5), Color(0xFFF8E8FF), Color(0xFFFFE8D6)),
            startY = scrT, endY = scrB
        ),
        topLeft = Offset(scrL, scrT),
        size = androidx.compose.ui.geometry.Size(scrR - scrL, scrB - scrT),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.07f)
    )

    // ── Notch ─────────────────────────────────────────────────────────────
    drawRoundRect(
        color = Color(0xFF2D2D3A).copy(alpha = 0.45f),
        topLeft = Offset(w * 0.38f, scrT + h * 0.015f),
        size = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.02f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.01f)
    )

    // ── Cute doodle marks inside screen ───────────────────────────────────
    // Small squiggle line (upper left)
    val squiggleColor = Color(0xFFFF6B9D).copy(alpha = 0.25f)
    drawLine(
        color = squiggleColor,
        start = Offset(scrL + w * 0.06f, scrT + h * 0.14f),
        end = Offset(scrL + w * 0.15f, scrT + h * 0.10f),
        strokeWidth = 2.5f
    )
    drawLine(
        color = squiggleColor,
        start = Offset(scrL + w * 0.15f, scrT + h * 0.10f),
        end = Offset(scrL + w * 0.20f, scrT + h * 0.16f),
        strokeWidth = 2.5f
    )

    // Small circle (upper right)
    drawCircle(
        color = Color(0xFFC3A6FF).copy(alpha = 0.25f),
        radius = w * 0.04f,
        center = Offset(scrR - w * 0.10f, scrT + h * 0.14f),
        style = Stroke(width = 2.5f)
    )

    // Small star dot (lower area)
    drawCircle(
        color = Color(0xFFFFD93D).copy(alpha = 0.30f),
        radius = w * 0.025f,
        center = Offset(w * 0.40f, scrB - h * 0.16f)
    )
    drawCircle(
        color = Color(0xFF6BCB77).copy(alpha = 0.25f),
        radius = w * 0.02f,
        center = Offset(w * 0.62f, scrB - h * 0.12f)
    )

    // ── Heart centre ──────────────────────────────────────────────────────
    val cx = w * 0.50f
    val cy = scrT + (scrB - scrT) * 0.52f
    val hSize = w * 0.15f * heartScale

    val heartPath = Path().apply {
        moveTo(cx, cy + hSize * 0.35f)
        cubicTo(cx - hSize * 0.05f, cy, cx - hSize * 1.0f, cy, cx - hSize * 1.0f, cy - hSize * 0.35f)
        cubicTo(cx - hSize * 1.0f, cy - hSize * 0.85f, cx - hSize * 0.05f, cy - hSize * 0.85f, cx, cy - hSize * 0.35f)
        cubicTo(cx + hSize * 0.05f, cy - hSize * 0.85f, cx + hSize * 1.0f, cy - hSize * 0.85f, cx + hSize * 1.0f, cy - hSize * 0.35f)
        cubicTo(cx + hSize * 1.0f, cy, cx + hSize * 0.05f, cy, cx, cy + hSize * 0.35f)
        close()
    }

    drawPath(
        path = heartPath,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFF6B9D), Color(0xFFFF8A65)),
            start = Offset(cx - hSize, cy - hSize),
            end = Offset(cx + hSize, cy + hSize)
        )
    )

    // Sparkle dots
    val sparkles = listOf(
        Offset(cx + hSize * 1.5f, cy - hSize * 0.8f),
        Offset(cx - hSize * 1.4f, cy - hSize * 0.6f),
        Offset(cx + hSize * 0.3f, cy - hSize * 1.4f),
    )
    sparkles.forEach { offset ->
        drawCircle(
            color = Color(0xFFFFD93D).copy(alpha = 0.8f),
            radius = w * 0.020f,
            center = offset
        )
    }

    // ── Home indicator ────────────────────────────────────────────────────
    drawRoundRect(
        color = Color(0xFF2D2D3A).copy(alpha = 0.18f),
        topLeft = Offset(w * 0.38f, scrB - h * 0.022f),
        size = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.012f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
    )
}

// ── Premium steps card ─────────────────────────────────────────────────────
@Composable
private fun PremiumStepsCard() {
    KawaiiCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "How it works",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = KawaiiPink
            )

            PremiumStepRow(1, "Tap 'Set as Wallpaper' below 👇")
            PremiumStepRow(2, "Tap 'Set wallpaper' in the preview")
            PremiumStepRow(3, "Pick 'Lock screen' or 'Both' 🏠")
            PremiumStepRow(4, "You're live! Doodles show instantly ✨")
        }
    }
}

@Composable
private fun PremiumStepRow(number: Int, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Gradient numbered circle
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(KawaiiPink, KawaiiCoral),
                        start = Offset(0f, 0f),
                        end = Offset(28f, 28f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = Color.White
            )
        }
        Text(
            text,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = KawaiiTextSec,
            modifier = Modifier.weight(1f),
            lineHeight = 20.sp
        )
    }
}
