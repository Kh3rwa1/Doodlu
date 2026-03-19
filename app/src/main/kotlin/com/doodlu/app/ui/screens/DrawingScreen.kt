package com.doodlu.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlu.app.sync.ConnectionState
import com.doodlu.app.sync.SyncManager
import com.doodlu.app.ui.components.*
import com.doodlu.app.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    onNavigateToTicTacToe: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSetWallpaper: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var strokes by remember { mutableStateOf(listOf<DrawPath>()) }
    var strokeHistory by remember { mutableStateOf(listOf<DrawPath>()) }
    var selectedColorHex by remember { mutableStateOf("#FFFFFF") }
    var selectedColor by remember { mutableStateOf(Color.White) }
    var strokeWidth by remember { mutableStateOf(6f) }
    var isEraser by remember { mutableStateOf(false) }
    var partnerCursor by remember { mutableStateOf<Offset?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var partnerDrawingToast by remember { mutableStateOf(false) }

    val connectionState by SyncManager.connectionState.collectAsState()
    val playerCount by SyncManager.playerCount.collectAsState()

    // Register stroke listener
    DisposableEffect(Unit) {
        val strokeListener = object : SyncManager.StrokeListener {
            override fun onStroke(stroke: com.doodlu.app.model.Stroke) {
                val points = stroke.points.map { (x, y) -> Offset(x, y) }
                val color = hexToColor(stroke.color)
                val path = DrawPath(points, color, stroke.width)
                strokes = strokes + path
                strokeHistory = strokes
                partnerDrawingToast = true
            }
        }
        val cursorListener = object : SyncManager.CursorListener {
            override fun onCursor(userId: String, x: Float, y: Float) {
                if (userId != SyncManager.myUserId.value) {
                    partnerCursor = Offset(x, y)
                }
            }
        }
        val canvasListener = object : SyncManager.CanvasListener {
            override fun onClearCanvas() {
                strokes = emptyList()
                strokeHistory = emptyList()
            }
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

    // Toast for partner drawing
    LaunchedEffect(partnerDrawingToast) {
        if (partnerDrawingToast) {
            Toast.makeText(context, "Partner is drawing...", Toast.LENGTH_SHORT).show()
            partnerDrawingToast = false
        }
    }

    // Mode switch listener
    DisposableEffect(Unit) {
        val modeListener = object : SyncManager.ModeListener {
            override fun onModeSwitch(mode: String) {
                if (mode == "tictactoe") {
                    onNavigateToTicTacToe()
                }
            }
        }
        SyncManager.addModeListener(modeListener)
        onDispose { SyncManager.removeModeListener(modeListener) }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = DoodluSurface,
            title = {
                Text("Clear Canvas?", color = DoodluTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will clear the canvas for everyone in the room.",
                    color = DoodluTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        SyncManager.sendClearCanvas()
                        strokes = emptyList()
                        strokeHistory = emptyList()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear", color = DoodluPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = DoodluTextSecondary)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        // Full screen drawing canvas
        DrawingCanvas(
            strokes = strokes,
            partnerCursor = partnerCursor,
            strokeColor = selectedColor,
            strokeWidth = strokeWidth,
            isEraser = isEraser,
            onStrokeComplete = { points ->
                if (points.isNotEmpty()) {
                    val path = DrawPath(
                        points = points.map { Offset(it.x, it.y) },
                        color = if (isEraser) Color(0xFF1A1A2E) else selectedColor,
                        width = if (isEraser) strokeWidth * 2 else strokeWidth
                    )
                    strokes = strokes + path
                    strokeHistory = strokes
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo
            Text(
                text = "Doodlu",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DoodluTextPrimary
            )

            // Right side
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConnectionIndicator(
                    state = connectionState,
                    playerCount = playerCount
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true }
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Menu",
                            tint = DoodluTextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = DoodluSurface,
                        modifier = Modifier.background(DoodluSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Set as Live Wallpaper", color = DoodluTextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Filled.Wallpaper, null, tint = DoodluPrimary)
                            },
                            onClick = {
                                showMenu = false
                                onSetWallpaper()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Switch to Tic-Tac-Toe", color = DoodluTextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Filled.GridOn, null, tint = DoodluSecondary)
                            },
                            onClick = {
                                showMenu = false
                                SyncManager.sendSwitchMode("tictactoe")
                                onNavigateToTicTacToe()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings", color = DoodluTextPrimary) },
                            leadingIcon = {
                                Icon(Icons.Filled.Settings, null, tint = DoodluTextSecondary)
                            },
                            onClick = {
                                showMenu = false
                                onNavigateToSettings()
                            }
                        )
                    }
                }
            }
        }

        // Bottom toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Color picker row
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(DoodluSurface.copy(alpha = 0.9f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ColorPicker(
                    selectedColor = if (isEraser) "" else selectedColorHex,
                    onColorSelected = { hex, color ->
                        selectedColorHex = hex
                        selectedColor = color
                        isEraser = false
                    }
                )
            }

            // Tool row
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(DoodluSurface.copy(alpha = 0.9f))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stroke width slider
                Text(
                    text = "✏️",
                    fontSize = 16.sp
                )
                Slider(
                    value = strokeWidth,
                    onValueChange = { strokeWidth = it },
                    valueRange = 2f..12f,
                    modifier = Modifier.width(100.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = DoodluPrimary,
                        activeTrackColor = DoodluPrimary,
                        inactiveTrackColor = DoodluSurfaceVariant
                    )
                )

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(DoodluSurfaceVariant)
                )

                // Eraser
                IconButton(
                    onClick = { isEraser = !isEraser },
                    modifier = Modifier
                        .size(36.dp)
                        .then(
                            if (isEraser) Modifier
                                .clip(CircleShape)
                                .background(DoodluPrimary.copy(alpha = 0.2f))
                            else Modifier
                        )
                ) {
                    Text(text = "⬜", fontSize = 18.sp)
                }

                // Undo
                IconButton(
                    onClick = {
                        if (strokes.isNotEmpty()) {
                            strokes = strokes.dropLast(1)
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Undo,
                        contentDescription = "Undo",
                        tint = DoodluTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Clear
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Clear",
                        tint = DoodluTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
