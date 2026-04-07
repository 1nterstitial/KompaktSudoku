package com.interstitial.sudoku.game.model

import com.interstitial.sudoku.puzzle.model.Difficulty

data class GameUiState(
    val board: IntArray = IntArray(81),
    val solution: IntArray = IntArray(81),
    val givens: BooleanArray = BooleanArray(81),
    val notes: Array<Set<Int>> = Array(81) { emptySet() },
    val conflictMask: BooleanArray = BooleanArray(81),
    val selectedCell: Int? = null,
    val inputMode: InputMode = InputMode.FILL,
    val difficulty: Difficulty = Difficulty.EASY,
    val cellsRemaining: Int = 0,
    val elapsedMs: Long = 0,
    val hasUndo: Boolean = false,
    val digitCounts: IntArray = IntArray(9),
    val isComplete: Boolean = false,
    val isPaused: Boolean = false,
    val hintsUsed: Int = 0,
    val hintedCells: Set<Int> = emptySet(),
    val isGenerating: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameUiState) return false
        return board.contentEquals(other.board) &&
            solution.contentEquals(other.solution) &&
            givens.contentEquals(other.givens) &&
            notes.contentDeepEquals(other.notes) &&
            conflictMask.contentEquals(other.conflictMask) &&
            selectedCell == other.selectedCell &&
            inputMode == other.inputMode &&
            difficulty == other.difficulty &&
            cellsRemaining == other.cellsRemaining &&
            elapsedMs == other.elapsedMs &&
            hasUndo == other.hasUndo &&
            digitCounts.contentEquals(other.digitCounts) &&
            isComplete == other.isComplete &&
            isPaused == other.isPaused &&
            hintsUsed == other.hintsUsed &&
            hintedCells == other.hintedCells &&
            isGenerating == other.isGenerating
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + solution.contentHashCode()
        result = 31 * result + givens.contentHashCode()
        result = 31 * result + notes.contentDeepHashCode()
        result = 31 * result + conflictMask.contentHashCode()
        result = 31 * result + (selectedCell?.hashCode() ?: 0)
        result = 31 * result + inputMode.hashCode()
        result = 31 * result + difficulty.hashCode()
        result = 31 * result + cellsRemaining
        result = 31 * result + elapsedMs.hashCode()
        result = 31 * result + hasUndo.hashCode()
        result = 31 * result + digitCounts.contentHashCode()
        result = 31 * result + isComplete.hashCode()
        result = 31 * result + isPaused.hashCode()
        result = 31 * result + hintsUsed
        result = 31 * result + hintedCells.hashCode()
        result = 31 * result + isGenerating.hashCode()
        return result
    }
}
