package com.mudita.sudoku.game.model

import com.mudita.sudoku.puzzle.model.Difficulty

/**
 * Sealed class for one-shot game events emitted via SharedFlow.
 *
 * Events are emitted once and not replayed on recomposition (SharedFlow with replay=0).
 * The UI observes these to trigger navigation or dialogs.
 */
sealed class GameEvent {

    /**
     * Emitted when all 81 cells match the solution.
     *
     * Carries the full completion payload so the SummaryScreen can display results
     * without requiring a separate state query.
     *
     * @param errorCount      Total number of incorrect digit entries made during play.
     * @param hintCount       Total number of hints used during play.
     * @param score           Final score computed as max(0, 100 - errorCount*10 - hintCount*5).
     * @param difficulty      Difficulty tier the puzzle was generated for.
     * @param isPersonalBest  True if [score] exceeds the previously stored high score for [difficulty].
     */
    data class Completed(
        val errorCount: Int,
        val hintCount: Int,
        val score: Int,
        val difficulty: Difficulty,
        val isPersonalBest: Boolean
    ) : GameEvent()
}
