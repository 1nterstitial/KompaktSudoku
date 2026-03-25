package com.mudita.sudoku.puzzle.engine

/**
 * Verifies that a Sudoku puzzle has exactly one valid solution.
 *
 * Uses an abort-on-second-solution backtracking search. Aborts as soon as a second
 * solution is found, so it never needlessly searches the full tree.
 *
 * Declared open to allow test doubles in SudokuGeneratorTest.
 */
open class UniquenessVerifier {

    /**
     * Returns true if [puzzle] has exactly one valid solution; false if zero or multiple.
     *
     * @param puzzle 81-element IntArray; 0 = empty cell. The array is NOT mutated.
     */
    open fun hasUniqueSolution(puzzle: IntArray): Boolean =
        countSolutions(puzzle.copyOf(), limit = 2) == 1

    /**
     * Counts solutions up to [limit], aborting early once [limit] is reached.
     * Exposed internally for testing the abort behaviour.
     */
    internal fun countSolutions(board: IntArray, limit: Int = 2): Int {
        val idx = board.indexOf(0)
        if (idx == -1) return 1  // complete board — one solution found
        var count = 0
        for (d in 1..9) {
            if (isValidPlacement(board, idx, d)) {
                board[idx] = d
                count += countSolutions(board, limit)
                board[idx] = 0
                if (count >= limit) return count  // abort: enough solutions found
            }
        }
        return count
    }
}
