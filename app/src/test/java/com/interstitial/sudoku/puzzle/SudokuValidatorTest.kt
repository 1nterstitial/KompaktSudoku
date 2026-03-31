package com.interstitial.sudoku.puzzle

import com.interstitial.sudoku.puzzle.engine.isValidPlacement
import org.junit.Assert.*
import org.junit.Test

class SudokuValidatorTest {

    private fun emptyBoard() = IntArray(81) { 0 }

    @Test fun `valid placement in empty board returns true`() {
        assertTrue(isValidPlacement(emptyBoard(), 0, 5))
    }

    @Test fun `row conflict returns false`() {
        val board = emptyBoard()
        board[0] = 5  // row 0, col 0
        assertFalse(isValidPlacement(board, 1, 5))  // row 0, col 1 — same row, same digit
    }

    @Test fun `column conflict returns false`() {
        val board = emptyBoard()
        board[0] = 5  // row 0, col 0
        assertFalse(isValidPlacement(board, 9, 5))  // row 1, col 0 — same column, same digit
    }

    @Test fun `3x3 box conflict returns false`() {
        val board = emptyBoard()
        board[0] = 5  // row 0, col 0 — top-left box
        assertFalse(isValidPlacement(board, 10, 5))  // row 1, col 1 — same top-left box
    }

    @Test fun `no conflict when same digit is in different row col and box`() {
        val board = emptyBoard()
        board[0] = 5  // row 0, col 0 — top-left box
        // row 4, col 4 is in the center box — different row, col, and box
        assertTrue(isValidPlacement(board, 40, 5))
    }
}
