package com.interstitial.sudoku.game.model

import kotlinx.serialization.Serializable

@Serializable
data class DifficultyRecord(
    val completedCount: Int = 0,
    val bestTimeMs: Long? = null,
    val bestNoHintTimeMs: Long? = null,
    val lastCompletedEpochMs: Long? = null
)
