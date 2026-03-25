package com.mudita.sudoku.ui.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mudita.mmd.ThemeMMD
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI tests for [MenuScreen].
 *
 * Verifies (NAV-01a and NAV-01b):
 * - "Sudoku" title is displayed
 * - "New Game" and "Best Scores" buttons are always displayed
 * - "Resume" button is shown when [hasSavedGame] is true, absent when false
 * - All three callbacks are invoked when the respective buttons are clicked
 *
 * ThemeMMD resolves to the real MMD AAR (from Maven Central).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class MenuScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ------------------------------------------------------------------ title

    @Test
    fun `displays Sudoku title`() {
        composeRule.setContent {
            ThemeMMD {
                MenuScreen(hasSavedGame = false, onNewGame = {}, onResume = {}, onBestScores = {})
            }
        }
        composeRule.onNodeWithText("Sudoku").assertIsDisplayed()
    }

    // ------------------------------------------------------------------ buttons always visible

    @Test
    fun `displays New Game button`() {
        composeRule.setContent {
            ThemeMMD {
                MenuScreen(hasSavedGame = false, onNewGame = {}, onResume = {}, onBestScores = {})
            }
        }
        composeRule.onNodeWithText("New Game").assertIsDisplayed()
    }

    @Test
    fun `displays Best Scores button`() {
        composeRule.setContent {
            ThemeMMD {
                MenuScreen(hasSavedGame = false, onNewGame = {}, onResume = {}, onBestScores = {})
            }
        }
        composeRule.onNodeWithText("Best Scores").assertIsDisplayed()
    }

    // ------------------------------------------------------------------ conditional Resume button

    @Test
    fun `shows Resume button when hasSavedGame is true`() {
        composeRule.setContent {
            ThemeMMD {
                MenuScreen(hasSavedGame = true, onNewGame = {}, onResume = {}, onBestScores = {})
            }
        }
        composeRule.onNodeWithText("Resume").assertIsDisplayed()
    }

    @Test
    fun `hides Resume button when hasSavedGame is false`() {
        composeRule.setContent {
            ThemeMMD {
                MenuScreen(hasSavedGame = false, onNewGame = {}, onResume = {}, onBestScores = {})
            }
        }
        composeRule.onNodeWithText("Resume").assertDoesNotExist()
    }

    // ------------------------------------------------------------------ callbacks

    @Test
    fun `New Game button calls onNewGame callback`() {
        var callCount = 0
        composeRule.setContent {
            ThemeMMD {
                MenuScreen(hasSavedGame = false, onNewGame = { callCount++ }, onResume = {}, onBestScores = {})
            }
        }
        composeRule.onNodeWithText("New Game").performClick()
        assertEquals(1, callCount)
    }

    @Test
    fun `Resume button calls onResume callback`() {
        var callCount = 0
        composeRule.setContent {
            ThemeMMD {
                MenuScreen(hasSavedGame = true, onNewGame = {}, onResume = { callCount++ }, onBestScores = {})
            }
        }
        composeRule.onNodeWithText("Resume").performClick()
        assertEquals(1, callCount)
    }

    @Test
    fun `Best Scores button calls onBestScores callback`() {
        var callCount = 0
        composeRule.setContent {
            ThemeMMD {
                MenuScreen(hasSavedGame = false, onNewGame = {}, onResume = {}, onBestScores = { callCount++ })
            }
        }
        composeRule.onNodeWithText("Best Scores").performClick()
        assertEquals(1, callCount)
    }
}
