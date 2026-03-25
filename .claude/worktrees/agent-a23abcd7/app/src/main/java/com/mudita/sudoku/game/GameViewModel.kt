package com.mudita.sudoku.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudita.sudoku.game.model.GameAction
import com.mudita.sudoku.game.model.GameEvent
import com.mudita.sudoku.game.model.GameUiState
import com.mudita.sudoku.game.model.InputMode
import com.mudita.sudoku.puzzle.engine.SudokuGenerator
import com.mudita.sudoku.puzzle.model.Difficulty
import com.mudita.sudoku.puzzle.model.SudokuPuzzle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Central state machine for the Sudoku game loop.
 *
 * Exposes immutable [uiState] (StateFlow) for Compose UI and [events] (SharedFlow) for one-shot
 * navigation/dialog triggers.
 *
 * @param generatePuzzle Puzzle generation function; defaults to [SudokuGenerator]. Injectable for
 *                       testing via [FakeGenerator] or any suspend lambda returning [SudokuPuzzle].
 */
class GameViewModel(
    private val generatePuzzle: suspend (Difficulty) -> SudokuPuzzle = { difficulty ->
        SudokuGenerator().generatePuzzle(difficulty)
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    /** Mutable undo history; lives in ViewModel, NOT in GameUiState (keeps state immutable). */
    private val undoStack = ArrayDeque<GameAction>()

    // ------------------------------------------------------------------ public actions

    /**
     * Starts a new game at [difficulty].
     *
     * Emits isLoading=true immediately, generates the puzzle on [Dispatchers.Default] (CPU-bound),
     * then emits a fresh [GameUiState] with the loaded puzzle.
     */
    fun startGame(difficulty: Difficulty) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) {
                generatePuzzle(difficulty)
            }
            undoStack.clear()
            val givenMask = BooleanArray(81) { i -> puzzle.board[i] != 0 }
            _uiState.update {
                GameUiState(
                    board = puzzle.board.copyOf(),
                    solution = puzzle.solution.copyOf(),
                    givenMask = givenMask,
                    difficulty = difficulty,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Selects the cell at [index] (0–80).
     *
     * Given cells can be selected (selection is always allowed); digit entry is blocked separately
     * in [enterDigit] via the givenMask guard.
     */
    fun selectCell(index: Int) {
        if (index !in 0..80) return
        _uiState.update { it.copy(selectedCellIndex = index) }
    }

    /**
     * Enters [digit] (1–9) into the currently selected cell.
     *
     * Routes to [applyFill] or [applyPencilMark] based on the current [InputMode].
     * No-ops if: no cell selected, digit out of range, or cell is a given.
     */
    fun enterDigit(digit: Int) {
        if (digit !in 1..9) return
        val state = _uiState.value
        val idx = state.selectedCellIndex ?: return
        if (state.givenMask[idx]) return
        when (state.inputMode) {
            InputMode.FILL -> applyFill(idx, digit, state)
            InputMode.PENCIL -> applyPencilMark(idx, digit, state)
        }
    }

    /**
     * Toggles [InputMode] between FILL and PENCIL.
     */
    fun toggleInputMode() {
        _uiState.update { state ->
            state.copy(
                inputMode = when (state.inputMode) {
                    InputMode.FILL -> InputMode.PENCIL
                    InputMode.PENCIL -> InputMode.FILL
                }
            )
        }
    }

    // ------------------------------------------------------------------ private helpers

    private fun applyFill(idx: Int, digit: Int, state: GameUiState) {
        // Push undo action for potential undo (Plan 03)
        undoStack.addLast(
            GameAction.FillCell(
                cellIndex = idx,
                previousValue = state.board[idx],
                previousPencilMarks = state.pencilMarks[idx]
            )
        )

        val newBoard = state.board.copyOf()
        newBoard[idx] = digit

        val isError = digit != state.solution[idx]
        val newErrorCount = if (isError) state.errorCount + 1 else state.errorCount

        // Clear pencil marks for this cell
        val newMarks = state.pencilMarks.copyOf().also { it[idx] = emptySet() }

        // Check puzzle completion
        val allCorrect = newBoard.indices.all { i -> newBoard[i] == state.solution[i] }

        _uiState.update {
            it.copy(
                board = newBoard,
                errorCount = newErrorCount,
                pencilMarks = newMarks,
                isComplete = allCorrect
            )
        }

        if (allCorrect) {
            viewModelScope.launch {
                _events.emit(GameEvent.Completed(newErrorCount))
            }
        }
    }

    /**
     * Stub implementation — pencil mark logic is implemented in Plan 03.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun applyPencilMark(idx: Int, digit: Int, state: GameUiState) {
        // Plan 03 implementation
    }
}
