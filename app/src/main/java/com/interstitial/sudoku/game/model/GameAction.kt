package com.interstitial.sudoku.game.model

import com.interstitial.sudoku.puzzle.model.Difficulty

sealed class GameAction {
    data class SelectCell(val index: Int) : GameAction()
    data class PlaceDigit(val digit: Int) : GameAction()
    data class ToggleNote(val digit: Int) : GameAction()
    data object Erase : GameAction()
    data object Undo : GameAction()
    data object Hint : GameAction()
    data object ToggleInputMode : GameAction()
    data class NewGame(val difficulty: Difficulty) : GameAction()
    data object ResumeGame : GameAction()
    data object PausePuzzle : GameAction()
    data object KeepForLater : GameAction()
    data object DiscardPuzzle : GameAction()
}
