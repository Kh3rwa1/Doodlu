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

    // Content entrance animation
    var contentVisible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 300),
        label = "alpha"
    )
    val contentSlide by animateFloatAsState(
        targetValue = if (contentVisible) 0f else 40f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "slide"
    )
    LaunchedEffect(Unit) { contentVisible = true }

    Box(modifier = Modifier.fillMaxSize()) {

        // ════════════════════════════════════════════════════════════════
        // BACKGROUND GRADIENT — full bleed
        // ════════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF0F5),
                            Color(0xFFF5E8FF),
                            Color(0xFFFFE8F5),
                            Color(0xFFFFF5E8)
                        )
                    )
                )
        )

        // Floating particles behind everything
        FloatingParticles(modifier = Modifier.fillMaxSize())

        // ════════════════════════════════════════════════════════════════
        // HERO CAT VIDEO — top 40% of screen, edge-to-edge
        // ════════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(screenH * 0.42f)
                .align(Alignment.TopCenter)
        ) {
            // Looping WebM cat video
            CatVideoHero(
                modifier = Modifier.fillMaxSize()
            )

            // Soft bottom fade so video blends into content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFFF0F5).copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Status bar safe area top fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFF0F5).copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // ════════════════════════════════════════════════════════════════
        // SCROLLABLE CONTENT — overlaps video bottom
        // ════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .graphicsLayer {
                    alpha         = contentAlpha
                    translationY  = contentSlide
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Push content down so it starts below the video hero
            Spacer(modifier = Modifier.height(screenH * 0.36f))

            // ── Logo + tagline ────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                DoodluLogo(fontSize = 40, showTagline = false)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Draw together, stay together 💕",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = KawaiiPurple,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── YOUR CODE CARD ────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                YourCodeCard(
                    myCode   = myCode,
                    clipboard = clipboard,
                    context  = context
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Divider with hearts ───────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier  = Modifier.weight(1f),
                        color     = KawaiiCodeBorder.copy(alpha = 0.4f),
                        thickness = 1.dp
                    )
                    Text(
                        text = "  💕 got a code? 💕  ",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 12.sp,
                        color      = KawaiiTextSec
                    )
                    HorizontalDivider(
                        modifier  = Modifier.weight(1f),
                        color     = KawaiiCodeBorder.copy(alpha = 0.4f),
                        thickness = 1.dp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── JOIN CARD ─────────────────────────────────────────
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
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = "made with 💕 for people in love",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = KawaiiPurple,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Cat hero — TextureView + H.264 MP4, center-crop fill, silent loop
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun CatVideoHero(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Keep MediaPlayer alive across recompositions
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
                        // Release any previous player
                        mediaPlayerRef.value?.apply { stop(); release() }

                        val mp = MediaPlayer()
                        try {
                            // Use H.264 MP4 — universally supported, has duration metadata
                            val afd = ctx.resources.openRawResourceFd(R.raw.cat_silly)
                            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()

                            mp.setSurface(Surface(st))
                            mp.isLooping = true
                            mp.setVolume(0f, 0f)

                            mp.setOnVideoSizeChangedListener { player, videoW, videoH ->
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
                            mp.prepare()   // synchronous — fine for local raw resource
                            mediaPlayerRef.value = mp
                        } catch (e: Exception) {
                            mp.release()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                        mediaPlayerRef.value?.let { player ->
                            val vW = player.videoWidth.toFloat()
                            val vH = player.videoHeight.toFloat()
                            if (vW > 0 && vH > 0) {
                                applyCenterCropMatrix(tv, vW, vH, w.toFloat(), h.toFloat())
                            }
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
        update = { /* view is self-managed */ }
    )
}

/**
 * Applies a center-crop Matrix transform to [tv] so the video
 * covers the entire view with no letterboxing/pillarboxing.
 */
private fun applyCenterCropMatrix(
    tv: TextureView,
    videoW: Float, videoH: Float,
    viewW: Float,  viewH: Float
) {
    if (viewW <= 0f || viewH <= 0f) return
    val videoAspect = videoW / videoH
    val viewAspect  = viewW  / viewH

    val scaleX: Float
    val scaleY: Float
    if (videoAspect > viewAspect) {
        // Video is wider → fill height, crop sides
        scaleX = videoAspect / viewAspect
        scaleY = 1f
    } else {
        // Video is taller → fill width, crop top/bottom
        scaleX = 1f
        scaleY = viewAspect / videoAspect
    }
    val matrix = android.graphics.Matrix()
    matrix.setScale(scaleX, scaleY, viewW / 2f, viewH / 2f)
    tv.setTransform(matrix)
}

// ════════════════════════════════════════════════════════════════════════════
// Your Code Card
// ════════════════════════════════════════════════════════════════════════════
@Composable
private fun YourCodeCard(
    myCode: String,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context
) {
    // Shimmer/pulse on the "your code" label
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "ga"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape     = RoundedCornerShape(28.dp),
                ambientColor = KawaiiPink.copy(alpha = 0.12f),
                spotColor    = KawaiiPurple.copy(alpha = 0.10f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pill label
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(KawaiiPink.copy(alpha = glowAlpha), KawaiiPurple.copy(alpha = glowAlpha))
                    )
                )
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Your secret code ✨",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
                fontSize   = 12.sp,
                color      = Color.White
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6 code bubbles
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            myCode.forEachIndexed { idx, char ->
                CodeBubble(char = char, animDelay = idx * 60)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Share this with your person 💌",
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Normal,
            fontSize   = 13.sp,
            color      = KawaiiTextSec
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Action row — copy + share
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Copy
            OutlinedButton(
                onClick = { clipboard.setText(AnnotatedString(myCode)) },
                modifier = Modifier.weight(1f),
                shape  = RoundedCornerShape(50.dp),
                border = BorderStroke(1.5.dp, KawaiiCodeBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor     = KawaiiTextSec,
                    containerColor   = Color.Transparent
                )
            ) {
                Icon(Icons.Filled.ContentCopy, null, Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Copy",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp
                )
            }

            // Share
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

// ════════════════════════════════════════════════════════════════════════════
// Join Card
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape     = RoundedCornerShape(28.dp),
                ambientColor = KawaiiPurple.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.88f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Join someone's room",
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp,
            color      = KawaiiTextPri
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter their 6-character code below",
            fontFamily = NunitoFamily,
            fontSize   = 13.sp,
            color      = KawaiiTextSec
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

        Spacer(modifier = Modifier.height(20.dp))

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
                fontSize   = 13.sp,
                color      = KawaiiPink
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// Individual join slot — unchanged logic
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
    val bgColor     = if (isFocused) KawaiiInputFocus else KawaiiInputBg
    val borderColor = if (isFocused) KawaiiPink else KawaiiCodeBorder

    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 54.dp)
            .shadow(
                elevation  = if (isFocused) 4.dp else 0.dp,
                shape      = RoundedCornerShape(14.dp),
                spotColor  = KawaiiPink.copy(alpha = 0.2f)
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
                fontSize   = 22.sp,
                color      = KawaiiTextPri,
                modifier   = Modifier.scale(charScale)
            )
        } else if (isFocused) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(22.dp)
                    .background(KawaiiPink)
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
