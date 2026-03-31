package com.interstitial.sudoku.puzzle

import com.interstitial.sudoku.puzzle.engine.SudokuGenerator
import com.interstitial.sudoku.puzzle.engine.UniquenessVerifier
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.interstitial.sudoku.puzzle.model.EASY_CONFIG
import com.interstitial.sudoku.puzzle.model.HARD_CONFIG
import com.interstitial.sudoku.puzzle.model.MEDIUM_CONFIG
import org.junit.Assert.*
import org.junit.Test

class SudokuGeneratorTest {

    private val generator = SudokuGenerator()

    @Test fun `generated puzzle board has exactly 81 cells`() {
        val puzzle = generator.generatePuzzle(Difficulty.EASY)
        assertEquals(81, puzzle.board.size)
    }

    @Test fun `generated puzzle solution contains no zeros`() {
        val puzzle = generator.generatePuzzle(Difficulty.EASY)
        assertTrue("Solution must have no zeros", puzzle.solution.none { it == 0 })
        assertEquals(81, puzzle.solution.size)
    }

    @Test fun `20 easy puzzles have givenCount in 36 to 45`() {
        repeat(20) { i ->
            val puzzle = generator.generatePuzzle(Difficulty.EASY)
            assertTrue(
                "Easy puzzle #$i has ${puzzle.givenCount} givens, expected 36–45",
                puzzle.givenCount in EASY_CONFIG.minGivens..EASY_CONFIG.maxGivens
            )
        }
    }

    @Test fun `20 medium puzzles have givenCount in 27 to 35`() {
        repeat(20) { i ->
            val puzzle = generator.generatePuzzle(Difficulty.MEDIUM)
            assertTrue(
                "Medium puzzle #$i has ${puzzle.givenCount} givens, expected 27–35",
                puzzle.givenCount in MEDIUM_CONFIG.minGivens..MEDIUM_CONFIG.maxGivens
            )
        }
    }

    @Test fun `20 hard puzzles have givenCount in 22 to 27`() {
        repeat(20) { i ->
            val puzzle = generator.generatePuzzle(Difficulty.HARD)
            assertTrue(
                "Hard puzzle #$i has ${puzzle.givenCount} givens, expected 22–27",
                puzzle.givenCount in HARD_CONFIG.minGivens..HARD_CONFIG.maxGivens
            )
        }
    }

    @Test fun `generatePuzzle throws after maxAttempts exceeded`() {
        // Anonymous subclass works because UniquenessVerifier and hasUniqueSolution are open (Plan 02)
        val alwaysRejectVerifier = object : UniquenessVerifier() {
            override fun hasUniqueSolution(puzzle: IntArray) = false
        }
        val strictGenerator = SudokuGenerator(
            verifier = alwaysRejectVerifier,
            maxAttempts = 3
        )
        try {
            strictGenerator.generatePuzzle(Difficulty.EASY)
            fail("Expected PuzzleGenerationException")
        } catch (e: PuzzleGenerationException) {
            assertTrue(e.message!!.contains("3 attempts"))
        }
    }
}
