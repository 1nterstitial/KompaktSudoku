package com.mudita.sudoku.game

import com.mudita.sudoku.game.model.GameAction
import com.mudita.sudoku.game.model.GameEvent
import com.mudita.sudoku.game.model.GameUiState
import com.mudita.sudoku.game.model.InputMode
import com.mudita.sudoku.puzzle.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameUiStateTest {

    // --- Default constructor ---

    @Test
    fun `default state has 81-element board of zeros`() {
        val state = GameUiState()
        assertEquals(81, state.board.size)
        assertTrue(state.board.all { it == 0 })
    }

    @Test
    fun `default state has 81-element solution of zeros`() {
        val state = GameUiState()
        assertEquals(81, state.solution.size)
    }

    @Test
    fun `default state has 81-element givenMask all false`() {
        val state = GameUiState()
        assertEquals(81, state.givenMask.size)
        assertTrue(state.givenMask.none { it })
    }

    @Test
    fun `default state has no selected cell`() {
        val state = GameUiState()
        assertNull(state.selectedCellIndex)
    }

    @Test
    fun `default state has FILL inputMode`() {
        val state = GameUiState()
        assertEquals(InputMode.FILL, state.inputMode)
    }

    @Test
    fun `default state has empty pencilMarks for all 81 cells`() {
        val state = GameUiState()
        assertEquals(81, state.pencilMarks.size)
        assertTrue(state.pencilMarks.all { it.isEmpty() })
    }

    @Test
    fun `default state has errorCount zero`() {
        val state = GameUiState()
        assertEquals(0, state.errorCount)
    }

    @Test
    fun `default state isComplete is false`() {
        val state = GameUiState()
        assertFalse(state.isComplete)
    }

    @Test
    fun `default state isLoading is false`() {
        val state = GameUiState()
        assertFalse(state.isLoading)
    }

    // --- equals / hashCode: board ---

    @Test
    fun `equals returns true for two instances with identical IntArray contents`() {
        val board = IntArray(81) { it % 9 + 1 }
        val state1 = GameUiState(board = board.copyOf())
        val state2 = GameUiState(board = board.copyOf())
        assertEquals(state1, state2)
    }

    @Test
    fun `equals returns false when boards differ by one cell`() {
        val board1 = IntArray(81) { 0 }
        val board2 = IntArray(81) { 0 }
        board2[0] = 5
        val state1 = GameUiState(board = board1)
        val state2 = GameUiState(board = board2)
        assertNotEquals(state1, state2)
    }

    @Test
    fun `hashCode is consistent with equals for board content`() {
        val board = IntArray(81) { it % 9 + 1 }
        val state1 = GameUiState(board = board.copyOf())
        val state2 = GameUiState(board = board.copyOf())
        assertEquals(state1.hashCode(), state2.hashCode())
    }

    // --- equals / hashCode: pencilMarks ---

    @Test
    fun `equals returns true for identical pencilMarks`() {
        val marks = Array(81) { emptySet<Int>() }
        marks[5] = setOf(1, 2, 3)
        val state1 = GameUiState(pencilMarks = marks.copyOf())
        val state2 = GameUiState(pencilMarks = marks.copyOf())
        assertEquals(state1, state2)
    }

    @Test
    fun `equals returns false when pencilMarks differ`() {
        val marks1 = Array(81) { emptySet<Int>() }
        val marks2 = Array(81) { emptySet<Int>() }
        marks2[10] = setOf(7)
        val state1 = GameUiState(pencilMarks = marks1)
        val state2 = GameUiState(pencilMarks = marks2)
        assertNotEquals(state1, state2)
    }

    // --- InputMode enum ---

    @Test
    fun `InputMode has exactly FILL and PENCIL values`() {
        val values = InputMode.values()
        assertEquals(2, values.size)
        assertTrue(values.contains(InputMode.FILL))
        assertTrue(values.contains(InputMode.PENCIL))
    }

    // --- GameAction ---

    @Test
    fun `GameAction FillCell stores cellIndex previousValue and previousPencilMarks`() {
        val pencilMarks = setOf(1, 2, 3)
        val action = GameAction.FillCell(cellIndex = 5, previousValue = 3, previousPencilMarks = pencilMarks)
        assertEquals(5, action.cellIndex)
        assertEquals(3, action.previousValue)
        assertEquals(pencilMarks, action.previousPencilMarks)
    }

    @Test
    fun `GameAction SetPencilMark stores cellIndex digit and wasAdded`() {
        val action = GameAction.SetPencilMark(cellIndex = 10, digit = 7, wasAdded = true)
        assertEquals(10, action.cellIndex)
        assertEquals(7, action.digit)
        assertTrue(action.wasAdded)
    }

    @Test
    fun `GameAction SetPencilMark wasAdded false for removal`() {
        val action = GameAction.SetPencilMark(cellIndex = 0, digit = 1, wasAdded = false)
        assertFalse(action.wasAdded)
    }

    // --- GameEvent ---

    @Test
    fun `GameEvent Completed stores errorCount`() {
        val event = GameEvent.Completed(errorCount = 3)
        assertEquals(3, event.errorCount)
    }

    @Test
    fun `GameEvent Completed with zero errors`() {
        val event = GameEvent.Completed(errorCount = 0)
        assertEquals(0, event.errorCount)
    }

    // --- difficulty field ---

    @Test
    fun `default difficulty is EASY`() {
        val state = GameUiState()
        assertEquals(Difficulty.EASY, state.difficulty)
    }

    @Test
    fun `difficulty can be set to HARD`() {
        val state = GameUiState(difficulty = Difficulty.HARD)
        assertEquals(Difficulty.HARD, state.difficulty)
    }

    // --- isLoading and isComplete in equals/hashCode ---

    @Test
    fun `equals returns false when isLoading differs`() {
        val state1 = GameUiState(isLoading = true)
        val state2 = GameUiState(isLoading = false)
        assertNotEquals(state1, state2)
    }

    @Test
    fun `equals returns false when isComplete differs`() {
        val state1 = GameUiState(isComplete = true)
        val state2 = GameUiState(isComplete = false)
        assertNotEquals(state1, state2)
    }
}
