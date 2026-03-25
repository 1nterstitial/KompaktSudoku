package com.mudita.sudoku.game.model

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
     * @param errorCount  Total number of incorrect digit entries made during play.
     *                    Used to calculate the final score.
     */
    data class Completed(val errorCount: Int) : GameEvent()
}
