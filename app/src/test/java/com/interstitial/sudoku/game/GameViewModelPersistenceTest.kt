package com.interstitial.sudoku.game

import app.cash.turbine.test
import com.interstitial.sudoku.game.model.GameUiState
import com.interstitial.sudoku.game.model.InputMode
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.interstitial.sudoku.puzzle.model.SudokuPuzzle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GameViewModelPersistenceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ------------------------------------------------------------------ helpers

    private val fakeGenerate: suspend (Difficulty) -> SudokuPuzzle = { difficulty ->
        FakeGenerator().generatePuzzle(difficulty)
    }

    /**
     * Builds a pre-filled GameUiState that simulates a saved in-progress game.
     * Uses FakeGenerator.BOARD + SOLUTION so it's a valid (non-empty) board state.
     */
    private fun buildSavedState(): GameUiState {
        val pencilMarks = Array(81) { emptySet<Int>() }
        pencilMarks[0] = setOf(1, 3)
        return GameUiState(
            board = FakeGenerator.BOARD.copyOf(),
            solution = FakeGenerator.SOLUTION.copyOf(),
            givenMask = BooleanArray(81) { i -> FakeGenerator.BOARD[i] != 0 },
            difficulty = Difficulty.MEDIUM,
            selectedCellIndex = 5,
            inputMode = InputMode.FILL,
            pencilMarks = pencilMarks,
            errorCount = 2,
            isComplete = false,
            isLoading = false
        )
    }

    /**
     * Creates a GameViewModel with a FakeGameRepository and UnconfinedTestDispatcher for IO.
     * The ioDispatcher injection ensures withContext(ioDispatcher) runs synchronously in tests,
     * which is necessary for init{} and other coroutines to complete under advanceUntilIdle().
     */
    private fun makeViewModel(
        savedState: GameUiState? = null,
        repository: FakeGameRepository = FakeGameRepository(savedState)
    ): Pair<GameViewModel, FakeGameRepository> {
        val vm = GameViewModel(
            generatePuzzle = fakeGenerate,
            repository = repository,
            ioDispatcher = UnconfinedTestDispatcher()
        )
        return vm to repository
    }

    // ------------------------------------------------------------------ Test 1: no saved state -> showResumeDialog false

    /**
     * Test 1 (STATE-02): When repository has no saved state, showResumeDialog is false after init.
     */
    @Test
    fun `showResumeDialog is false when repository has no saved state`() = runTest {
        val (vm, _) = makeViewModel(savedState = null)
        advanceUntilIdle()

        vm.showResumeDialog.test {
            assertFalse("showResumeDialog should be false when no saved state", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 2: saved state -> showResumeDialog true

    /**
     * Test 2 (STATE-02): When repository has saved state, showResumeDialog is true after init.
     */
    @Test
    fun `showResumeDialog is true when repository has saved state`() = runTest {
        val (vm, _) = makeViewModel(savedState = buildSavedState())
        advanceUntilIdle()

        vm.showResumeDialog.test {
            assertTrue("showResumeDialog should be true when saved state exists", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 3: resumeGame() restores all fields

    /**
     * Test 3 (STATE-03): resumeGame() restores board, solution, givenMask, difficulty,
     * selectedCellIndex, pencilMarks, and errorCount from saved state.
     */
    @Test
    fun `resumeGame restores all persisted fields from saved state`() = runTest {
        val saved = buildSavedState()
        val (vm, _) = makeViewModel(savedState = saved)
        advanceUntilIdle()

        vm.resumeGame()
        val state = vm.uiState.value

        assertTrue("board should match saved board", state.board.contentEquals(saved.board))
        assertTrue("solution should match saved solution", state.solution.contentEquals(saved.solution))
        assertTrue("givenMask should match saved givenMask", state.givenMask.contentEquals(saved.givenMask))
        assertEquals("difficulty should match saved difficulty", saved.difficulty, state.difficulty)
        assertEquals("selectedCellIndex should match saved selectedCellIndex", saved.selectedCellIndex, state.selectedCellIndex)
        assertEquals("errorCount should match saved errorCount", saved.errorCount, state.errorCount)
        // Check pencilMarks[0] specifically
        assertEquals("pencilMarks[0] should match saved pencilMarks[0]", saved.pencilMarks[0], state.pencilMarks[0])
    }

    // ------------------------------------------------------------------ Test 4: resumeGame() empties undo stack

    /**
     * Test 4 (STATE-03 / D-05): After resumeGame(), undo() is a no-op (undo stack is empty).
     */
    @Test
    fun `undo is a no-op after resumeGame (undo stack cleared)`() = runTest {
        val saved = buildSavedState()
        val (vm, _) = makeViewModel(savedState = saved)
        advanceUntilIdle()

        vm.resumeGame()
        val stateBefore = vm.uiState.value

        vm.uiState.test {
            awaitItem() // consume current value
            vm.undo()
            expectNoEvents() // undo should be a no-op
            assertEquals("state should be unchanged after no-op undo", stateBefore, vm.uiState.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 5: resumeGame() sets showResumeDialog to false

    /**
     * Test 5 (STATE-03): resumeGame() sets showResumeDialog to false.
     */
    @Test
    fun `resumeGame sets showResumeDialog to false`() = runTest {
        val (vm, _) = makeViewModel(savedState = buildSavedState())
        advanceUntilIdle()

        vm.showResumeDialog.test {
            assertTrue("showResumeDialog starts true", awaitItem())

            vm.resumeGame()
            val afterResume = awaitItem()
            assertFalse("showResumeDialog should be false after resumeGame()", afterResume)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 6: startNewGame() clears saved state and starts Easy

    /**
     * Test 6 (STATE-02 / D-04): startNewGame() clears saved state via repository.clearGame()
     * and starts an Easy game.
     */
    @Test
    fun `startNewGame clears saved state and starts Easy game`() = runTest {
        val (vm, repo) = makeViewModel(savedState = buildSavedState())
        advanceUntilIdle()

        vm.uiState.test {
            awaitItem() // initial state

            vm.startNewGame()

            // Drain loading state then wait for loaded state
            val loading = awaitItem()
            assertTrue("Expected isLoading=true after startNewGame()", loading.isLoading)
            val loadedState = awaitItem()
            assertFalse("new game should not be loading", loadedState.isLoading)

            // Should have cleared the saved state (clearGame runs on ioDispatcher)
            advanceUntilIdle()
            assertEquals("clearGame should have been called once", 1, repo.clearCallCount)

            // showResumeDialog should be false
            assertFalse("showResumeDialog should be false after startNewGame()", vm.showResumeDialog.value)

            // Should have started a new Easy game
            assertEquals("new game should be EASY difficulty", loadedState.difficulty, Difficulty.EASY)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 7: saveNow() calls repository.saveGame()

    /**
     * Test 7 (STATE-01): saveNow() calls repository.saveGame() with current state.
     */
    @Test
    fun `saveNow calls repository saveGame with current state`() = runTest {
        val (vm, repo) = makeViewModel()
        advanceUntilIdle()

        // Start a game first and wait for it to fully load via Turbine
        vm.uiState.test {
            awaitItem() // initial idle state
            vm.startGame(Difficulty.EASY)
            awaitItem() // loading state
            val loadedState = awaitItem() // loaded state
            assertFalse("game should be loaded before saveNow()", loadedState.isLoading)
            assertFalse("board should be non-empty", loadedState.board.all { it == 0 })

            vm.saveNow()
            assertEquals("saveGame should have been called once", 1, repo.saveCallCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 8: saveNow() does NOT save when isLoading

    /**
     * Test 8 (STATE-01): saveNow() does NOT save when isLoading is true.
     *
     * Tests the guard directly by building a state with isLoading=true and calling saveNow().
     * This avoids a race condition where FakeGenerator completes instantly on Dispatchers.Default
     * before the test can catch the brief loading state.
     */
    @Test
    fun `saveNow does not save when isLoading is true`() = runTest {
        val (vm, repo) = makeViewModel()
        advanceUntilIdle()

        // Build a state with isLoading=true and a non-empty board to ensure only the
        // isLoading guard (not the empty-board guard) is responsible for blocking the save.
        // We do this by manually setting the state through selectCell + startGame
        // and using a fake repository that reflects the current guard behavior.
        //
        // The simplest approach: call startGame and immediately call saveNow
        // before the puzzle generation coroutine completes (state.isLoading is true).
        vm.startGame(Difficulty.EASY)
        // State is now isLoading=true (set synchronously before the coroutine fires)
        assertTrue("isLoading should be true immediately after startGame()", vm.uiState.value.isLoading)

        vm.saveNow()
        assertEquals("saveGame should NOT be called when isLoading", 0, repo.saveCallCount)
    }

    // ------------------------------------------------------------------ Test 9: saveNow() does NOT save when isComplete

    /**
     * Test 9 (STATE-01): saveNow() does NOT save when isComplete is true.
     */
    @Test
    fun `saveNow does not save when isComplete is true`() = runTest {
        val (vm, repo) = makeViewModel()
        advanceUntilIdle()

        // Start a game and wait for it to load via Turbine
        vm.uiState.test {
            awaitItem() // initial idle
            vm.startGame(Difficulty.EASY)
            awaitItem() // loading
            awaitItem() // loaded

            val emptyIndices = FakeGenerator.emptyIndices()
            for (idx in emptyIndices) {
                vm.selectCell(idx)
                vm.enterDigit(FakeGenerator.correctDigitAt(idx))
                awaitItem() // selectCell state change
                awaitItem() // enterDigit state change
            }
            advanceUntilIdle()

            assertTrue("game should be complete", vm.uiState.value.isComplete)

            // Record save count after completion (clearGame may have been called)
            val saveCountBefore = repo.saveCallCount

            vm.saveNow()
            assertEquals("saveGame should NOT be called when isComplete", saveCountBefore, repo.saveCallCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 10: saveNow() does NOT save when board is empty

    /**
     * Test 10 (STATE-01): saveNow() does NOT save when board is all zeros (empty).
     */
    @Test
    fun `saveNow does not save when board is all zeros`() = runTest {
        val (vm, repo) = makeViewModel()
        advanceUntilIdle()

        // ViewModel starts with empty board (all zeros)
        assertTrue("initial board should be all zeros", vm.uiState.value.board.all { it == 0 })

        vm.saveNow()
        assertEquals("saveGame should NOT be called for empty board", 0, repo.saveCallCount)
    }

    // ------------------------------------------------------------------ Test 11: completion triggers repository.clearGame()

    /**
     * Test 11: Completing a game (filling last correct cell) calls repository.clearGame().
     */
    @Test
    fun `completing a game calls repository clearGame`() = runTest {
        val (vm, repo) = makeViewModel()
        advanceUntilIdle()

        // Use Turbine to wait for the game to fully load before filling cells
        vm.uiState.test {
            awaitItem() // initial idle
            vm.startGame(Difficulty.EASY)
            awaitItem() // loading
            awaitItem() // loaded

            val emptyIndices = FakeGenerator.emptyIndices()
            for (idx in emptyIndices) {
                vm.selectCell(idx)
                vm.enterDigit(FakeGenerator.correctDigitAt(idx))
                awaitItem() // selectCell
                awaitItem() // enterDigit (board update)
            }
            advanceUntilIdle()

            assertTrue("game should be complete", vm.uiState.value.isComplete)
            assertTrue("clearGame should have been called at least once", repo.clearCallCount >= 1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 12: resume + saveNow round-trip

    /**
     * Test 12 (STATE-03): Resume then saveNow() round-trip — resumed state is re-saveable
     * without corruption.
     */
    @Test
    fun `resume then saveNow round-trip preserves state integrity`() = runTest {
        val saved = buildSavedState()
        val (vm, repo) = makeViewModel(savedState = saved)
        advanceUntilIdle()

        vm.resumeGame()

        // State should not be loading, not complete, board non-empty
        val state = vm.uiState.value
        assertFalse("resumed state should not be loading", state.isLoading)
        assertFalse("resumed state should not be complete", state.isComplete)
        assertFalse("resumed board should not be all zeros", state.board.all { it == 0 })

        vm.saveNow()
        assertEquals("saveGame should have been called once after resume + saveNow", 1, repo.saveCallCount)
    }

    // ------------------------------------------------------------------ Test 13: hasSavedGame() returns true when pendingSavedState non-null

    /**
     * Test 13: hasSavedGame() returns true when pendingSavedState is non-null.
     */
    @Test
    fun `hasSavedGame returns true when saved state is pending`() = runTest {
        val (vm, _) = makeViewModel(savedState = buildSavedState())
        advanceUntilIdle()

        assertTrue("hasSavedGame() should return true when saved state exists", vm.hasSavedGame())
    }

    // ------------------------------------------------------------------ Test 14: hasSavedGame() returns false when no saved state

    /**
     * Test 14: hasSavedGame() returns false when no saved state.
     */
    @Test
    fun `hasSavedGame returns false when no saved state`() = runTest {
        val (vm, _) = makeViewModel(savedState = null)
        advanceUntilIdle()

        assertFalse("hasSavedGame() should return false when no saved state", vm.hasSavedGame())
    }
}
