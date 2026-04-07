package com.interstitial.sudoku.puzzle.engine

object ConflictDetector {

    fun buildConflictMask(board: IntArray): BooleanArray {
        val mask = BooleanArray(81)
        for (i in 0 until 81) {
            if (board[i] == 0) continue
            val row = i / 9
            val col = i % 9
            val boxRow = (row / 3) * 3
            val boxCol = (col / 3) * 3

            for (c in 0 until 9) {
                val j = row * 9 + c
                if (j != i && board[j] == board[i]) {
                    mask[i] = true
                    mask[j] = true
                }
            }
            for (r in 0 until 9) {
                val j = r * 9 + col
                if (j != i && board[j] == board[i]) {
                    mask[i] = true
                    mask[j] = true
                }
            }
            for (dr in 0 until 3) {
                for (dc in 0 until 3) {
                    val j = (boxRow + dr) * 9 + (boxCol + dc)
                    if (j != i && board[j] == board[i]) {
                        mask[i] = true
                        mask[j] = true
                    }
                }
            }
        }
        return mask
    }
}
