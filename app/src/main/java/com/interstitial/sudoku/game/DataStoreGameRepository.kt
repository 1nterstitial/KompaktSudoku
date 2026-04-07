package com.interstitial.sudoku.game

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.interstitial.sudoku.game.model.PersistedGameState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "game_state")

class DataStoreGameRepository(private val context: Context) : GameRepository {

    private val key = stringPreferencesKey("in_progress_game")

    override suspend fun saveGame(state: PersistedGameState) {
        val json = Json.encodeToString(PersistedGameState.serializer(), state)
        context.gameDataStore.edit { it[key] = json }
    }

    override suspend fun loadGame(): PersistedGameState? {
        val json = context.gameDataStore.data.map { it[key] }.first() ?: return null
        return try {
            Json.decodeFromString(PersistedGameState.serializer(), json)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearGame() {
        context.gameDataStore.edit { it.remove(key) }
    }

    override suspend fun hasSavedGame(): Boolean {
        return context.gameDataStore.data.map { it.contains(key) }.first()
    }
}
