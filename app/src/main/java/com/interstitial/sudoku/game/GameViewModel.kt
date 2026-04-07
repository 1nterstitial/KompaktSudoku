package com.interstitial.sudoku.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interstitial.sudoku.game.model.*
import com.interstitial.sudoku.puzzle.engine.ConflictDetector
import com.interstitial.sudoku.puzzle.engine.SudokuGenerator
import com.interstitial.sudoku.puzzle.model.Difficulty
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UndoEntry(
    val cellIndex: Int,
    val previousValue: Int,
    val previousNotes: Set<Int>
)

class GameViewModel(
    private val generator: SudokuGenerator = SudokuGenerator(),
    private val gameRepository: GameRepository,
    private val recordsRepository: RecordsRepository,
    private val generationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>()
    val events = _events.asSharedFlow()

    private val undoStack = ArrayDeque<UndoEntry>()

    private var timerStartRealtimeMs: Long? = null

    fun onAction(action: GameAction) {
        when (action) {
            is GameAction.NewGame -> startNewGame(action.difficulty)
            is GameAction.ResumeGame -> resumeGame()
            is GameAction.SelectCell -> selectCell(action.index)
            is GameAction.PlaceDigit -> placeDigit(action.digit)
            is GameAction.ToggleNote -> toggleNote(action.digit)
            is GameAction.Erase -> erase()
            is GameAction.Undo -> undo()
            is GameAction.Hint -> hint()
            is GameAction.ToggleInputMode -> toggleInputMode()
            is GameAction.PausePuzzle -> pausePuzzle()
            is GameAction.KeepForLater -> keepForLater()
            is GameAction.DiscardPuzzle -> discardPuzzle()
        }
    }

    private fun startNewGame(difficulty: Difficulty) {
        _uiState.value = GameUiState(isGenerating = true, difficulty = difficulty)
        undoStack.clear()
        viewModelScope.launch {
            val puzzle = withContext(generationDispatcher) {
                generator.generatePuzzle(difficulty)
            }
            val givens = BooleanArray(81) { puzzle.board[it] != 0 }
            val board = puzzle.board.copyOf()
            _uiState.value = GameUiState(
                board = board,
                solution = puzzle.solution.copyOf(),
                givens = givens,
                difficulty = difficulty,
                cellsRemaining = board.count { it == 0 },
                digitCounts = computeDigitCounts(board),
                conflictMask = ConflictDetector.buildConflictMask(board)
            )
            timerStartRealtimeMs = System.currentTimeMillis()
        }
    }

    private fun resumeGame() {
        viewModelScope.launch {
            val persisted = gameRepository.loadGame() ?: return@launch
            val board = persisted.board.toIntArray()
            val givens = persisted.givens.toBooleanArray()
            val notes = Array(81) { persisted.notes[it].toSet() }
            val difficulty = Difficulty.valueOf(persisted.difficulty)
            undoStack.clear()
            _uiState.value = GameUiState(
                board = board,
                solution = persisted.solution.toIntArray(),
                givens = givens,
                notes = notes,
                difficulty = difficulty,
                cellsRemaining = board.count { it == 0 },
                elapsedMs = persisted.elapsedMs,
                hintsUsed = persisted.hintsUsed,
                hintedCells = persisted.hintedCells.toSet(),
                digitCounts = computeDigitCounts(board),
                conflictMask = ConflictDetector.buildConflictMask(board)
            )
            timerStartRealtimeMs = System.currentTimeMillis()
        }
    }

    private fun selectCell(index: Int) {
        val current = _uiState.value
        _uiState.value = current.copy(
            selectedCell = if (current.selectedCell == index) null else index
        )
    }

    private fun placeDigit(digit: Int) {
        val state = _uiState.value
        val cell = state.selectedCell ?: return
        if (state.givens[cell] || cell in state.hintedCells) return

        if (state.inputMode == InputMode.NOTES) {
            toggleNote(digit)
            return
        }

        val prevValue = state.board[cell]
        val prevNotes = state.notes[cell]
        undoStack.addLast(UndoEntry(cell, prevValue, prevNotes))

        val newBoard = state.board.copyOf()
        newBoard[cell] = digit
        val newNotes = state.notes.copyOf()
        newNotes[cell] = emptySet()

        val newCellsRemaining = newBoard.count { it == 0 }
        val newConflictMask = ConflictDetector.buildConflictMask(newBoard)
        val newDigitCounts = computeDigitCounts(newBoard)
        val isComplete = newCellsRemaining == 0 && newConflictMask.none { it }

        _uiState.value = state.copy(
            board = newBoard,
            notes = newNotes,
            cellsRemaining = newCellsRemaining,
            conflictMask = newConflictMask,
            digitCounts = newDigitCounts,
            hasUndo = true,
            isComplete = isComplete
        )

        if (isComplete) onCompletion()
    }

    private fun toggleNote(digit: Int) {
        val state = _uiState.value
        val cell = state.selectedCell ?: return
        if (state.givens[cell] || cell in state.hintedCells) return
        if (state.board[cell] != 0) return

        val currentNotes = state.notes[cell]
        val wasPresent = digit in currentNotes
        undoStack.addLast(UndoEntry(cell, state.board[cell], currentNotes))

        val newNotes = state.notes.copyOf()
        newNotes[cell] = if (wasPresent) currentNotes - digit else currentNotes + digit

        _uiState.value = state.copy(notes = newNotes, hasUndo = true)
    }

    private fun erase() {
        val state = _uiState.value
        val cell = state.selectedCell ?: return
        if (state.givens[cell] || cell in state.hintedCells) return

        val prevValue = state.board[cell]
        val prevNotes = state.notes[cell]
        if (prevValue == 0 && prevNotes.isEmpty()) return

        undoStack.addLast(UndoEntry(cell, prevValue, prevNotes))

        val newBoard = state.board.copyOf()
        newBoard[cell] = 0
        val newNotes = state.notes.copyOf()
        newNotes[cell] = emptySet()

        _uiState.value = state.copy(
            board = newBoard,
            notes = newNotes,
            cellsRemaining = newBoard.count { it == 0 },
            conflictMask = ConflictDetector.buildConflictMask(newBoard),
            digitCounts = computeDigitCounts(newBoard),
            hasUndo = true
        )
    }

    private fun undo() {
        if (undoStack.isEmpty()) return
        val entry = undoStack.removeLast()
        val state = _uiState.value

        val newBoard = state.board.copyOf()
        newBoard[entry.cellIndex] = entry.previousValue
        val newNotes = state.notes.copyOf()
        newNotes[entry.cellIndex] = entry.previousNotes

        _uiState.value = state.copy(
            board = newBoard,
            notes = newNotes,
            cellsRemaining = newBoard.count { it == 0 },
            conflictMask = ConflictDetector.buildConflictMask(newBoard),
            digitCounts = computeDigitCounts(newBoard),
            hasUndo = undoStack.isNotEmpty()
        )
    }

    private fun hint() {
        val state = _uiState.value
        val cell = state.selectedCell
        if (cell == null || state.givens[cell] || cell in state.hintedCells) {
            viewModelScope.launch {
                _events.emit(GameEvent.HintUnavailable("Select a cell to reveal"))
            }
            return
        }
        if (state.board[cell] == state.solution[cell]) {
            viewModelScope.launch {
                _events.emit(GameEvent.HintUnavailable("Select a cell to reveal"))
            }
            return
        }

        val newBoard = state.board.copyOf()
        newBoard[cell] = state.solution[cell]
        val newNotes = state.notes.copyOf()
        newNotes[cell] = emptySet()

        val newCellsRemaining = newBoard.count { it == 0 }
        val newConflictMask = ConflictDetector.buildConflictMask(newBoard)
        val isComplete = newCellsRemaining == 0 && newConflictMask.none { it }

        _uiState.value = state.copy(
            board = newBoard,
            notes = newNotes,
            hintedCells = state.hintedCells + cell,
            hintsUsed = state.hintsUsed + 1,
            cellsRemaining = newCellsRemaining,
            conflictMask = newConflictMask,
            digitCounts = computeDigitCounts(newBoard),
            isComplete = isComplete
        )

        if (isComplete) onCompletion()
    }

    private fun toggleInputMode() {
        val state = _uiState.value
        _uiState.value = state.copy(
            inputMode = if (state.inputMode == InputMode.FILL) InputMode.NOTES else InputMode.FILL
        )
    }

    private fun pausePuzzle() {
        updateElapsedTime()
        _uiState.value = _uiState.value.copy(isPaused = true)
    }

    private fun keepForLater() {
        viewModelScope.launch {
            updateElapsedTime()
            saveCurrentGame()
        }
    }

    private fun discardPuzzle() {
        viewModelScope.launch {
            gameRepository.clearGame()
        }
    }

    fun saveOnStop() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.isComplete && state.board.any { it != 0 }) {
                updateElapsedTime()
                saveCurrentGame()
            }
        }
    }

    private suspend fun saveCurrentGame() {
        val state = _uiState.value
        gameRepository.saveGame(
            PersistedGameState(
                board = state.board.toList(),
                solution = state.solution.toList(),
                givens = state.givens.toList(),
                notes = state.notes.map { it.toList() },
                difficulty = state.difficulty.name,
                elapsedMs = state.elapsedMs,
                hintsUsed = state.hintsUsed,
                hintedCells = state.hintedCells.toList()
            )
        )
    }

    private fun updateElapsedTime() {
        val start = timerStartRealtimeMs ?: return
        val now = System.currentTimeMillis()
        val delta = now - start
        _uiState.value = _uiState.value.copy(elapsedMs = _uiState.value.elapsedMs + delta)
        timerStartRealtimeMs = now
    }

    private fun onCompletion() {
        updateElapsedTime()
        timerStartRealtimeMs = null
        val state = _uiState.value
        viewModelScope.launch {
            gameRepository.clearGame()
            val record = recordsRepository.getRecord(state.difficulty)
            val isNewBest = record.bestTimeMs == null || state.elapsedMs < record.bestTimeMs
            val isNewNoHintBest = state.hintsUsed == 0 &&
                (record.bestNoHintTimeMs == null || state.elapsedMs < record.bestNoHintTimeMs)
            val updated = record.copy(
                completedCount = record.completedCount + 1,
                bestTimeMs = if (isNewBest) state.elapsedMs else record.bestTimeMs,
                bestNoHintTimeMs = if (isNewNoHintBest) state.elapsedMs else record.bestNoHintTimeMs,
                lastCompletedEpochMs = System.currentTimeMillis()
            )
            recordsRepository.updateRecord(state.difficulty, updated)
            _events.emit(
                GameEvent.Completed(
                    difficulty = state.difficulty,
                    elapsedMs = state.elapsedMs,
                    hintsUsed = state.hintsUsed,
                    isPersonalBest = isNewBest
                )
            )
        }
    }

    private fun computeDigitCounts(board: IntArray): IntArray {
        val counts = IntArray(9)
        for (v in board) {
            if (v in 1..9) counts[v - 1]++
        }
        return counts
    }
}
