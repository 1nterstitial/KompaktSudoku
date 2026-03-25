package com.mudita.sudoku.puzzle

/**
 * Thrown when [SudokuGenerator] cannot find a valid puzzle within the configured attempt limit.
 *
 * This should not occur in normal operation; if seen in production, increase [maxAttempts]
 * or investigate technique classifier rejections for the given difficulty.
 */
class PuzzleGenerationException(message: String) : Exception(message)
