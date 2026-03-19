package com.doodlu.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlu.app.model.TicTacToeState

@Composable
fun TicTacToeBoard(
    state: TicTacToeState,
    mySymbol: String,
    onSquareTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val xColor = Color(0xFFE94560)
    val oColor = Color(0xFF118AB2)
    val gridColor = Color(0xFF8892B0)
    val glowColor = Color(0xFF16213E)

    // Track animated squares
    val animatedSquares = remember { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }

    LaunchedEffect(state.board) {
        state.board.forEachIndexed { index, value ->
            if (value != null && !animatedSquares.containsKey(index)) {
                val anim = Animatable(0f)
                animatedSquares[index] = anim
                anim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
        // Clean up removed squares (new game)
        val keysToRemove = animatedSquares.keys.filter { state.board[it] == null }
        keysToRemove.forEach { animatedSquares.remove(it) }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status text
        val statusText = when {
            state.winner != null -> if (state.winner == mySymbol) "You win! 🎉" else "They win!"
            state.draw -> "It's a draw!"
            state.turn == mySymbol -> "Your turn"
            else -> "Their turn..."
        }
        val statusColor = when {
            state.winner == mySymbol -> Color(0xFF06D6A0)
            state.winner != null -> Color(0xFFFF6B35)
            state.draw -> Color(0xFFFFC947)
            state.turn == mySymbol -> xColor
            else -> oColor
        }

        Text(
            text = statusText,
            color = statusColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Grid
        Box(
            modifier = Modifier
                .size(300.dp)
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSize = size.width / 3f
                val lineColor = gridColor

                // Draw grid lines
                for (i in 1..2) {
                    // Vertical
                    drawLine(
                        color = lineColor,
                        start = Offset(cellSize * i, 0f),
                        end = Offset(cellSize * i, size.height),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                    // Horizontal
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, cellSize * i),
                        end = Offset(size.width, cellSize * i),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }

                // Draw X and O
                state.board.forEachIndexed { index, value ->
                    if (value == null) return@forEachIndexed
                    val scale = animatedSquares[index]?.value ?: 1f

                    val col = index % 3
                    val row = index / 3
                    val cx = cellSize * col + cellSize / 2
                    val cy = cellSize * row + cellSize / 2
                    val padding = cellSize * 0.2f * scale
                    val actualSize = (cellSize - padding * 2) * scale

                    if (value == "X") {
                        val color = xColor
                        val strokeW = 8f
                        val x1 = cx - actualSize / 2
                        val y1 = cy - actualSize / 2
                        val x2 = cx + actualSize / 2
                        val y2 = cy + actualSize / 2
                        // X glow
                        drawLine(color.copy(alpha = 0.3f), Offset(x1, y1), Offset(x2, y2), strokeW + 6, StrokeCap.Round)
                        drawLine(color.copy(alpha = 0.3f), Offset(x2, y1), Offset(x1, y2), strokeW + 6, StrokeCap.Round)
                        // X solid
                        drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeW, StrokeCap.Round)
                        drawLine(color, Offset(x2, y1), Offset(x1, y2), strokeW, StrokeCap.Round)
                    } else if (value == "O") {
                        val color = oColor
                        val radius = actualSize / 2
                        val strokeW = 8f
                        // O glow
                        drawCircle(color.copy(alpha = 0.3f), radius = radius + 3f, center = Offset(cx, cy), style = Stroke(strokeW + 6))
                        // O solid
                        drawCircle(color, radius = radius, center = Offset(cx, cy), style = Stroke(strokeW))
                    }
                }

                // Winning line highlight
                if (state.winner != null) {
                    val winningLine = findWinningLine(state.board)
                    winningLine?.let { line ->
                        val (i0, i1, i2) = line
                        val c0x = (i0 % 3) * cellSize + cellSize / 2
                        val c0y = (i0 / 3) * cellSize + cellSize / 2
                        val c2x = (i2 % 3) * cellSize + cellSize / 2
                        val c2y = (i2 / 3) * cellSize + cellSize / 2
                        val winColor = if (state.winner == "X") xColor else oColor
                        drawLine(
                            color = winColor.copy(alpha = 0.7f),
                            start = Offset(c0x, c0y),
                            end = Offset(c2x, c2y),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Invisible click targets
            val canMove = state.winner == null && !state.draw && state.turn == mySymbol
            for (i in 0 until 9) {
                val col = i % 3
                val row = i / 3
                Box(
                    modifier = Modifier
                        .fillMaxSize(1f / 3f)
                        .offset(
                            x = (col * 300 / 3).dp,
                            y = (row * 300 / 3).dp
                        )
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (canMove && state.board[i] == null)
                                Modifier.clickable { onSquareTapped(i) }
                            else Modifier
                        )
                )
            }
        }
    }
}

private fun findWinningLine(board: List<String?>): Triple<Int, Int, Int>? {
    val lines = listOf(
        Triple(0, 1, 2), Triple(3, 4, 5), Triple(6, 7, 8),
        Triple(0, 3, 6), Triple(1, 4, 7), Triple(2, 5, 8),
        Triple(0, 4, 8), Triple(2, 4, 6)
    )
    return lines.firstOrNull { (a, b, c) ->
        board[a] != null && board[a] == board[b] && board[b] == board[c]
    }
}
