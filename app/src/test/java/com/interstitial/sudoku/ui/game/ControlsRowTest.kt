package com.interstitial.sudoku.ui.game

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.mudita.mmd.ThemeMMD
import com.interstitial.sudoku.game.model.InputMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric Compose UI tests for [ControlsRow].
 *
 * Verifies:
 * - Mode toggle buttons only call onToggleMode when switching (not when already active)
 * - Undo button always dispatches onUndo
 * - All three interactive elements meet the 56dp minimum touch target height (UI-03)
 *
 * ThemeMMD here resolves to the test stub in com.mudita.mmd (test source set),
 * which delegates to MaterialTheme — no MMD AAR required at runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ControlsRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ------------------------------------------------------------------ mode toggle: FILL active

    @Test
    fun `in FILL mode tapping Fill does NOT call onToggleMode`() {
        var toggleCount = 0
        composeRule.setContent {
            ThemeMMD {
                ControlsRow(
                    inputMode = InputMode.FILL,
                    onToggleMode = { toggleCount++ },
                    onUndo = {},
                    onHint = {},
                    canRequestHint = true
                )
            }
        }
        composeRule.onNodeWithText("Fill").performClick()
        assertEquals("Tapping already-active Fill mode should not toggle", 0, toggleCount)
    }

    @Test
    fun `in FILL mode tapping Pencil calls onToggleMode once`() {
        var toggleCount = 0
        composeRule.setContent {
            ThemeMMD {
                ControlsRow(
                    inputMode = InputMode.FILL,
                    onToggleMode = { toggleCount++ },
                    onUndo = {},
                    onHint = {},
                    canRequestHint = true
                )
            }
        }
        composeRule.onNodeWithText("Pencil").performClick()
        assertEquals("Tapping inactive Pencil mode should toggle once", 1, toggleCount)
    }

    // ------------------------------------------------------------------ mode toggle: PENCIL active

    @Test
    fun `in PENCIL mode tapping Pencil does NOT call onToggleMode`() {
        var toggleCount = 0
        composeRule.setContent {
            ThemeMMD {
                ControlsRow(
                    inputMode = InputMode.PENCIL,
                    onToggleMode = { toggleCount++ },
                    onUndo = {},
                    onHint = {},
                    canRequestHint = true
                )
            }
        }
        composeRule.onNodeWithText("Pencil").performClick()
        assertEquals("Tapping already-active Pencil mode should not toggle", 0, toggleCount)
    }

    @Test
    fun `in PENCIL mode tapping Fill calls onToggleMode once`() {
        var toggleCount = 0
        composeRule.setContent {
            ThemeMMD {
                ControlsRow(
                    inputMode = InputMode.PENCIL,
                    onToggleMode = { toggleCount++ },
                    onUndo = {},
                    onHint = {},
                    canRequestHint = true
                )
            }
        }
        composeRule.onNodeWithText("Fill").performClick()
        assertEquals("Tapping inactive Fill mode should toggle once", 1, toggleCount)
    }

    // ------------------------------------------------------------------ undo

    @Test
    fun `tapping Undo invokes onUndo exactly once`() {
        var undoCount = 0
        composeRule.setContent {
            ThemeMMD {
                ControlsRow(
                    inputMode = InputMode.FILL,
                    onToggleMode = {},
                    onUndo = { undoCount++ },
                    onHint = {},
                    canRequestHint = true
                )
            }
        }
        composeRule.onNodeWithText("Undo").performClick()
        assertEquals(1, undoCount)
    }

    // ------------------------------------------------------------------ touch targets (UI-03)

    @Test
    fun `Fill button has height at least 56dp`() {
        composeRule.setContent {
            ThemeMMD {
                ControlsRow(
                    inputMode = InputMode.FILL,
                    onToggleMode = {},
                    onUndo = {},
                    onHint = {},
                    canRequestHint = true
                )
            }
        }
        composeRule.onNodeWithText("Fill").assertHeightIsAtLeast(56.dp)
    }

    @Test
    fun `Pencil button has height at least 56dp`() {
        composeRule.setContent {
            ThemeMMD {
                ControlsRow(
                    inputMode = InputMode.FILL,
                    onToggleMode = {},
                    onUndo = {},
                    onHint = {},
                    canRequestHint = true
                )
            }
        }
        composeRule.onNodeWithText("Pencil").assertHeightIsAtLeast(56.dp)
    }

    @Test
    fun `Undo button has height at least 56dp`() {
        composeRule.setContent {
            ThemeMMD {
                ControlsRow(
                    inputMode = InputMode.FILL,
                    onToggleMode = {},
                    onUndo = {},
                    onHint = {},
                    canRequestHint = true
                )
            }
        }
        composeRule.onNodeWithText("Undo").assertHeightIsAtLeast(56.dp)
    }
}
