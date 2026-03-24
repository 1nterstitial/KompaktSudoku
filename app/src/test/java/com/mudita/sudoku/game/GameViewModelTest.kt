package com.mudita.sudoku.game

import app.cash.turbine.test
import com.mudita.sudoku.game.model.GameEvent
import com.mudita.sudoku.game.model.InputMode
import com.mudita.sudoku.puzzle.model.Difficulty
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        viewModel = GameViewModel(generatePuzzle = { difficulty ->
            FakeGenerator().generatePuzzle(difficulty)
        })
    }

    // ------------------------------------------------------------------ initial state

    @Test
    fun `initial state has isLoading false, selectedCellIndex null, inputMode FILL, errorCount 0`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.selectedCellIndex)
            assertEquals(InputMode.FILL, state.inputMode)
            assertEquals(0, state.errorCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ startGame

    @Test
    fun `startGame EASY - emits loading then loaded state - DIFF-01`() = runTest {
        viewModel.uiState.test {
            awaitItem() // initial idle state

            viewModel.startGame(Difficulty.EASY)

            val loadingState = awaitItem()
            assertTrue("Expected isLoading=true after startGame", loadingState.isLoading)

            val loadedState = awaitItem()
            assertFalse("Expected isLoading=false after puzzle ready", loadedState.isLoading)
            assertEquals(Difficulty.EASY, loadedState.difficulty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startGame HARD sets difficulty to HARD - DIFF-02`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.HARD)
            awaitItem() // loading
            val loadedState = awaitItem()
            assertEquals(Difficulty.HARD, loadedState.difficulty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startGame produces non-empty board with matching givenMask`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem() // loading
            val state = awaitItem()

            // board has non-zero cells
            assertTrue("board should have non-zero cells", state.board.any { it != 0 })

            // givenMask matches board non-zero pattern
            for (i in 0..80) {
                val expected = state.board[i] != 0
                assertEquals(
                    "givenMask[$i] should be ${expected} (board[$i]=${state.board[i]})",
                    expected,
                    state.givenMask[i]
                )
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startGame clears previous game undo history and error count`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            // First game with an error
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            val firstLoaded = awaitItem()
            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()
            viewModel.enterDigit(FakeGenerator.wrongDigitAt(emptyIdx))
            awaitItem()

            // Start a second game — error count and undo should reset
            viewModel.startGame(Difficulty.EASY)
            awaitItem() // loading
            val secondLoaded = awaitItem()
            assertEquals("errorCount should reset on new game", 0, secondLoaded.errorCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ selectCell

    @Test
    fun `selectCell on empty cell updates selectedCellIndex - INPUT-01`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem() // loaded

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            val state = awaitItem()
            assertEquals(emptyIdx, state.selectedCellIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectCell on given cell updates selectedCellIndex (selection allowed)`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            // Find a given cell index
            val givenIdx = FakeGenerator.BOARD.indexOfFirst { it != 0 }
            viewModel.selectCell(givenIdx)
            val state = awaitItem()
            assertEquals(givenIdx, state.selectedCellIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selectCell replaces previous selection (no multi-select)`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val indices = FakeGenerator.emptyIndices()
            val firstIdx = indices[0]
            val secondIdx = indices[1]

            viewModel.selectCell(firstIdx)
            awaitItem()

            viewModel.selectCell(secondIdx)
            val state = awaitItem()
            assertEquals(secondIdx, state.selectedCellIndex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ enterDigit

    @Test
    fun `enterDigit on selected empty cell fills board - INPUT-02`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()

            viewModel.enterDigit(FakeGenerator.correctDigitAt(emptyIdx))
            val state = awaitItem()
            assertEquals(FakeGenerator.correctDigitAt(emptyIdx), state.board[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `enterDigit on given cell is a no-op`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            val loadedState = awaitItem()

            val givenIdx = FakeGenerator.BOARD.indexOfFirst { it != 0 }
            val originalValue = loadedState.board[givenIdx]

            viewModel.selectCell(givenIdx)
            awaitItem()

            viewModel.enterDigit(FakeGenerator.wrongDigitAt(givenIdx))
            // No state change expected — board[givenIdx] unchanged
            expectNoEvents()
            assertEquals(originalValue, viewModel.uiState.value.board[givenIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `enterDigit with no cell selected is a no-op`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            // No selectCell call
            viewModel.enterDigit(5)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `enterDigit overwrites existing fill value`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()

            // First fill
            val firstDigit = FakeGenerator.wrongDigitAt(emptyIdx)
            viewModel.enterDigit(firstDigit)
            val afterFirst = awaitItem()
            assertEquals(firstDigit, afterFirst.board[emptyIdx])

            // Overwrite
            val correctDigit = FakeGenerator.correctDigitAt(emptyIdx)
            viewModel.enterDigit(correctDigit)
            val afterSecond = awaitItem()
            assertEquals(correctDigit, afterSecond.board[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `enterDigit wrong digit increments errorCount`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()

            viewModel.enterDigit(FakeGenerator.wrongDigitAt(emptyIdx))
            val state = awaitItem()
            assertEquals(1, state.errorCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `enterDigit correct digit does not increment errorCount`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()

            viewModel.enterDigit(FakeGenerator.correctDigitAt(emptyIdx))
            val state = awaitItem()
            assertEquals(0, state.errorCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `enterDigit fill records FillCell in undoStack for overwrite verification (undo in Plan 03)`() = runTest {
        // This test verifies that overwriting a previously filled cell does not crash
        // and that state is consistent. The undo stack itself is verified in Plan 03.
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()

            val first = FakeGenerator.wrongDigitAt(emptyIdx)
            val second = FakeGenerator.correctDigitAt(emptyIdx)
            viewModel.enterDigit(first)
            awaitItem()
            // Overwrite — should push FillCell(idx, first, ...) onto undoStack
            viewModel.enterDigit(second)
            val state = awaitItem()
            // Board should show second value, no crash
            assertEquals(second, state.board[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ toggleInputMode

    @Test
    fun `toggleInputMode switches from FILL to PENCIL - INPUT-03`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(InputMode.FILL, initial.inputMode)

            viewModel.toggleInputMode()
            val toggled = awaitItem()
            assertEquals(InputMode.PENCIL, toggled.inputMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleInputMode switches from PENCIL back to FILL`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.toggleInputMode()
            awaitItem() // PENCIL
            viewModel.toggleInputMode()
            val state = awaitItem()
            assertEquals(InputMode.FILL, state.inputMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ completion event

    @Test
    fun `completing all cells emits GameEvent Completed with correct errorCount`() = runTest {
        val emptyIndices = FakeGenerator.emptyIndices()

        // Subscribe to events first before starting game
        viewModel.events.test {
            // Start game and wait for puzzle to load via uiState
            viewModel.startGame(Difficulty.EASY)
            // Poll uiState until loaded (isLoading becomes false)
            var attempts = 0
            while (viewModel.uiState.value.isLoading || viewModel.uiState.value.board.all { it == 0 }) {
                kotlinx.coroutines.delay(10)
                if (++attempts > 100) break
            }

            // Fill the first empty cell with a wrong digit (+1 error)
            val wrongIdx = emptyIndices.first()
            viewModel.selectCell(wrongIdx)
            viewModel.enterDigit(FakeGenerator.wrongDigitAt(wrongIdx))

            // Correct that cell
            viewModel.selectCell(wrongIdx)
            viewModel.enterDigit(FakeGenerator.correctDigitAt(wrongIdx))

            // Fill remaining empty cells with correct digits
            for (idx in emptyIndices.drop(1)) {
                viewModel.selectCell(idx)
                viewModel.enterDigit(FakeGenerator.correctDigitAt(idx))
            }

            val event = awaitItem()
            assertTrue("Expected Completed event", event is GameEvent.Completed)
            val completedEvent = event as GameEvent.Completed
            // wrongIdx filled wrong (+1 error), then corrected (0 extra) = 1 total
            assertEquals(1, completedEvent.errorCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
