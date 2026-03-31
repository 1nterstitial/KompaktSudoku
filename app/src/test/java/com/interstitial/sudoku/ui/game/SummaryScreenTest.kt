package com.interstitial.sudoku.ui.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mudita.mmd.ThemeMMD
import com.interstitial.sudoku.game.model.CompletionResult
import com.interstitial.sudoku.puzzle.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI tests for [SummaryScreen].
 *
 * Verifies:
 * - All stat labels and values are displayed correctly
 * - Difficulty label is formatted correctly (capitalised)
 * - Personal best notice is shown/hidden based on [CompletionResult.isPersonalBest]
 * - Action button callbacks are invoked on click
 *
 * ThemeMMD resolves to the real MMD AAR (from Maven Central).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class SummaryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun defaultResult(
        difficulty: Difficulty = Difficulty.EASY,
        errorCount: Int = 2,
        hintCount: Int = 1,
        finalScore: Int = 75,
        isPersonalBest: Boolean = false
    ) = CompletionResult(
        difficulty = difficulty,
        errorCount = errorCount,
        hintCount = hintCount,
        finalScore = finalScore,
        isPersonalBest = isPersonalBest
    )

    // ------------------------------------------------------------------ heading

    @Test
    fun `displays Puzzle Complete heading`() {
        composeRule.setContent {
            ThemeMMD {
                SummaryScreen(
                    result = defaultResult(),
                    onViewLeaderboard = {},
                    onBackToMenu = {}
                )
            }
        }
        composeRule.onNodeWithText("Puzzle Complete").assertIsDisplayed()
    }

    // ------------------------------------------------------------------ difficulty label

    @Test
    fun `displays difficulty label in title case`() {
        composeRule.setContent {
            ThemeMMD {
                SummaryScreen(
                    result = defaultResult(difficulty = Difficulty.EASY),
                    onViewLeaderboard = {},
                    onBackToMenu = {}
                )
            }
        }
        composeRule.onNodeWithText("Easy").assertIsDisplayed()
    }

    // ------------------------------------------------------------------ stat rows

    @Test
    fun `displays error count`() {
        composeRule.setContent {
            ThemeMMD {
                SummaryScreen(
                    result = defaultResult(errorCount = 3),
                    onViewLeaderboard = {},
                    onBackToMenu = {}
                )
            }
        }
        composeRule.onNodeWithText("Errors").assertIsDisplayed()
        composeRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun `displays hint count`() {
        composeRule.setContent {
            ThemeMMD {
                SummaryScreen(
                    // Use a hintCount with a unique value not shared by other stats
                    result = defaultResult(errorCount = 1, hintCount = 4, finalScore = 75),
                    onViewLeaderboard = {},
                    onBackToMenu = {}
                )
            }
        }
        composeRule.onNodeWithText("Hints").assertIsDisplayed()
        composeRule.onNodeWithText("4").assertIsDisplayed()
    }

    @Test
    fun `displays final score`() {
        composeRule.setContent {
            ThemeMMD {
                SummaryScreen(
                    result = defaultResult(finalScore = 60),
                    onViewLeaderboard = {},
                    onBackToMenu = {}
                )
            }
        }
        composeRule.onNodeWithText("Score").assertIsDisplayed()
        composeRule.onNodeWithText("60").assertIsDisplayed()
    }

    // ------------------------------------------------------------------ personal best notice

    @Test
    fun `shows personal best notice when isPersonalBest is true`() {
        composeRule.setContent {
            ThemeMMD {
                SummaryScreen(
                    result = defaultResult(isPersonalBest = true),
                    onViewLeaderboard = {},
                    onBackToMenu = {}
                )
            }
        }
        composeRule.onNodeWithText("New personal best!").assertIsDisplayed()
    }

    @Test
    fun `hides personal best notice when isPersonalBest is false`() {
        composeRule.setContent {
            ThemeMMD {
                SummaryScreen(
                    result = defaultResult(isPersonalBest = false),
                    onViewLeaderboard = {},
                    onBackToMenu = {}
                )
            }
        }
        composeRule.onNodeWithText("New personal best!").assertIsNotDisplayed()
    }

    // ------------------------------------------------------------------ button callbacks

    @Test
    fun `View Leaderboard button calls onViewLeaderboard callback`() {
        var callCount = 0
        composeRule.setContent {
            ThemeMMD {
                SummaryScreen(
                    result = defaultResult(),
                    onViewLeaderboard = { callCount++ },
                    onBackToMenu = {}
                )
            }
        }
        composeRule.onNodeWithText("View Leaderboard").performClick()
        assertEquals("View Leaderboard click should invoke callback once", 1, callCount)
    }

    @Test
    fun `Back to Menu button calls onBackToMenu callback`() {
        var callCount = 0
        composeRule.setContent {
            ThemeMMD {
                SummaryScreen(
                    result = defaultResult(),
                    onViewLeaderboard = {},
                    onBackToMenu = { callCount++ }
                )
            }
        }
        composeRule.onNodeWithText("Back to Menu").performClick()
        assertEquals("Back to Menu click should invoke callback once", 1, callCount)
    }
}
