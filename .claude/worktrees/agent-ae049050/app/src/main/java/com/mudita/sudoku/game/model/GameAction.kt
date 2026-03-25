package com.mudita.sudoku.game.model

/**
 * Sealed class representing undoable game actions.
 *
 * Each variant captures enough state to reverse the action when the user presses undo.
 * Stored in GameViewModel's undoStack (ArrayDeque<GameAction>) — NOT in GameUiState.
 */
sealed class GameAction {

    /**
     * Represents a cell fill action.
     *
     * @param cellIndex      Index (0–80) of the cell that was filled.
     * @param previousValue  The board value before the fill (0 = was empty, 1–9 = was filled).
     * @param previousPencilMarks  Pencil marks that were cleared when this cell was filled.
     *                            Undo should restore these marks.
     */
    data class FillCell(
        val cellIndex: Int,
        val previousValue: Int,
        val previousPencilMarks: Set<Int>
    ) : GameAction()

    /**
     * Represents a pencil mark toggle action.
     *
     * @param cellIndex  Index (0–80) of the cell that had a pencil mark toggled.
     * @param digit      The digit (1–9) that was added or removed.
     * @param wasAdded   true if the digit was added (undo should remove it);
     *                   false if the digit was removed (undo should add it back).
     */
    data class SetPencilMark(
        val cellIndex: Int,
        val digit: Int,
        val wasAdded: Boolean
    ) : GameAction()
}
