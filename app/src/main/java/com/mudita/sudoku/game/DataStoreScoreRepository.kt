package com.mudita.sudoku.game

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mudita.sudoku.puzzle.model.Difficulty
import kotlinx.coroutines.flow.first

/**
 * DataStore instance for high scores, scoped to the Application context.
 *
 * MUST be a separate instance from [gameDataStore] — a single DataStore file can only have
 * one active instance per process. Using two instances on the same file causes data corruption.
 */
val Context.scoreDataStore: DataStore<Preferences> by preferencesDataStore(name = "score_state")

private val HIGH_SCORE_EASY = intPreferencesKey("high_score_easy")
private val HIGH_SCORE_MEDIUM = intPreferencesKey("high_score_medium")
private val HIGH_SCORE_HARD = intPreferencesKey("high_score_hard")

/**
 * DataStore-backed implementation of [ScoreRepository].
 *
 * Stores one Int per difficulty using [intPreferencesKey]. Keys follow the naming convention
 * documented in CLAUDE.md §Local Persistence: high_score_easy, high_score_medium, high_score_hard.
 *
 * No [kotlinx.coroutines.Dispatchers.IO] dispatch inside the repository — the ViewModel caller
 * is responsible for dispatching to IO (consistent with the [DataStoreGameRepository] pattern).
 *
 * @param dataStore Injected [DataStore] instance — use [Context.scoreDataStore] in production.
 */
class DataStoreScoreRepository(
    private val dataStore: DataStore<Preferences>
) : ScoreRepository {

    override suspend fun getBestScore(difficulty: Difficulty): Int? {
        val prefs = dataStore.data.first()
        return prefs[difficulty.toPreferenceKey()]
    }

    override suspend fun saveBestScore(difficulty: Difficulty, score: Int) {
        dataStore.edit { prefs ->
            prefs[difficulty.toPreferenceKey()] = score
        }
    }

    private fun Difficulty.toPreferenceKey(): Preferences.Key<Int> = when (this) {
        Difficulty.EASY -> HIGH_SCORE_EASY
        Difficulty.MEDIUM -> HIGH_SCORE_MEDIUM
        Difficulty.HARD -> HIGH_SCORE_HARD
    }
}
