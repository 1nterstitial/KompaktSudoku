package com.interstitial.sudoku.game

import com.interstitial.sudoku.game.model.PersistedGameState

class FakeGameRepository : GameRepository {
    private var saved: PersistedGameState? = null
    override suspend fun saveGame(state: PersistedGameState) { saved = state }
    override suspend fun loadGame(): PersistedGameState? = saved
    override suspend fun clearGame() { saved = null }
    override suspend fun hasSavedGame(): Boolean = saved != null
}
