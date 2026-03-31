package com.interstitial.sudoku.game

import com.interstitial.sudoku.game.model.GameUiState

/**
 * In-memory test double for [GameRepository].
 *
 * Stores a single [GameUiState] in memory. Tracks call counts for [saveGame] and [clearGame]
 * so tests can assert that the ViewModel triggered persistence at the expected times.
 *
 * @param savedState Optional pre-loaded game state — simulates an app launch with a saved game.
 */
class FakeGameRepository(
    private var savedState: GameUiState? = null
) : GameRepository {

    var saveCallCount = 0
        private set

    var clearCallCount = 0
        private set

    override suspend fun saveGame(state: GameUiState) {
        savedState = state
        saveCallCount++
    }

    override suspend fun loadGame(): GameUiState? = savedState

    override suspend fun clearGame() {
        savedState = null
        clearCallCount++
    }
}
