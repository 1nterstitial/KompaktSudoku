package com.interstitial.sudoku.game

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.interstitial.sudoku.game.model.GameUiState
import com.interstitial.sudoku.game.model.PersistedGameState
import com.interstitial.sudoku.game.model.toPersistedState
import com.interstitial.sudoku.game.model.toUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** DataStore instance scoped to the Application context via Kotlin property delegation. */
val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "game_state")

private val IN_PROGRESS_GAME_KEY = stringPreferencesKey("in_progress_game")

/**
 * DataStore-backed implementation of [GameRepository].
 *
 * All operations dispatch to [Dispatchers.IO] to avoid ANR on the main thread.
 * JSON serialization uses [kotlinx.serialization] — [PersistedGameState] is @Serializable.
 *
 * Corrupt or missing data in DataStore is treated as "no saved game" — [loadGame] returns null
 * on any deserialization exception rather than crashing. This is safe: the player starts fresh.
 *
 * @param dataStore Injected [DataStore] instance — use [Context.gameDataStore] in production.
 */
class DataStoreGameRepository(
    private val dataStore: DataStore<Preferences>
) : GameRepository {

    override suspend fun saveGame(state: GameUiState) {
        withContext(Dispatchers.IO) {
            val json = Json.encodeToString(state.toPersistedState())
            dataStore.edit { prefs ->
                prefs[IN_PROGRESS_GAME_KEY] = json
            }
        }
    }

    override suspend fun loadGame(): GameUiState? {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = dataStore.data.first()
                val json = prefs[IN_PROGRESS_GAME_KEY] ?: return@withContext null
                @Suppress("DEPRECATION")
                Json.decodeFromString<PersistedGameState>(json).toUiState()
            } catch (e: Exception) {
                // Corrupt or unreadable data — treat as no saved game
                null
            }
        }
    }

    override suspend fun clearGame() {
        withContext(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs.remove(IN_PROGRESS_GAME_KEY)
            }
        }
    }
}
