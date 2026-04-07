package com.interstitial.sudoku.game

import com.interstitial.sudoku.game.model.GameAction
import com.interstitial.sudoku.game.model.InputMode
import com.interstitial.sudoku.puzzle.engine.SudokuGenerator
import com.interstitial.sudoku.puzzle.model.Difficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private lateinit var gameRepo: FakeGameRepository
    private lateinit var recordsRepo: FakeRecordsRepository
    private lateinit var viewModel: GameViewModel

    @Before
    fun setup() {
        gameRepo = FakeGameRepository()
        recordsRepo = FakeRecordsRepository()
        viewModel = GameViewModel(
            generator = SudokuGenerator(),
            gameRepository = gameRepo,
            recordsRepository = recordsRepo,
            generationDispatcher = Dispatchers.Main
        )
    }

    private fun startEasyGame() {
        viewModel.onAction(GameAction.NewGame(Difficulty.EASY))
        val state = viewModel.uiState.value
        assertFalse("Game should have finished generating", state.isGenerating)
        assertTrue("Board should have cells", state.board.any { it != 0 })
    }

    @Test
    fun `selecting a cell updates selectedCell`() {
        startEasyGame()
        viewModel.onAction(GameAction.SelectCell(40))
        assertEquals(40, viewModel.uiState.value.selectedCell)
    }

    @Test
    fun `selecting same cell again deselects`() {
        startEasyGame()
        viewModel.onAction(GameAction.SelectCell(40))
        viewModel.onAction(GameAction.SelectCell(40))
        assertNull(viewModel.uiState.value.selectedCell)
    }

    @Test
    fun `placing digit in fill mode updates board`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.PlaceDigit(1))
        assertEquals(1, viewModel.uiState.value.board[emptyCell])
    }

    @Test
    fun `placing digit on given cell is no-op`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val givenCell = state.givens.indices.first { state.givens[it] }
        val originalValue = state.board[givenCell]
        viewModel.onAction(GameAction.SelectCell(givenCell))
        viewModel.onAction(GameAction.PlaceDigit(if (originalValue == 1) 2 else 1))
        assertEquals(originalValue, viewModel.uiState.value.board[givenCell])
    }

    @Test
    fun `placing digit clears notes on that cell`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.ToggleInputMode)
        viewModel.onAction(GameAction.ToggleNote(3))
        viewModel.onAction(GameAction.ToggleNote(7))
        assertTrue(viewModel.uiState.value.notes[emptyCell].containsAll(setOf(3, 7)))

        viewModel.onAction(GameAction.ToggleInputMode)
        viewModel.onAction(GameAction.PlaceDigit(5))
        assertTrue(viewModel.uiState.value.notes[emptyCell].isEmpty())
    }

    @Test
    fun `toggle note adds and removes candidate`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.ToggleInputMode)
        viewModel.onAction(GameAction.ToggleNote(5))
        assertTrue(5 in viewModel.uiState.value.notes[emptyCell])
        viewModel.onAction(GameAction.ToggleNote(5))
        assertFalse(5 in viewModel.uiState.value.notes[emptyCell])
    }

    @Test
    fun `notes support full 1-9 candidates`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.ToggleInputMode)
        for (d in 1..9) viewModel.onAction(GameAction.ToggleNote(d))
        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7, 8, 9), viewModel.uiState.value.notes[emptyCell])
    }

    @Test
    fun `erase removes player digit`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.PlaceDigit(3))
        assertEquals(3, viewModel.uiState.value.board[emptyCell])
        viewModel.onAction(GameAction.Erase)
        assertEquals(0, viewModel.uiState.value.board[emptyCell])
    }

    @Test
    fun `erase on given cell is no-op`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val givenCell = state.givens.indices.first { state.givens[it] }
        val originalValue = state.board[givenCell]
        viewModel.onAction(GameAction.SelectCell(givenCell))
        viewModel.onAction(GameAction.Erase)
        assertEquals(originalValue, viewModel.uiState.value.board[givenCell])
    }

    @Test
    fun `undo restores previous value`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.PlaceDigit(7))
        assertEquals(7, viewModel.uiState.value.board[emptyCell])
        viewModel.onAction(GameAction.Undo)
        assertEquals(0, viewModel.uiState.value.board[emptyCell])
    }

    @Test
    fun `undo with empty stack is no-op`() {
        startEasyGame()
        assertFalse(viewModel.uiState.value.hasUndo)
        viewModel.onAction(GameAction.Undo)
    }

    @Test
    fun `toggle input mode switches between FILL and NOTES`() {
        startEasyGame()
        assertEquals(InputMode.FILL, viewModel.uiState.value.inputMode)
        viewModel.onAction(GameAction.ToggleInputMode)
        assertEquals(InputMode.NOTES, viewModel.uiState.value.inputMode)
        viewModel.onAction(GameAction.ToggleInputMode)
        assertEquals(InputMode.FILL, viewModel.uiState.value.inputMode)
    }

    @Test
    fun `hint on empty selected cell reveals solution`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        val expectedValue = state.solution[emptyCell]
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.Hint)
        assertEquals(expectedValue, viewModel.uiState.value.board[emptyCell])
        assertTrue(emptyCell in viewModel.uiState.value.hintedCells)
        assertEquals(1, viewModel.uiState.value.hintsUsed)
    }

    @Test
    fun `hinted cell cannot be edited`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.Hint)
        val hintedValue = viewModel.uiState.value.board[emptyCell]
        viewModel.onAction(GameAction.PlaceDigit(if (hintedValue == 1) 2 else 1))
        assertEquals(hintedValue, viewModel.uiState.value.board[emptyCell])
    }

    @Test
    fun `hint is not undoable`() {
        startEasyGame()
        val undoBefore = viewModel.uiState.value.hasUndo
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.Hint)
        assertEquals(undoBefore, viewModel.uiState.value.hasUndo)
    }

    @Test
    fun `digit counts track placements`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val initialOnes = state.board.count { it == 1 }
        assertEquals(initialOnes, state.digitCounts[0])

        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.PlaceDigit(1))
        assertEquals(initialOnes + 1, viewModel.uiState.value.digitCounts[0])
    }
}
