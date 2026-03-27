package com.ledgerman.sudoku.game

import app.cash.turbine.test
import com.ledgerman.sudoku.puzzle.model.Difficulty
import com.ledgerman.sudoku.puzzle.model.SudokuPuzzle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GameViewModelQuitTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeGenerate: suspend (Difficulty) -> SudokuPuzzle = { difficulty ->
        FakeGenerator().generatePuzzle(difficulty)
    }

    private fun makeViewModel(
        repository: FakeGameRepository = FakeGameRepository()
    ): Pair<GameViewModel, FakeGameRepository> {
        val vm = GameViewModel(
            generatePuzzle = fakeGenerate,
            repository = repository,
            ioDispatcher = UnconfinedTestDispatcher()
        )
        return vm to repository
    }

    // ------------------------------------------------------------------ quitGame tests

    /**
     * Test 1: After startGame + quitGame, uiState is reset to default GameUiState()
     * (board all zeros, isLoading false, selectedCellIndex null).
     */
    @Test
    fun `quitGame resets uiState to default GameUiState`() = runTest {
        val (vm, _) = makeViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            awaitItem() // initial idle
            vm.startGame(Difficulty.EASY)
            awaitItem() // loading
            awaitItem() // loaded

            vm.quitGame()

            val stateAfterQuit = awaitItem()
            assertFalse("isLoading should be false after quitGame", stateAfterQuit.isLoading)
            assertNull("selectedCellIndex should be null after quitGame", stateAfterQuit.selectedCellIndex)
            assertTrue("board should be all zeros after quitGame", stateAfterQuit.board.all { it == 0 })
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 2: After startGame + quitGame, showResumeDialog is false.
     */
    @Test
    fun `quitGame sets showResumeDialog to false`() = runTest {
        val (vm, _) = makeViewModel()
        advanceUntilIdle()

        // Start a game and wait for it to load
        vm.uiState.test {
            awaitItem()
            vm.startGame(Difficulty.EASY)
            awaitItem() // loading
            awaitItem() // loaded
            cancelAndIgnoreRemainingEvents()
        }

        vm.quitGame()

        assertFalse("showResumeDialog should be false after quitGame", vm.showResumeDialog.value)
    }

    /**
     * Test 3: After startGame + enterDigit + quitGame, undoStack is empty.
     * Verified by calling undo() after quitGame and asserting uiState is unchanged
     * (undo is a no-op when stack is empty).
     */
    @Test
    fun `quitGame clears undoStack - undo is no-op after quitGame`() = runTest {
        val (vm, _) = makeViewModel()
        advanceUntilIdle()

        vm.uiState.test {
            awaitItem() // initial idle
            vm.startGame(Difficulty.EASY)
            awaitItem() // loading
            awaitItem() // loaded

            // Enter a digit to push to undoStack
            val emptyIdx = FakeGenerator.emptyIndices().first()
            vm.selectCell(emptyIdx)
            awaitItem()
            vm.enterDigit(FakeGenerator.wrongDigitAt(emptyIdx))
            awaitItem()

            vm.quitGame()
            awaitItem() // state reset emission

            // undo() should be a no-op (stack was cleared by quitGame)
            val stateBeforeUndo = vm.uiState.value
            vm.undo()
            // No additional state emission expected since undo stack is empty
            assertEquals(
                "board should remain all zeros after undo (stack was cleared)",
                stateBeforeUndo,
                vm.uiState.value
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * Test 4: quitGame calls repository.clearGame().
     */
    @Test
    fun `quitGame calls repository clearGame`() = runTest {
        val (vm, repo) = makeViewModel()
        advanceUntilIdle()

        // Start a game and wait for it to load
        vm.uiState.test {
            awaitItem()
            vm.startGame(Difficulty.EASY)
            awaitItem() // loading
            awaitItem() // loaded
            cancelAndIgnoreRemainingEvents()
        }

        val clearCountBefore = repo.clearCallCount
        vm.quitGame()

        assertEquals(
            "clearGame should be called once by quitGame",
            clearCountBefore + 1,
            repo.clearCallCount
        )
    }
}
