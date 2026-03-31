package com.interstitial.sudoku.game

import com.interstitial.sudoku.puzzle.model.Difficulty
import com.interstitial.sudoku.puzzle.model.SudokuPuzzle

/**
 * Deterministic puzzle generator for ViewModel and game-logic unit tests.
 *
 * Returns the same known-valid 9x9 Sudoku puzzle regardless of the requested difficulty,
 * tagged with the requested difficulty for tests that check the difficulty field.
 *
 * Advantages over using a real SudokuGenerator in tests:
 *   - Instant (no backtracking, no Sudoklify calls)
 *   - Deterministic (same board every test run)
 *   - Known empty cells and correct/wrong digits (test helpers below)
 *
 * BOARD has 20 cells zeroed out (indices: 0,2,5,9,11,14,18,20,23,27,29,32,
 * 36,38,41,45,47,50,54,56). This leaves 61 given cells (easy-level range)
 * and 20 empty cells for testing fill, pencil, and undo operations.
 */
class FakeGenerator {

    companion object {

        /**
         * Complete 9x9 Sudoku solution — no zeros, every row/col/box valid.
         * Index = row*9+col.
         */
        val SOLUTION = intArrayOf(
            5, 3, 4, 6, 7, 8, 9, 1, 2,  // row 0
            6, 7, 2, 1, 9, 5, 3, 4, 8,  // row 1
            1, 9, 8, 3, 4, 2, 5, 6, 7,  // row 2
            8, 5, 9, 7, 6, 1, 4, 2, 3,  // row 3
            4, 2, 6, 8, 5, 3, 7, 9, 1,  // row 4
            7, 1, 3, 9, 2, 4, 8, 5, 6,  // row 5
            9, 6, 1, 5, 3, 7, 2, 8, 4,  // row 6
            2, 8, 7, 4, 1, 9, 6, 3, 5,  // row 7
            3, 4, 5, 2, 8, 6, 1, 7, 9   // row 8
        )

        // Indices of cells to zero out — 20 empty cells spread across the grid
        private val EMPTY_CELL_INDICES = setOf(
            0, 2, 5,          // row 0: cells 0,2,5
            9, 11, 14,        // row 1: cells 0,2,5 (shifted by 9)
            18, 20, 23,       // row 2: cells 0,2,5 (shifted by 18)
            27, 29, 32,       // row 3: cells 0,2,5 (shifted by 27)
            36, 38, 41,       // row 4: cells 0,2,5 (shifted by 36)
            45, 47, 50,       // row 5: cells 0,2,5 (shifted by 45)
            54, 56            // row 6: cells 0,2 (shifted by 54)
        )

        /**
         * Puzzle board: SOLUTION with 20 cells zeroed out.
         * 61 given cells (easy-level), 20 empty cells for test operations.
         */
        val BOARD = IntArray(81) { index ->
            if (index in EMPTY_CELL_INDICES) 0 else SOLUTION[index]
        }

        /**
         * Returns the indices of all empty cells in BOARD (where value == 0).
         * Use this in tests to find valid cells for fill/pencil operations.
         */
        fun emptyIndices(): List<Int> = BOARD.indices.filter { BOARD[it] == 0 }

        /**
         * Returns the correct digit for the given cell index.
         * Shorthand for SOLUTION[index].
         */
        fun correctDigitAt(index: Int): Int = SOLUTION[index]

        /**
         * Returns a digit (1–9) that is NOT the correct answer for the given cell.
         * Use this in tests that need to trigger an error count increment.
         */
        fun wrongDigitAt(index: Int): Int = (1..9).first { it != SOLUTION[index] }
    }

    /**
     * Returns a deterministic [SudokuPuzzle] tagged with [difficulty].
     * Always returns the same board/solution regardless of difficulty value.
     */
    fun generatePuzzle(difficulty: Difficulty): SudokuPuzzle =
        SudokuPuzzle(
            board = BOARD.copyOf(),
            solution = SOLUTION.copyOf(),
            difficulty = difficulty
        )
}
