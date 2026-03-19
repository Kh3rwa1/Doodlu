package com.doodlu.app.wallpaper

import android.graphics.*
import android.os.Handler
import android.os.HandlerThread
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.doodlu.app.data.PreferencesManager
import com.doodlu.app.model.Stroke
import com.doodlu.app.model.TicTacToeState
import com.doodlu.app.sync.ConnectionState
import com.doodlu.app.sync.SyncManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class DoodluWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DoodluEngine()

    inner class DoodluEngine : Engine() {
        private val TAG = "DoodluWallpaperEngine"

        // Handler thread for non-main rendering
        private val renderThread = HandlerThread("DoodluRender").also { it.start() }
        private val renderHandler = Handler(renderThread.looper)

        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        // Canvas state
        private val strokes = mutableListOf<Stroke>()
        private var partnerX = -1f
        private var partnerY = -1f
        private var currentMode = "whiteboard"
        private var tttState = TicTacToeState()

        // Touch state for drawing
        private val currentPoints = mutableListOf<Pair<Float, Float>>()
        private var lastSentIndex = 0

        // Dirty flag — only redraw when needed
        @Volatile
        private var needsRedraw = false

        // Paints
        private val bgPaint = Paint().apply {
            color = Color.parseColor("#1A1A2E")
            style = Paint.Style.FILL
        }
        private val gridPaint = Paint().apply {
            color = Color.parseColor("#8892B0")
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }
        private val xPaint = Paint().apply {
            color = Color.parseColor("#E94560")
            strokeWidth = 8f
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
        }
        private val oPaint = Paint().apply {
            color = Color.parseColor("#118AB2")
            strokeWidth = 8f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        private val cursorPaint = Paint().apply {
            color = Color.parseColor("#E94560")
            isAntiAlias = true
        }
        private val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        // Listeners
        private val strokeListener = object : SyncManager.StrokeListener {
            override fun onStroke(stroke: Stroke) {
                strokes.add(stroke)
                scheduleRedraw()
            }
        }
        private val cursorListener = object : SyncManager.CursorListener {
            override fun onCursor(userId: String, x: Float, y: Float) {
                if (userId != SyncManager.myUserId.value) {
                    partnerX = x
                    partnerY = y
                    scheduleRedraw()
                }
            }
        }
        private val modeListener = object : SyncManager.ModeListener {
            override fun onModeSwitch(mode: String) {
                currentMode = mode
                scheduleRedraw()
            }
        }
        private val gameStateListener = object : SyncManager.GameStateListener {
            override fun onGameState(state: TicTacToeState) {
                tttState = state
                scheduleRedraw()
            }
        }
        private val canvasListener = object : SyncManager.CanvasListener {
            override fun onClearCanvas() {
                strokes.clear()
                scheduleRedraw()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            registerListeners()

            // Load room info and connect if not already connected
            scope.launch {
                val prefs = PreferencesManager(this@DoodluWallpaperService)
                val roomId = prefs.roomId.first()
                val userId = prefs.userId.first()
                if (!roomId.isNullOrEmpty() && !userId.isNullOrEmpty()) {
                    if (SyncManager.connectionState.value != ConnectionState.CONNECTED) {
                        SyncManager.connect(roomId, userId)
                    }
                }
                // Sync current mode
                currentMode = SyncManager.currentMode.value
                tttState = SyncManager.gameState.value.tictactoe
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            unregisterListeners()
            scope.cancel()
            renderThread.quitSafely()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                // Reconnect on becoming visible
                scope.launch {
                    val prefs = PreferencesManager(this@DoodluWallpaperService)
                    val roomId = prefs.roomId.first()
                    val userId = prefs.userId.first()
                    if (!roomId.isNullOrEmpty() && !userId.isNullOrEmpty()) {
                        SyncManager.resumeConnection()
                    }
                }
                scheduleRedraw()
            } else {
                SyncManager.pauseConnection()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            scheduleRedraw()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            scheduleRedraw()
        }

        override fun onTouchEvent(event: MotionEvent) {
            when (currentMode) {
                "whiteboard" -> handleDrawTouch(event)
                "tictactoe" -> handleTttTouch(event)
            }
        }

        private fun handleDrawTouch(event: MotionEvent) {
            val x = event.x
            val y = event.y
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    currentPoints.clear()
                    currentPoints.add(Pair(x, y))
                    lastSentIndex = 0
                    SyncManager.sendCursor(x, y)
                }
                MotionEvent.ACTION_MOVE -> {
                    currentPoints.add(Pair(x, y))
                    SyncManager.sendCursor(x, y)
                    if (currentPoints.size - lastSentIndex >= 5) {
                        val batch = currentPoints.subList(lastSentIndex, currentPoints.size).toList()
                        SyncManager.sendStroke(batch, "#FFFFFF", 4f)
                        lastSentIndex = currentPoints.size
                    }
                    // Draw current stroke locally
                    val localStroke = Stroke(currentPoints.toList(), "#FFFFFF", 4f)
                    scheduleRedraw()
                }
                MotionEvent.ACTION_UP -> {
                    if (lastSentIndex < currentPoints.size) {
                        val remaining = currentPoints.subList(lastSentIndex, currentPoints.size).toList()
                        if (remaining.isNotEmpty()) {
                            SyncManager.sendStroke(remaining, "#FFFFFF", 4f)
                        }
                    }
                    strokes.add(Stroke(currentPoints.toList(), "#FFFFFF", 4f))
                    currentPoints.clear()
                    lastSentIndex = 0
                    scheduleRedraw()
                }
            }
        }

        private fun handleTttTouch(event: MotionEvent) {
            if (event.action != MotionEvent.ACTION_UP) return
            if (tttState.winner != null || tttState.draw) return
            if (tttState.turn != SyncManager.mySymbol.value) return

            val holder = surfaceHolder
            val width = holder.surfaceFrame.width().toFloat()
            val height = holder.surfaceFrame.height().toFloat()
            val gridSize = minOf(width, height) * 0.8f
            val startX = (width - gridSize) / 2
            val startY = (height - gridSize) / 2
            val cellSize = gridSize / 3f

            val col = ((event.x - startX) / cellSize).toInt()
            val row = ((event.y - startY) / cellSize).toInt()

            if (col in 0..2 && row in 0..2) {
                val square = row * 3 + col
                if (tttState.board[square] == null) {
                    SyncManager.sendMove(square)
                }
            }
        }

        private fun scheduleRedraw() {
            needsRedraw = true
            renderHandler.removeCallbacksAndMessages(null)
            renderHandler.post { drawFrame() }
        }

        private fun drawFrame() {
            if (!needsRedraw) return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    when (currentMode) {
                        "tictactoe" -> drawTicTacToe(canvas)
                        else -> drawWhiteboard(canvas)
                    }
                    needsRedraw = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Draw error: ${e.message}")
            } finally {
                canvas?.let {
                    try { holder.unlockCanvasAndPost(it) } catch (e: Exception) { }
                }
            }
        }

        private fun drawWhiteboard(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()

            // Background
            canvas.drawRect(0f, 0f, w, h, bgPaint)

            // Draw all strokes
            strokes.forEach { stroke ->
                drawStroke(canvas, stroke)
            }

            // Draw current in-progress stroke
            if (currentPoints.size > 1) {
                drawStroke(canvas, Stroke(currentPoints.toList(), "#FFFFFF", 4f))
            }

            // Draw partner cursor
            if (partnerX >= 0 && partnerY >= 0) {
                // Glow
                val glowPaint = Paint().apply {
                    color = Color.parseColor("#E94560")
                    alpha = 80
                    isAntiAlias = true
                    maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawCircle(partnerX, partnerY, 14f, glowPaint)
                // Dot
                cursorPaint.alpha = 255
                canvas.drawCircle(partnerX, partnerY, 8f, cursorPaint)
                val whitePaint = Paint().apply { color = Color.WHITE; isAntiAlias = true }
                canvas.drawCircle(partnerX, partnerY, 4f, whitePaint)
            }
        }

        private fun drawStroke(canvas: Canvas, stroke: Stroke) {
            if (stroke.points.size < 2) return
            val paint = Paint().apply {
                try { color = Color.parseColor(stroke.color) } catch (e: Exception) { color = Color.WHITE }
                strokeWidth = stroke.width
                style = Paint.Style.STROKE
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val path = Path()
            path.moveTo(stroke.points[0].first, stroke.points[0].second)
            for (i in 1 until stroke.points.size) {
                path.lineTo(stroke.points[i].first, stroke.points[i].second)
            }
            canvas.drawPath(path, paint)
        }

        private fun drawTicTacToe(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()

            // Background
            canvas.drawRect(0f, 0f, w, h, bgPaint)

            val gridSize = minOf(w, h) * 0.8f
            val startX = (w - gridSize) / 2
            val startY = (h - gridSize) / 2
            val cellSize = gridSize / 3f

            // Draw grid lines
            for (i in 1..2) {
                canvas.drawLine(startX + cellSize * i, startY, startX + cellSize * i, startY + gridSize, gridPaint)
                canvas.drawLine(startX, startY + cellSize * i, startX + gridSize, startY + cellSize * i, gridPaint)
            }

            // Draw X and O
            tttState.board.forEachIndexed { index, value ->
                if (value == null) return@forEachIndexed
                val col = (index % 3).toFloat()
                val row = (index / 3).toFloat()
                val cx = startX + col * cellSize + cellSize / 2
                val cy = startY + row * cellSize + cellSize / 2
                val pad = cellSize * 0.25f

                when (value) {
                    "X" -> {
                        canvas.drawLine(cx - pad, cy - pad, cx + pad, cy + pad, xPaint)
                        canvas.drawLine(cx + pad, cy - pad, cx - pad, cy + pad, xPaint)
                    }
                    "O" -> {
                        canvas.drawCircle(cx, cy, pad, oPaint)
                    }
                }
            }

            // Status text
            val statusText = when {
                tttState.winner != null -> if (tttState.winner == SyncManager.mySymbol.value) "You win! 🎉" else "They win!"
                tttState.draw -> "Draw!"
                tttState.turn == SyncManager.mySymbol.value -> "Your turn"
                else -> "Their turn..."
            }
            textPaint.color = when {
                tttState.winner == SyncManager.mySymbol.value -> Color.parseColor("#06D6A0")
                tttState.winner != null -> Color.parseColor("#FF6B35")
                tttState.draw -> Color.parseColor("#FFC947")
                else -> Color.WHITE
            }
            canvas.drawText(statusText, w / 2, startY - 30f, textPaint)

            // Doodlu label
            val labelPaint = Paint().apply {
                color = Color.parseColor("#8892B0")
                textSize = 24f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("Doodlu", w / 2, h - 40f, labelPaint)
        }

        private fun registerListeners() {
            SyncManager.addStrokeListener(strokeListener)
            SyncManager.addCursorListener(cursorListener)
            SyncManager.addModeListener(modeListener)
            SyncManager.addGameStateListener(gameStateListener)
            SyncManager.addCanvasListener(canvasListener)
        }

        private fun unregisterListeners() {
            SyncManager.removeStrokeListener(strokeListener)
            SyncManager.removeCursorListener(cursorListener)
            SyncManager.removeModeListener(modeListener)
            SyncManager.removeGameStateListener(gameStateListener)
            SyncManager.removeCanvasListener(canvasListener)
        }
    }
}
