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

    // ------------------------------------------------------------------ pencil marks (INPUT-04)

    @Test
    fun `pencilMark enterDigit in PENCIL mode adds digit to pencilMarks - INPUT-04`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()

            viewModel.toggleInputMode() // FILL -> PENCIL
            awaitItem()

            viewModel.enterDigit(5)
            val state = awaitItem()
            assertTrue("pencilMarks[$emptyIdx] should contain 5", 5 in state.pencilMarks[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pencilMark enterDigit same digit again removes it (toggle off)`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()
            viewModel.toggleInputMode()
            awaitItem()

            viewModel.enterDigit(5)
            awaitItem() // added

            viewModel.enterDigit(5)
            val state = awaitItem() // toggled off
            assertFalse("pencilMarks[$emptyIdx] should not contain 5 after toggle", 5 in state.pencilMarks[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pencilMark multiple digits coexist in same cell`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()
            viewModel.toggleInputMode()
            awaitItem()

            viewModel.enterDigit(1)
            awaitItem()
            viewModel.enterDigit(3)
            awaitItem()
            viewModel.enterDigit(7)
            val state = awaitItem()
            assertEquals(setOf(1, 3, 7), state.pencilMarks[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pencilMark enterDigit in PENCIL mode on given cell is a no-op`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val givenIdx = FakeGenerator.BOARD.indexOfFirst { it != 0 }
            viewModel.selectCell(givenIdx)
            awaitItem()
            viewModel.toggleInputMode()
            awaitItem()

            viewModel.enterDigit(5)
            // No state change expected — given cell is protected
            expectNoEvents()
            assertTrue("pencilMarks[$givenIdx] should be empty for given cell", viewModel.uiState.value.pencilMarks[givenIdx].isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pencilMark enterDigit in PENCIL mode with no selection is a no-op`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            // no selectCell call
            viewModel.toggleInputMode()
            awaitItem()

            viewModel.enterDigit(5)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pencilMark filling cell in FILL mode clears that cells pencil marks`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()

            // Add pencil marks
            viewModel.toggleInputMode() // PENCIL
            awaitItem()
            viewModel.enterDigit(1)
            awaitItem()
            viewModel.enterDigit(3)
            awaitItem()

            // Switch to FILL and fill the cell
            viewModel.toggleInputMode() // FILL
            awaitItem()
            viewModel.enterDigit(FakeGenerator.correctDigitAt(emptyIdx))
            val state = awaitItem()
            assertTrue("pencilMarks[$emptyIdx] should be empty after fill", state.pencilMarks[emptyIdx].isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ undo (INPUT-05)

    @Test
    fun `undo after fill restores previous board value`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()
            viewModel.enterDigit(FakeGenerator.wrongDigitAt(emptyIdx))
            awaitItem()

            viewModel.undo()
            val state = awaitItem()
            assertEquals("board[$emptyIdx] should be 0 after undo", 0, state.board[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `undo after fill restores pencil marks that were cleared`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()

            // Add pencil marks
            viewModel.toggleInputMode()
            awaitItem()
            viewModel.enterDigit(2)
            awaitItem()
            viewModel.enterDigit(4)
            awaitItem()

            // Fill the cell (clears pencil marks)
            viewModel.toggleInputMode()
            awaitItem()
            viewModel.enterDigit(FakeGenerator.correctDigitAt(emptyIdx))
            val afterFill = awaitItem()
            assertTrue("pencilMarks cleared after fill", afterFill.pencilMarks[emptyIdx].isEmpty())

            // Undo: pencil marks should be restored
            viewModel.undo()
            val state = awaitItem()
            assertEquals("pencilMarks should be restored after undo", setOf(2, 4), state.pencilMarks[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `undo after pencil mark add removes the digit`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()
            viewModel.toggleInputMode()
            awaitItem()
            viewModel.enterDigit(5)
            awaitItem()
            assertTrue("5 added", 5 in viewModel.uiState.value.pencilMarks[emptyIdx])

            viewModel.undo()
            val state = awaitItem()
            assertFalse("5 should be removed after undo", 5 in state.pencilMarks[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `undo after pencil mark remove adds the digit back`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()
            viewModel.toggleInputMode()
            awaitItem()

            // Add then remove (toggle)
            viewModel.enterDigit(5)
            awaitItem()
            viewModel.enterDigit(5)
            val afterRemove = awaitItem()
            assertFalse("5 removed", 5 in afterRemove.pencilMarks[emptyIdx])

            // Undo the removal → 5 should come back
            viewModel.undo()
            val state = awaitItem()
            assertTrue("5 should be restored after undo of removal", 5 in state.pencilMarks[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `undo with empty stack is a no-op`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val stateBefore = viewModel.uiState.value
            viewModel.undo() // stack is empty
            expectNoEvents()
            assertEquals("state should be unchanged after no-op undo", stateBefore, viewModel.uiState.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `undo multi-step reverses actions in LIFO order`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val indices = FakeGenerator.emptyIndices()
            val idxA = indices[0]
            val idxB = indices[1]

            // Fill A
            viewModel.selectCell(idxA)
            awaitItem()
            viewModel.enterDigit(FakeGenerator.wrongDigitAt(idxA))
            awaitItem()

            // Fill B
            viewModel.selectCell(idxB)
            awaitItem()
            viewModel.enterDigit(FakeGenerator.wrongDigitAt(idxB))
            awaitItem()

            // Undo B (most recent)
            viewModel.undo()
            val afterUndoB = awaitItem()
            assertEquals("B should be restored to 0", 0, afterUndoB.board[idxB])
            assertEquals("A should still be filled", FakeGenerator.wrongDigitAt(idxA), afterUndoB.board[idxA])

            // Undo A
            viewModel.undo()
            val afterUndoA = awaitItem()
            assertEquals("A should be restored to 0", 0, afterUndoA.board[idxA])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `undo stack is cleared when startGame is called`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            // Fill a cell (pushes to undo stack)
            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()
            viewModel.enterDigit(FakeGenerator.wrongDigitAt(emptyIdx))
            awaitItem()

            // Start a new game (clears undo stack)
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            // Undo should be a no-op (stack was cleared)
            val stateBefore = viewModel.uiState.value
            viewModel.undo()
            expectNoEvents()
            assertEquals("state unchanged after no-op undo", stateBefore, viewModel.uiState.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ error tracking (SCORE-01)

    @Test
    fun `errorTracking correct digit does not increment errorCount - SCORE-01`() = runTest {
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
            assertEquals("errorCount should be 0 for correct fill", 0, state.errorCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `errorTracking wrong digit increments errorCount by 1`() = runTest {
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
            assertEquals("errorCount should be 1 after wrong fill", 1, state.errorCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `errorTracking multiple wrong digits accumulate errorCount`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val indices = FakeGenerator.emptyIndices()
            viewModel.selectCell(indices[0])
            awaitItem()
            viewModel.enterDigit(FakeGenerator.wrongDigitAt(indices[0]))
            awaitItem()

            viewModel.selectCell(indices[1])
            awaitItem()
            viewModel.enterDigit(FakeGenerator.wrongDigitAt(indices[1]))
            val state = awaitItem()
            assertEquals("errorCount should be 2 after two wrong fills", 2, state.errorCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `errorTracking errorCount is not decremented on undo of wrong fill`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()
            viewModel.enterDigit(FakeGenerator.wrongDigitAt(emptyIdx))
            val afterFill = awaitItem()
            assertEquals("errorCount should be 1 after wrong fill", 1, afterFill.errorCount)

            viewModel.undo()
            val afterUndo = awaitItem()
            assertEquals("errorCount should still be 1 after undo (errors are permanent)", 1, afterUndo.errorCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ completion detection (SCORE-02)

    @Test
    fun `completion filling last correct cell sets isComplete true - SCORE-02`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIndices = FakeGenerator.emptyIndices()

            // Fill all but last correct
            for (idx in emptyIndices.dropLast(1)) {
                viewModel.selectCell(idx)
                awaitItem()
                viewModel.enterDigit(FakeGenerator.correctDigitAt(idx))
                awaitItem()
            }

            // Fill last
            val lastIdx = emptyIndices.last()
            viewModel.selectCell(lastIdx)
            awaitItem()
            viewModel.enterDigit(FakeGenerator.correctDigitAt(lastIdx))
            val state = awaitItem()
            assertTrue("isComplete should be true after last correct fill", state.isComplete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completion filling all cells with some wrong does NOT trigger isComplete`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIndices = FakeGenerator.emptyIndices()

            // Fill first with wrong digit
            viewModel.selectCell(emptyIndices.first())
            awaitItem()
            viewModel.enterDigit(FakeGenerator.wrongDigitAt(emptyIndices.first()))
            awaitItem()

            // Fill remaining with correct digits
            for (idx in emptyIndices.drop(1)) {
                viewModel.selectCell(idx)
                awaitItem()
                viewModel.enterDigit(FakeGenerator.correctDigitAt(idx))
                awaitItem()
            }

            assertFalse("isComplete should be false when any cell is wrong", viewModel.uiState.value.isComplete)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completion undo after completing sets isComplete back to false`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIndices = FakeGenerator.emptyIndices()
            for (idx in emptyIndices) {
                viewModel.selectCell(idx)
                awaitItem()
                viewModel.enterDigit(FakeGenerator.correctDigitAt(idx))
                awaitItem()
            }
            assertTrue("should be complete", viewModel.uiState.value.isComplete)

            viewModel.undo()
            val afterUndo = awaitItem()
            assertFalse("isComplete should be false after undo", afterUndo.isComplete)
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

    // ------------------------------------------------------------------ pencil mark cap (GRID-02)

    @Test
    fun `pencilMark 4 marks can be added to a cell - GRID-02`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()
            viewModel.toggleInputMode() // FILL -> PENCIL
            awaitItem()

            viewModel.enterDigit(1)
            awaitItem()
            viewModel.enterDigit(2)
            awaitItem()
            viewModel.enterDigit(3)
            awaitItem()
            viewModel.enterDigit(4)
            val state = awaitItem()
            assertEquals("All 4 marks should be present", setOf(1, 2, 3, 4), state.pencilMarks[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pencilMark 5th mark is silently blocked when cell has 4 - GRID-02`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()
            viewModel.toggleInputMode() // FILL -> PENCIL
            awaitItem()

            // Add 4 marks
            viewModel.enterDigit(1)
            awaitItem()
            viewModel.enterDigit(2)
            awaitItem()
            viewModel.enterDigit(3)
            awaitItem()
            viewModel.enterDigit(4)
            awaitItem()

            // Attempt to add 5th mark — should be a no-op
            viewModel.enterDigit(5)
            expectNoEvents()

            val state = viewModel.uiState.value
            assertEquals("Size should still be 4 after blocked add", 4, state.pencilMarks[emptyIdx].size)
            assertEquals("Set should still be {1,2,3,4}", setOf(1, 2, 3, 4), state.pencilMarks[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pencilMark toggle-off allowed when cell has 4 marks - GRID-02`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()
            viewModel.toggleInputMode() // FILL -> PENCIL
            awaitItem()

            // Add 4 marks
            viewModel.enterDigit(1)
            awaitItem()
            viewModel.enterDigit(2)
            awaitItem()
            viewModel.enterDigit(3)
            awaitItem()
            viewModel.enterDigit(4)
            awaitItem()

            // Toggle off digit 3 (already present → remove)
            viewModel.enterDigit(3)
            val state = awaitItem()
            assertEquals("After removing 3, set should be {1,2,4}", setOf(1, 2, 4), state.pencilMarks[emptyIdx])
            assertEquals("Size should be 3 after removal", 3, state.pencilMarks[emptyIdx].size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pencilMark adding mark after removing one from full cell succeeds - GRID-02`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()
            viewModel.toggleInputMode() // FILL -> PENCIL
            awaitItem()

            // Add 4 marks
            viewModel.enterDigit(1)
            awaitItem()
            viewModel.enterDigit(2)
            awaitItem()
            viewModel.enterDigit(3)
            awaitItem()
            viewModel.enterDigit(4)
            awaitItem()

            // Remove digit 3
            viewModel.enterDigit(3)
            awaitItem()

            // Now add digit 7 (cell has 3 marks, cap not hit)
            viewModel.enterDigit(7)
            val state = awaitItem()
            assertEquals("Set should be {1,2,4,7} after removing 3 and adding 7", setOf(1, 2, 4, 7), state.pencilMarks[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pencilMark undo stack unchanged when 5th mark blocked - GRID-02`() = runTest {
        viewModel.uiState.test {
            awaitItem()
            viewModel.startGame(Difficulty.EASY)
            awaitItem()
            awaitItem()

            val emptyIdx = FakeGenerator.emptyIndices().first()
            viewModel.selectCell(emptyIdx)
            awaitItem()
            viewModel.toggleInputMode() // FILL -> PENCIL
            awaitItem()

            // Add 4 marks (last one is digit 4)
            viewModel.enterDigit(1)
            awaitItem()
            viewModel.enterDigit(2)
            awaitItem()
            viewModel.enterDigit(3)
            awaitItem()
            viewModel.enterDigit(4)
            awaitItem()

            // Attempt blocked 5th mark — no state change
            viewModel.enterDigit(5)
            expectNoEvents()

            // Undo should undo the 4th mark add (digit 4), not the blocked 5th
            viewModel.undo()
            val state = awaitItem()
            assertEquals("Undo should remove digit 4 (last successful add)", setOf(1, 2, 3), state.pencilMarks[emptyIdx])
            cancelAndIgnoreRemainingEvents()
        }
    }
}
