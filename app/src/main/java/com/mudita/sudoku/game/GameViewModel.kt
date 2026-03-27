package com.mudita.sudoku.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudita.sudoku.game.model.GameAction
import com.mudita.sudoku.game.model.GameEvent
import com.mudita.sudoku.game.model.GameUiState
import com.mudita.sudoku.game.model.InputMode
import com.mudita.sudoku.game.model.calculateScore
import com.mudita.sudoku.puzzle.engine.SudokuGenerator
import com.mudita.sudoku.puzzle.model.Difficulty
import com.mudita.sudoku.puzzle.model.SudokuPuzzle
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlin.random.Random

/**
 * Central state machine for the Sudoku game loop.
 *
 * Exposes immutable [uiState] (StateFlow) for Compose UI and [events] (SharedFlow) for one-shot
 * navigation/dialog triggers.
 *
 * @param generatePuzzle Puzzle generation function; defaults to [SudokuGenerator]. Injectable for
 *                       testing via [FakeGenerator] or any suspend lambda returning [SudokuPuzzle].
 * @param repository Persistence contract for save/load/clear. Defaults to [NoOpGameRepository]
 *                   for backward compatibility with callers that do not require persistence.
 * @param scoreRepository Contract for reading and writing per-difficulty high scores. Defaults to
 *                        [NoOpScoreRepository] for callers that do not require score persistence.
 * @param ioDispatcher Dispatcher for I/O-bound work (DataStore reads/writes). Injectable for
 *                     tests — pass [UnconfinedTestDispatcher] to run synchronously under runTest.
 * @param random Random instance used for hint cell selection. Defaults to [Random.Default].
 *               Injectable for tests — pass [Random(seed)] for deterministic hint selection.
 */
