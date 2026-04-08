package com.interstitial.sudoku.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.game.model.GameAction
import com.interstitial.sudoku.game.model.GameUiState
import com.interstitial.sudoku.game.model.InputMode
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.snackbar.SnackbarHostMMD
import com.mudita.mmd.components.snackbar.SnackbarHostStateMMD
import com.mudita.mmd.components.text.TextMMD

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
            // Compact header: back + difficulty + cells remaining
            PuzzleTopBar(
                difficulty = state.difficulty,
                cellsRemaining = state.cellsRemaining,
                onBack = {
                    onAction(GameAction.PausePuzzle)
                    showLeaveDialog = true
                }
            )

            // Grid sized by width, maintaining square aspect ratio
            SudokuGrid(
                state = state,
                onCellTap = { row, col -> onAction(GameAction.SelectCell(row * 9 + col)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 4.dp)
            )

            // Combined controls row: Fill/Notes toggle + Undo + Erase + Hint
            ControlsRow(
                inputMode = state.inputMode,
                hasUndo = state.hasUndo,
                canErase = state.selectedCell != null &&
                    state.selectedCell !in state.hintedCells &&
                    !state.givens[state.selectedCell],
                canHint = state.selectedCell != null,
                onToggleMode = { onAction(GameAction.ToggleInputMode) },
                onUndo = { onAction(GameAction.Undo) },
                onErase = { onAction(GameAction.Erase) },
                onHint = { onAction(GameAction.Hint) }
            )

            // Compact digit pad
            DigitPad(
                digitCounts = state.digitCounts,
                onDigit = { digit ->
                    if (state.inputMode == InputMode.NOTES) {
                        onAction(GameAction.ToggleNote(digit))
                    } else {
                        onAction(GameAction.PlaceDigit(digit))
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }

        SnackbarHostMMD(
            hostState = snackbarHostState,
            modifier = Modifier.statusBarsPadding()
        )
    }
}

@Composable
private fun ControlsRow(
    inputMode: InputMode,
    hasUndo: Boolean,
    canErase: Boolean,
    canHint: Boolean,
    onToggleMode: () -> Unit,
    onUndo: () -> Unit,
    onErase: () -> Unit,
    onHint: () -> Unit
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fill/Notes segmented toggle
        Row(
            modifier = Modifier
                .weight(2f)
                .height(34.dp)
                .border(2.dp, onSurface)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (inputMode == InputMode.FILL) onSurface else surface)
                    .clickable(onClick = { if (inputMode != InputMode.FILL) onToggleMode() }),
                contentAlignment = Alignment.Center
            ) {
                TextMMD(
                    text = "Fill",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (inputMode == InputMode.FILL) surface else onSurface
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (inputMode == InputMode.NOTES) onSurface else surface)
                    .clickable(onClick = { if (inputMode != InputMode.NOTES) onToggleMode() }),
                contentAlignment = Alignment.Center
            ) {
                TextMMD(
                    text = "Notes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (inputMode == InputMode.NOTES) surface else onSurface
                )
            }
        }

        // Action buttons
        ButtonMMD(
            onClick = onUndo,
            enabled = hasUndo,
            modifier = Modifier.weight(1f).height(34.dp)
        ) { TextMMD("Undo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }

        ButtonMMD(
            onClick = onErase,
            enabled = canErase,
            modifier = Modifier.weight(1f).height(34.dp)
        ) { TextMMD("Erase", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }

        ButtonMMD(
            onClick = onHint,
            enabled = canHint,
            modifier = Modifier.weight(1f).height(34.dp)
        ) { TextMMD("Hint", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
    }
}
