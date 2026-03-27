package com.ledgerman.sudoku.game.model

import com.ledgerman.sudoku.puzzle.model.Difficulty

/**
 * Immutable snapshot of all game UI state.
 *
 * Exposed from GameViewModel as StateFlow<GameUiState>. Each player action produces a new copy
 * via .copy(), keeping state fully immutable and Compose recomposition-safe.
 *
 * Array fields (board, solution, givenMask, pencilMarks) require manual equals/hashCode because
 * Kotlin data class uses referential equality for arrays by default.
 *
 * @param board            81-element IntArray; 0 = empty cell, 1–9 = player or given digit.
 * @param solution         81-element IntArray; complete solution — never shown during play.
 * @param givenMask        81-element BooleanArray; true = original given cell, immutable during play.
 * @param difficulty       Difficulty tier the current puzzle was generated for.
 * @param selectedCellIndex Index (0–80) of the currently selected cell, or null for no selection.
 * @param inputMode        Current input mode: FILL writes digits, PENCIL toggles candidates.
 * @param pencilMarks      81-element array of candidate digit sets, one per cell.
 * @param errorCount       Number of incorrect digit entries (silent, not surfaced during play).
 * @param hintCount        Number of hints requested during play (each hint incurs a score penalty).
 * @param isComplete       True when all 81 cells match the solution.
 * @param isLoading        True while puzzle generation is in progress.
 */
data class GameUiState(
    val board: IntArray = IntArray(81),
    val solution: IntArray = IntArray(81),
    val givenMask: BooleanArray = BooleanArray(81),
    val difficulty: Difficulty = Difficulty.EASY,
    val selectedCellIndex: Int? = null,
    val inputMode: InputMode = InputMode.FILL,
    val pencilMarks: Array<Set<Int>> = Array(81) { emptySet() },
    val errorCount: Int = 0,
    val hintCount: Int = 0,
    val isComplete: Boolean = false,
    val isLoading: Boolean = false
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameUiState) return false
        return board.contentEquals(other.board) &&
                solution.contentEquals(other.solution) &&
                givenMask.contentEquals(other.givenMask) &&
                difficulty == other.difficulty &&
                selectedCellIndex == other.selectedCellIndex &&
                inputMode == other.inputMode &&
                pencilMarks.contentDeepEquals(other.pencilMarks) &&
                errorCount == other.errorCount &&
                hintCount == other.hintCount &&
                isComplete == other.isComplete &&
                isLoading == other.isLoading
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + solution.contentHashCode()
        result = 31 * result + givenMask.contentHashCode()
        result = 31 * result + difficulty.hashCode()
        result = 31 * result + (selectedCellIndex?.hashCode() ?: 0)
        result = 31 * result + inputMode.hashCode()
        result = 31 * result + pencilMarks.contentDeepHashCode()
        result = 31 * result + errorCount
        result = 31 * result + hintCount
        result = 31 * result + isComplete.hashCode()
        result = 31 * result + isLoading.hashCode()
        return result
    }
}
