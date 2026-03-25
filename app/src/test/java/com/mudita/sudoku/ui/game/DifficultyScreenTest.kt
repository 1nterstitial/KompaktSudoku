package com.mudita.sudoku.ui.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mudita.mmd.ThemeMMD
import com.mudita.sudoku.puzzle.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI tests for [DifficultyScreen].
 *
 * Verifies (NAV-01c and NAV-01d):
 * - "Select Difficulty" heading is displayed
 * - Easy, Medium, Hard, and Back buttons are displayed
 * - Each difficulty button invokes onDifficultySelected with the correct [Difficulty] value
 * - Back button invokes onBack callback
 *
 * ThemeMMD resolves to the real MMD AAR (from Maven Central).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class DifficultyScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ------------------------------------------------------------------ heading

    @Test
    fun `displays Select Difficulty heading`() {
        composeRule.setContent {
            ThemeMMD {
                DifficultyScreen(onDifficultySelected = {}, onBack = {})
            }
        }
        composeRule.onNodeWithText("Select Difficulty").assertIsDisplayed()
    }

    // ------------------------------------------------------------------ difficulty buttons

    @Test
    fun `displays Easy button`() {
        composeRule.setContent {
            ThemeMMD {
                DifficultyScreen(onDifficultySelected = {}, onBack = {})
            }
        }
        composeRule.onNodeWithText("Easy").assertIsDisplayed()
    }

    @Test
    fun `displays Medium button`() {
        composeRule.setContent {
            ThemeMMD {
                DifficultyScreen(onDifficultySelected = {}, onBack = {})
            }
        }
        composeRule.onNodeWithText("Medium").assertIsDisplayed()
    }

    @Test
    fun `displays Hard button`() {
        composeRule.setContent {
            ThemeMMD {
                DifficultyScreen(onDifficultySelected = {}, onBack = {})
            }
        }
        composeRule.onNodeWithText("Hard").assertIsDisplayed()
    }

    @Test
    fun `displays Back button`() {
        composeRule.setContent {
            ThemeMMD {
                DifficultyScreen(onDifficultySelected = {}, onBack = {})
            }
        }
        composeRule.onNodeWithText("Back").assertIsDisplayed()
    }

    // ------------------------------------------------------------------ callbacks

    @Test
    fun `Easy button calls onDifficultySelected with EASY`() {
        var selected: Difficulty? = null
        composeRule.setContent {
            ThemeMMD {
                DifficultyScreen(onDifficultySelected = { selected = it }, onBack = {})
            }
        }
        composeRule.onNodeWithText("Easy").performClick()
        assertEquals(Difficulty.EASY, selected)
    }

    @Test
    fun `Medium button calls onDifficultySelected with MEDIUM`() {
        var selected: Difficulty? = null
        composeRule.setContent {
            ThemeMMD {
                DifficultyScreen(onDifficultySelected = { selected = it }, onBack = {})
            }
        }
        composeRule.onNodeWithText("Medium").performClick()
        assertEquals(Difficulty.MEDIUM, selected)
    }

    @Test
    fun `Hard button calls onDifficultySelected with HARD`() {
        var selected: Difficulty? = null
        composeRule.setContent {
            ThemeMMD {
                DifficultyScreen(onDifficultySelected = { selected = it }, onBack = {})
            }
        }
        composeRule.onNodeWithText("Hard").performClick()
        assertEquals(Difficulty.HARD, selected)
    }

    @Test
    fun `Back button calls onBack callback`() {
        var callCount = 0
        composeRule.setContent {
            ThemeMMD {
                DifficultyScreen(onDifficultySelected = {}, onBack = { callCount++ })
            }
        }
        composeRule.onNodeWithText("Back").performClick()
        assertEquals(1, callCount)
    }
}
