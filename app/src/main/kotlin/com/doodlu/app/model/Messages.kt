package com.doodlu.app.model

sealed class IncomingMessage {
    data class Init(
        val state: GameState,
        val userId: String,
        val symbol: String,
        val players: Int
    ) : IncomingMessage()

    data class StrokeMessage(val stroke: Stroke) : IncomingMessage()

    data class CursorMessage(
        val userId: String,
        val x: Float,
        val y: Float
    ) : IncomingMessage()

    data class GameStateUpdate(val tictactoe: TicTacToeState) : IncomingMessage()

    data class SwitchMode(val mode: String) : IncomingMessage()

    object ClearCanvas : IncomingMessage()

    data class PlayerJoined(val players: Int) : IncomingMessage()

    data class PlayerLeft(val players: Int) : IncomingMessage()

    object Pong : IncomingMessage()
}

sealed class OutgoingMessage {
    data class StrokeMessage(
        val points: List<Pair<Float, Float>>,
        val color: String,
        val width: Float
    ) : OutgoingMessage()

    data class CursorMessage(val x: Float, val y: Float) : OutgoingMessage()

    data class Move(val square: Int) : OutgoingMessage()

    data class SwitchMode(val mode: String) : OutgoingMessage()

    object NewGame : OutgoingMessage()

    object ClearCanvas : OutgoingMessage()

    object Ping : OutgoingMessage()
}
