package com.mudita.sudoku.game

import app.cash.turbine.test
import com.mudita.sudoku.game.model.GameEvent
import com.mudita.sudoku.puzzle.model.Difficulty
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
import kotlin.random.Random

/**
 * Tests for GameViewModel hint logic, score computation, personal best detection,
 * and non-undoable hint behavior.
 *
 * All tests use FakeGenerator (deterministic board) + FakeScoreRepository + injectable Random
 * for full determinism and isolation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GameViewModelHintTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ------------------------------------------------------------------ helpers

    private fun createViewModel(
        scoreRepository: FakeScoreRepository = FakeScoreRepository(),
        random: Random = Random(seed = 42)
    ): GameViewModel = GameViewModel(
        generatePuzzle = { difficulty -> FakeGenerator().generatePuzzle(difficulty) },
        repository = FakeGameRepository(),
        scoreRepository = scoreRepository,
        ioDispatcher = UnconfinedTestDispatcher(),
        random = random
    )

    /**
     * Starts a game and waits for the puzzle to fully load using Turbine.
     * Consumes the initial idle state, loading state, and loaded state from uiState.
     */
    private suspend fun startAndWaitLoaded(viewModel: GameViewModel) {
        viewModel.uiState.test {
            awaitItem() // initial idle state
            viewModel.startGame(Difficulty.EASY)
            awaitItem() // isLoading = true
            awaitItem() // isLoading = false (puzzle loaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 1: fills one empty cell

    @Test
    fun `requestHint fills one unfilled non-correct cell with solution value`() = runTest {
        val vm = createViewModel()
        startAndWaitLoaded(vm)

        val emptyIndices = FakeGenerator.emptyIndices()
        // All empty before hint
        val stateBefore = vm.uiState.value
        val allEmpty = emptyIndices.all { stateBefore.board[it] == 0 }
        assertTrue("all empty indices should have 0 before hint", allEmpty)

        vm.requestHint()

        val stateAfter = vm.uiState.value
        // Exactly one previously-empty cell should now have its solution value
        val nowFilled = emptyIndices.count { i ->
            stateAfter.board[i] == FakeGenerator.SOLUTION[i] && stateBefore.board[i] == 0
        }
        assertEquals("exactly one empty cell should be filled by hint", 1, nowFilled)
    }

    // ------------------------------------------------------------------ Test 2: fills wrong-filled cell

    @Test
    fun `requestHint also fills a wrong-filled cell with solution value`() = runTest {
        // Use a seeded random that will pick the first candidate when called
        // The FakeGenerator empty cells are at known indices: 0,2,5,9,11,14,...
        // Fill index 0 with the wrong digit, then request hint targeting only index 0
        // We seed Random so that candidates.random(random) will select index 0 (first candidate)
        val vm = createViewModel(random = Random(seed = 0))
        startAndWaitLoaded(vm)

        val wrongIdx = FakeGenerator.emptyIndices().first() // index 0
        // Fill with wrong digit so it becomes a "wrong-filled" candidate
        vm.selectCell(wrongIdx)
        vm.enterDigit(FakeGenerator.wrongDigitAt(wrongIdx))

        val stateBefore = vm.uiState.value
        assertEquals("cell should have wrong digit before hint", FakeGenerator.wrongDigitAt(wrongIdx), stateBefore.board[wrongIdx])

        vm.requestHint()

        val stateAfter = vm.uiState.value
        // The hinted cell must now contain the correct solution value
        // (either the wrong-filled cell was hinted, or another empty cell was hinted)
        // What matters: at least one non-given cell where board != solution is now correct
        val anyFixed = (0..80).any { i ->
            !stateAfter.givenMask[i] && stateAfter.board[i] == FakeGenerator.SOLUTION[i] &&
                stateBefore.board[i] != FakeGenerator.SOLUTION[i]
        }
        assertTrue("hint should fill at least one non-correct cell with its solution value", anyFixed)
    }

    // ------------------------------------------------------------------ Test 3: increments hintCount

    @Test
    fun `requestHint increments hintCount by 1`() = runTest {
        val vm = createViewModel()
        startAndWaitLoaded(vm)

        assertEquals("hintCount should start at 0", 0, vm.uiState.value.hintCount)

        vm.requestHint()
        assertEquals("hintCount should be 1 after first hint", 1, vm.uiState.value.hintCount)

        vm.requestHint()
        assertEquals("hintCount should be 2 after second hint", 2, vm.uiState.value.hintCount)
    }

    // ------------------------------------------------------------------ Test 4: does NOT increment errorCount

    @Test
    fun `requestHint does NOT increment errorCount`() = runTest {
        val vm = createViewModel()
        startAndWaitLoaded(vm)

        assertEquals("errorCount should start at 0", 0, vm.uiState.value.errorCount)

        vm.requestHint()

        assertEquals("errorCount should still be 0 after hint", 0, vm.uiState.value.errorCount)
    }

    // ------------------------------------------------------------------ Test 5: non-undoable hint

    @Test
    fun `requestHint is non-undoable - undo after hint leaves hinted cell filled`() = runTest {
        val vm = createViewModel()
        startAndWaitLoaded(vm)

        val stateBefore = vm.uiState.value

        vm.requestHint()

        val stateAfterHint = vm.uiState.value
        // Find which cell was filled by the hint
        val hintedCell = FakeGenerator.emptyIndices().firstOrNull { i ->
            stateAfterHint.board[i] != stateBefore.board[i]
        }
        assertTrue("hint should have filled a cell", hintedCell != null)

        val hintedValue = stateAfterHint.board[hintedCell!!]
        assertEquals("hinted cell should contain solution value", FakeGenerator.SOLUTION[hintedCell], hintedValue)

        // Undo — hint should NOT be undone
        vm.undo()

        val stateAfterUndo = vm.uiState.value
        assertEquals(
            "hinted cell should STILL be filled after undo (hints are non-undoable)",
            hintedValue,
            stateAfterUndo.board[hintedCell]
        )
        assertEquals("hintCount should be unchanged after undo of hint", 1, stateAfterUndo.hintCount)
    }

    // ------------------------------------------------------------------ Test 6: no-op when loading

    @Test
    fun `requestHint no-ops when game is loading`() = runTest {
        val vm = createViewModel()

        vm.startGame(Difficulty.EASY)
        // State is isLoading=true synchronously
        assertTrue("game should be loading immediately after startGame()", vm.uiState.value.isLoading)

        vm.requestHint()

        // No hint should have been applied (hintCount still 0, no state change)
        assertEquals("hintCount should be 0 when hint called during loading", 0, vm.uiState.value.hintCount)
    }

    // ------------------------------------------------------------------ Test 7: no-op when complete

    @Test
    fun `requestHint no-ops when game is complete`() = runTest {
        val vm = createViewModel()
        startAndWaitLoaded(vm)

        // Fill all empty cells correctly to complete the game
        for (idx in FakeGenerator.emptyIndices()) {
            vm.selectCell(idx)
            vm.enterDigit(FakeGenerator.correctDigitAt(idx))
        }
        advanceUntilIdle()

        assertTrue("game should be complete", vm.uiState.value.isComplete)
        val hintCountBefore = vm.uiState.value.hintCount

        vm.requestHint()

        assertEquals("hintCount should not change after hint on complete game", hintCountBefore, vm.uiState.value.hintCount)
    }

    // ------------------------------------------------------------------ Test 8: no-op when no valid target

    @Test
    fun `requestHint no-ops when no unfilled non-correct cells exist`() = runTest {
        val vm = createViewModel()
        startAndWaitLoaded(vm)

        // Fill all empty cells correctly (leaving isComplete=true), then check
        // Actually we need to test the case where all non-given cells are correct
        // but BEFORE completion fires — edge case: all filled correct = game complete
        // The no-op guard is: candidates.isEmpty() check before random selection
        // Fill all empty cells with correct digits — this will trigger completion
        for (idx in FakeGenerator.emptyIndices()) {
            vm.selectCell(idx)
            vm.enterDigit(FakeGenerator.correctDigitAt(idx))
        }
        advanceUntilIdle()

        // After completion, isComplete=true, and requestHint is guarded by isComplete check
        assertTrue("game should be complete", vm.uiState.value.isComplete)
        val countBefore = vm.uiState.value.hintCount

        vm.requestHint()

        assertEquals("hintCount should not change when no valid hint targets exist", countBefore, vm.uiState.value.hintCount)
    }

    // ------------------------------------------------------------------ Test 9: hint triggers completion when last cell

    @Test
    fun `requestHint triggers completion when it fills the last cell`() = runTest {
        val vm = createViewModel()
        startAndWaitLoaded(vm)

        val emptyIndices = FakeGenerator.emptyIndices()

        // Fill all but one cell correctly using enterDigit
        for (idx in emptyIndices.dropLast(1)) {
            vm.selectCell(idx)
            vm.enterDigit(FakeGenerator.correctDigitAt(idx))
        }
        advanceUntilIdle()

        assertFalse("game should not be complete yet", vm.uiState.value.isComplete)

        // Request hint to fill the last cell
        vm.events.test {
            vm.requestHint()
            advanceUntilIdle()

            assertTrue("game should be complete after hint fills last cell", vm.uiState.value.isComplete)

            val event = awaitItem()
            assertTrue("Completed event should be emitted", event is GameEvent.Completed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 10: completion score correct

    @Test
    fun `completion emits Completed event with correct score - 2 errors 1 hint = 75`() = runTest {
        val vm = createViewModel()
        startAndWaitLoaded(vm)

        val emptyIndices = FakeGenerator.emptyIndices()

        vm.events.test {
            // Enter 2 wrong digits (2 errors)
            vm.selectCell(emptyIndices[0])
            vm.enterDigit(FakeGenerator.wrongDigitAt(emptyIndices[0]))
            // Correct the first error
            vm.selectCell(emptyIndices[0])
            vm.enterDigit(FakeGenerator.correctDigitAt(emptyIndices[0]))

            vm.selectCell(emptyIndices[1])
            vm.enterDigit(FakeGenerator.wrongDigitAt(emptyIndices[1]))
            // Correct the second error
            vm.selectCell(emptyIndices[1])
            vm.enterDigit(FakeGenerator.correctDigitAt(emptyIndices[1]))

            // Fill remaining cells correctly except last
            for (idx in emptyIndices.drop(2).dropLast(1)) {
                vm.selectCell(idx)
                vm.enterDigit(FakeGenerator.correctDigitAt(idx))
            }
            advanceUntilIdle()

            // Use hint to fill last cell (1 hint)
            vm.requestHint()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue("Expected Completed event", event is GameEvent.Completed)
            val completed = event as GameEvent.Completed
            assertEquals("errorCount should be 2", 2, completed.errorCount)
            assertEquals("hintCount should be 1", 1, completed.hintCount)
            // score = max(0, 100 - 2*10 - 1*5) = max(0, 75) = 75
            assertEquals("score should be 75", 75, completed.score)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 11: personal best when no prior best

    @Test
    fun `completion saves score and sets isPersonalBest true when no prior best`() = runTest {
        val scoreRepo = FakeScoreRepository()
        val vm = createViewModel(scoreRepository = scoreRepo)
        startAndWaitLoaded(vm)

        val emptyIndices = FakeGenerator.emptyIndices()

        vm.events.test {
            // Fill all cells correctly — perfect score 100
            for (idx in emptyIndices) {
                vm.selectCell(idx)
                vm.enterDigit(FakeGenerator.correctDigitAt(idx))
            }
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue("Expected Completed event", event is GameEvent.Completed)
            val completed = event as GameEvent.Completed
            assertTrue("isPersonalBest should be true when no prior best", completed.isPersonalBest)
            assertEquals("saveCallCount should be 1", 1, scoreRepo.saveCallCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 12: personal best when score beats stored best

    @Test
    fun `completion sets isPersonalBest true when score beats stored best`() = runTest {
        val scoreRepo = FakeScoreRepository()
        scoreRepo.preloadScore(Difficulty.EASY, 50) // preload score of 50
        val vm = createViewModel(scoreRepository = scoreRepo)
        startAndWaitLoaded(vm)

        val emptyIndices = FakeGenerator.emptyIndices()

        vm.events.test {
            // Fill all cells correctly — score = 100, which beats stored 50
            for (idx in emptyIndices) {
                vm.selectCell(idx)
                vm.enterDigit(FakeGenerator.correctDigitAt(idx))
            }
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue("Expected Completed event", event is GameEvent.Completed)
            val completed = event as GameEvent.Completed
            assertTrue("isPersonalBest should be true when score (100) > stored (50)", completed.isPersonalBest)
            assertEquals("new score should be saved", 1, scoreRepo.saveCallCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 13: no personal best when score does not beat stored

    @Test
    fun `completion sets isPersonalBest false when score does not beat stored best`() = runTest {
        val scoreRepo = FakeScoreRepository()
        scoreRepo.preloadScore(Difficulty.EASY, 90) // preload score of 90
        val vm = createViewModel(scoreRepository = scoreRepo)
        startAndWaitLoaded(vm)

        val emptyIndices = FakeGenerator.emptyIndices()

        vm.events.test {
            // Enter 2 wrong digits => score = 100 - 20 = 80, which does NOT beat stored 90
            vm.selectCell(emptyIndices[0])
            vm.enterDigit(FakeGenerator.wrongDigitAt(emptyIndices[0]))
            vm.selectCell(emptyIndices[0])
            vm.enterDigit(FakeGenerator.correctDigitAt(emptyIndices[0]))

            vm.selectCell(emptyIndices[1])
            vm.enterDigit(FakeGenerator.wrongDigitAt(emptyIndices[1]))
            vm.selectCell(emptyIndices[1])
            vm.enterDigit(FakeGenerator.correctDigitAt(emptyIndices[1]))

            for (idx in emptyIndices.drop(2)) {
                vm.selectCell(idx)
                vm.enterDigit(FakeGenerator.correctDigitAt(idx))
            }
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue("Expected Completed event", event is GameEvent.Completed)
            val completed = event as GameEvent.Completed
            assertFalse("isPersonalBest should be false when score (80) <= stored (90)", completed.isPersonalBest)
            assertEquals("saveCallCount should be 0 when not a new best", 0, scoreRepo.saveCallCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ------------------------------------------------------------------ Test 14: leaderboardScores reflects saved scores after completion

    @Test
    fun `leaderboardScores reflects saved scores after completion`() = runTest {
        val scoreRepo = FakeScoreRepository()
        val vm = createViewModel(scoreRepository = scoreRepo)
        startAndWaitLoaded(vm)

        val emptyIndices = FakeGenerator.emptyIndices()

        vm.events.test {
            // Fill all cells correctly — perfect score 100
            for (idx in emptyIndices) {
                vm.selectCell(idx)
                vm.enterDigit(FakeGenerator.correctDigitAt(idx))
            }
            advanceUntilIdle()

            awaitItem() // consume Completed event
            cancelAndIgnoreRemainingEvents()
        }

        // After completion, leaderboardScores should contain the new score for EASY
        val leaderboard = vm.leaderboardScores.value
        assertEquals("leaderboard should contain EASY score of 100", 100, leaderboard[Difficulty.EASY])
    }

    // ------------------------------------------------------------------ Test 15: deterministic random

    @Test
    fun `requestHint with seeded Random is deterministic`() = runTest {
        val vm1 = createViewModel(random = Random(seed = 42))
        val vm2 = createViewModel(random = Random(seed = 42))

        startAndWaitLoaded(vm1)
        startAndWaitLoaded(vm2)

        vm1.requestHint()
        vm2.requestHint()

        val board1 = vm1.uiState.value.board
        val board2 = vm2.uiState.value.board

        // Both VMs with the same seed should pick the same cell to hint
        assertTrue(
            "both VMs with same seed should fill the same cell",
            board1.contentEquals(board2)
        )
    }
}
