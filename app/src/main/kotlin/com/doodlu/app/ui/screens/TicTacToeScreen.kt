package com.celestial.spire.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.celestial.spire.model.TicTacToeState
import com.celestial.spire.sync.SyncManager
import com.celestial.spire.ui.components.*
import com.celestial.spire.ui.theme.*

@Composable
fun TicTacToeScreen(onBackToDrawing: () -> Unit) {
    var tttState     by remember { mutableStateOf(TicTacToeState()) }
    val mySymbol     by SyncManager.mySymbol.collectAsState()
    val connState    by SyncManager.connectionState.collectAsState()
    val playerCount  by SyncManager.playerCount.collectAsState()
    var showConfetti by remember { mutableStateOf(false) }
    var prevWinner   by remember { mutableStateOf<String?>(null) }
    val isConnected  = connState == com.celestial.spire.sync.ConnectionState.CONNECTED

    // ── Safe Navigation State Handling ────────────────────────────
    var hasNavigatedBack by remember { mutableStateOf(false) }
    
    val handleBack = {
        if (!hasNavigatedBack) {
            hasNavigatedBack = true
            SyncManager.sendSwitchMode("whiteboard")
            onBackToDrawing()
        }
    }

    // ── Server-driven navigation: react only to explicit switchmode broadcasts ─
    // Using modeSwitchEvent (SharedFlow) avoids reacting to stale init state.
    LaunchedEffect(Unit) {
        SyncManager.modeSwitchEvent.collect { mode ->
            if (!hasNavigatedBack) {
                when (mode) {
                    "whiteboard",
                    "kicked"    -> {
                        hasNavigatedBack = true
                        onBackToDrawing()
                    }
                }
            }
        }
    }

    // Handle system back button / gesture
    BackHandler {
        handleBack()
    }

    DisposableEffect(Unit) {
        val gsListener = object : SyncManager.GameStateListener {
            override fun onGameState(state: TicTacToeState) {
                tttState = state
                if (state.winner != null && state.winner != prevWinner) {
                    showConfetti = true
                    prevWinner = state.winner
                }
            }
        }
        SyncManager.addGameStateListener(gsListener)
        onDispose {
            SyncManager.removeGameStateListener(gsListener)
        }
    }

    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            kotlinx.coroutines.delay(3500)
            showConfetti = false
        }
    }
    LaunchedEffect(tttState.board) {
        if (tttState.board.all { it == null }) prevWinner = null
    }

    // Determine whose turn
    val xCount = tttState.board.count { it == "X" }
    val oCount = tttState.board.count { it == "O" }
    val isMyTurn = when {
        tttState.winner != null || tttState.draw -> false
        mySymbol == "X" -> xCount == oCount
        else -> oCount < xCount
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(kawaiiBgGradient)
    ) {
        FloatingParticles(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ───────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(KawaiiCard)
                        .clickable(
                            interactionSource = remember {
                                androidx.compose.foundation.interaction.MutableInteractionSource()
                            },
                            indication = null
                        ) {
                            handleBack()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = KawaiiTextSec,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Tic-Tac-Toe 🎮",
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = KawaiiTextPri
                    )
                }

                KawaiiConnectionBadge(
                    isConnected = isConnected,
                    playerCount = playerCount
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // ── Player indicators ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerBadge(
                    symbol = "X",
                    label = if (mySymbol == "X") "You" else "Them",
                    isActive = isMyTurn == (mySymbol == "X"),
                    color = KawaiiPink
                )

                Text("vs", fontFamily = NunitoFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = KawaiiTextSec)

                PlayerBadge(
                    symbol = "O",
                    label = if (mySymbol == "O") "You" else "Them",
                    isActive = isMyTurn == (mySymbol == "O"),
                    color = KawaiiPurple
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Turn indicator
            AnimatedContent(
                targetState = when {
                    tttState.winner != null -> if (tttState.winner == mySymbol) "You won! 🎉🎉🎉" else "They got you this time 😅"
                    tttState.draw -> "Great minds think alike 🤝"
                    isMyTurn -> "Your turn 🎯"
                    else -> "Their turn 💭"
                },
                transitionSpec = {
                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                },
                label = "turn"
            ) { text ->
                Text(
                    text = text,
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = KawaiiTextPri
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Game Board ────────────────────────────────────────────────
            KawaiiTicTacToeBoard(
                state = tttState,
                mySymbol = mySymbol,
                onSquareTapped = { sq -> SyncManager.sendMove(sq) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Game-over buttons ─────────────────────────────────────────
            AnimatedVisibility(
                visible = tttState.winner != null || tttState.draw,
                enter = fadeIn() + scaleIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KawaiiPrimaryButton(
                        text = "Play Again",
                        emoji = "🔄",
                        modifier = Modifier.fillMaxWidth(0.75f),
                        onClick = { SyncManager.sendNewGame() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            KawaiiSecondaryButton(
                text = "Back to Doodlu",
                emoji = "✏️",
                modifier = Modifier.fillMaxWidth(0.75f),
                onClick = {
                    handleBack()
                }
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        // ── Confetti overlay ───────────────────────────────────────────────
        if (showConfetti) {
            KawaiiConfettiOverlay(isWinner = tttState.winner == mySymbol)
        }
    }
}

// ── Player badge ───────────────────────────────────────────────────────────
@Composable
fun PlayerBadge(symbol: String, label: String, isActive: Boolean, color: Color) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "badge"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .shadow(
                    elevation = if (isActive) 8.dp else 2.dp,
                    shape = CircleShape,
                    spotColor = color.copy(alpha = 0.3f)
                )
                .clip(CircleShape)
                .background(if (isActive) color else KawaiiCard)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                symbol,
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = if (isActive) Color.White else color
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = if (isActive) color else KawaiiTextSec
        )
    }
}

// ── Kawaii Tic-Tac-Toe Board ───────────────────────────────────────────────
@Suppress("UNUSED_PARAMETER")
@Composable
fun KawaiiTicTacToeBoard(
    state: TicTacToeState,
    mySymbol: String,
    onSquareTapped: (Int) -> Unit
) {
    val cellSize = 90.dp
    val winnerLine = state.winnerLine

    Box(
        modifier = Modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = KawaiiPurple.copy(alpha = 0.12f),
                spotColor = KawaiiPurple.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(KawaiiCard)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            (0..2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (0..2).forEach { col ->
                        val idx = row * 3 + col
                        val cell = state.board[idx]
                        val isWinCell = winnerLine?.contains(idx) == true
                        KawaiiCell(
                            symbol = cell,
                            isWinCell = isWinCell,
                            size = cellSize,
                            onClick = {
                                if (cell == null && state.winner == null && !state.draw) {
                                    onSquareTapped(idx)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KawaiiCell(
    symbol: String?,
    isWinCell: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    var appeared by remember(symbol) { mutableStateOf(symbol == null) }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cell_appear"
    )
    LaunchedEffect(symbol) {
        if (symbol != null) appeared = true
    }

    val bgColor by animateColorAsState(
        targetValue = when {
            isWinCell && symbol == "X" -> KawaiiPink.copy(alpha = 0.15f)
            isWinCell && symbol == "O" -> KawaiiPurple.copy(alpha = 0.15f)
            else -> KawaiiInputBg
        },
        animationSpec = tween(300),
        label = "cell_bg"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (isWinCell) 2.dp else 1.dp,
                color = if (isWinCell) {
                    if (symbol == "X") KawaiiPink else KawaiiPurple
                } else KawaiiCodeBorder.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (symbol != null) {
            // Draw X or O using Canvas for hand-drawn feel
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .size((size.value * 0.55f).dp)
                    .scale(scale)
            ) {
                val w = this.size.width
                val h = this.size.height
                val stroke = Stroke(
                    width = this.size.width * 0.12f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                if (symbol == "X") {
                    // Draw X — slightly wobbly
                    val path1 = Path().apply {
                        moveTo(w * 0.05f, h * 0.05f)
                        cubicTo(w * 0.3f, h * 0.2f, w * 0.6f, h * 0.75f, w * 0.95f, h * 0.95f)
                    }
                    val path2 = Path().apply {
                        moveTo(w * 0.95f, h * 0.05f)
                        cubicTo(w * 0.7f, h * 0.25f, w * 0.35f, h * 0.72f, w * 0.05f, h * 0.95f)
                    }
                    drawPath(path1, KawaiiPink, style = stroke)
                    drawPath(path2, KawaiiPink, style = stroke)
                } else {
                    // Draw O — slightly imperfect circle
                    val path = Path().apply {
                        addOval(
                            androidx.compose.ui.geometry.Rect(
                                left = w * 0.08f,
                                top = h * 0.08f,
                                right = w * 0.92f,
                                bottom = h * 0.92f
                            )
                        )
                    }
                    drawPath(path, KawaiiPurple, style = stroke)
                }
            }
        }
    }
}

// ── Confetti overlay ───────────────────────────────────────────────────────
@Composable
fun KawaiiConfettiOverlay(isWinner: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "confetti_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = KawaiiPink.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(KawaiiCard)
                .padding(horizontal = 48.dp, vertical = 36.dp)
                .scale(scale)
        ) {
            Text(
                text = if (isWinner) "🎉" else "😅",
                fontSize = 78.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isWinner) "You won!" else "They got you!",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                color = if (isWinner) KawaiiPink else KawaiiPurple
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (isWinner) "You're amazing 💕" else "Better luck next time 💪",
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = KawaiiTextSec
            )
        }
    }
}
