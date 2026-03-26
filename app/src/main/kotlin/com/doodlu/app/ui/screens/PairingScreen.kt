package com.doodlu.app.ui.screens

import android.content.Intent
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.doodlu.app.R
import com.doodlu.app.data.PreferencesManager
import com.doodlu.app.sync.SyncManager
import com.doodlu.app.ui.components.*
import com.doodlu.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

fun generateRoomCode(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..6).map { chars.random() }.joinToString("")
}

@Composable
fun PairingScreen(onPaired: (showSetup: Boolean) -> Unit) {
    val context   = LocalContext.current
    val scope     = rememberCoroutineScope()
    val prefs     = remember { PreferencesManager(context) }
    val setupShown by prefs.wallpaperSetupShown.collectAsState(initial = false)
    val clipboard = LocalClipboardManager.current
    val focusMgr  = LocalFocusManager.current
    val screenH   = LocalConfiguration.current.screenHeightDp.dp

    var myCode    by remember { mutableStateOf(generateRoomCode()) }
    var joinChars by remember { mutableStateOf(Array(6) { "" }) }
    var focusedIdx by remember { mutableIntStateOf(-1) }
    var isJoining  by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf("") }

    val joinCode = joinChars.joinToString("")
    val canJoin  = joinCode.length == 6 && !isJoining
    val focusRequesters = remember { Array(6) { FocusRequester() } }

    // ── Staggered entrance animations ────────────────────────────────
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200); stage = 1   // hero
        kotlinx.coroutines.delay(250); stage = 2   // logo
        kotlinx.coroutines.delay(200); stage = 3   // code card
        kotlinx.coroutines.delay(200); stage = 4   // join card
        kotlinx.coroutines.delay(200); stage = 5   // footer
    }

    fun stageAlpha(s: Int) = if (stage >= s) 1f else 0f
    fun stageSlide(s: Int) = if (stage >= s) 0f else 50f

    val heroAlpha by animateFloatAsState(stageAlpha(1), tween(500), label = "ha")
    val logoAlpha by animateFloatAsState(stageAlpha(2), tween(600), label = "la")
    val logoSlide by animateFloatAsState(stageSlide(2),
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "ls")
    val codeAlpha by animateFloatAsState(stageAlpha(3), tween(500), label = "ca")
    val codeSlide by animateFloatAsState(stageSlide(3),
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "cs")
    val joinAlpha by animateFloatAsState(stageAlpha(4), tween(500), label = "ja")
    val joinSlide by animateFloatAsState(stageSlide(4),
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), label = "js")
    val footerAlpha by animateFloatAsState(stageAlpha(5), tween(600), label = "fa")

    Box(modifier = Modifier
        .fillMaxSize()
        .background(kawaiiBgGradient)
    ) {

        // Floating particles (subtle)
        FloatingParticles(modifier = Modifier.fillMaxSize())

        // ═══════════════════════════════════════════════════════════════
        // HERO CAT VIDEO — top portion, fading into aurora
        // ═══════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenH * 0.40f)
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = heroAlpha }
        ) {
            CatVideoHero(modifier = Modifier.fillMaxSize())

            // Bottom fade into kawaii background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                KawaiiBlush.copy(alpha = 0.7f),
                                KawaiiBlush
                            )
                        )
                    )
            )

            // Top status bar gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                KawaiiBlush.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // ═══════════════════════════════════════════════════════════════
        // SCROLLABLE CONTENT — overlaps hero
        // ═══════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Push below hero
            Spacer(modifier = Modifier.height(screenH * 0.33f))

            // ── Logo + tagline ─────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        alpha = logoAlpha
                        translationY = logoSlide
                    }
            ) {
                DoodluLogo(fontSize = 44, showTagline = false)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Draw together, stay together 💕",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = KawaiiTextSec,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Version pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(KawaiiCodeBg)
                        .border(1.dp, KawaiiCodeBorder.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "✨ v1.0 beta",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = KawaiiTextSec
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── YOUR CODE CARD — Glassmorphism ────────────────────────
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        alpha = codeAlpha
                        translationY = codeSlide
                    }
            ) {
                YourCodeCard(
                    myCode    = myCode,
                    clipboard = clipboard,
                    context   = context
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Gradient divider ─────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, KawaiiCodeBorder.copy(alpha = 0.4f))
                                )
                            )
                    )
                    Text(
                        text = "  💕 got a code? 💕  ",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = KawaiiTextSec
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(KawaiiCodeBorder.copy(alpha = 0.4f), Color.Transparent)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── JOIN CARD — Glassmorphism ─────────────────────────────
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .graphicsLayer {
                        alpha = joinAlpha
                        translationY = joinSlide
                    }
            ) {
                JoinCard(
                    joinChars       = joinChars,
                    focusedIdx      = focusedIdx,
                    focusRequesters = focusRequesters,
                    errorMsg        = errorMsg,
                    canJoin         = canJoin,
                    isJoining       = isJoining,
                    onFocusChanged  = { idx, focused -> if (focused) focusedIdx = idx },
                    onChar          = { idx, c ->
                        joinChars = joinChars.clone().also { it[idx] = c.uppercase() }
                        if (c.isNotEmpty() && idx < 5) focusRequesters[idx + 1].requestFocus()
                    },
                    onBackspace = { idx ->
                        if (joinChars[idx].isEmpty() && idx > 0) {
                            joinChars = joinChars.clone().also { it[idx - 1] = "" }
                            focusRequesters[idx - 1].requestFocus()
                        } else {
                            joinChars = joinChars.clone().also { it[idx] = "" }
                            if (idx > 0) focusRequesters[idx - 1].requestFocus()
                        }
                    },
                    onJoin = {
                        if (joinCode.length == 6) {
                            scope.launch {
                                isJoining = true
                                errorMsg  = ""
                                focusMgr.clearFocus()
                                joinRoom(joinCode, prefs) { onPaired(!setupShown) }
                                isJoining = false
                            }
                        }
                    },
                    onUseMine = {
                        scope.launch { joinRoom(myCode, prefs) { onPaired(!setupShown) } }
                    }
                )
            }

            // ── Footer ────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(40.dp))
            Column(
                modifier = Modifier.graphicsLayer { alpha = footerAlpha },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Gradient separator
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "made with 💕 for people in love",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = KawaiiTextSec,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Cat hero — TextureView + H.264 MP4, center-crop fill, silent loop
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun CatVideoHero(modifier: Modifier = Modifier) {
    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayerRef.value?.apply { stop(); release() }
            mediaPlayerRef.value = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).also { tv ->
                tv.layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                        mediaPlayerRef.value?.apply { stop(); release() }
                        val mp = MediaPlayer()
                        try {
                            val afd = ctx.resources.openRawResourceFd(R.raw.cat_silly)
                            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                            mp.setSurface(Surface(st))
                            mp.isLooping = true
                            mp.setVolume(0f, 0f)
                            mp.setOnVideoSizeChangedListener { _, videoW, videoH ->
                                if (videoW > 0 && videoH > 0) {
                                    applyCenterCropMatrix(tv, videoW.toFloat(), videoH.toFloat(), w.toFloat(), h.toFloat())
                                }
                            }
                            mp.setOnPreparedListener { player ->
                                val vW = player.videoWidth.toFloat()
                                val vH = player.videoHeight.toFloat()
                                if (vW > 0 && vH > 0) {
                                    applyCenterCropMatrix(tv, vW, vH, w.toFloat(), h.toFloat())
                                }
                                player.start()
                            }
                            mp.setOnErrorListener { _, _, _ -> false }
                            mp.prepare()
                            mediaPlayerRef.value = mp
                        } catch (e: Exception) {
                            mp.release()
                        }
                    }
                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                        mediaPlayerRef.value?.let { player ->
                            val vW = player.videoWidth.toFloat()
                            val vH = player.videoHeight.toFloat()
                            if (vW > 0 && vH > 0) applyCenterCropMatrix(tv, vW, vH, w.toFloat(), h.toFloat())
                        }
                    }
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        mediaPlayerRef.value?.apply { stop(); release() }
                        mediaPlayerRef.value = null
                        return true
                    }
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        update = { /* self-managed */ }
    )
}

