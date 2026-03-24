package com.mudita.sudoku.puzzle.engine

import com.mudita.sudoku.puzzle.model.DifficultyConfig
import com.mudita.sudoku.puzzle.model.SudokuPuzzle
import com.mudita.sudoku.puzzle.model.TechniqueTier

/**
 * Classifies the minimum technique tier required to solve a Sudoku puzzle.
 *
 * Uses a human-style constraint-propagation approach: attempts progressively harder
 * techniques and returns the tier at which the puzzle first becomes solvable.
 *
 * Technique tiers implemented:
 *   NAKED_SINGLES_ONLY — every empty cell has a single candidate; no deduction needed
 *   HIDDEN_PAIRS       — requires finding hidden singles (a digit forced into one cell per unit)
 *   ADVANCED           — requires techniques beyond naked/hidden singles (X-wing, chains, etc.)
 *
 * Naked-pairs elimination is intentionally excluded to keep the solver termination-safe.
 * Any puzzle requiring pairs or harder is classified ADVANCED.
 */
class DifficultyClassifier {

    /**
     * Returns the minimum [TechniqueTier] required to solve [puzzle].
     *
     * @param puzzle Puzzle to classify. Only [puzzle.board] is used; solution is not consulted.
     */
    fun classifyTechniqueTier(puzzle: SudokuPuzzle): TechniqueTier =
        classifyBoard(puzzle.board.copyOf())

    /**
     * Returns true if [puzzle]'s actual technique tier satisfies [config]'s required tier exactly.
     *
     * An EASY puzzle does NOT meet HARD requirements (too easy — would not provide the
     * intended challenge for Hard players). An ADVANCED puzzle does NOT meet EASY requirements.
     */
    fun meetsRequirements(puzzle: SudokuPuzzle, config: DifficultyConfig): Boolean {
        val actual = classifyTechniqueTier(puzzle)
        return actual == config.requiredTechniqueTier
    }

    // --- Internal solving passes ---

    internal fun classifyBoard(board: IntArray): TechniqueTier {
        if (solveWithNakedSingles(board.copyOf())) return TechniqueTier.NAKED_SINGLES_ONLY
        if (solveWithHiddenSingles(board.copyOf())) return TechniqueTier.HIDDEN_PAIRS
        return TechniqueTier.ADVANCED
    }

    /**
     * Returns true if the board can be fully solved using naked singles only.
     * Terminates: each iteration places at least one digit or exits (progress=false).
     */
    internal fun solveWithNakedSingles(board: IntArray): Boolean {
        var progress = true
        while (progress) {
            progress = false
            for (i in 0 until 81) {
                if (board[i] != 0) continue
                val candidates = candidatesFor(board, i)
                if (candidates.size == 1) {
                    board[i] = candidates.first()
                    progress = true
                }
            }
        }
        return board.none { it == 0 }
    }

    /**
     * Returns true if the board can be fully solved using naked singles and hidden singles.
     * Terminates: each iteration places at least one digit or exits (progress=false).
     */
    internal fun solveWithHiddenSingles(board: IntArray): Boolean {
        var progress = true
        while (progress) {
            progress = false
            // Naked singles
            for (i in 0 until 81) {
                if (board[i] != 0) continue
                val cands = candidatesFor(board, i)
                if (cands.size == 1) {
                    board[i] = cands.first()
                    progress = true
                }
            }
            // Hidden singles in rows, columns, boxes
            if (applyHiddenSingles(board)) progress = true
        }
        return board.none { it == 0 }
    }

    /** Candidates for cell at [index]: digits 1–9 not in conflict with current board state. */
    private fun candidatesFor(board: IntArray, index: Int): Set<Int> =
        (1..9).filter { d -> isValidPlacement(board, index, d) }.toSet()

    /**
     * For each unit (row/col/box), if a digit can only go in one cell — place it.
     * Returns true if any placement was made.
     */
    private fun applyHiddenSingles(board: IntArray): Boolean {
        var placed = false
        for (unit in allUnits()) {
            for (digit in 1..9) {
                val possible = unit.filter { board[it] == 0 && isValidPlacement(board, it, digit) }
                if (possible.size == 1) {
                    board[possible[0]] = digit
                    placed = true
                }
            }
        }
        return placed
    }

    /** Returns all 27 units (9 rows + 9 columns + 9 boxes) as lists of cell indices. */
    private fun allUnits(): List<List<Int>> {
        val units = mutableListOf<List<Int>>()
        // Rows
        for (r in 0..8) units.add((0..8).map { c -> r * 9 + c })
        // Columns
        for (c in 0..8) units.add((0..8).map { r -> r * 9 + c })
        // 3x3 boxes
        for (br in 0..2) for (bc in 0..2) {
            units.add((0..8).map { i -> (br * 3 + i / 3) * 9 + (bc * 3 + i % 3) })
        }
        return units
    }
}
