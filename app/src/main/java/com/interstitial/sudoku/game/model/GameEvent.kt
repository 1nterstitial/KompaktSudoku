package com.interstitial.sudoku.game.model

import com.interstitial.sudoku.puzzle.model.Difficulty

sealed class GameEvent {
    data class Completed(
        val difficulty: Difficulty,
        val elapsedMs: Long,
        val hintsUsed: Int,
        val isPersonalBest: Boolean
    ) : GameEvent()

    data class HintUnavailable(val message: String) : GameEvent()
}
