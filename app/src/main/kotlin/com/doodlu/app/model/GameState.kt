package com.doodlu.app.model

data class TicTacToeState(
    val board: List<String?> = List(9) { null }, // null, "X", or "O"
    val turn: String = "X",
    val winner: String? = null,
    val draw: Boolean = false
)

data class GameState(
    val mode: String = "whiteboard", // "whiteboard" or "tictactoe"
    val strokes: List<Stroke> = emptyList(),
    val tictactoe: TicTacToeState = TicTacToeState()
)

data class CursorState(
    val userId: String,
    val x: Float,
    val y: Float
)
