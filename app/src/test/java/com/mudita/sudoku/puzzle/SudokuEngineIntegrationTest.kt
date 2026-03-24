package com.mudita.sudoku.puzzle

import com.mudita.sudoku.puzzle.engine.DifficultyClassifier
import com.mudita.sudoku.puzzle.engine.SudokuGenerator
import com.mudita.sudoku.puzzle.engine.UniquenessVerifier
import com.mudita.sudoku.puzzle.model.*
import org.junit.Assert.*
import org.junit.Test

class SudokuEngineIntegrationTest {

    private val generator   = SudokuGenerator()
    private val verifier    = UniquenessVerifier()
    private val classifier  = DifficultyClassifier()

    // --- PUZZ-01: every generated puzzle has exactly one solution ---

    @Test fun `20 easy puzzles all have unique solutions`() {
        repeat(20) { i ->
            val puzzle = generator.generatePuzzle(Difficulty.EASY)
            assertTrue(
                "Easy puzzle #$i failed uniqueness check",
                verifier.hasUniqueSolution(puzzle.board)
            )
        }
    }

    @Test fun `20 medium puzzles all have unique solutions`() {
        repeat(20) { i ->
            val puzzle = generator.generatePuzzle(Difficulty.MEDIUM)
            assertTrue(
                "Medium puzzle #$i failed uniqueness check",
                verifier.hasUniqueSolution(puzzle.board)
            )
        }
    }

    @Test fun `20 hard puzzles all have unique solutions`() {
        repeat(20) { i ->
            val puzzle = generator.generatePuzzle(Difficulty.HARD)
            assertTrue(
                "Hard puzzle #$i failed uniqueness check",
                verifier.hasUniqueSolution(puzzle.board)
            )
        }
    }

    // --- PUZZ-02: technique tier matches requested difficulty ---
    // Note: Sudoklify presets do not produce HIDDEN_PAIRS-tier puzzles.
    // MEDIUM_CONFIG.requiredTechniqueTier is NAKED_SINGLES_ONLY (27–35 givens differentiates from EASY).
    // HARD_CONFIG.requiredTechniqueTier is ADVANCED.

    @Test fun `20 easy puzzles meet NAKED_SINGLES_ONLY technique requirement`() {
        repeat(20) { i ->
            val puzzle = generator.generatePuzzle(Difficulty.EASY)
            assertTrue(
                "Easy puzzle #$i failed technique classification. Actual tier: ${classifier.classifyTechniqueTier(puzzle)}",
                classifier.meetsRequirements(puzzle, EASY_CONFIG)
            )
        }
    }

    @Test fun `20 medium puzzles meet MEDIUM_CONFIG technique requirement`() {
        repeat(20) { i ->
            val puzzle = generator.generatePuzzle(Difficulty.MEDIUM)
            assertTrue(
                "Medium puzzle #$i failed technique classification. Actual tier: ${classifier.classifyTechniqueTier(puzzle)}",
                classifier.meetsRequirements(puzzle, MEDIUM_CONFIG)
            )
        }
    }

    @Test fun `20 hard puzzles meet ADVANCED technique requirement`() {
        repeat(20) { i ->
            val puzzle = generator.generatePuzzle(Difficulty.HARD)
            assertTrue(
                "Hard puzzle #$i failed technique classification. Actual tier: ${classifier.classifyTechniqueTier(puzzle)}",
                classifier.meetsRequirements(puzzle, HARD_CONFIG)
            )
        }
    }

    // --- PUZZ-03 performance proxy (device validation is manual per VALIDATION.md) ---

    @Test fun `generation of easy puzzle completes in under 2000ms on JVM`() {
        val start = System.currentTimeMillis()
        generator.generatePuzzle(Difficulty.EASY)
        val elapsed = System.currentTimeMillis() - start
        assertTrue(
            "Easy generation took ${elapsed}ms on JVM, expected < 2000ms",
            elapsed < 2000
        )
    }

    @Test fun `generation of hard puzzle completes in under 2000ms on JVM`() {
        val start = System.currentTimeMillis()
        generator.generatePuzzle(Difficulty.HARD)
        val elapsed = System.currentTimeMillis() - start
        assertTrue(
            "Hard generation took ${elapsed}ms on JVM, expected < 2000ms. " +
            "If this fails intermittently, consider a background pre-generation pool per RESEARCH.md Pitfall 3.",
            elapsed < 2000
        )
    }
}
