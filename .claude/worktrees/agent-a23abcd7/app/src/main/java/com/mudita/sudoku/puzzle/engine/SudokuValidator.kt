package com.mudita.sudoku.puzzle.engine

/**
 * Checks whether placing [digit] at [index] in [board] violates any Sudoku constraint.
 *
 * @param board  81-element IntArray; 0 = empty, 1–9 = placed digit. index = row*9+col.
 * @param index  Cell position to check (0–80).
 * @param digit  Digit to place (1–9).
 * @return true if placement is valid (no row, column, or box conflict); false otherwise.
 */
fun isValidPlacement(board: IntArray, index: Int, digit: Int): Boolean {
    val row = index / 9
    val col = index % 9
    val boxRow = (row / 3) * 3
    val boxCol = (col / 3) * 3
    for (i in 0..8) {
        if (board[row * 9 + i] == digit) return false        // row conflict
        if (board[i * 9 + col] == digit) return false        // column conflict
        val br = boxRow + i / 3
        val bc = boxCol + i % 3
        if (board[br * 9 + bc] == digit) return false        // box conflict
    }
    return true
}

class SudokuValidator {
    fun isValidPlacement(board: IntArray, index: Int, digit: Int): Boolean =
        com.mudita.sudoku.puzzle.engine.isValidPlacement(board, index, digit)
}
