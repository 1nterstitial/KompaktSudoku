package com.interstitial.sudoku.game.model

import kotlinx.serialization.Serializable

@Serializable
data class PersistedGameState(
    val schemaVersion: Int = 1,
    val board: List<Int>,
    val solution: List<Int>,
    val givens: List<Boolean>,
    val notes: List<List<Int>>,
    val difficulty: String,
    val elapsedMs: Long,
    val hintsUsed: Int,
    val hintedCells: List<Int>
)
