package com.celestial.spire.model

data class TicTacToeState(
    val board: List<String?> = List(9) { null }, // null, "X", or "O"
    val turn: String = "X",
    val winner: String? = null,
    val draw: Boolean = false
) {
    /** Indices of the three cells forming the winning line, or null if no winner. */
    val winnerLine: List<Int>?
        get() {
            if (winner == null) return null
            val lines = listOf(
                listOf(0,1,2), listOf(3,4,5), listOf(6,7,8), // rows
                listOf(0,3,6), listOf(1,4,7), listOf(2,5,8), // cols
                listOf(0,4,8), listOf(2,4,6)                  // diags
            )
            return lines.firstOrNull { (a,b,c) ->
                board[a] == winner && board[b] == winner && board[c] == winner
            }
        }
}

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
