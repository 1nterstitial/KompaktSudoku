package com.mudita.sudoku.ui.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.mudita.mmd.ThemeMMD
import com.mudita.sudoku.game.GameViewModel
import com.mudita.sudoku.puzzle.model.Difficulty
import com.mudita.sudoku.puzzle.model.SudokuPuzzle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration smoke tests for [GameScreen].
 *
 * Verifies:
 * - Screen renders without crash on initial composition
 * - After puzzle load completes, the number pad is visible (key interaction element)
 *
 * GameScreen uses ThemeMMD which resolves to the test stub (test source set),
 * which delegates to MaterialTheme. ButtonMMD/TextMMD also resolve to stubs.
 *
 * The fake puzzle generator returns a known valid puzzle instantly, so tests
 * do not need to wait for real puzzle generation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class GameScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Fake puzzle generator that returns a known board instantly (no backtracking). */
    private val fakePuzzleGenerator: suspend (Difficulty) -> SudokuPuzzle = { difficulty ->
        SudokuPuzzle(
            board = IntArray(81) { index ->
                // 20 empty cells spread across grid, rest filled
                if (index in setOf(0, 2, 5, 9, 11, 14, 18, 20, 23, 27, 29, 32,
                        36, 38, 41, 45, 47, 50, 54, 56)) 0
                else (index % 9) + 1
            },
            solution = IntArray(81) { (it % 9) + 1 },
            difficulty = difficulty
        )
    }

    // ------------------------------------------------------------------ smoke tests

    @Test
    fun `GameScreen renders without crash on composition`() {
        composeRule.setContent {
            ThemeMMD {
                GameScreen(
                    viewModel = GameViewModel(generatePuzzle = fakePuzzleGenerator)
                )
            }
        }
        // Compose tree renders without throwing — root node exists
        composeRule.onRoot().assertExists()
    }

    @Test
    fun `GameScreen shows number pad after puzzle loads`() {
        composeRule.setContent {
            ThemeMMD {
                GameScreen(
                    viewModel = GameViewModel(generatePuzzle = fakePuzzleGenerator)
                )
            }
        }
        // After startGame completes (fake generator is instant, Compose test rule waits for idle),
        // the loaded game screen shows the NumberPad with digit "1"
        composeRule.onNodeWithText("1").assertIsDisplayed()
    }
}
