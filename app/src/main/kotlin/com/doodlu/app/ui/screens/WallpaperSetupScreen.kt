package com.doodlu.app.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlu.app.data.PreferencesManager
import com.doodlu.app.ui.components.*
import com.doodlu.app.ui.theme.*
import com.doodlu.app.util.buildWallpaperPickerIntent
import kotlinx.coroutines.launch

@Composable
fun WallpaperSetupScreen(
    onContinue: () -> Unit   // called after picker closes OR user taps "do it later"
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val prefs   = remember { PreferencesManager(context) }

    // Heart pulse on the phone illustration
    val infiniteTransition = rememberInfiniteTransition(label = "heartPulse")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                1f at 0; 1.25f at 200; 1f at 450; 1.12f at 700; 1f at 950
            },
            repeatMode = RepeatMode.Restart
        ), label = "hs"
    )

    // Bounce-in entrance animation
    var visible by remember { mutableStateOf(false) }
    val enterScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "enter"
    )
    val enterAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "enterAlpha"
    )
    LaunchedEffect(Unit) { visible = true }

    // Launcher: when user returns from the system wallpaper picker, mark done + navigate
    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Whether they set it or cancelled, mark shown and navigate away
        scope.launch {
            prefs.setWallpaperSetupShown(true)
            onContinue()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(kawaiiBgGradient)
    ) {
        // Soft floating particles in background
        FloatingParticles(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 32.dp)
                .graphicsLayer(scaleX = enterScale, scaleY = enterScale, alpha = enterAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ── Phone illustration ─────────────────────────────────────────
            PhoneWithHeartIllustration(heartScale = heartScale)

            Spacer(modifier = Modifier.height(36.dp))

            // ── Header ─────────────────────────────────────────────────────
            Text(
                text = "One last thing! ✨",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = KawaiiTextPri,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Body ───────────────────────────────────────────────────────
            Text(
                text = "Set Doodlu as your live wallpaper so you can see each other's doodles on your lock screen and home screen — in real time 💕",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = KawaiiTextSec,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Steps hint ─────────────────────────────────────────────────
            StepsHintCard()

            Spacer(modifier = Modifier.height(32.dp))

            // ── Primary CTA ────────────────────────────────────────────────
            KawaiiPrimaryButton(
                text = "Set as Wallpaper",
                emoji = "🎨",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val intent = buildWallpaperPickerIntent(context)
                    wallpaperLauncher.launch(intent)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Skip / do it later ─────────────────────────────────────────
            androidx.compose.material3.TextButton(
                onClick = {
                    scope.launch {
                        prefs.setWallpaperSetupShown(true)
                        onContinue()
                    }
                }
            ) {
                Text(
                    text = "I'll do it later from Settings ⚙️",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = KawaiiPurple
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ── Cute phone + heart illustration drawn with Canvas ─────────────────────
@Composable
private fun PhoneWithHeartIllustration(heartScale: Float) {
    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft glow circle behind phone
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            KawaiiPink.copy(alpha = 0.12f),
                            KawaiiLavender.copy(alpha = 0.0f)
                        )
                    )
                )
        )

        // Phone body + heart
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(120.dp)
        ) {
            drawPhoneWithHeart(heartScale)
        }
    }
}

private fun DrawScope.drawPhoneWithHeart(heartScale: Float) {
    val w = size.width
    val h = size.height

    // ── Phone frame ──────────────────────────────────────────────────────
    val phoneLeft   = w * 0.22f
    val phoneTop    = h * 0.04f
    val phoneRight  = w * 0.78f
    val phoneBottom = h * 0.96f
    val cornerR     = w * 0.10f

    // Phone shadow
    drawRoundRect(
        color = Color(0xFF000000).copy(alpha = 0.08f),
        topLeft     = androidx.compose.ui.geometry.Offset(phoneLeft + 4f, phoneTop + 6f),
        size        = androidx.compose.ui.geometry.Size(phoneRight - phoneLeft, phoneBottom - phoneTop),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR)
    )

    // Phone body
    drawRoundRect(
        color = Color(0xFF2D2D3A),
        topLeft     = androidx.compose.ui.geometry.Offset(phoneLeft, phoneTop),
        size        = androidx.compose.ui.geometry.Size(phoneRight - phoneLeft, phoneBottom - phoneTop),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR)
    )

    // Screen area (inset)
    val scrLeft   = phoneLeft + w * 0.03f
    val scrTop    = phoneTop  + h * 0.06f
    val scrRight  = phoneRight - w * 0.03f
    val scrBottom = phoneBottom - h * 0.06f
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF0F5), Color(0xFFF8E8FF), Color(0xFFFFE8D6)),
            startY = scrTop, endY = scrBottom
        ),
        topLeft     = androidx.compose.ui.geometry.Offset(scrLeft, scrTop),
        size        = androidx.compose.ui.geometry.Size(scrRight - scrLeft, scrBottom - scrTop),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR * 0.7f)
    )

    // Notch/pill at top of screen
    drawRoundRect(
        color = Color(0xFF2D2D3A).copy(alpha = 0.5f),
        topLeft     = androidx.compose.ui.geometry.Offset(w * 0.38f, scrTop + h * 0.015f),
        size        = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.022f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.011f)
    )

    // Small clock text suggestion — two tiny rounded rects
    drawRoundRect(
        color = KawaiiTextPri.copy(alpha = 0.18f),
        topLeft     = androidx.compose.ui.geometry.Offset(w * 0.34f, scrTop + h * 0.08f),
        size        = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.025f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
    )
    drawRoundRect(
        color = KawaiiTextPri.copy(alpha = 0.10f),
        topLeft     = androidx.compose.ui.geometry.Offset(w * 0.39f, scrTop + h * 0.115f),
        size        = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.018f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
    )

    // ── Heart on the screen ──────────────────────────────────────────────
    val cx     = w * 0.50f
    val cy     = scrTop + (scrBottom - scrTop) * 0.55f
    val hSize  = w * 0.18f * heartScale

    // Heart shape via cubic bezier
    val heartPath = Path().apply {
        moveTo(cx, cy + hSize * 0.35f)
        // Left lobe
        cubicTo(
            cx - hSize * 0.05f, cy,
            cx - hSize * 1.0f,  cy,
            cx - hSize * 1.0f,  cy - hSize * 0.35f
        )
        cubicTo(
            cx - hSize * 1.0f,  cy - hSize * 0.85f,
            cx - hSize * 0.05f, cy - hSize * 0.85f,
            cx,                 cy - hSize * 0.35f
        )
        // Right lobe (mirror)
        cubicTo(
            cx + hSize * 0.05f, cy - hSize * 0.85f,
            cx + hSize * 1.0f,  cy - hSize * 0.85f,
            cx + hSize * 1.0f,  cy - hSize * 0.35f
        )
        cubicTo(
            cx + hSize * 1.0f,  cy,
            cx + hSize * 0.05f, cy,
            cx,                 cy + hSize * 0.35f
        )
        close()
    }

    // Heart shadow
    drawPath(
        path  = heartPath,
        color = KawaiiPink.copy(alpha = 0.2f),
        style = androidx.compose.ui.graphics.drawscope.Fill
    )
    // Heart fill
    drawPath(
        path  = heartPath,
        brush = Brush.linearGradient(
            colors = listOf(KawaiiPink, KawaiiCoral),
            start  = androidx.compose.ui.geometry.Offset(cx - hSize, cy - hSize),
            end    = androidx.compose.ui.geometry.Offset(cx + hSize, cy + hSize)
        )
    )

    // Small sparkle dots around heart
    val sparklePositions = listOf(
        Pair(cx + hSize * 1.5f, cy - hSize * 0.8f),
        Pair(cx - hSize * 1.4f, cy - hSize * 0.6f),
        Pair(cx + hSize * 0.3f, cy - hSize * 1.4f),
    )
    sparklePositions.forEach { (sx, sy) ->
        drawCircle(
            color  = KawaiiYellow.copy(alpha = 0.8f),
            radius = w * 0.022f,
            center = androidx.compose.ui.geometry.Offset(sx, sy)
        )
    }

    // Bottom home indicator bar
    drawRoundRect(
        color = Color(0xFF2D2D3A).copy(alpha = 0.2f),
        topLeft     = androidx.compose.ui.geometry.Offset(w * 0.38f, scrBottom - h * 0.025f),
        size        = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.012f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
    )
}

// ── Quick steps hint card ──────────────────────────────────────────────────
@Composable
private fun StepsHintCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = KawaiiPurple.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(KawaiiCardAlpha)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "How it works",
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = KawaiiPink
        )
        StepRow("1", "Tap 'Set as Wallpaper' below 👇")
        StepRow("2", "Tap 'Set wallpaper' in the preview")
        StepRow("3", "Pick 'Lock screen' or 'Both' 🏠")
        StepRow("4", "You're live! Draw and it shows up instantly ✨")
    }
}

@Composable
private fun StepRow(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(KawaiiCodeBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = KawaiiPink
            )
        }
        Text(
            text,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = KawaiiTextSec,
            modifier = Modifier.weight(1f)
        )
    }
}
