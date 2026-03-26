package com.doodlu.app.sync

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.doodlu.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
}

object SyncManager {
    private const val TAG = "SyncManager"
    private const val BASE_URL = "wss://sync-canvas-backend.dulalkisku0.workers.dev/room"
    private const val PING_INTERVAL_MS = 30_000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    // Volatile ensures cross-thread visibility
    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var currentRoomId: String? = null
    @Volatile private var currentUserId: String? = null

    // Reconnect state — all accessed only on handlerThread
    private var reconnectDelay = 1000L
    private val isReconnecting = AtomicBoolean(false)
    private val shouldReconnect = AtomicBoolean(false)
    // Guard against duplicate sockets
    private val isConnecting = AtomicBoolean(false)

    private val handlerThread = HandlerThread("DoodluSync").also { it.start() }
    val handler = Handler(handlerThread.looper)

    // Ping runnable
    private val pingRunnable = object : Runnable {
        override fun run() {
            sendPing()
            handler.postDelayed(this, PING_INTERVAL_MS)
        }
    }

    // Reconnect runnable — stored so we can cancel it
    private val reconnectRunnable = Runnable {
        if (shouldReconnect.get()) {
            isConnecting.set(false)   // allow new attempt
            doConnect()
        }
    }

    // ── State flows ──────────────────────────────────────────────────────────
    val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val playerCount     = MutableStateFlow(0)
    val currentMode     = MutableStateFlow("whiteboard")
    val gameState       = MutableStateFlow(GameState())
    val mySymbol        = MutableStateFlow("X")
    val myUserId        = MutableStateFlow("")

