package com.mudita.sudoku.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.sudoku.game.GameViewModel
import com.mudita.sudoku.game.model.CompletionResult
import com.mudita.sudoku.game.model.GameEvent
import com.mudita.sudoku.puzzle.model.Difficulty
import kotlinx.coroutines.launch

/**
 * Root game screen composable. Wires GameViewModel to the UI and delegates rendering to
 * GameGrid, ControlsRow, and NumberPad.
 *
 * Layout order (D-07): difficulty label → grid → controls row → number pad.
 * No ripple, no animation, no Animated* composable (UI-02).
 *
 * @param onCompleted   Called when the game is completed with full score data. The caller
 *                      (e.g. MainActivity or parent composable) navigates to the Summary screen.
 * @param onBackToMenu  Called when the player presses back mid-game — saves state then returns
 *                      to the main menu.
 */
@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(),
    onCompleted: (CompletionResult) -> Unit = {},
    onBackToMenu: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    BackHandler {
        coroutineScope.launch {
            viewModel.saveNow()
            onBackToMenu()
        }
    }

    // canRequestHint: derived from uiState — true when there is at least one non-given cell
    // where the current board value differs from the solution (empty OR wrong-filled).
    // Recomputed on every recomposition so the button disables instantly when no targets remain.
    val canRequestHint = !uiState.isComplete && !uiState.isLoading &&
        (0..80).any { i -> !uiState.givenMask[i] && uiState.board[i] != uiState.solution[i] }

    // Collect one-shot completion events and route to onCompleted callback
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GameEvent.Completed -> {
                    onCompleted(
                        CompletionResult(
                            difficulty = event.difficulty,
                            errorCount = event.errorCount,
                            hintCount = event.hintCount,
                            finalScore = event.score,
                            isPersonalBest = event.isPersonalBest
                        )
                    )
                }
            }
        }
    }

    if (uiState.isLoading) {
        LoadingScreen()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 8.dp)
    ) {
        // 1. Difficulty label — top bar (D-07)
        DifficultyBar(difficulty = uiState.difficulty)

        // 2. Grid — fills all remaining vertical space (D-07)
        GameGrid(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            board = uiState.board,
            solution = uiState.solution,
            givenMask = uiState.givenMask,
            selectedCellIndex = uiState.selectedCellIndex,
            pencilMarks = uiState.pencilMarks,
            onCellClick = viewModel::selectCell
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Controls row — Fill/Pencil mode toggles + Undo + Get Hint (D-07, D-08, D-02)
        ControlsRow(
            inputMode = uiState.inputMode,
            onToggleMode = viewModel::toggleInputMode,
            onUndo = viewModel::undo,
            onHint = viewModel::requestHint,
            canRequestHint = canRequestHint
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Number pad — digits 1–9 + Erase (D-07, D-05, D-06)
        NumberPad(
            onDigitClick = viewModel::enterDigit,
            onErase = viewModel::eraseCell
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * Difficulty label displayed at the top of the game screen (D-07).
 * Shows current difficulty tier centered. Uses TextMMD for E-ink optimized rendering.
 */
@Composable
private fun DifficultyBar(difficulty: Difficulty) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        TextMMD(
            text = difficulty.name.lowercase()
                .replaceFirstChar { it.uppercase() }
        )
    }
}

/**
 * Loading placeholder shown while the puzzle is being generated.
 *
 * Static text only — no spinner, no progress indicator, no animation (UI-02).
 * Spinners cause E-ink partial refresh artifacts (ghosting).
 */
@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        TextMMD(text = "Generating puzzle\u2026") // … (ellipsis)
    }
}
