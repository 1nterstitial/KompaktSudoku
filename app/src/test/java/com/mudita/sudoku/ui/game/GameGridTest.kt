package com.mudita.sudoku.ui.game

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI tests for [GameGrid].
 *
 * Verifies:
 * - Canvas tap dispatches onCellClick with the correct cell index (0–80)
 * - Cell index mapping: (row * 9 + col) for cells at corners and interior
 * - Grid renders without crash on empty board and on full board with pencil marks
 *
 * GameGrid uses only standard Compose (no MMD) so MaterialTheme is sufficient as wrapper.
 *
 * Touch input coordinate strategy:
 * GameGrid is set to fillMaxSize() so it occupies the entire test root node area.
 * This ensures performTouchInput { width/height } on onRoot() matches the grid dimensions.
 * The Canvas uses pointerInput + detectTapGestures internally; fraction-based coordinates
 * (col + 0.5) / 9 * width give deterministic cell targeting.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class GameGridTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ------------------------------------------------------------------ tap-to-index mapping

    @Test
    fun `tapping top-left cell region dispatches onCellClick with index 0`() {
        var clicked: Int? = null
        composeRule.setContent {
            MaterialTheme {
                // fillMaxSize ensures root node has a layout size for pointer-input hit testing.
                // GameGrid uses minOf(maxWidth, maxHeight) as the square grid size.
                GameGrid(
                    modifier = Modifier.fillMaxSize(),
                    board = IntArray(81),
                    solution = IntArray(81),
                    givenMask = BooleanArray(81),
                    selectedCellIndex = null,
                    pencilMarks = Array(81) { emptySet() },
                    onCellClick = { clicked = it }
                )
            }
        }
        // The grid is square with size = min(rootWidth, rootHeight).
        // Use cellSize = min(width, height) / 9 for both x and y tap coords.
        composeRule.onRoot().performTouchInput {
            val gridSize = minOf(width, height).toFloat()
            val cellSize = gridSize / 9f
            // Cell (row=0, col=0) center
            click(Offset(cellSize * 0.5f, cellSize * 0.5f))
        }
        assertEquals("Cell 0 (row=0, col=0) should dispatch index 0", 0, clicked)
    }

    @Test
    fun `tapping bottom-right cell region dispatches onCellClick with index 80`() {
        var clicked: Int? = null
        composeRule.setContent {
            MaterialTheme {
                GameGrid(
                    modifier = Modifier.fillMaxSize(),
                    board = IntArray(81),
                    solution = IntArray(81),
                    givenMask = BooleanArray(81),
                    selectedCellIndex = null,
                    pencilMarks = Array(81) { emptySet() },
                    onCellClick = { clicked = it }
                )
            }
        }
        // The grid is square: use min(width, height) as the grid dimension.
        // Cell (row=8, col=8) center
        composeRule.onRoot().performTouchInput {
            val gridSize = minOf(width, height).toFloat()
            val cellSize = gridSize / 9f
            click(Offset(cellSize * 8.5f, cellSize * 8.5f))
        }
        assertEquals("Cell 80 (row=8, col=8) should dispatch index 80", 80, clicked)
    }

    @Test
    fun `tapping row 1 col 2 dispatches onCellClick with index 11`() {
        var clicked: Int? = null
        composeRule.setContent {
            MaterialTheme {
                GameGrid(
                    modifier = Modifier.fillMaxSize(),
                    board = IntArray(81),
                    solution = IntArray(81),
                    givenMask = BooleanArray(81),
                    selectedCellIndex = null,
                    pencilMarks = Array(81) { emptySet() },
                    onCellClick = { clicked = it }
                )
            }
        }
        // The grid is square: use min(width, height) as the grid dimension.
        // Cell (row=1, col=2) center — expected index: 1 * 9 + 2 = 11
        composeRule.onRoot().performTouchInput {
            val gridSize = minOf(width, height).toFloat()
            val cellSize = gridSize / 9f
            click(Offset(cellSize * 2.5f, cellSize * 1.5f))
        }
        assertEquals("Cell 11 (row=1, col=2) should dispatch index 11", 11, clicked)
    }

    // ------------------------------------------------------------------ render stability

    @Test
    fun `renders without crash when board is all zeros (empty puzzle)`() {
        composeRule.setContent {
            MaterialTheme {
                GameGrid(
                    modifier = Modifier.size(360.dp),
                    board = IntArray(81) { 0 },
                    solution = IntArray(81) { 0 },
                    givenMask = BooleanArray(81) { false },
                    selectedCellIndex = null,
                    pencilMarks = Array(81) { emptySet() },
                    onCellClick = {}
                )
            }
        }
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `renders without crash when all cells have digit values and pencil marks`() {
        composeRule.setContent {
            MaterialTheme {
                GameGrid(
                    modifier = Modifier.size(360.dp),
                    board = IntArray(81) { (it % 9) + 1 },
                    solution = IntArray(81) { (it % 9) + 1 },
                    givenMask = BooleanArray(81) { it < 40 },
                    selectedCellIndex = 40,
                    pencilMarks = Array(81) { setOf(1, 2, 3, 4, 5) },
                    onCellClick = {}
                )
            }
        }
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `renders without crash with no selection`() {
        composeRule.setContent {
            MaterialTheme {
                GameGrid(
                    modifier = Modifier.size(360.dp),
                    board = IntArray(81),
                    solution = IntArray(81),
                    givenMask = BooleanArray(81),
                    selectedCellIndex = null,
                    pencilMarks = Array(81) { emptySet() },
                    onCellClick = {}
                )
            }
        }
        composeRule.onRoot().assertExists()
    }
}
