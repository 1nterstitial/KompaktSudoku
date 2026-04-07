package com.interstitial.sudoku.game

import com.interstitial.sudoku.game.model.PersistedGameState
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class PersistedGameStateTest {

    @Test
    fun `round trip serialization preserves all fields`() {
        val state = PersistedGameState(
            schemaVersion = 1,
            board = (1..81).toList(),
            solution = (1..81).map { ((it - 1) % 9) + 1 },
            givens = (1..81).map { it % 3 == 0 },
            notes = (1..81).map { if (it % 5 == 0) listOf(1, 3, 7) else emptyList() },
            difficulty = "MEDIUM",
            elapsedMs = 123456L,
            hintsUsed = 2,
            hintedCells = listOf(10, 45)
        )
        val json = Json.encodeToString(PersistedGameState.serializer(), state)
        val restored = Json.decodeFromString(PersistedGameState.serializer(), json)
        assertEquals(state, restored)
    }

    @Test
    fun `notes with full 1-9 candidates survive round trip`() {
        val state = PersistedGameState(
            schemaVersion = 1,
            board = List(81) { 0 },
            solution = List(81) { 1 },
            givens = List(81) { false },
            notes = List(81) { listOf(1, 2, 3, 4, 5, 6, 7, 8, 9) },
            difficulty = "HARD",
            elapsedMs = 0L,
            hintsUsed = 0,
            hintedCells = emptyList()
        )
        val json = Json.encodeToString(PersistedGameState.serializer(), state)
        val restored = Json.decodeFromString(PersistedGameState.serializer(), json)
        assertEquals(9, restored.notes[0].size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9), restored.notes[0])
    }
}
