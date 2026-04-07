package com.interstitial.sudoku.puzzle.engine

fun isValidPlacement(board: IntArray, index: Int, digit: Int): Boolean {
    val row = index / 9
    val col = index % 9
    val boxRow = (row / 3) * 3
    val boxCol = (col / 3) * 3
    for (i in 0..8) {
        if (board[row * 9 + i] == digit) return false
        if (board[i * 9 + col] == digit) return false
        val br = boxRow + i / 3
        val bc = boxCol + i % 3
        if (board[br * 9 + bc] == digit) return false
    }
    return true
}

class SudokuValidator {
    fun isValidPlacement(board: IntArray, index: Int, digit: Int): Boolean =
        com.interstitial.sudoku.puzzle.engine.isValidPlacement(board, index, digit)
}
