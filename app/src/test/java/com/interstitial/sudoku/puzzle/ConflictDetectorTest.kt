package com.interstitial.sudoku.puzzle

import com.interstitial.sudoku.puzzle.engine.ConflictDetector
import org.junit.Assert.*
import org.junit.Test

class ConflictDetectorTest {

    private fun emptyBoard() = IntArray(81)

    @Test
    fun `empty board has no conflicts`() {
        val mask = ConflictDetector.buildConflictMask(emptyBoard())
        assertTrue(mask.none { it })
    }

    @Test
    fun `board with no duplicates has no conflicts`() {
        val board = emptyBoard()
        board[0] = 1
        board[1] = 2
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask.none { it })
    }

    @Test
    fun `row duplicate marks both cells`() {
        val board = emptyBoard()
        board[0] = 5
        board[3] = 5
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask[0])
        assertTrue(mask[3])
    }

    @Test
    fun `column duplicate marks both cells`() {
        val board = emptyBoard()
        board[0] = 5
        board[27] = 5
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask[0])
        assertTrue(mask[27])
    }

    @Test
    fun `box duplicate marks both cells`() {
        val board = emptyBoard()
        board[0] = 5
        board[10] = 5
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask[0])
        assertTrue(mask[10])
    }

    @Test
    fun `three-way conflict marks all three cells`() {
        val board = emptyBoard()
        board[0] = 5
        board[1] = 5
        board[9] = 5
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask[0])
        assertTrue(mask[1])
        assertTrue(mask[9])
    }

    @Test
    fun `non-conflicting cells are not marked`() {
        val board = emptyBoard()
        board[0] = 5
        board[3] = 5
        board[40] = 7
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask[0])
        assertTrue(mask[3])
        assertFalse(mask[40])
    }

    @Test
    fun `conflict cleared after value removed`() {
        val board = emptyBoard()
        board[0] = 5
        board[3] = 5
        val mask1 = ConflictDetector.buildConflictMask(board)
        assertTrue(mask1[0])
        board[3] = 0
        val mask2 = ConflictDetector.buildConflictMask(board)
        assertFalse(mask2[0])
        assertFalse(mask2[3])
    }

    @Test
    fun `conflict cleared after value replaced`() {
        val board = emptyBoard()
        board[0] = 5
        board[3] = 5
        val mask1 = ConflictDetector.buildConflictMask(board)
        assertTrue(mask1[0])
        board[3] = 6
        val mask2 = ConflictDetector.buildConflictMask(board)
        assertFalse(mask2[0])
        assertFalse(mask2[3])
    }

    @Test
    fun `valid complete board has no conflicts`() {
        val board = intArrayOf(
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
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask.none { it })
    }
}
