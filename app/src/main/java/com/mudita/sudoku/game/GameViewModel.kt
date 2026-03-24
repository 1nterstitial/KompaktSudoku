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

    /**
     * Clears the selected cell's digit and pencil marks.
     *
     * No-op if: no cell selected, or the cell is a given.
     * Pushes a FillCell action onto undoStack so undo() can restore the previous value.
     */
    fun eraseCell() {
        val state = _uiState.value
        val idx = state.selectedCellIndex ?: return
        if (state.givenMask[idx]) return

        undoStack.addLast(
            GameAction.FillCell(
                cellIndex = idx,
                previousValue = state.board[idx],
                previousPencilMarks = state.pencilMarks[idx]
            )
        )

        val newBoard = state.board.copyOf()
        newBoard[idx] = 0
        val newMarks = state.pencilMarks.copyOf().also { it[idx] = emptySet() }

        _uiState.update {
            it.copy(
                board = newBoard,
                pencilMarks = newMarks
            )
        }
    }

    // ------------------------------------------------------------------ private helpers

    private fun applyFill(idx: Int, digit: Int, state: GameUiState) {
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
     * Undoes the last action from [undoStack].
     *
     * - [GameAction.FillCell]: restores the previous board value and pencil marks at that cell.
     *   Rechecks completion state — if undoing the last correct fill, sets isComplete=false.
     *   Does NOT decrement errorCount — errors are permanent once counted (SCORE-01 design).
     * - [GameAction.SetPencilMark]: reverses the pencil mark toggle.
     * - Empty stack: no-op.
     */
    fun undo() {
        if (undoStack.isEmpty()) return
        when (val action = undoStack.removeLast()) {
            is GameAction.FillCell -> {
                _uiState.update { state ->
                    val newBoard = state.board.copyOf()
                    newBoard[action.cellIndex] = action.previousValue
                    val newMarks = state.pencilMarks.copyOf()
                    newMarks[action.cellIndex] = action.previousPencilMarks
                    // Recheck completion after undo (may no longer be complete)
                    val allCorrect = newBoard.indices.all { i -> newBoard[i] == state.solution[i] }
                    state.copy(
                        board = newBoard,
                        pencilMarks = newMarks,
                        isComplete = allCorrect
                    )
                }
            }
            is GameAction.SetPencilMark -> {
                _uiState.update { state ->
                    val newMarks = state.pencilMarks.copyOf()
                    newMarks[action.cellIndex] = if (action.wasAdded) {
                        state.pencilMarks[action.cellIndex] - action.digit
                    } else {
                        state.pencilMarks[action.cellIndex] + action.digit
                    }
                    state.copy(pencilMarks = newMarks)
                }
            }
        }
    }

    /**
     * Toggles a pencil mark candidate for [digit] at cell [idx].
     *
     * - If [digit] is NOT in the current pencil mark set, adds it (wasAdded=true).
     * - If [digit] IS already present, removes it (wasAdded=false).
     * - Pushes a [GameAction.SetPencilMark] onto undoStack for undo support.
     */
    private fun applyPencilMark(idx: Int, digit: Int, state: GameUiState) {
        val currentSet = state.pencilMarks[idx]
        val wasAdded = digit !in currentSet
        undoStack.addLast(GameAction.SetPencilMark(idx, digit, wasAdded))
        val newMarks = state.pencilMarks.copyOf()
        newMarks[idx] = if (wasAdded) currentSet + digit else currentSet - digit
        _uiState.update { it.copy(pencilMarks = newMarks) }
    }
}
