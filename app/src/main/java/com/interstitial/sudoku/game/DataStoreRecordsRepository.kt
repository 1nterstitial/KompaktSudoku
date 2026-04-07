package com.interstitial.sudoku.game

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.interstitial.sudoku.game.model.DifficultyRecord
import com.interstitial.sudoku.puzzle.model.Difficulty
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.recordsDataStore: DataStore<Preferences> by preferencesDataStore(name = "records")

class DataStoreRecordsRepository(private val context: Context) : RecordsRepository {

    private fun keyFor(difficulty: Difficulty) = stringPreferencesKey("record_${difficulty.name}")

    override suspend fun getRecord(difficulty: Difficulty): DifficultyRecord {
        val json = context.recordsDataStore.data.map { it[keyFor(difficulty)] }.first()
            ?: return DifficultyRecord()
        return try {
            Json.decodeFromString(DifficultyRecord.serializer(), json)
        } catch (e: Exception) {
            DifficultyRecord()
        }
    }

    override suspend fun updateRecord(difficulty: Difficulty, record: DifficultyRecord) {
        val json = Json.encodeToString(DifficultyRecord.serializer(), record)
        context.recordsDataStore.edit { it[keyFor(difficulty)] = json }
    }

    override suspend fun getAllRecords(): Map<Difficulty, DifficultyRecord> {
        return Difficulty.entries.associateWith { getRecord(it) }
    }
}
