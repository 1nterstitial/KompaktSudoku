package com.mudita.sudoku.game.model

import com.mudita.sudoku.puzzle.model.Difficulty
import kotlinx.serialization.Serializable

/**
 * Serialization DTO for persisting [GameUiState] to DataStore.
 *
 * Uses List-based fields instead of arrays because kotlinx.serialization handles
 * generic List<T> natively without custom serializers. The round-trip:
 *   GameUiState -> toPersistedState() -> Json.encodeToString -> DataStore
 *   DataStore -> Json.decodeFromString -> toUiState() -> GameUiState
 *
 * Fields intentionally NOT persisted (reset on resume):
 * - inputMode: defaults to FILL on resume (player starts in fill mode)
 * - isLoading: defaults to false (puzzle is already loaded)
 * - undoStack: not persisted per decision D-05 (undo history lost on pause)
 */
@Serializable
data class PersistedGameState(
    val board: List<Int>,
    val solution: List<Int>,
    val givenMask: List<Boolean>,
    val difficulty: String,
    val selectedCellIndex: Int? = null,
    val pencilMarks: List<List<Int>>,
    val errorCount: Int,
    val isComplete: Boolean
)

/**
 * Converts [GameUiState] to [PersistedGameState] for JSON serialization.
 *
 * Pencil marks are stored as sorted lists for deterministic JSON output.
 * Difficulty is stored as its enum name string for forward-compatible deserialization.
 */
fun GameUiState.toPersistedState(): PersistedGameState = PersistedGameState(
    board = board.toList(),
    solution = solution.toList(),
    givenMask = givenMask.toList(),
    difficulty = difficulty.name,
    selectedCellIndex = selectedCellIndex,
    pencilMarks = pencilMarks.map { it.sorted() },
    errorCount = errorCount,
    isComplete = isComplete
)

/**
 * Converts [PersistedGameState] back to [GameUiState] after deserialization.
 *
 * inputMode resets to FILL — player starts in fill mode after resuming.
 * isLoading resets to false — puzzle is already loaded on resume.
 * undoStack starts empty — not persisted per decision D-05.
 */
fun PersistedGameState.toUiState(): GameUiState = GameUiState(
    board = board.toIntArray(),
    solution = solution.toIntArray(),
    givenMask = givenMask.toBooleanArray(),
    difficulty = Difficulty.valueOf(difficulty),
    selectedCellIndex = selectedCellIndex,
    inputMode = InputMode.FILL,
    pencilMarks = Array(81) { i -> pencilMarks[i].toSet() },
    errorCount = errorCount,
    isComplete = isComplete,
    isLoading = false
)
