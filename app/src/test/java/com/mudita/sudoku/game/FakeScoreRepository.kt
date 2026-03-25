package com.mudita.sudoku.game

import com.mudita.sudoku.puzzle.model.Difficulty

/**
 * In-memory test double for [ScoreRepository].
 *
 * Tracks save call count for assertion in ViewModel tests.
 * Provides [preloadScore] to seed scores without incrementing [saveCallCount].
 */
class FakeScoreRepository : ScoreRepository {
    private val scores = mutableMapOf<Difficulty, Int>()

    var saveCallCount = 0
        private set

    override suspend fun getBestScore(difficulty: Difficulty): Int? = scores[difficulty]

    override suspend fun saveBestScore(difficulty: Difficulty, score: Int) {
        scores[difficulty] = score
        saveCallCount++
    }

    /** Test helper to pre-load a score without incrementing [saveCallCount]. */
    fun preloadScore(difficulty: Difficulty, score: Int) {
        scores[difficulty] = score
    }
}