private fun applyCenterCropMatrix(
    tv: TextureView, videoW: Float, videoH: Float, viewW: Float, viewH: Float
) {
    if (viewW <= 0f || viewH <= 0f) return
    val videoAspect = videoW / videoH
    val viewAspect  = viewW  / viewH
    val scaleX: Float
    val scaleY: Float
    if (videoAspect > viewAspect) {
        scaleX = videoAspect / viewAspect; scaleY = 1f
    } else {
        scaleX = 1f; scaleY = viewAspect / videoAspect
    }
    val matrix = android.graphics.Matrix()
    matrix.setScale(scaleX, scaleY, viewW / 2f, viewH / 2f)
    tv.setTransform(matrix)
}

// ════════════════════════════════════════════════════════════════════════════
// Your Code Card — Premium Glassmorphism
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun YourCodeCard(
    myCode: String,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "ga"
    )


    KawaiiCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 28.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pill label with animated glow
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
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Your secret code ✨",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6 code bubbles — staggered entrance
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                myCode.forEachIndexed { idx, char ->
                    CodeBubble(char = char, animDelay = idx * 80)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Share this with your person 💌",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = KawaiiTextSec
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action row — copy + share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Copy button — outlined glass style
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(myCode)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.5.dp,
                        KawaiiCodeBorder.copy(alpha = 0.5f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor   = KawaiiTextPri,
                        containerColor = KawaiiInputBg
                    )
                ) {
                    Icon(Icons.Filled.ContentCopy, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Copy",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp
                    )
                }

                // Share button — premium gradient
                KawaiiPrimaryButton(
                    text     = "Share",
                    emoji    = "💌",
                    modifier = Modifier.weight(1f),
                    onClick  = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Join me on Doodlu! 💕 Enter code: $myCode"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, "Share with your person"))
                    }
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Join Card — Premium Glassmorphism
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun JoinCard(
    joinChars: Array<String>,
    focusedIdx: Int,
    focusRequesters: Array<FocusRequester>,
    errorMsg: String,
    canJoin: Boolean,
    isJoining: Boolean,
    onFocusChanged: (Int, Boolean) -> Unit,
    onChar: (Int, String) -> Unit,
    onBackspace: (Int) -> Unit,
    onJoin: () -> Unit,
    onUseMine: () -> Unit
) {
    KawaiiCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 28.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Join someone's room",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = KawaiiTextPri
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Enter their 6-character code below",
                fontFamily = NunitoFamily,
                fontSize = 15.sp,
                color = KawaiiTextSec
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 6 input slots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(6) { idx ->
                    JoinCodeSlot(
                        char            = joinChars[idx],
                        isFocused       = focusedIdx == idx,
                        focusRequester  = focusRequesters[idx],
                        onFocusChanged  = { focused -> onFocusChanged(idx, focused) },
                        onChar          = { c -> onChar(idx, c) },
                        onBackspace     = { onBackspace(idx) }
                    )
                }
            }

            if (errorMsg.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    errorMsg,
                    color      = KawaiiRed,
                    fontFamily = NunitoFamily,
                    fontSize   = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            KawaiiSecondaryButton(
                text     = if (isJoining) "Connecting…" else "Join Room",
                emoji    = if (isJoining) "" else "🎉",
                enabled  = canJoin,
                modifier = Modifier.fillMaxWidth(),
                onClick  = onJoin
            )

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick  = onUseMine,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "Use my own code instead →",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 15.sp,
                    color      = ShimmerPink
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Join code slot — premium glass style with glow focus ring
// ════════════════════════════════════════════════════════════════════════════
@Composable
fun JoinCodeSlot(
    char: String,
    isFocused: Boolean,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    val charScale by animateFloatAsState(
        targetValue  = if (char.isNotEmpty()) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label        = "char_scale"
    )

    val focusGlow by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(300),
        label = "focus_glow"
    )

    val bgColor = when {
        isFocused -> KawaiiInputFocus
        else -> KawaiiInputBg
    }
    val borderColor = if (isFocused) KawaiiPink.copy(alpha = 0.7f) else KawaiiCodeBorder.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 54.dp)
            .shadow(
                elevation = if (isFocused) 8.dp else 0.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = KawaiiPink.copy(alpha = 0.3f * focusGlow)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when {
                        event.key == Key.Backspace -> { onBackspace(); true }
                        event.utf16CodePoint in 65..90 || event.utf16CodePoint in 97..122 ||
                        event.utf16CodePoint in 48..57 -> {
                            onChar(event.utf16CodePoint.toChar().toString()); true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication        = null
            ) { focusRequester.requestFocus() },
        contentAlignment = Alignment.Center
    ) {
        TextField(
            value    = char,
            onValueChange = { new ->
                val filtered = new.filter { it.isLetterOrDigit() }.uppercase()
                when {
                    filtered.isEmpty()  -> onChar("")
                    filtered.length == 1 -> onChar(filtered)
                    else                -> onChar(filtered.last().toString())
                }
            },
            modifier  = Modifier.size(1.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction      = ImeAction.Next
            )
        )
        if (char.isNotEmpty()) {
            Text(
                text       = char,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 24.sp,
                color      = KawaiiTextPri,
                modifier   = Modifier.scale(charScale)
            )
        } else if (isFocused) {
            // Animated cursor
            val cursorAlpha by rememberInfiniteTransition(label = "cursor").animateFloat(
                initialValue = 0.3f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(500), RepeatMode.Reverse
                ), label = "ca"
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(KawaiiPink.copy(alpha = cursorAlpha))
            )
        }
    }
}

private suspend fun joinRoom(
    code: String,
    prefs: PreferencesManager,
    onPaired: () -> Unit
) {
    val userId = UUID.randomUUID().toString().take(8)
    prefs.saveRoom(code, userId, "X")
    SyncManager.connect(code, userId)
    onPaired()
}
