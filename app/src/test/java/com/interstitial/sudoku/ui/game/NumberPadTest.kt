package com.interstitial.sudoku.ui.game

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.mudita.mmd.ThemeMMD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI tests for [NumberPad].
 *
 * Verifies:
 * - Digit button taps dispatch the correct digit to onDigitClick
 * - Erase (×) button tap dispatches onErase
 * - All 10 buttons are present (1–9 + ×)
 *
 * ThemeMMD here resolves to the test stub in com.mudita.mmd (test source set),
 * which delegates to MaterialTheme — no MMD AAR required at runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class NumberPadTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ------------------------------------------------------------------ digit dispatch

    @Test
    fun `tapping digit 1 invokes onDigitClick with 1`() {
        var received: Int? = null
        composeRule.setContent {
            ThemeMMD {
                NumberPad(
                    onDigitClick = { received = it },
                    onErase = {}
                )
            }
        }
        composeRule.onNodeWithText("1").performClick()
        assertEquals(1, received)
    }

    @Test
    fun `tapping digit 5 invokes onDigitClick with 5`() {
        var received: Int? = null
        composeRule.setContent {
            ThemeMMD {
                NumberPad(
                    onDigitClick = { received = it },
                    onErase = {}
                )
            }
        }
        composeRule.onNodeWithText("5").performClick()
        assertEquals(5, received)
    }

    @Test
    fun `tapping digit 9 invokes onDigitClick with 9`() {
        var received: Int? = null
        composeRule.setContent {
            ThemeMMD {
                NumberPad(
                    onDigitClick = { received = it },
                    onErase = {}
                )
            }
        }
        composeRule.onNodeWithText("9").performClick()
        assertEquals(9, received)
    }

    @Test
    fun `tapping erase button invokes onErase exactly once`() {
        var eraseCount = 0
        composeRule.setContent {
            ThemeMMD {
                NumberPad(
                    onDigitClick = {},
                    onErase = { eraseCount++ }
                )
            }
        }
        composeRule.onNodeWithText("\u00D7").performClick()
        assertEquals(1, eraseCount)
    }

    // ------------------------------------------------------------------ button presence

    @Test
    fun `all digit buttons 1 through 9 are present`() {
        composeRule.setContent {
            ThemeMMD {
                NumberPad(onDigitClick = {}, onErase = {})
            }
        }
        for (digit in 1..9) {
            composeRule.onNodeWithText(digit.toString()).assertExists()
        }
    }

    @Test
    fun `erase button is present`() {
        composeRule.setContent {
            ThemeMMD {
                NumberPad(onDigitClick = {}, onErase = {})
            }
        }
        composeRule.onNodeWithText("\u00D7").assertExists()
    }

    // ------------------------------------------------------------------ touch targets

    @Test
    fun `digit 1 button has height at least 56dp`() {
        composeRule.setContent {
            ThemeMMD {
                NumberPad(onDigitClick = {}, onErase = {})
            }
        }
        composeRule.onNodeWithText("1").assertHeightIsAtLeast(56.dp)
    }

    @Test
    fun `erase button has height at least 56dp`() {
        composeRule.setContent {
            ThemeMMD {
                NumberPad(onDigitClick = {}, onErase = {})
            }
        }
        composeRule.onNodeWithText("\u00D7").assertHeightIsAtLeast(56.dp)
    }
}
