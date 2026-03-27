package com.ledgerman.sudoku.ui.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mudita.mmd.ThemeMMD
import com.ledgerman.sudoku.puzzle.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI tests for [LeaderboardScreen].
 *
 * Verifies:
 * - "Best Scores" heading is displayed
 * - Stored scores are displayed per difficulty
 * - Em-dash is shown for null (no recorded score) difficulties
 * - All three difficulty labels are present
 * - Back to Menu callback is invoked on button click
 *
 * ThemeMMD resolves to the real MMD AAR (from Maven Central).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class LeaderboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ------------------------------------------------------------------ heading

    @Test
    fun `displays Best Scores heading`() {
        composeRule.setContent {
            ThemeMMD {
                LeaderboardScreen(
                    scores = emptyMap(),
                    onBackToMenu = {}
                )
            }
        }
        composeRule.onNodeWithText("Best Scores").assertIsDisplayed()
    }

    // ------------------------------------------------------------------ score values

    @Test
    fun `displays stored score for Easy`() {
        composeRule.setContent {
            ThemeMMD {
                LeaderboardScreen(
                    scores = mapOf(Difficulty.EASY to 85),
                    onBackToMenu = {}
                )
            }
        }
        composeRule.onNodeWithText("85").assertIsDisplayed()
    }

    @Test
    fun `displays em-dash for difficulty with no score`() {
        composeRule.setContent {
            ThemeMMD {
                LeaderboardScreen(
                    // All nulls: all three rows show em-dash; use allNodes to confirm at least one
                    scores = mapOf(Difficulty.EASY to null, Difficulty.MEDIUM to null, Difficulty.HARD to null),
                    onBackToMenu = {}
                )
            }
        }
        // Em-dash \u2014 appears for each difficulty with a null score.
        // Use onAllNodesWithText to assert at least one em-dash is present.
        composeRule.onAllNodes(
            androidx.compose.ui.test.hasText("\u2014"),
            useUnmergedTree = false
        ).fetchSemanticsNodes().let { nodes ->
            assert(nodes.isNotEmpty()) { "Expected at least one em-dash node but found none" }
        }
    }

    // ------------------------------------------------------------------ difficulty labels

    @Test
    fun `displays all three difficulty labels`() {
        composeRule.setContent {
            ThemeMMD {
                LeaderboardScreen(
                    scores = emptyMap(),
                    onBackToMenu = {}
                )
            }
        }
        composeRule.onNodeWithText("Easy").assertIsDisplayed()
        composeRule.onNodeWithText("Medium").assertIsDisplayed()
        composeRule.onNodeWithText("Hard").assertIsDisplayed()
    }

    // ------------------------------------------------------------------ button callback

    @Test
    fun `Back to Menu button calls onBackToMenu callback`() {
        var callCount = 0
        composeRule.setContent {
            ThemeMMD {
                LeaderboardScreen(
                    scores = emptyMap(),
                    onBackToMenu = { callCount++ }
                )
            }
        }
        composeRule.onNodeWithText("Back to Menu").performClick()
        assertEquals("Back to Menu click should invoke callback once", 1, callCount)
    }
}
