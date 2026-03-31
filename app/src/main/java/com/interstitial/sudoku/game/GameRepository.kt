package com.interstitial.sudoku.game

import com.interstitial.sudoku.game.model.GameUiState

/**
 * Contract for persisting and restoring game state.
 *
 * Implementations must be coroutine-safe and must not perform blocking I/O
 * on the calling dispatcher — all operations use suspend and should dispatch
 * to Dispatchers.IO internally if they touch disk or network.
 */
interface GameRepository {
    suspend fun saveGame(state: GameUiState)
    suspend fun loadGame(): GameUiState?
    suspend fun clearGame()
}

/**
 * No-op implementation used as a default when no persistence is needed
 * (e.g., before Plan 02 wires up the real DataStoreGameRepository).
 *
 * loadGame() always returns null — no saved game exists.
 * saveGame() and clearGame() do nothing.
 */
class NoOpGameRepository : GameRepository {
    override suspend fun saveGame(state: GameUiState) {}
    override suspend fun loadGame(): GameUiState? = null
    override suspend fun clearGame() {}
}
