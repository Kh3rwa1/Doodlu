package com.doodlu.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlu.app.model.TicTacToeState
import com.doodlu.app.sync.SyncManager
import com.doodlu.app.ui.components.ConnectionIndicator
import com.doodlu.app.ui.components.TicTacToeBoard
import com.doodlu.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeScreen(
    onBackToDrawing: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var tttState by remember { mutableStateOf(TicTacToeState()) }
    val mySymbol by SyncManager.mySymbol.collectAsState()
    val connectionState by SyncManager.connectionState.collectAsState()
    val playerCount by SyncManager.playerCount.collectAsState()

    // Confetti state
    var showConfetti by remember { mutableStateOf(false) }
    var prevWinner by remember { mutableStateOf<String?>(null) }

    // Register game state listener
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
        val modeListener = object : SyncManager.ModeListener {
            override fun onModeSwitch(mode: String) {
                if (mode == "whiteboard") {
                    onBackToDrawing()
                }
            }
        }
        SyncManager.addGameStateListener(gsListener)
        SyncManager.addModeListener(modeListener)
        onDispose {
            SyncManager.removeGameStateListener(gsListener)
            SyncManager.removeModeListener(modeListener)
        }
    }

    // Confetti auto-hide
    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            kotlinx.coroutines.delay(3000)
            showConfetti = false
        }
    }

    LaunchedEffect(tttState.board) {
        if (tttState.board.all { it == null }) {
            prevWinner = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        SyncManager.sendSwitchMode("whiteboard")
                        onBackToDrawing()
                    }
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back to Doodlu",
                        tint = DoodluTextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Tic-Tac-Toe",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DoodluTextPrimary
                    )
                    Text(
                        text = "You are ${mySymbol}",
                        fontSize = 12.sp,
                        color = if (mySymbol == "X") Color(0xFFE94560) else Color(0xFF118AB2)
                    )
                }

                ConnectionIndicator(
                    state = connectionState,
                    playerCount = playerCount
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main board
            TicTacToeBoard(
                state = tttState,
                mySymbol = mySymbol,
                onSquareTapped = { square ->
                    SyncManager.sendMove(square)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Game over buttons
            AnimatedVisibility(
                visible = tttState.winner != null || tttState.draw,
                enter = fadeIn() + scaleIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { SyncManager.sendNewGame() },
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DoodluSuccess
                        ),
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text(
                            "Play Again",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A2E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to doodle button
            OutlinedButton(
                onClick = {
                    SyncManager.sendSwitchMode("whiteboard")
                    onBackToDrawing()
                },
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DoodluTextSecondary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, DoodluSurfaceVariant),
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Back to Doodlu")
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // Confetti overlay
        if (showConfetti) {
            ConfettiOverlay(
                isWinner = tttState.winner == mySymbol
            )
        }
    }
}

@Composable
fun ConfettiOverlay(isWinner: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(DoodluSurface.copy(alpha = 0.95f))
                .padding(32.dp)
                .scale(scale)
        ) {
            Text(
                text = if (isWinner) "🎉" else "😅",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isWinner) "You won!" else "They got you!",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isWinner) DoodluSuccess else DoodluPrimary
            )
        }
    }
}
