package com.doodlu.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlu.app.data.PreferencesManager
import com.doodlu.app.sync.ConnectionState
import com.doodlu.app.sync.SyncManager
import com.doodlu.app.ui.components.*
import com.doodlu.app.ui.theme.*
import com.doodlu.app.util.buildWallpaperPickerIntent
import com.doodlu.app.util.isDoodluActiveWallpaper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    onNavigateToTicTacToe: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSetWallpaper: () -> Unit,
    onKicked: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs   = remember { PreferencesManager(context) }

    var strokes            by remember { mutableStateOf(listOf<DrawPath>()) }
    var selectedColorHex   by remember { mutableStateOf("#FF8A65") }
    var selectedColor      by remember { mutableStateOf(DrawColorCoral) }
    var strokeWidth        by remember { mutableStateOf(5f) }
    var isEraser           by remember { mutableStateOf(false) }

    // ── Navigate based on EXPLICIT server mode-switch events only ────────────
    // We intentionally use modeSwitchEvent (SharedFlow) instead of currentMode
    // (StateFlow) so that reconnect "init" messages with stale mode can never
    // trigger navigation. Only a real "switchmode" broadcast moves us.
    LaunchedEffect(Unit) {
        SyncManager.modeSwitchEvent.collect { mode ->
            when (mode) {
                "tictactoe" -> onNavigateToTicTacToe()
                "kicked"    -> {
                    SyncManager.disconnect()
                    prefs.clearRoom()
                    onKicked()
                }
                // "whiteboard": we're already here — no action needed
            }
        }
    }

    // Canvas state
    var partnerCursor      by remember { mutableStateOf<Offset?>(null) }
    var showMenu           by remember { mutableStateOf(false) }
    var showClearDialog    by remember { mutableStateOf(false) }

    // ── Wallpaper banner state ─────────────────────────────────────────────
    // Check on composition + re-check on resume
    var doodluIsWallpaper  by remember { mutableStateOf(isDoodluActiveWallpaper(context)) }
    var bannerDismissed    by remember { mutableStateOf(false) }
    val showBanner = !doodluIsWallpaper && !bannerDismissed

    // Re-check wallpaper status when user returns from picker
    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        doodluIsWallpaper = isDoodluActiveWallpaper(context)
    }

    val connectionState by SyncManager.connectionState.collectAsState()
    val playerCount     by SyncManager.playerCount.collectAsState()
    val isConnected = connectionState == ConnectionState.CONNECTED

    // Listeners
    DisposableEffect(Unit) {
        val strokeListener = object : SyncManager.StrokeListener {
            override fun onStroke(stroke: com.doodlu.app.model.Stroke) {
                val points = stroke.points.map { (x, y) -> Offset(x, y) }
                strokes = strokes + DrawPath(points, hexToColor(stroke.color), stroke.width, isLocal = false)
            }
        }
        val cursorListener = object : SyncManager.CursorListener {
            override fun onCursor(userId: String, x: Float, y: Float) {
                if (userId != SyncManager.myUserId.value) partnerCursor = Offset(x, y)
            }
        }
        val canvasListener = object : SyncManager.CanvasListener {
            override fun onClearCanvas() { strokes = emptyList() }
        }
        SyncManager.addStrokeListener(strokeListener)
        SyncManager.addCursorListener(cursorListener)
        SyncManager.addCanvasListener(canvasListener)
        onDispose {
            SyncManager.removeStrokeListener(strokeListener)
            SyncManager.removeCursorListener(cursorListener)
            SyncManager.removeCanvasListener(canvasListener)
        }
    }

    // Clear canvas dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = KawaiiCard,
            title = {
                Text(
                    "Clear Canvas? 🗑️",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    color = KawaiiTextPri
                )
            },
            text = {
                Text(
                    "This clears it for everyone in the room.",
                    fontFamily = NunitoFamily,
                    color = KawaiiTextSec
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    SyncManager.sendClearCanvas()
                    strokes = emptyList()
                    showClearDialog = false
                }) {
                    Text(
                        "Clear 🗑️",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        color = KawaiiRed
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(
                        "Cancel",
                        fontFamily = NunitoFamily,
                        color = KawaiiTextSec
                    )
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg)
    ) {
        // ── Full-screen dark drawing canvas ───────────────────────────────
        DrawingCanvas(
            strokes = strokes,
            partnerCursor = partnerCursor,
            strokeColor = selectedColor,
            strokeWidth = strokeWidth,
            isEraser = isEraser,
            onStrokeComplete = { points ->
                if (points.isNotEmpty()) {
                    strokes = strokes + DrawPath(
                        points = points.map { Offset(it.x, it.y) },
                        color = if (isEraser) Color(0xFF1A1A2E) else selectedColor,
                        width = if (isEraser) strokeWidth * 2 else strokeWidth
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // ── Top bar (semi-transparent frosted) ───────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Mini Doodlu logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(KawaiiCard.copy(alpha = 0.85f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Doodlu",
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = KawaiiPink
                )
                Text(" ❤️", fontSize = 16.sp)
            }

            // Center: connection indicator
            KawaiiConnectionBadge(
                isConnected = isConnected,
                playerCount = playerCount
            )

            // Right side: mode toggle + menu
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Game mode toggle pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(KawaiiCard.copy(alpha = 0.85f))
                        .clickable {
                            SyncManager.sendSwitchMode("tictactoe")
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "🎮 Play",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = KawaiiTextSec
                    )
                }

                // Overflow menu
                Box {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(KawaiiCard.copy(alpha = 0.85f))
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Menu",
                            tint = KawaiiTextSec,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = KawaiiCard,
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Set as Live Wallpaper",
                                    fontFamily = NunitoFamily,
                                    color = KawaiiTextPri
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Wallpaper, null, tint = KawaiiPink)
                            },
                            onClick = { showMenu = false; onSetWallpaper() }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Settings",
                                    fontFamily = NunitoFamily,
                                    color = KawaiiTextPri
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Settings, null, tint = KawaiiTextSec)
                            },
                            onClick = { showMenu = false; onNavigateToSettings() }
                        )
                    }
                }
            }
        }

        // ── Bottom floating toolbar ───────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Wallpaper promo banner (shown until set or dismissed) ─────
            AnimatedVisibility(
                visible = showBanner,
                enter = slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) { it } + fadeIn(),
                exit = slideOutVertically(tween(280)) { it } + fadeOut(tween(200))
            ) {
                WallpaperPromoBanner(
                    onSetNow = {
                        val intent = buildWallpaperPickerIntent(context)
                        wallpaperLauncher.launch(intent)
                    },
                    onDismiss = { bannerDismissed = true }
                )
            }

            // Color picker row
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(50.dp),
                        ambientColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .clip(RoundedCornerShape(50.dp))
                    .background(KawaiiCard)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DrawingColorsCompose.zip(DrawingColors).forEach { (color, hex) ->
                    val isSelected = !isEraser && selectedColorHex == hex
                    val circleScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.25f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "color_scale_$hex"
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .scale(circleScale)
                            .shadow(
                                elevation = if (isSelected) 4.dp else 0.dp,
                                shape = CircleShape,
                                spotColor = color.copy(alpha = 0.4f)
                            )
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(
                                    2.5.dp, KawaiiCard, CircleShape
                                ) else Modifier
                            )
                            .clickable(
                                interactionSource = remember {
                                    androidx.compose.foundation.interaction.MutableInteractionSource()
                                },
                                indication = null
                            ) {
                                selectedColorHex = hex
                                selectedColor = color
                                isEraser = false
                            }
                    )
                }
            }

            // Tools row
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(50.dp),
                        ambientColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .clip(RoundedCornerShape(50.dp))
                    .background(KawaiiCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Brush sizes (3 presets)
                listOf(3f to "·", 6f to "●", 10f to "◉").forEach { (size, icon) ->
                    val isSelected = !isEraser && strokeWidth == size
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) KawaiiCodeBg else Color.Transparent
                            )
                            .clickable(
                                interactionSource = remember {
                                    androidx.compose.foundation.interaction.MutableInteractionSource()
                                },
                                indication = null
                            ) {
                                strokeWidth = size
                                isEraser = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = icon,
                            fontSize = when (size) {
                                3f -> 10.sp
                                6f -> 16.sp
                                else -> 22.sp
                            },
                            color = if (isSelected) KawaiiPink else KawaiiTextSec
                        )
                    }
                }

                // Divider
                Box(
                    Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(KawaiiCodeBorder.copy(alpha = 0.3f))
                )

                // Eraser
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isEraser) KawaiiCodeBg else Color.Transparent)
                        .clickable(
                            interactionSource = remember {
                                androidx.compose.foundation.interaction.MutableInteractionSource()
                            },
                            indication = null
                        ) { isEraser = !isEraser },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⬜", fontSize = 16.sp)
                }

                // Undo
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember {
                                androidx.compose.foundation.interaction.MutableInteractionSource()
                            },
                            indication = null
                        ) {
                            // Only undo the last stroke drawn by the local user
                            val lastLocalIdx = strokes.indexOfLast { it.isLocal }
                            if (lastLocalIdx >= 0) strokes = strokes.toMutableList().also { it.removeAt(lastLocalIdx) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = KawaiiTextSec,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Clear
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember {
                                androidx.compose.foundation.interaction.MutableInteractionSource()
                            },
                            indication = null
                        ) { showClearDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Clear",
                        tint = KawaiiRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Persistent wallpaper promo banner ─────────────────────────────────────
@Composable
private fun WallpaperPromoBanner(
    onSetNow: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = KawaiiPink.copy(alpha = 0.12f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFFFE8EC))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Promo text (takes all remaining space)
        Text(
            text = "Set as wallpaper to see doodles on your lock screen 💕",
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = Color(0xFF2D2D3A),
            modifier = Modifier.weight(1f),
            lineHeight = 18.sp
        )

        // "Set now" pill button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(KawaiiPink)
                .clickable(
                    interactionSource = remember {
                        androidx.compose.foundation.interaction.MutableInteractionSource()
                    },
                    indication = null,
                    onClick = onSetNow
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Set now",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
            )
        }

        // Dismiss X
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember {
                        androidx.compose.foundation.interaction.MutableInteractionSource()
                    },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Dismiss",
                tint = Color(0xFF8E8EA0),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
