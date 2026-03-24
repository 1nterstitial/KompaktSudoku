package com.mudita.sudoku.puzzle

import com.mudita.sudoku.puzzle.model.Difficulty
import com.mudita.sudoku.puzzle.model.TechniqueTier
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test

@Ignore("Wave 0 stub — DifficultyClassifier not yet implemented")
class DifficultyClassifierTest {

    @Test fun `puzzle solvable by naked singles classifies as NAKED_SINGLES_ONLY`() {
        TODO("Implement after DifficultyClassifier exists")
    }

    @Test fun `puzzle requiring hidden pairs does not classify as NAKED_SINGLES_ONLY`() {
        TODO("Implement after DifficultyClassifier exists")
    }

    @Test fun `puzzle requiring hidden pairs classifies as HIDDEN_PAIRS or ADVANCED`() {
        TODO("Implement after DifficultyClassifier exists")
    }

    @Test fun `puzzle requiring X-wing classifies as ADVANCED`() {
        TODO("Implement after DifficultyClassifier exists")
    }

    @Test fun `ADVANCED puzzle does not meet NAKED_SINGLES_ONLY requirement`() {
        TODO("Implement after DifficultyClassifier exists")
    }
}
