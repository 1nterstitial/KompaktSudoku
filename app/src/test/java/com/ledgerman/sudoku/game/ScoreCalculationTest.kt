package com.ledgerman.sudoku.game

import com.ledgerman.sudoku.game.model.calculateScore
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the score formula: max(0, 100 - errorCount * 10 - hintCount * 5).
 */
class ScoreCalculationTest {

    @Test
    fun `perfect game - 0 errors 0 hints - returns 100`() {
        assertEquals(100, calculateScore(errorCount = 0, hintCount = 0))
    }

    @Test
    fun `3 errors 2 hints - returns 60`() {
        // 100 - 30 - 10 = 60
        assertEquals(60, calculateScore(errorCount = 3, hintCount = 2))
    }

    @Test
    fun `10 errors 0 hints - returns 0`() {
        // 100 - 100 - 0 = 0 (exactly at floor)
        assertEquals(0, calculateScore(errorCount = 10, hintCount = 0))
    }

    @Test
    fun `5 errors 5 hints - returns 25`() {
        // 100 - 50 - 25 = 25
        assertEquals(25, calculateScore(errorCount = 5, hintCount = 5))
    }

    @Test
    fun `11 errors 0 hints - floors at 0 not negative`() {
        // max(0, 100 - 110 - 0) = max(0, -10) = 0
        assertEquals(0, calculateScore(errorCount = 11, hintCount = 0))
    }

    @Test
    fun `0 errors 20 hints - floors at 0 not negative`() {
        // max(0, 100 - 0 - 100) = max(0, 0) = 0
        assertEquals(0, calculateScore(errorCount = 0, hintCount = 20))
    }

    @Test
    fun `1 error 0 hints - returns 90`() {
        assertEquals(90, calculateScore(errorCount = 1, hintCount = 0))
    }

    @Test
    fun `0 errors 1 hint - returns 95`() {
        assertEquals(95, calculateScore(errorCount = 0, hintCount = 1))
    }
}
