package com.interstitial.sudoku.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.game.model.GameAction
import com.interstitial.sudoku.game.model.GameUiState
import com.interstitial.sudoku.game.model.InputMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

        // Action buttons — dark filled, white icons, briefly reverse on press
        val scope = rememberCoroutineScope()
        var pressedButton by remember { mutableIntStateOf(0) }

        ActionIconButton(
            icon = UndoIcon,
            contentDescription = "Undo",
            enabled = hasUndo,
            pressed = pressedButton == 1,
            onSurface = onSurface,
            surface = surface,
            onClick = {
                pressedButton = 1
                onUndo()
                scope.launch { delay(150); pressedButton = 0 }
            },
            modifier = Modifier.weight(1f)
        )

        ActionIconButton(
            icon = EraseIcon,
            contentDescription = "Erase",
            enabled = canErase,
            pressed = pressedButton == 2,
            onSurface = onSurface,
            surface = surface,
            onClick = {
                pressedButton = 2
                onErase()
                scope.launch { delay(150); pressedButton = 0 }
            },
            modifier = Modifier.weight(1f)
        )

        ActionIconButton(
            icon = HintIcon,
            contentDescription = "Hint",
            enabled = canHint,
            pressed = pressedButton == 3,
            onSurface = onSurface,
            surface = surface,
            onClick = {
                pressedButton = 3
                onHint()
                scope.launch { delay(150); pressedButton = 0 }
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    pressed: Boolean,
    onSurface: Color,
    surface: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        !enabled -> onSurface.copy(alpha = 0.38f)
        pressed -> surface
        else -> onSurface
    }
    val tint = when {
        !enabled -> surface
        pressed -> onSurface
        else -> surface
    }
    Box(
        modifier = modifier
            .height(34.dp)
            .border(2.dp, onSurface)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp), tint = tint)
    }
}
