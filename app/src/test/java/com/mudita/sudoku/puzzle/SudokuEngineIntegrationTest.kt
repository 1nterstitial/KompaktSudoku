package com.mudita.sudoku.puzzle

import com.mudita.sudoku.puzzle.model.Difficulty
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test

@Ignore("Wave 0 stub — SudokuGenerator and verifiers not yet implemented")
class SudokuEngineIntegrationTest {

    // PUZZ-01: all 20 generated puzzles have exactly one solution
    @Test fun `20 easy puzzles all have unique solutions`() {
        TODO("Implement after SudokuGenerator and UniquenessVerifier exist")
    }

    @Test fun `20 medium puzzles all have unique solutions`() {
        TODO("Implement after SudokuGenerator and UniquenessVerifier exist")
    }

    @Test fun `20 hard puzzles all have unique solutions`() {
        TODO("Implement after SudokuGenerator and UniquenessVerifier exist")
    }

    // PUZZ-02: technique classification matches requested difficulty
    @Test fun `20 easy puzzles meet NAKED_SINGLES_ONLY technique requirement`() {
        TODO("Implement after DifficultyClassifier exists")
    }

    @Test fun `20 medium puzzles meet HIDDEN_PAIRS technique requirement`() {
        TODO("Implement after DifficultyClassifier exists")
    }

    @Test fun `20 hard puzzles meet ADVANCED technique requirement`() {
        TODO("Implement after DifficultyClassifier exists")
    }

    // Performance (manual on device; automated timing proxy on JVM)
    @Test fun `generation of easy puzzle completes in under 2000ms on JVM`() {
        TODO("Implement after SudokuGenerator exists")
    }

    @Test fun `generation of hard puzzle completes in under 2000ms on JVM`() {
        TODO("Implement after SudokuGenerator exists")
    }
}
