package com.interstitial.sudoku.puzzle.model

data class SudokuPuzzle(
    val board: IntArray,
    val solution: IntArray,
    val difficulty: Difficulty,
    val givenCount: Int = board.count { it != 0 }
) {
    init {
        require(board.size == 81) { "board must have exactly 81 cells, got ${board.size}" }
        require(solution.size == 81) { "solution must have exactly 81 cells, got ${solution.size}" }
        require(solution.none { it == 0 }) { "solution must be fully filled (no zeros)" }
        require(givenCount in 17..81) { "givenCount $givenCount is below the mathematical minimum of 17" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SudokuPuzzle) return false
        return board.contentEquals(other.board) &&
               solution.contentEquals(other.solution) &&
               difficulty == other.difficulty
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + solution.contentHashCode()
        result = 31 * result + difficulty.hashCode()
        return result
    }
}
