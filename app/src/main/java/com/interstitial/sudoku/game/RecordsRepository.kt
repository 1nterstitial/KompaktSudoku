package com.interstitial.sudoku.game

import com.interstitial.sudoku.game.model.DifficultyRecord
import com.interstitial.sudoku.puzzle.model.Difficulty

interface RecordsRepository {
    suspend fun getRecord(difficulty: Difficulty): DifficultyRecord
    suspend fun updateRecord(difficulty: Difficulty, record: DifficultyRecord)
    suspend fun getAllRecords(): Map<Difficulty, DifficultyRecord>
}
