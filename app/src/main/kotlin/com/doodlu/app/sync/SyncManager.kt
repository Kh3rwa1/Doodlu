package com.doodlu.app.sync

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.doodlu.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING
}

object SyncManager {
    private const val TAG = "SyncManager"
    private const val BASE_URL = "wss://sync-canvas-backend.dulalkisku0.workers.dev/room"
    private const val PING_INTERVAL_MS = 30_000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // No timeout for WebSocket
        .build()

    private var webSocket: WebSocket? = null
    private var currentRoomId: String? = null
    private var currentUserId: String? = null

    // Reconnect backoff
    private var reconnectDelay = 1000L
    private var isReconnecting = false
    private var shouldReconnect = true

    private val handlerThread = HandlerThread("DoodluSync").also { it.start() }
    private val handler = Handler(handlerThread.looper)

    // Ping runnable
    private val pingRunnable = object : Runnable {
        override fun run() {
            sendPing()
            handler.postDelayed(this, PING_INTERVAL_MS)
        }
    }

    // State flows
    val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val playerCount = MutableStateFlow(0)
    val currentMode = MutableStateFlow("whiteboard")
    val gameState = MutableStateFlow(com.doodlu.app.model.GameState())
    val mySymbol = MutableStateFlow("X")
    val myUserId = MutableStateFlow("")

    // Listener interfaces
    interface StrokeListener {
        fun onStroke(stroke: Stroke)
    }

    interface CursorListener {
        fun onCursor(userId: String, x: Float, y: Float)
    }

    interface GameStateListener {
        fun onGameState(state: TicTacToeState)
    }

    interface ModeListener {
        fun onModeSwitch(mode: String)
    }

    interface CanvasListener {
        fun onClearCanvas()
    }

    private val strokeListeners = mutableListOf<StrokeListener>()
    private val cursorListeners = mutableListOf<CursorListener>()
    private val gameStateListeners = mutableListOf<GameStateListener>()
    private val modeListeners = mutableListOf<ModeListener>()
    private val canvasListeners = mutableListOf<CanvasListener>()

    fun addStrokeListener(l: StrokeListener) { synchronized(strokeListeners) { strokeListeners.add(l) } }
    fun removeStrokeListener(l: StrokeListener) { synchronized(strokeListeners) { strokeListeners.remove(l) } }
    fun addCursorListener(l: CursorListener) { synchronized(cursorListeners) { cursorListeners.add(l) } }
    fun removeCursorListener(l: CursorListener) { synchronized(cursorListeners) { cursorListeners.remove(l) } }
    fun addGameStateListener(l: GameStateListener) { synchronized(gameStateListeners) { gameStateListeners.add(l) } }
    fun removeGameStateListener(l: GameStateListener) { synchronized(gameStateListeners) { gameStateListeners.remove(l) } }
    fun addModeListener(l: ModeListener) { synchronized(modeListeners) { modeListeners.add(l) } }
    fun removeModeListener(l: ModeListener) { synchronized(modeListeners) { modeListeners.remove(l) } }
    fun addCanvasListener(l: CanvasListener) { synchronized(canvasListeners) { canvasListeners.add(l) } }
    fun removeCanvasListener(l: CanvasListener) { synchronized(canvasListeners) { canvasListeners.remove(l) } }

    fun connect(roomId: String, userId: String) {
        currentRoomId = roomId
        currentUserId = userId
        myUserId.value = userId
        shouldReconnect = true
        reconnectDelay = 1000L
        doConnect()
    }

    private fun doConnect() {
        val roomId = currentRoomId ?: return
        val userId = currentUserId ?: return

        connectionState.value = if (isReconnecting) ConnectionState.RECONNECTING else ConnectionState.CONNECTING
        Log.d(TAG, "Connecting to room $roomId as $userId")

        val url = "$BASE_URL/$roomId?userId=$userId"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                connectionState.value = ConnectionState.CONNECTED
                isReconnecting = false
                reconnectDelay = 1000L
                handler.removeCallbacks(pingRunnable)
                handler.postDelayed(pingRunnable, PING_INTERVAL_MS)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                connectionState.value = ConnectionState.DISCONNECTED
                handler.removeCallbacks(pingRunnable)
                scheduleReconnect()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed")
                connectionState.value = ConnectionState.DISCONNECTED
                handler.removeCallbacks(pingRunnable)
                if (code != 1000) scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        isReconnecting = true
        Log.d(TAG, "Reconnecting in ${reconnectDelay}ms")
        handler.postDelayed({
            doConnect()
            reconnectDelay = minOf(reconnectDelay * 2, 30_000L)
        }, reconnectDelay)
    }

    fun disconnect() {
        shouldReconnect = false
        handler.removeCallbacks(pingRunnable)
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        connectionState.value = ConnectionState.DISCONNECTED
    }

    fun pauseConnection() {
        shouldReconnect = false
        handler.removeCallbacks(pingRunnable)
        webSocket?.close(1000, "Paused")
    }

