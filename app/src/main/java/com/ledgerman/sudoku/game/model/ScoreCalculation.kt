package com.ledgerman.sudoku.game.model

/**
 * Computes the final score for a completed Sudoku game.
 *
 * Formula: max(0, 100 - errorCount * 10 - hintCount * 5)
 *
 * - Perfect game (0 errors, 0 hints) = 100 points.
 * - Each error deducts 10 points.
 * - Each hint deducts 5 points.
 * - Score floors at 0 — never goes negative.
 *
 * @param errorCount  Number of incorrect digit entries made during play.
 * @param hintCount   Number of hints requested during play.
 * @return            Final score in the range [0, 100].
 */
fun calculateScore(errorCount: Int, hintCount: Int): Int =
    maxOf(0, 100 - errorCount * 10 - hintCount * 5)
