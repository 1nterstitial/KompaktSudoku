package com.interstitial.sudoku.puzzle

import com.interstitial.sudoku.puzzle.engine.UniquenessVerifier
import org.junit.Assert.*
import org.junit.Test

class UniquenessVerifierTest {

    private val verifier = UniquenessVerifier()

    // A minimal known-unique puzzle (first published 17-clue puzzle)
    // Source: Gordon Royle's list of 17-clue Sudoku puzzles
    private val knownUniquePuzzle = intArrayOf(
        0,0,0, 0,0,0, 0,1,0,
        0,0,0, 0,0,2, 0,0,3,
        0,0,0, 4,0,0, 0,0,0,
        0,0,0, 0,0,0, 5,0,0,
        4,0,1, 6,0,0, 0,0,0,
        0,0,7, 1,0,0, 0,0,0,
        0,5,0, 0,0,0, 2,0,0,
        0,0,0, 0,8,0, 0,4,0,
        0,3,0, 9,1,0, 0,0,0
    )

    @Test fun `known unique-solution puzzle returns true`() {
        assertTrue(verifier.hasUniqueSolution(knownUniquePuzzle))
    }

    @Test fun `known multi-solution board returns false`() {
        // Construct a board with 79 empty cells and 2 cells that can be swapped: definitely multi-solution
        val board = IntArray(81) { 0 }
        // Fill 79 cells; leave indices 0 and 1 empty where both (1,2) and (2,1) are valid
        board[2] = 3; board[3] = 4; board[4] = 5; board[5] = 6; board[6] = 7; board[7] = 8; board[8] = 9
        // With only row 0 cols 0 and 1 empty and digits 1 and 2 both valid for those positions,
        // and no constraints forcing one arrangement, this is definitionally multi-solution
        assertFalse(verifier.hasUniqueSolution(board))
    }

    @Test fun `fully filled valid board returns true`() {
        // A known valid completed Sudoku board
        val filled = intArrayOf(
            5,3,4, 6,7,8, 9,1,2,
            6,7,2, 1,9,5, 3,4,8,
            1,9,8, 3,4,2, 5,6,7,
            8,5,9, 7,6,1, 4,2,3,
            4,2,6, 8,5,3, 7,9,1,
            7,1,3, 9,2,4, 8,5,6,
            9,6,1, 5,3,7, 2,8,4,
            2,8,7, 4,1,9, 6,3,5,
            3,4,5, 2,8,6, 1,7,9
        )
        assertTrue(verifier.hasUniqueSolution(filled))
    }

    @Test fun `countSolutions aborts at limit 2 and does not search full tree`() {
        // An all-zeros board has millions of solutions; countSolutions(limit=2) must return quickly
        val emptyBoard = IntArray(81) { 0 }
        val start = System.currentTimeMillis()
        val count = verifier.countSolutions(emptyBoard.copyOf(), limit = 2)
        val elapsed = System.currentTimeMillis() - start
        assertTrue("countSolutions must return >= 2 for empty board", count >= 2)
        assertTrue("Abort must happen within 5000ms; took ${elapsed}ms", elapsed < 5000)
    }
}
