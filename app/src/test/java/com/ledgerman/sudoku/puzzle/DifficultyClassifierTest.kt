package com.ledgerman.sudoku.puzzle

import com.ledgerman.sudoku.puzzle.engine.DifficultyClassifier
import com.ledgerman.sudoku.puzzle.model.*
import org.junit.Assert.*
import org.junit.Test

class DifficultyClassifierTest {

    private val classifier = DifficultyClassifier()

    // A known naked-singles-only puzzle: every empty cell can be determined by naked singles alone.
    // This is the canonical "easy" sudoku from Wikipedia's Sudoku article.
    private val nakedSinglesBoard = intArrayOf(
        5,3,0, 0,7,0, 0,0,0,
        6,0,0, 1,9,5, 0,0,0,
        0,9,8, 0,0,0, 0,6,0,
        8,0,0, 0,6,0, 0,0,3,
        4,0,0, 8,0,3, 0,0,1,
        7,0,0, 0,2,0, 0,0,6,
        0,6,0, 0,0,0, 2,8,0,
        0,0,0, 4,1,9, 0,0,5,
        0,0,0, 0,8,0, 0,7,9
    )

    private fun makePuzzle(board: IntArray) = SudokuPuzzle(
        board = board,
        solution = IntArray(81) { 1 },  // dummy solution — classifier only reads the board
        difficulty = Difficulty.EASY
    )

    @Test fun `puzzle solvable by naked singles classifies as NAKED_SINGLES_ONLY`() {
        val puzzle = makePuzzle(nakedSinglesBoard)
        assertEquals(TechniqueTier.NAKED_SINGLES_ONLY, classifier.classifyTechniqueTier(puzzle))
    }

    @Test fun `puzzle requiring hidden pairs does not classify as NAKED_SINGLES_ONLY`() {
        // A board where naked-singles alone leave empty cells → not NAKED_SINGLES_ONLY
        val hardBoard = intArrayOf(
            0,0,0, 6,0,0, 4,0,0,
            7,0,0, 0,0,3, 6,0,0,
            0,0,0, 0,9,1, 0,8,0,
            0,0,0, 0,0,0, 0,0,0,
            0,5,0, 1,8,0, 0,0,3,
            0,0,0, 3,0,6, 0,4,5,
            0,4,0, 2,0,0, 0,6,0,
            9,0,3, 0,0,0, 0,0,0,
            0,2,0, 0,0,0, 1,0,0
        )
        val puzzle = makePuzzle(hardBoard)
        val tier = classifier.classifyTechniqueTier(puzzle)
        assertNotEquals(
            "A board requiring hidden pairs must not classify as NAKED_SINGLES_ONLY",
            TechniqueTier.NAKED_SINGLES_ONLY, tier
        )
    }

    @Test fun `puzzle requiring hidden pairs classifies as HIDDEN_PAIRS or ADVANCED`() {
        val hardBoard = intArrayOf(
            0,0,0, 6,0,0, 4,0,0,
            7,0,0, 0,0,3, 6,0,0,
            0,0,0, 0,9,1, 0,8,0,
            0,0,0, 0,0,0, 0,0,0,
            0,5,0, 1,8,0, 0,0,3,
            0,0,0, 3,0,6, 0,4,5,
            0,4,0, 2,0,0, 0,6,0,
            9,0,3, 0,0,0, 0,0,0,
            0,2,0, 0,0,0, 1,0,0
        )
        val puzzle = makePuzzle(hardBoard)
        val tier = classifier.classifyTechniqueTier(puzzle)
        assertTrue(
            "Hard board should classify as HIDDEN_PAIRS or ADVANCED, got $tier",
            tier == TechniqueTier.HIDDEN_PAIRS || tier == TechniqueTier.ADVANCED
        )
    }

    @Test fun `meetsRequirements returns true when tier matches exactly`() {
        val puzzle = makePuzzle(nakedSinglesBoard)
        assertTrue(classifier.meetsRequirements(puzzle, EASY_CONFIG))
    }

    @Test fun `ADVANCED puzzle does not meet NAKED_SINGLES_ONLY requirement`() {
        val hardBoard = intArrayOf(
            0,0,0, 6,0,0, 4,0,0,
            7,0,0, 0,0,3, 6,0,0,
            0,0,0, 0,9,1, 0,8,0,
            0,0,0, 0,0,0, 0,0,0,
            0,5,0, 1,8,0, 0,0,3,
            0,0,0, 3,0,6, 0,4,5,
            0,4,0, 2,0,0, 0,6,0,
            9,0,3, 0,0,0, 0,0,0,
            0,2,0, 0,0,0, 1,0,0
        )
        val puzzle = makePuzzle(hardBoard)
        // If this puzzle requires more than naked singles, it must not meet Easy requirements
        if (classifier.classifyTechniqueTier(puzzle) != TechniqueTier.NAKED_SINGLES_ONLY) {
            assertFalse(
                "A non-naked-singles puzzle must not meet EASY_CONFIG requirements",
                classifier.meetsRequirements(puzzle, EASY_CONFIG)
            )
        }
    }
}
