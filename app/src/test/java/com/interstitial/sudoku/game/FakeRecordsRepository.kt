package com.interstitial.sudoku.game

import com.interstitial.sudoku.game.model.DifficultyRecord
import com.interstitial.sudoku.puzzle.model.Difficulty

class FakeRecordsRepository : RecordsRepository {
    private val records = mutableMapOf<Difficulty, DifficultyRecord>()
    override suspend fun getRecord(difficulty: Difficulty) = records[difficulty] ?: DifficultyRecord()
    override suspend fun updateRecord(difficulty: Difficulty, record: DifficultyRecord) { records[difficulty] = record }
    override suspend fun getAllRecords() = Difficulty.entries.associateWith { getRecord(it) }
}