    fun resumeConnection() {
        if (currentRoomId != null && currentUserId != null) {
            shouldReconnect = true
            isReconnecting = false
            reconnectDelay = 1000L
            doConnect()
        }
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (val type = json.getString("type")) {
                "init" -> {
                    val symbol = json.optString("symbol", "X")
                    val players = json.optInt("players", 1)
                    mySymbol.value = symbol
                    playerCount.value = players

                    val stateJson = json.optJSONObject("state")
                    if (stateJson != null) {
                        val mode = stateJson.optString("mode", "whiteboard")
                        currentMode.value = mode

                        val tttJson = stateJson.optJSONObject("tictactoe")
                        if (tttJson != null) {
                            val tttState = parseTicTacToe(tttJson)
                            gameState.value = gameState.value.copy(
                                mode = mode,
                                tictactoe = tttState
                            )
                            synchronized(gameStateListeners) {
                                gameStateListeners.forEach { it.onGameState(tttState) }
                            }
                        }

                        // Replay strokes from init state
                        val strokesJson = stateJson.optJSONArray("strokes")
                        if (strokesJson != null) {
                            for (i in 0 until strokesJson.length()) {
                                val strokeJson = strokesJson.getJSONObject(i)
                                val stroke = parseStroke(strokeJson)
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
                    val data = json.getJSONObject("data")
                    val stroke = parseStroke(data)
                    if (stroke != null) {
                        synchronized(strokeListeners) {
                            strokeListeners.forEach { it.onStroke(stroke) }
                        }
                    }
                }

                "cursor" -> {
                    val userId = json.optString("userId", "")
                    val x = json.optDouble("x", 0.0).toFloat()
                    val y = json.optDouble("y", 0.0).toFloat()
                    if (userId.isNotEmpty()) {
                        synchronized(cursorListeners) {
                            cursorListeners.forEach { it.onCursor(userId, x, y) }
                        }
                    }
                }

                "gameState" -> {
                    val tttJson = json.optJSONObject("tictactoe")
                    if (tttJson != null) {
                        val tttState = parseTicTacToe(tttJson)
                        gameState.value = gameState.value.copy(tictactoe = tttState)
                        synchronized(gameStateListeners) {
                            gameStateListeners.forEach { it.onGameState(tttState) }
                        }
                    }
                }

                "switchmode" -> {
                    val mode = json.optString("mode", "whiteboard")
                    currentMode.value = mode
                    synchronized(modeListeners) {
                        modeListeners.forEach { it.onModeSwitch(mode) }
                    }
                }

                "clearcanvas" -> {
                    synchronized(canvasListeners) {
                        canvasListeners.forEach { it.onClearCanvas() }
                    }
                }

                "playerJoined" -> {
                    playerCount.value = json.optInt("players", playerCount.value)
                }

                "playerLeft" -> {
                    playerCount.value = json.optInt("players", playerCount.value)
                }

                "pong" -> {
                    Log.v(TAG, "Pong received")
                }

                else -> Log.d(TAG, "Unknown message type: $type")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message: ${e.message}", e)
        }
    }

    private fun parseStroke(data: JSONObject): Stroke? {
        return try {
            val pointsJson = data.getJSONArray("points")
            val points = mutableListOf<Pair<Float, Float>>()
            for (i in 0 until pointsJson.length()) {
                val pt = pointsJson.getJSONArray(i)
                points.add(Pair(pt.getDouble(0).toFloat(), pt.getDouble(1).toFloat()))
            }
            val color = data.optString("color", "#FFFFFF")
            val width = data.optDouble("width", 4.0).toFloat()
            Stroke(points, color, width)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stroke: ${e.message}")
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
        } else {
            List(9) { null }
        }
        return TicTacToeState(
            board = board,
            turn = json.optString("turn", "X"),
            winner = if (json.isNull("winner")) null else json.optString("winner"),
            draw = json.optBoolean("draw", false)
        )
    }

    // Sending methods
    fun sendStroke(points: List<Pair<Float, Float>>, color: String, width: Float) {
        val pointsArray = JSONArray()
        points.forEach { (x, y) ->
            pointsArray.put(JSONArray().apply { put(x); put(y) })
        }
        val data = JSONObject().apply {
            put("points", pointsArray)
            put("color", color)
            put("width", width)
        }
        val msg = JSONObject().apply {
            put("type", "stroke")
            put("data", data)
        }
        send(msg.toString())
    }

    fun sendCursor(x: Float, y: Float) {
        val msg = JSONObject().apply {
            put("type", "cursor")
            put("x", x)
            put("y", y)
        }
        send(msg.toString())
    }

    fun sendMove(square: Int) {
        val msg = JSONObject().apply {
            put("type", "move")
            put("square", square)
        }
        send(msg.toString())
    }

    fun sendSwitchMode(mode: String) {
        val msg = JSONObject().apply {
            put("type", "switchmode")
            put("mode", mode)
        }
        send(msg.toString())
        currentMode.value = mode
    }

    fun sendNewGame() {
        send(JSONObject().apply { put("type", "newgame") }.toString())
    }

    fun sendClearCanvas() {
        send(JSONObject().apply { put("type", "clearcanvas") }.toString())
    }

    private fun sendPing() {
        send(JSONObject().apply { put("type", "ping") }.toString())
    }

    private fun send(text: String): Boolean {
        return try {
            webSocket?.send(text) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}")
            false
        }
    }
}
