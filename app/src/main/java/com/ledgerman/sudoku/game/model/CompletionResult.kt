package com.ledgerman.sudoku.game.model

import com.ledgerman.sudoku.puzzle.model.Difficulty

/**
 * Carries completion data from GameViewModel to SummaryScreen.
 *
 * Not persisted — in-memory only. No @Serializable needed.
 *
 * @param difficulty      Difficulty tier the puzzle was generated for.
 * @param errorCount      Total incorrect digit entries made during play.
 * @param hintCount       Total hints used during play.
 * @param finalScore      Computed score: max(0, 100 - errorCount*10 - hintCount*5).
 * @param isPersonalBest  True if [finalScore] exceeds the previously stored high score for [difficulty].
 */
data class CompletionResult(
    val difficulty: Difficulty,
    val errorCount: Int,
    val hintCount: Int,
    val finalScore: Int,
    val isPersonalBest: Boolean
)
