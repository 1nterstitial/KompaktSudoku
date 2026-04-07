package com.interstitial.sudoku.puzzle.engine

open class UniquenessVerifier {
    open fun hasUniqueSolution(puzzle: IntArray): Boolean =
        countSolutions(puzzle.copyOf(), limit = 2) == 1

    internal fun countSolutions(board: IntArray, limit: Int = 2): Int {
        val idx = board.indexOf(0)
        if (idx == -1) return 1
        var count = 0
        for (d in 1..9) {
            if (isValidPlacement(board, idx, d)) {
                board[idx] = d
                count += countSolutions(board, limit)
                board[idx] = 0
                if (count >= limit) return count
            }
        }
        return count
    }
}
