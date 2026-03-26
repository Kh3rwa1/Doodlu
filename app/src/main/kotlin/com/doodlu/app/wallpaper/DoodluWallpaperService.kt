package com.doodlu.app.wallpaper

import android.app.Service
import android.content.Intent
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

    /**
     * Return START_STICKY so Android restarts the service if it is killed.
     * WallpaperService handles onStartCommand internally; we just override it
     * to set the restart flag.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return Service.START_STICKY
    }

    // ─────────────────────────────────────────────────────────────────────────

    inner class DoodluEngine : Engine() {
        private val TAG = "DoodluEngine"

        // Dedicated render thread — keep UI thread free
        private val renderThread = HandlerThread("DoodluRender").also { it.start() }
        private val renderHandler = Handler(renderThread.looper)

        // Coroutine scope for DataStore reads (IO dispatcher)
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        // ── Canvas state (only touched from renderHandler) ────────────────────
        private val strokes = mutableListOf<Stroke>()
        private var partnerX = -1f
        private var partnerY = -1f
        private var currentMode = "whiteboard"
        private var tttState = TicTacToeState()
        private val currentPoints = mutableListOf<Pair<Float, Float>>()
        private var lastSentIndex = 0

        // Cursor fade: track when we last received a partner cursor update
        private var lastCursorTime = 0L
        private val CURSOR_FADE_MS = 3000L  // cursor fades after 3s of no updates

        // Periodic redraw for smooth animations (~20fps when visible)
        private val FRAME_INTERVAL_MS = 50L
        private val frameRunnable = object : Runnable {
            override fun run() {
                if (isVisible) {
                    drawFrame()
                    renderHandler.postDelayed(this, FRAME_INTERVAL_MS)
                }
            }
        }

        // Only redraw when we have new data
        @Volatile private var needsRedraw = false

        // Track whether wallpaper is visible — prevents wasted renders
        @Volatile private var isVisible = false

        // Reentrancy guard: setTouchEventsEnabled() internally calls
        // updateSurface() which can trigger onSurfaceCreated /
        // onVisibilityChanged again on some Android versions, causing
        // infinite recursion.  This flag + handler.post() breaks the loop.
        private var insideSurfaceCallback = false

        // Room credentials loaded from DataStore
        @Volatile private var savedRoomId: String? = null
        @Volatile private var savedUserId: String? = null

        // ── Pre-allocated Paints ──────────────────────────────────────────────
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
        private val glowPaint = Paint().apply {
            color = Color.parseColor("#E94560")
            alpha = 80
            isAntiAlias = true
            maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
        }
        private val whiteDotPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }

        // Reusable paint for drawStroke — mutated per call to avoid per-stroke allocations
        private val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        // ── SyncManager listeners ─────────────────────────────────────────────
        private val strokeListener = object : SyncManager.StrokeListener {
            override fun onStroke(stroke: Stroke) {
                renderHandler.post {
                    strokes.add(stroke)
                    if (isVisible) scheduleRedraw()
                }
            }
        }
        private val cursorListener = object : SyncManager.CursorListener {
            override fun onCursor(userId: String, x: Float, y: Float) {
                if (userId != SyncManager.myUserId.value) {
                    renderHandler.post {
                        partnerX = x
                        partnerY = y
                        lastCursorTime = System.currentTimeMillis()
                        needsRedraw = true
                    }
                }
            }
        }
        private val modeListener = object : SyncManager.ModeListener {
            override fun onModeSwitch(mode: String) {
                renderHandler.post {
                    currentMode = mode
                    if (isVisible) scheduleRedraw()
                }
            }
        }
        private val gameStateListener = object : SyncManager.GameStateListener {
            override fun onGameState(state: TicTacToeState) {
                renderHandler.post {
                    tttState = state
                    if (isVisible) scheduleRedraw()
                }
            }
        }
        private val canvasListener = object : SyncManager.CanvasListener {
            override fun onClearCanvas() {
                renderHandler.post {
                    strokes.clear()
                    if (isVisible) scheduleRedraw()
                }
            }
        }

        // ── Engine lifecycle ──────────────────────────────────────────────────

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            safeTouchEnable()
            registerListeners()
            SyncManager.registerClient()

            // Load credentials, then connect/sync state
            scope.launch {
                val prefs = PreferencesManager(this@DoodluWallpaperService)
                val roomId = prefs.roomId.first()
                val userId = prefs.userId.first()

                // Cache for later visibility changes
                savedRoomId = roomId
                savedUserId = userId

                if (!roomId.isNullOrEmpty() && !userId.isNullOrEmpty()) {
                    when (SyncManager.connectionState.value) {
                        ConnectionState.CONNECTED -> {
                            // Already connected — just sync local state
                            syncStateFromManager()
                        }
                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> {
                            // In-flight — just sync state when it lands
                            syncStateFromManager()
                        }
                        ConnectionState.DISCONNECTED -> {
                            SyncManager.connect(roomId, userId)
                        }
                    }
                } else {
                    Log.w(TAG, "No room credentials in DataStore — wallpaper will show blank")
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            unregisterListeners()
            SyncManager.unregisterClient()
            // Don't disconnect the socket — the Activity might still be using it.
            // If no one is left, the OS killing the service is fine too.
            scope.cancel()
            renderThread.quitSafely()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            if (visible) {
                // Re-assert touch enabled every time we become visible — Android
                // can silently reset this flag across lock/unlock cycles on
                // some OEM devices and Android versions.
                safeTouchEnable()
                Log.d(TAG, "Wallpaper VISIBLE — ensuring WebSocket is alive")
                scope.launch {
                    val roomId = savedRoomId
                    val userId = savedUserId

                    // Re-read from DataStore if we don't have them yet
                    val finalRoomId = if (roomId.isNullOrEmpty()) {
                        PreferencesManager(this@DoodluWallpaperService).roomId.first()
                    } else roomId

                    val finalUserId = if (userId.isNullOrEmpty()) {
                        PreferencesManager(this@DoodluWallpaperService).userId.first()
                    } else userId

                    if (!finalRoomId.isNullOrEmpty() && !finalUserId.isNullOrEmpty()) {
                        savedRoomId = finalRoomId
                        savedUserId = finalUserId
                        SyncManager.resumeForWallpaper(finalRoomId, finalUserId)
                    }
                }
                // Start the periodic frame loop
                renderHandler.removeCallbacks(frameRunnable)
                renderHandler.post(frameRunnable)
            } else {
                Log.d(TAG, "Wallpaper HIDDEN — requesting connection pause")
                // Stop the frame loop
                renderHandler.removeCallbacks(frameRunnable)
                // Uses smart pause: only actually pauses if Activity isn't active
                SyncManager.pauseForWallpaper()
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            insideSurfaceCallback = true
            try {
                super.onSurfaceCreated(holder)
                scheduleRedraw()
            } finally {
                insideSurfaceCallback = false
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            scheduleRedraw()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            // Cancel pending render to avoid locked-canvas crash
            renderHandler.removeCallbacksAndMessages(null)
            super.onSurfaceDestroyed(holder)
        }

        // ── Touch input ───────────────────────────────────────────────────────

        override fun onTouchEvent(event: MotionEvent) {
            // MotionEvent objects are recycled by the framework after this call
            // returns, so we must obtain a copy before posting to the render
            // thread. Failing to do this was causing currentPoints/strokes to
            // be mutated on the main thread while drawFrame reads them on
            // renderHandler — a silent ConcurrentModificationException swallowed
            // by drawFrame's try/catch, resulting in dropped frames on the lock
            // screen.
            val copy = MotionEvent.obtain(event)
            renderHandler.post {
                when (currentMode) {
                    "whiteboard" -> handleDrawTouch(copy)
                    "tictactoe"  -> handleTttTouch(copy)
                }
                copy.recycle()
            }
        }

        // NOTE: handleDrawTouch and handleTttTouch are ONLY called from
        // renderHandler (via the copy posted in onTouchEvent). All mutations of
        // currentPoints, strokes, lastSentIndex, and tttState therefore happen
        // on the same thread as drawFrame — no synchronisation needed.
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
                        // Overlap: keep last point as start of next batch so
                        // consecutive segments connect without gaps.
                        lastSentIndex = currentPoints.size - 1
                    }
                    // Already on renderHandler — draw immediately instead of posting
                    drawFrame()
                }
                MotionEvent.ACTION_UP -> {
                    val remaining = currentPoints.subList(lastSentIndex, currentPoints.size).toList()
                    if (remaining.size > 1) SyncManager.sendStroke(remaining, "#FFFFFF", 4f)
                    strokes.add(Stroke(currentPoints.toList(), "#FFFFFF", 4f))
                    currentPoints.clear()
                    lastSentIndex = 0
                    drawFrame()
                }
            }
        }

        private fun handleTttTouch(event: MotionEvent) {
            if (event.action != MotionEvent.ACTION_UP) return
            if (tttState.winner != null || tttState.draw) return
            if (tttState.turn != SyncManager.mySymbol.value) return

            val frame = surfaceHolder.surfaceFrame
            val w = frame.width().toFloat()
            val h = frame.height().toFloat()
            val gridSize = minOf(w, h) * 0.8f
            val startX = (w - gridSize) / 2
            val startY = (h - gridSize) / 2
            val cellSize = gridSize / 3f

            val col = ((event.x - startX) / cellSize).toInt()
            val row = ((event.y - startY) / cellSize).toInt()
            if (col in 0..2 && row in 0..2) {
                val square = row * 3 + col
                if (tttState.board[square] == null) SyncManager.sendMove(square)
            }
        }

        // ── Rendering ─────────────────────────────────────────────────────────

        /** Call from any thread — renders on renderHandler. */
        private fun scheduleRedraw() {
            needsRedraw = true
            // Post without removing: keep existing frames in queue (up to 1 pending is fine)
            renderHandler.post { drawFrame() }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                when (currentMode) {
                    "tictactoe" -> drawTicTacToe(canvas)
                    else        -> drawWhiteboard(canvas)
                }
                needsRedraw = false
            } catch (e: Exception) {
                Log.e(TAG, "drawFrame error: ${e.message}")
            } finally {
                canvas?.let {
                    try { holder.unlockCanvasAndPost(it) } catch (_: Exception) {}
                }
            }
        }

        private fun drawWhiteboard(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            canvas.drawRect(0f, 0f, w, h, bgPaint)

            strokes.forEach { drawStroke(canvas, it) }

            // In-progress stroke
            if (currentPoints.size > 1) drawStroke(canvas, Stroke(currentPoints.toList(), "#FFFFFF", 4f))

            // Partner cursor with animated glow + fade
            if (partnerX >= 0 && partnerY >= 0) {
                val timeSinceCursor = System.currentTimeMillis() - lastCursorTime
                val cursorAlpha = if (timeSinceCursor < CURSOR_FADE_MS) {
                    1f - (timeSinceCursor.toFloat() / CURSOR_FADE_MS)
                } else 0f

                if (cursorAlpha > 0f) {
                    val breathe = (Math.sin(System.currentTimeMillis() / 500.0) * 0.3 + 0.7).toFloat()
                    glowPaint.alpha = (80 * cursorAlpha * breathe).toInt()
                    cursorPaint.alpha = (255 * cursorAlpha).toInt()
                    whiteDotPaint.alpha = (255 * cursorAlpha).toInt()
                    canvas.drawCircle(partnerX, partnerY, 18f, glowPaint)
                    canvas.drawCircle(partnerX, partnerY, 8f, cursorPaint)
                    canvas.drawCircle(partnerX, partnerY, 4f, whiteDotPaint)
                    // Reset alphas for next frame
                    glowPaint.alpha = 80
                    cursorPaint.alpha = 255
                    whiteDotPaint.alpha = 255
                }
            }

            // Small branding watermark to show it's live
            val now = System.currentTimeMillis()
            val breatheAlpha = ((Math.sin(now / 2000.0) * 30 + 50).toInt()).coerceIn(20, 80)
            val brandPaint = Paint().apply {
                color = Color.WHITE
                alpha = breatheAlpha
                textSize = 24f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("Doodlu ❤️", w / 2f, h - 48f, brandPaint)
        }

        private fun drawStroke(canvas: Canvas, stroke: Stroke) {
            if (stroke.points.size < 2) return
            strokePaint.color = try { Color.parseColor(stroke.color) } catch (_: Exception) { Color.WHITE }
            strokePaint.strokeWidth = stroke.width
            val path = Path().apply {
                moveTo(stroke.points[0].first, stroke.points[0].second)
                for (i in 1 until stroke.points.size) lineTo(stroke.points[i].first, stroke.points[i].second)
            }
            canvas.drawPath(path, strokePaint)
        }

        private fun drawTicTacToe(canvas: Canvas) {
            val w = canvas.width.toFloat()
            val h = canvas.height.toFloat()
            canvas.drawRect(0f, 0f, w, h, bgPaint)

            val gridSize = minOf(w, h) * 0.8f
            val startX = (w - gridSize) / 2f
            val startY = (h - gridSize) / 2f
            val cellSize = gridSize / 3f

            // Grid lines
            for (i in 1..2) {
                canvas.drawLine(startX + cellSize * i, startY, startX + cellSize * i, startY + gridSize, gridPaint)
                canvas.drawLine(startX, startY + cellSize * i, startX + gridSize, startY + cellSize * i, gridPaint)
            }

            // X and O marks
            tttState.board.forEachIndexed { index, value ->
                if (value == null) return@forEachIndexed
                val col = (index % 3).toFloat()
                val row = (index / 3).toFloat()
                val cx  = startX + col * cellSize + cellSize / 2f
                val cy  = startY + row * cellSize + cellSize / 2f
                val pad = cellSize * 0.25f
                when (value) {
                    "X" -> {
                        canvas.drawLine(cx - pad, cy - pad, cx + pad, cy + pad, xPaint)
                        canvas.drawLine(cx + pad, cy - pad, cx - pad, cy + pad, xPaint)
                    }
                    "O" -> canvas.drawCircle(cx, cy, pad, oPaint)
                }
            }

            // Status text
            val statusText = when {
                tttState.winner != null ->
                    if (tttState.winner == SyncManager.mySymbol.value) "You win!" else "They win!"
                tttState.draw -> "Draw!"
                tttState.turn == SyncManager.mySymbol.value -> "Your turn"
                else -> "Their turn..."
            }
            textPaint.color = when {
                tttState.winner == SyncManager.mySymbol.value -> Color.parseColor("#06D6A0")
                tttState.winner != null                       -> Color.parseColor("#FF6B35")
                tttState.draw                                 -> Color.parseColor("#FFC947")
                else                                          -> Color.WHITE
            }
            canvas.drawText(statusText, w / 2f, startY - 32f, textPaint)

            // Brand label
            val labelPaint = Paint().apply {
                color = Color.parseColor("#8892B0")
                textSize = 22f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText("Doodlu", w / 2f, h - 36f, labelPaint)
        }

        // ── Helpers ───────────────────────────────────────────────────────────

        /**
         * Safely enables touch events without risking recursion.
         *
         * setTouchEventsEnabled() calls updateSurface() internally, which
         * on some Android versions/OEMs re-enters onSurfaceCreated() or
         * onVisibilityChanged(). By posting to the main handler we ensure
         * the call happens AFTER the current lifecycle callback returns,
         * and the insideSurfaceCallback guard provides extra safety.
         */
        private fun safeTouchEnable() {
            if (insideSurfaceCallback) return
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    insideSurfaceCallback = true
                    setTouchEventsEnabled(true)
                } catch (e: Exception) {
                    Log.w(TAG, "setTouchEventsEnabled failed: ${e.message}")
                } finally {
                    insideSurfaceCallback = false
                }
            }
        }

        private fun syncStateFromManager() {
            currentMode = SyncManager.currentMode.value
            tttState = SyncManager.gameState.value.tictactoe
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