class GameViewModel(
    private val generatePuzzle: suspend (Difficulty) -> SudokuPuzzle = { difficulty ->
        SudokuGenerator().generatePuzzle(difficulty)
    },
    private val repository: GameRepository = NoOpGameRepository(),
    private val scoreRepository: ScoreRepository = NoOpScoreRepository(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val random: Random = Random.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val _showResumeDialog = MutableStateFlow(false)
    val showResumeDialog: StateFlow<Boolean> = _showResumeDialog.asStateFlow()

    private val _leaderboardScores = MutableStateFlow<Map<Difficulty, Int?>>(emptyMap())
    val leaderboardScores: StateFlow<Map<Difficulty, Int?>> = _leaderboardScores.asStateFlow()

    /** Saved game state pending user decision (resume or start new). Null if no saved game. */
    private var pendingSavedState: GameUiState? = null

    /** Mutable undo history; lives in ViewModel, NOT in GameUiState (keeps state immutable). */
    private val undoStack = ArrayDeque<GameAction>()

    init {
        viewModelScope.launch {
            val saved = withContext(ioDispatcher) { repository.loadGame() }
            if (saved != null) {
                pendingSavedState = saved
                _showResumeDialog.value = true
            }
            refreshLeaderboard()
        }
    }

    // ------------------------------------------------------------------ public actions

    /**
     * Returns true if a previously saved game is pending resume decision.
     */
    fun hasSavedGame(): Boolean = pendingSavedState != null || _showResumeDialog.value

    /**
     * Restores the previously saved game state.
     *
     * Clears the undo stack (per decision D-05 — undo history is not persisted).
     * Sets showResumeDialog to false and replaces [uiState] with the saved state.
     * No-op if no saved state is pending.
     */
    fun resumeGame() {
        val saved = pendingSavedState ?: return
        pendingSavedState = null
        _showResumeDialog.value = false
        undoStack.clear()
        _uiState.value = saved
    }

    /**
     * Discards any saved game state and starts a fresh Easy game.
     *
     * Per decision D-04: the new game difficulty is always Easy when starting from
     * the resume-or-new decision dialog.
     * Calls [repository.clearGame] to remove the saved state from storage.
     */
    fun startNewGame() {
        pendingSavedState = null
        _showResumeDialog.value = false
        viewModelScope.launch {
            withContext(ioDispatcher) { repository.clearGame() }
        }
        startGame(Difficulty.EASY)
    }

    /**
     * Persists the current game state to [repository].
     *
     * Guards:
     * - Does nothing if [GameUiState.isLoading] is true (puzzle not yet ready)
     * - Does nothing if [GameUiState.isComplete] is true (game over, no need to persist)
     * - Does nothing if board is all zeros (no game started yet)
     */
    suspend fun saveNow() {
        val state = _uiState.value
        if (state.isLoading || state.isComplete || state.board.all { it == 0 }) return
        repository.saveGame(state)
    }

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

    /**
     * Requests a hint: fills one random non-given cell where board[i] != solution[i] with its
     * correct solution value.
     *
     * Hint eligibility covers BOTH empty cells (board[i] == 0) AND cells filled with the wrong
     * value (board[i] != 0 && board[i] != solution[i]). Both are "non-correct" cells the player
     * wants revealed (per decision D-03).
     *
     * Hints are permanently non-undoable. This is a locked product decision: calling undo() after
     * requestHint() must leave the hinted cell filled. Reverting a hint while hintCount stays
     * elevated would create an inconsistent state (hintCount inflated, board as-if-no-hint).
     * Consequently, hint actions are NOT pushed to the undoStack.
     *
     * Guards:
     * - No-op if game is loading ([GameUiState.isLoading] == true)
     * - No-op if game is complete ([GameUiState.isComplete] == true)
     * - No-op if no valid hint targets exist (all non-given cells already correct)
     */
    fun requestHint() {
        val state = _uiState.value
        if (state.isLoading || state.isComplete) return

        // Hint targets: any non-given cell where current value != solution
        // This includes empty cells (board[i] == 0) AND cells with wrong user-entered values.
        // Both are valid hint targets because the player wants the correct answer for any
        // cell they have not yet filled correctly (per D-03).
        val candidates = (0..80).filter { i ->
            !state.givenMask[i] && state.board[i] != state.solution[i]
        }
        if (candidates.isEmpty()) return

        val idx = candidates.random(random)
        val newBoard = state.board.copyOf()
        newBoard[idx] = state.solution[idx]
        val newMarks = state.pencilMarks.copyOf().also { it[idx] = emptySet() }
        val allCorrect = newBoard.indices.all { i -> newBoard[i] == state.solution[i] }

        _uiState.update {
            it.copy(
                board = newBoard,
                pencilMarks = newMarks,
                hintCount = it.hintCount + 1,
                isComplete = allCorrect
            )
        }

        // Do NOT push to undoStack — hints are permanently non-undoable.
        // This is a locked product decision: undo() after a hint must leave the
        // hinted cell filled. Reverting a hint while hintCount stays elevated
        // would be an inconsistent state.

        if (allCorrect) handleCompletion(_uiState.value)
    }

    // ------------------------------------------------------------------ private helpers

    /**
     * Handles game completion: computes score, checks/saves personal best, refreshes leaderboard,
     * clears saved game state, and emits [GameEvent.Completed].
     *
     * Order matters (Pitfall 3): saveBestScore → refreshLeaderboard → clearGame → emit event.
     * The leaderboard is refreshed BEFORE clearing the game so it reflects the new best immediately.
     */
    private fun handleCompletion(finalState: GameUiState) {
        viewModelScope.launch {
            val score = calculateScore(finalState.errorCount, finalState.hintCount)
            val currentBest = withContext(ioDispatcher) {
                scoreRepository.getBestScore(finalState.difficulty)
            }
            val isPersonalBest = currentBest == null || score > currentBest
            if (isPersonalBest) {
                withContext(ioDispatcher) {
                    scoreRepository.saveBestScore(finalState.difficulty, score)
                }
            }
            // Refresh leaderboard BEFORE clearing game (order matters per Pitfall 3)
            refreshLeaderboard()
            withContext(ioDispatcher) { repository.clearGame() }
            _events.emit(
                GameEvent.Completed(
                    errorCount = finalState.errorCount,
                    hintCount = finalState.hintCount,
                    score = score,
                    difficulty = finalState.difficulty,
                    isPersonalBest = isPersonalBest
                )
            )
        }
    }

    /**
     * Refreshes [leaderboardScores] by reading all difficulty best scores from [scoreRepository].
     * Called after completion and during init.
     */
    private suspend fun refreshLeaderboard() {
        _leaderboardScores.value = mapOf(
            Difficulty.EASY to withContext(ioDispatcher) { scoreRepository.getBestScore(Difficulty.EASY) },
            Difficulty.MEDIUM to withContext(ioDispatcher) { scoreRepository.getBestScore(Difficulty.MEDIUM) },
            Difficulty.HARD to withContext(ioDispatcher) { scoreRepository.getBestScore(Difficulty.HARD) }
        )
    }

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
            handleCompletion(_uiState.value)
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

        // D-05: Cap at 4 pencil marks per cell. Silent no-op if adding would exceed.
        // Toggle-off (removing existing mark) is always allowed.
        if (wasAdded && currentSet.size >= 4) return

        undoStack.addLast(GameAction.SetPencilMark(idx, digit, wasAdded))
        val newMarks = state.pencilMarks.copyOf()
        newMarks[idx] = if (wasAdded) currentSet + digit else currentSet - digit
        _uiState.update { it.copy(pencilMarks = newMarks) }
    }
}
