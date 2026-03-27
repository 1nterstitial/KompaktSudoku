package com.ledgerman.sudoku.game

import com.ledgerman.sudoku.game.model.GameUiState
import com.ledgerman.sudoku.game.model.InputMode
import com.ledgerman.sudoku.game.model.PersistedGameState
import com.ledgerman.sudoku.game.model.toPersistedState
import com.ledgerman.sudoku.game.model.toUiState
import com.ledgerman.sudoku.puzzle.model.Difficulty
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PersistedGameStateTest {

    /** Builds a GameUiState with non-default values for all fields. */
    private fun buildTestState(): GameUiState = GameUiState(
        board = FakeGenerator.BOARD.copyOf(),
        solution = FakeGenerator.SOLUTION.copyOf(),
        givenMask = BooleanArray(81) { i -> FakeGenerator.BOARD[i] != 0 },
        difficulty = Difficulty.MEDIUM,
        selectedCellIndex = 5,
        inputMode = InputMode.PENCIL,
        pencilMarks = Array(81) { if (it == 0) setOf(1, 3, 7) else emptySet() },
        errorCount = 3,
        isComplete = false,
        isLoading = true
    )

    // Test 1: Round-trip conversion preserves all persisted fields
    @Test
    fun `round-trip conversion preserves board`() {
        val original = buildTestState()
        val restored = original.toPersistedState().toUiState()
        assertArrayEquals(original.board, restored.board)
    }

    @Test
    fun `round-trip conversion preserves solution`() {
        val original = buildTestState()
        val restored = original.toPersistedState().toUiState()
        assertArrayEquals(original.solution, restored.solution)
    }

    @Test
    fun `round-trip conversion preserves givenMask`() {
        val original = buildTestState()
        val restored = original.toPersistedState().toUiState()
        // BooleanArray equality via contentEquals
        assert(original.givenMask.contentEquals(restored.givenMask)) {
            "givenMask did not round-trip correctly"
        }
    }

    @Test
    fun `round-trip conversion preserves difficulty`() {
        val original = buildTestState()
        val restored = original.toPersistedState().toUiState()
        assertEquals(original.difficulty, restored.difficulty)
    }

    @Test
    fun `round-trip conversion preserves selectedCellIndex`() {
        val original = buildTestState()
        val restored = original.toPersistedState().toUiState()
        assertEquals(original.selectedCellIndex, restored.selectedCellIndex)
    }

    @Test
    fun `round-trip conversion preserves errorCount`() {
        val original = buildTestState()
        val restored = original.toPersistedState().toUiState()
        assertEquals(original.errorCount, restored.errorCount)
    }

    @Test
    fun `round-trip conversion preserves isComplete`() {
        val original = buildTestState()
        val restored = original.toPersistedState().toUiState()
        assertEquals(original.isComplete, restored.isComplete)
    }

    // Test 2: inputMode is NOT persisted — defaults to FILL after round-trip
    @Test
    fun `round-trip resets inputMode to FILL regardless of original`() {
        val original = buildTestState() // inputMode = PENCIL
        val restored = original.toPersistedState().toUiState()
        assertEquals(InputMode.FILL, restored.inputMode)
    }

    // Test 3: isLoading is NOT persisted — defaults to false after round-trip
    @Test
    fun `round-trip resets isLoading to false regardless of original`() {
        val original = buildTestState() // isLoading = true
        val restored = original.toPersistedState().toUiState()
        assertFalse(restored.isLoading)
    }

    // Test 4: JSON round-trip produces identical object
    @Test
    fun `JSON round-trip produces identical PersistedGameState`() {
        val original = buildTestState().toPersistedState()
        val json = Json.encodeToString(original)
        @Suppress("DEPRECATION")
        val decoded = Json.decodeFromString<PersistedGameState>(json)
        assertEquals(original, decoded)
    }

    // Test 5: Pencil marks with non-empty sets round-trip correctly, sorted
    @Test
    fun `pencil marks with non-empty sets round-trip correctly`() {
        val original = buildTestState()
        val restored = original.toPersistedState().toUiState()
        // Cell 0 had {1, 3, 7}
        assertEquals(setOf(1, 3, 7), restored.pencilMarks[0])
        // Other cells are empty
        assertEquals(emptySet<Int>(), restored.pencilMarks[1])
    }

    @Test
    fun `pencil marks stored as sorted list in PersistedGameState`() {
        val original = buildTestState()
        val persisted = original.toPersistedState()
        // Cell 0 had {1, 3, 7} — should be stored sorted
        assertEquals(listOf(1, 3, 7), persisted.pencilMarks[0])
    }

    // Test 6: Null selectedCellIndex round-trips correctly
    @Test
    fun `null selectedCellIndex round-trips correctly`() {
        val original = GameUiState(selectedCellIndex = null)
        val restored = original.toPersistedState().toUiState()
        assertNull(restored.selectedCellIndex)
    }

    // Test 7: Empty board (all zeros) round-trips correctly
    @Test
    fun `empty board all zeros round-trips correctly`() {
        val original = GameUiState(
            board = IntArray(81) { 0 },
            solution = FakeGenerator.SOLUTION.copyOf()
        )
        val restored = original.toPersistedState().toUiState()
        assertArrayEquals(IntArray(81) { 0 }, restored.board)
    }

    // Test 8: Backward compatibility — JSON without hintCount deserializes to hintCount = 0
    @Test
    fun `JSON missing hintCount field deserializes to hintCount 0`() {
        // Simulate Phase 4 saved data that predates hintCount
        val jsonWithoutHintCount = """
            {
                "board": [${IntArray(81) { 0 }.joinToString(",")}],
                "solution": [${FakeGenerator.SOLUTION.joinToString(",")}],
                "givenMask": [${BooleanArray(81) { false }.joinToString(",") { it.toString() }}],
                "difficulty": "EASY",
                "selectedCellIndex": null,
                "pencilMarks": [${(0 until 81).joinToString(",") { "[]" }}],
                "errorCount": 2,
                "isComplete": false
            }
        """.trimIndent()

        @Suppress("DEPRECATION")
        val decoded = Json.decodeFromString<PersistedGameState>(jsonWithoutHintCount)
        assertEquals(0, decoded.hintCount)
    }

    // Test 9: hintCount round-trips correctly when non-zero
    @Test
    fun `hintCount 3 round-trips through JSON correctly`() {
        val original = GameUiState(
            board = FakeGenerator.BOARD.copyOf(),
            solution = FakeGenerator.SOLUTION.copyOf(),
            givenMask = BooleanArray(81) { i -> FakeGenerator.BOARD[i] != 0 },
            difficulty = Difficulty.HARD,
            pencilMarks = Array(81) { emptySet() },
            errorCount = 1,
            hintCount = 3,
            isComplete = false
        )
        val json = Json.encodeToString(original.toPersistedState())
        @Suppress("DEPRECATION")
        val decoded = Json.decodeFromString<PersistedGameState>(json)
        assertEquals(3, decoded.hintCount)
        assertEquals(3, decoded.toUiState().hintCount)
    }
}
