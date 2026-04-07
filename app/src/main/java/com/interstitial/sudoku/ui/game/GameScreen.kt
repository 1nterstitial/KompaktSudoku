package com.interstitial.sudoku.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.game.model.GameAction
import com.interstitial.sudoku.game.model.GameUiState
import com.interstitial.sudoku.game.model.InputMode
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.snackbar.SnackbarHostMMD
import com.mudita.mmd.components.snackbar.SnackbarHostStateMMD

@Composable
fun GameScreen(
    state: GameUiState,
    snackbarHostState: SnackbarHostStateMMD,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLeaveDialog by remember { mutableStateOf(false) }

    BackHandler {
        onAction(GameAction.PausePuzzle)
        showLeaveDialog = true
    }

    if (showLeaveDialog) {
        LeavePuzzleDialog(
            onKeepForLater = {
                showLeaveDialog = false
                onAction(GameAction.KeepForLater)
            },
            onDiscard = {
                showLeaveDialog = false
                onAction(GameAction.DiscardPuzzle)
            }
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PuzzleTopBar(
                difficulty = state.difficulty,
                onBack = {
                    onAction(GameAction.PausePuzzle)
                    showLeaveDialog = true
                }
            )

            PuzzleMetaStrip(cellsRemaining = state.cellsRemaining)

            HorizontalDividerMMD(thickness = 2.dp)

            SudokuGrid(
                state = state,
                onCellTap = { row, col -> onAction(GameAction.SelectCell(row * 9 + col)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )

            HorizontalDividerMMD(thickness = 2.dp)

            InputModeToggle(
                currentMode = state.inputMode,
                onToggle = { onAction(GameAction.ToggleInputMode) },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            PuzzleActionRow(
                hasUndo = state.hasUndo,
                canErase = state.selectedCell != null &&
                    state.selectedCell !in state.hintedCells &&
                    !state.givens[state.selectedCell],
                canHint = state.selectedCell != null,
                onUndo = { onAction(GameAction.Undo) },
                onErase = { onAction(GameAction.Erase) },
                onHint = { onAction(GameAction.Hint) },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDividerMMD(thickness = 2.dp)

            DigitPad(
                digitCounts = state.digitCounts,
                onDigit = { digit ->
                    if (state.inputMode == InputMode.NOTES) {
                        onAction(GameAction.ToggleNote(digit))
                    } else {
                        onAction(GameAction.PlaceDigit(digit))
                    }
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        SnackbarHostMMD(
            hostState = snackbarHostState,
            modifier = Modifier.statusBarsPadding()
        )
    }
}
