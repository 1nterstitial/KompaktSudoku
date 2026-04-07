package com.interstitial.sudoku.game

import com.interstitial.sudoku.game.model.PersistedGameState

interface GameRepository {
    suspend fun saveGame(state: PersistedGameState)
    suspend fun loadGame(): PersistedGameState?
    suspend fun clearGame()
    suspend fun hasSavedGame(): Boolean
}