    /**
     * Fires ONLY when the server sends an explicit "switchmode" broadcast.
     * Use this for navigation so that stale "init" state never triggers unwanted
     * screen transitions. The value is the new mode string.
     */
    val modeSwitchEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)

    // ── Listener interfaces ──────────────────────────────────────────────────
    interface StrokeListener    { fun onStroke(stroke: Stroke) }
    interface CursorListener    { fun onCursor(userId: String, x: Float, y: Float) }
    interface GameStateListener { fun onGameState(state: TicTacToeState) }
    interface ModeListener      { fun onModeSwitch(mode: String) }
    interface CanvasListener    { fun onClearCanvas() }

    private val strokeListeners    = mutableListOf<StrokeListener>()
    private val cursorListeners    = mutableListOf<CursorListener>()
    private val gameStateListeners = mutableListOf<GameStateListener>()
    private val modeListeners      = mutableListOf<ModeListener>()
    private val canvasListeners    = mutableListOf<CanvasListener>()

    fun addStrokeListener(l: StrokeListener)         { synchronized(strokeListeners)    { strokeListeners.add(l) } }
    fun removeStrokeListener(l: StrokeListener)      { synchronized(strokeListeners)    { strokeListeners.remove(l) } }
    fun addCursorListener(l: CursorListener)         { synchronized(cursorListeners)    { cursorListeners.add(l) } }
    fun removeCursorListener(l: CursorListener)      { synchronized(cursorListeners)    { cursorListeners.remove(l) } }
    fun addGameStateListener(l: GameStateListener)   { synchronized(gameStateListeners) { gameStateListeners.add(l) } }
    fun removeGameStateListener(l: GameStateListener){ synchronized(gameStateListeners) { gameStateListeners.remove(l) } }
    fun addModeListener(l: ModeListener)             { synchronized(modeListeners)      { modeListeners.add(l) } }
    fun removeModeListener(l: ModeListener)          { synchronized(modeListeners)      { modeListeners.remove(l) } }
    fun addCanvasListener(l: CanvasListener)         { synchronized(canvasListeners)    { canvasListeners.add(l) } }
    fun removeCanvasListener(l: CanvasListener)      { synchronized(canvasListeners)    { canvasListeners.remove(l) } }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Full connect — call from Activity or WallpaperEngine. Safe to call multiple times. */
    fun connect(roomId: String, userId: String) {
        handler.post {
            currentRoomId = roomId
            currentUserId = userId
            myUserId.value = userId
            shouldReconnect.set(true)
            reconnectDelay = 1000L
            isReconnecting.set(false)
            // Cancel any pending reconnect before starting fresh
            handler.removeCallbacks(reconnectRunnable)
            isConnecting.set(false)
            doConnect()
        }
    }

    /**
     * Pause from WallpaperService.onVisibilityChanged(false).
     * Does NOT touch the socket if the Activity is showing — checked via [activeClients].
     */
    private val activeClients = MutableStateFlow(0)

    fun registerClient()   { activeClients.value++ }
    fun unregisterClient() { activeClients.value = maxOf(0, activeClients.value - 1) }

    /** Called by wallpaper when screen goes off / wallpaper hidden. */
    fun pauseForWallpaper() {
        handler.post {
            // Only pause if no Activity is also using the connection
            if (activeClients.value <= 0) {
                Log.d(TAG, "Pausing WebSocket (no active clients)")
                shouldReconnect.set(false)
                // Cancel any queued reconnect runnables BEFORE closing
                handler.removeCallbacks(reconnectRunnable)
                handler.removeCallbacks(pingRunnable)
                webSocket?.close(1001, "Wallpaper hidden")
                webSocket = null
                isConnecting.set(false)
                connectionState.value = ConnectionState.DISCONNECTED
            } else {
                Log.d(TAG, "Wallpaper hidden but Activity still active — keeping socket alive")
            }
        }
    }

    /** Called by wallpaper when it becomes visible again. */
    fun resumeForWallpaper(roomId: String, userId: String) {
        handler.post {
            currentRoomId = roomId
            currentUserId = userId
            myUserId.value = userId
            shouldReconnect.set(true)
            reconnectDelay = 1000L
            isReconnecting.set(false)
            handler.removeCallbacks(reconnectRunnable)

            // Don't open a duplicate socket
            if (connectionState.value == ConnectionState.CONNECTED ||
                isConnecting.get()) {
                Log.d(TAG, "resumeForWallpaper: already connected or connecting — skipping")
                return@post
            }
            isConnecting.set(false)
            doConnect()
        }
    }

    fun disconnect() {
        handler.post {
            shouldReconnect.set(false)
            handler.removeCallbacks(reconnectRunnable)
            handler.removeCallbacks(pingRunnable)
            webSocket?.close(1000, "User disconnected")
            webSocket = null
            isConnecting.set(false)
            connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun doConnect() {
        // Must be called on handlerThread
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return

        if (!shouldReconnect.get()) return

        // Guard: don't open a second socket if one is in-flight
        if (isConnecting.compareAndSet(false, true) == false) {
            Log.d(TAG, "doConnect skipped — already connecting")
            return
        }

        val state = if (isReconnecting.get()) ConnectionState.RECONNECTING else ConnectionState.CONNECTING
        connectionState.value = state
        Log.d(TAG, "doConnect → room=$roomId userId=$userId delay=${reconnectDelay}ms")

        val url = "$BASE_URL/$roomId?userId=$userId"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                handler.post {
                    Log.d(TAG, "WebSocket OPEN")
                    connectionState.value = ConnectionState.CONNECTED
                    isReconnecting.set(false)
                    isConnecting.set(false)
                    reconnectDelay = 1000L
                    handler.removeCallbacks(pingRunnable)
                    handler.postDelayed(pingRunnable, PING_INTERVAL_MS)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handler.post {
                    Log.e(TAG, "WebSocket FAILURE: ${t.message}")
                    connectionState.value = ConnectionState.DISCONNECTED
                    handler.removeCallbacks(pingRunnable)
                    isConnecting.set(false)
                    scheduleReconnect()
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket CLOSING code=$code reason=$reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handler.post {
                    Log.d(TAG, "WebSocket CLOSED code=$code")
                    connectionState.value = ConnectionState.DISCONNECTED
                    handler.removeCallbacks(pingRunnable)
                    isConnecting.set(false)
                    // 1000 = normal close (disconnect() or pauseForWallpaper)
                    // 1001 = going away (pauseForWallpaper)
                    // anything else = unexpected → reconnect
                    if (code != 1000 && code != 1001) {
                        scheduleReconnect()
                    }
                }
            }
        })
    }

    /**
     * Schedules a reconnect with exponential backoff.
     * Delay is DOUBLED before scheduling so the next attempt uses the updated value.
     */
    private fun scheduleReconnect() {
        // Must be called on handlerThread
        if (!shouldReconnect.get()) {
            Log.d(TAG, "scheduleReconnect: shouldReconnect=false, skipping")
            return
        }
        isReconnecting.set(true)
        // Cancel any previous reconnect runnable
        handler.removeCallbacks(reconnectRunnable)

        val delay = reconnectDelay
        Log.d(TAG, "Scheduling reconnect in ${delay}ms (next: ${minOf(delay * 2, 30_000L)}ms)")
        // Update delay BEFORE scheduling so next iteration uses new value
        reconnectDelay = minOf(delay * 2, 30_000L)

        handler.postDelayed(reconnectRunnable, delay)
    }

    // ── Message handling ──────────────────────────────────────────────────────

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (val type = json.getString("type")) {
                "init" -> {
                    val symbol  = json.optString("symbol", "X")
                    val players = json.optInt("players", 1)
                    mySymbol.value    = symbol
                    playerCount.value = players

                    val stateJson = json.optJSONObject("state")
                    if (stateJson != null) {
                        val mode = stateJson.optString("mode", "whiteboard")
                        currentMode.value = mode

                        val tttJson = stateJson.optJSONObject("tictactoe")
                        if (tttJson != null) {
                            val tttState = parseTicTacToe(tttJson)
                            gameState.value = gameState.value.copy(mode = mode, tictactoe = tttState)
                            synchronized(gameStateListeners) {
                                gameStateListeners.forEach { it.onGameState(tttState) }
                            }
                        }

                        val strokesJson = stateJson.optJSONArray("strokes")
                        if (strokesJson != null) {
                            // Clear local strokes before replaying server state so
                            // stale drawings don't persist after a canvas clear.
                            synchronized(canvasListeners) { canvasListeners.forEach { it.onClearCanvas() } }
                            for (i in 0 until strokesJson.length()) {
                                val stroke = parseStroke(strokesJson.getJSONObject(i))
                                if (stroke != null) {
                                    synchronized(strokeListeners) {
                                        strokeListeners.forEach { it.onStroke(stroke) }
                                    }
                                }
                            }
                        }
                    }
                }

                "stroke" -> {
                    val stroke = parseStroke(json.getJSONObject("data"))
                    if (stroke != null) {
                        synchronized(strokeListeners) { strokeListeners.forEach { it.onStroke(stroke) } }
                    }
                }

                "cursor" -> {
                    val userId = json.optString("userId", "")
                    val x = json.optDouble("x", 0.0).toFloat()
                    val y = json.optDouble("y", 0.0).toFloat()
                    if (userId.isNotEmpty()) {
                        synchronized(cursorListeners) { cursorListeners.forEach { it.onCursor(userId, x, y) } }
                    }
                }

                "gameState" -> {
                    val tttJson = json.optJSONObject("tictactoe")
                    if (tttJson != null) {
                        val tttState = parseTicTacToe(tttJson)
                        gameState.value = gameState.value.copy(tictactoe = tttState)
                        synchronized(gameStateListeners) { gameStateListeners.forEach { it.onGameState(tttState) } }
                    }
                }

                "switchmode" -> {
                    val mode = json.optString("mode", "whiteboard")
                    currentMode.value = mode
                    modeSwitchEvent.tryEmit(mode)
                    synchronized(modeListeners) { modeListeners.forEach { it.onModeSwitch(mode) } }
                }

                "clearcanvas" -> {
                    synchronized(canvasListeners) { canvasListeners.forEach { it.onClearCanvas() } }
                }

                "playerJoined" -> { playerCount.value = json.optInt("players", playerCount.value) }
                "playerLeft"   -> { playerCount.value = json.optInt("players", playerCount.value) }
                "pong"         -> { Log.v(TAG, "pong") }

                else -> Log.d(TAG, "Unknown type: $type")
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleMessage error: ${e.message}", e)
        }
    }

    private fun parseStroke(data: JSONObject): Stroke? {
        return try {
            val pointsJson = data.getJSONArray("points")
            val points = (0 until pointsJson.length()).map { i ->
                val pt = pointsJson.getJSONArray(i)
                Pair(pt.getDouble(0).toFloat(), pt.getDouble(1).toFloat())
            }
            Stroke(points, data.optString("color", "#FFFFFF"), data.optDouble("width", 4.0).toFloat())
        } catch (e: Exception) {
            Log.e(TAG, "parseStroke error: ${e.message}")
            null
        }
    }

    private fun parseTicTacToe(json: JSONObject): TicTacToeState {
        val boardJson = json.optJSONArray("board")
        val board = if (boardJson != null) {
            (0 until boardJson.length()).map { i ->
                val v = boardJson.opt(i)
                if (v == null || v == JSONObject.NULL) null else v.toString()
            }
        } else List(9) { null }

        return TicTacToeState(
            board   = board,
            turn    = json.optString("turn", "X"),
            winner  = if (json.isNull("winner")) null else json.optString("winner"),
            draw    = json.optBoolean("draw", false)
        )
    }

    // ── Send helpers ──────────────────────────────────────────────────────────

    fun sendStroke(points: List<Pair<Float, Float>>, color: String, width: Float) {
        val arr = JSONArray().also { a ->
            points.forEach { (x, y) -> a.put(JSONArray().apply { put(x); put(y) }) }
        }
        send(JSONObject().apply {
            put("type", "stroke")
            put("data", JSONObject().apply { put("points", arr); put("color", color); put("width", width) })
        }.toString())
    }

    fun sendCursor(x: Float, y: Float) {
        send(JSONObject().apply { put("type", "cursor"); put("x", x); put("y", y) }.toString())
    }

    fun sendMove(square: Int) {
        send(JSONObject().apply { put("type", "move"); put("square", square) }.toString())
    }

    fun sendSwitchMode(mode: String) {
        currentMode.value = mode
        send(JSONObject().apply { put("type", "switchmode"); put("mode", mode) }.toString())
    }

    fun sendNewGame()     { send(JSONObject().apply { put("type", "newgame") }.toString()) }
    fun sendClearCanvas() { send(JSONObject().apply { put("type", "clearcanvas") }.toString()) }
    /**
     * Kicks the partner out of the room by broadcasting a special mode value.
     * Does NOT update our own currentMode — only the receiving side reacts to "kicked".
     */
    fun sendKickUser() {
        send(JSONObject().apply { put("type", "switchmode"); put("mode", "kicked") }.toString())
    }
    private fun sendPing(){ send(JSONObject().apply { put("type", "ping") }.toString()) }

    private fun send(text: String): Boolean {
        return try {
            webSocket?.send(text) ?: run {
                Log.w(TAG, "send() called but webSocket is null")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "send() error: ${e.message}")
            false
        }
    }
}
