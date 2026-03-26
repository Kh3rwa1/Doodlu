package com.doodlu.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.doodlu.app.model.Stroke as DoodluStroke
import com.doodlu.app.sync.SyncManager
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

data class DrawPath(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val isEraser: Boolean = false,
    val isLocal: Boolean = true
)

@Composable
fun DrawingCanvas(
    strokes: List<DrawPath>,
    partnerCursor: Offset?,
    strokeColor: Color,
    strokeWidth: Float,
    isEraser: Boolean,
    canvasBgColor: Color = Color(0xFF1A1A2E),
    onStrokeComplete: (List<Offset>) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgHex = colorToHex(canvasBgColor)
    var currentPath by remember { mutableStateOf(listOf<Offset>()) }
    var lastSentIndex by remember { mutableStateOf(0) }

    // Partner cursor pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Canvas(
        modifier = modifier
            .pointerInput(strokeColor, strokeWidth, isEraser) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPath = listOf(offset)
                        lastSentIndex = 0
                        SyncManager.sendCursor(offset.x, offset.y)
                    },
                    onDrag = { change, _ ->
                        val newPoint = change.position
                        currentPath = currentPath + newPoint

                        // Send cursor
                        SyncManager.sendCursor(newPoint.x, newPoint.y)

                        // Batch-send points every 5
                        if (currentPath.size - lastSentIndex >= 5) {
                            val batch = currentPath.subList(lastSentIndex, currentPath.size)
                            SyncManager.sendStroke(
                                points = batch.map { Pair(it.x, it.y) },
                                color = if (isEraser) bgHex else colorToHex(strokeColor),
                                width = strokeWidth
                            )
                            // Overlap: keep last point as start of next batch so
                            // consecutive segments connect without gaps.
                            lastSentIndex = currentPath.size - 1
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        // Send remaining points (from overlap point onward)
                        if (lastSentIndex < currentPath.size) {
                            val batch = currentPath.subList(lastSentIndex, currentPath.size)
                            if (batch.size > 1) {
                                SyncManager.sendStroke(
                                    points = batch.map { Pair(it.x, it.y) },
                                    color = if (isEraser) bgHex else colorToHex(strokeColor),
                                    width = strokeWidth
                                )
                            }
                        }
                        onStrokeComplete(currentPath)
                        currentPath = emptyList()
                        lastSentIndex = 0
                    }
                )
            }
    ) {
        // Draw all completed strokes
        strokes.forEach { path ->
            drawPath(path)
        }

        // Draw current stroke in progress
        if (currentPath.size > 1) {
            drawIntoCanvas { canvas ->
                val androidPath = android.graphics.Path()
                androidPath.moveTo(currentPath[0].x, currentPath[0].y)
                for (i in 1 until currentPath.size) {
                    androidPath.lineTo(currentPath[i].x, currentPath[i].y)
                }
                val resolvedWidth = if (isEraser) strokeWidth * 2 else strokeWidth
                val nativePaint = android.graphics.Paint().apply {
                    style = android.graphics.Paint.Style.STROKE
                    this.color = (if (isEraser) canvasBgColor else strokeColor).toArgb()
                    this.strokeWidth = resolvedWidth
                    isAntiAlias = true
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                }
                canvas.nativeCanvas.drawPath(androidPath, nativePaint)
            }
        }

        // Draw partner cursor
        partnerCursor?.let { cursor ->
            // Outer glow ring
            drawCircle(
                color = Color(0xFFE94560).copy(alpha = pulseAlpha * 0.4f),
                radius = 12f * pulseScale,
                center = cursor
            )
            // Inner dot
            drawCircle(
                color = Color(0xFFE94560),
                radius = 8f,
                center = cursor
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = cursor
            )
        }
    }
}

private fun DrawScope.drawPath(path: DrawPath) {
    if (path.points.size < 2) return

    drawIntoCanvas { canvas ->
        val androidPath = android.graphics.Path()
        androidPath.moveTo(path.points[0].x, path.points[0].y)
        for (i in 1 until path.points.size) {
            androidPath.lineTo(path.points[i].x, path.points[i].y)
        }
        val paint = android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.STROKE
            color = path.color.toArgb()
            strokeWidth = path.width
            isAntiAlias = true
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
        canvas.nativeCanvas.drawPath(androidPath, paint)
    }
}

fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", (0xFFFFFF and argb))
}

fun hexToColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.White
    }
}
