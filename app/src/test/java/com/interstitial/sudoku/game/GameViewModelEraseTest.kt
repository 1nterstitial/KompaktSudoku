package com.interstitial.sudoku.game

import app.cash.turbine.test
import com.interstitial.sudoku.puzzle.model.Difficulty
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameViewModelEraseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        viewModel = GameViewModel(generatePuzzle = { difficulty ->
            FakeGenerator().generatePuzzle(difficulty)
        })
    }

    /**
     * Helper: start a game and wait for the puzzle to load.
     * Must be called inside a test { } lambda on uiState or after an explicit await.
     */
    private suspend fun startAndLoad() {
        viewModel.startGame(Difficulty.EASY)
    }

    // Test 1: eraseCell() on a non-given cell with a digit → board[idx] becomes 0, pencilMarks[idx] becomes emptySet()
    @Test
    fun `eraseCell on non-given cell with digit clears board and pencilMarks`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial
            startAndLoad()
            awaitItem() // loading
            awaitItem() // loaded

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()

            // Fill with a digit
            viewModel.enterDigit(FakeGenerator.wrongDigitAt(emptyIdx))
            val afterFill = awaitItem()
            assertEquals(FakeGenerator.wrongDigitAt(emptyIdx), afterFill.board[emptyIdx])

            // Erase
            viewModel.eraseCell()
            val afterErase = awaitItem()
            assertEquals("board[$emptyIdx] should be 0 after erase", 0, afterErase.board[emptyIdx])
            assertTrue("pencilMarks[$emptyIdx] should be empty after erase", afterErase.pencilMarks[emptyIdx].isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Test 2: eraseCell() on a non-given cell with pencil marks → pencilMarks[idx] becomes emptySet(), board[idx] stays 0
    @Test
    fun `eraseCell on non-given cell with pencil marks clears pencilMarks and board stays 0`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            startAndLoad()
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()

            // Add pencil marks
            viewModel.toggleInputMode() // PENCIL
            awaitItem()
            viewModel.enterDigit(3)
            awaitItem()
            viewModel.enterDigit(7)
            awaitItem()
            assertEquals(setOf(3, 7), viewModel.uiState.value.pencilMarks[emptyIdx])

            // Erase
            viewModel.toggleInputMode() // back to FILL so eraseCell sees consistent mode
            awaitItem()
            viewModel.eraseCell()
            val afterErase = awaitItem()
            assertTrue("pencilMarks[$emptyIdx] should be empty after erase", afterErase.pencilMarks[emptyIdx].isEmpty())
            assertEquals("board[$emptyIdx] should stay 0", 0, afterErase.board[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Test 3: eraseCell() on a given cell → board is unchanged, pencilMarks unchanged
    @Test
    fun `eraseCell on given cell is a no-op`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            startAndLoad()
            awaitItem()
            val loadedState = awaitItem()

            val givenIdx = FakeGenerator.BOARD.indexOfFirst { it != 0 }
            val originalValue = loadedState.board[givenIdx]

            viewModel.selectCell(givenIdx)
            awaitItem()

            viewModel.eraseCell()
            // No state change expected
            expectNoEvents()
            assertEquals("given cell value should be unchanged", originalValue, viewModel.uiState.value.board[givenIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Test 4: eraseCell() with no cell selected → no-op, state unchanged
    @Test
    fun `eraseCell with no cell selected is a no-op`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            startAndLoad()
            awaitItem()
            awaitItem()

            // No selectCell call — selectedCellIndex is null
            val stateBefore = viewModel.uiState.value
            viewModel.eraseCell()
            expectNoEvents()
            assertEquals("state should be unchanged when no cell selected", stateBefore, viewModel.uiState.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // Test 5: eraseCell() then undo() → restores previous digit and pencil marks exactly
    @Test
    fun `eraseCell then undo restores previous digit and pencilMarks`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            startAndLoad()
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()

            // Fill with a digit
            val digit = FakeGenerator.wrongDigitAt(emptyIdx)
            viewModel.enterDigit(digit)
            val afterFill = awaitItem()
            assertEquals(digit, afterFill.board[emptyIdx])

            // Erase
            viewModel.eraseCell()
            val afterErase = awaitItem()
            assertEquals(0, afterErase.board[emptyIdx])

            // Undo erase → restores previous digit
            viewModel.undo()
            val afterUndo = awaitItem()
            assertEquals("undo should restore digit $digit", digit, afterUndo.board[emptyIdx])
            assertTrue("pencilMarks should be empty after undo of erase (was empty before erase)", afterUndo.pencilMarks[emptyIdx].isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
