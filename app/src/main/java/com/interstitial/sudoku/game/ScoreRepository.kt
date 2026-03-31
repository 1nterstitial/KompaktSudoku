package com.interstitial.sudoku.game

import com.interstitial.sudoku.puzzle.model.Difficulty

/**
 * Contract for reading and writing per-difficulty high scores.
 *
 * Simpler than [GameRepository] — raw Int preferences, no JSON serialization.
 * Returns null from [getBestScore] when no score has been stored yet for a difficulty.
 */
interface ScoreRepository {
    suspend fun getBestScore(difficulty: Difficulty): Int?
    suspend fun saveBestScore(difficulty: Difficulty, score: Int)
}

/**
 * No-op default used when no persistence is needed
 * (e.g., before wiring in MainActivity or in tests that don't require score persistence).
 *
 * [getBestScore] always returns null — no high scores exist.
 * [saveBestScore] does nothing.
 */
class NoOpScoreRepository : ScoreRepository {
    override suspend fun getBestScore(difficulty: Difficulty): Int? = null
    override suspend fun saveBestScore(difficulty: Difficulty, score: Int) {}
}
