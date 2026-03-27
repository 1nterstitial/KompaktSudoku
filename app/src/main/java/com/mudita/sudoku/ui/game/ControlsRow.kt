package com.mudita.sudoku.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.sudoku.game.model.InputMode

/**
 * Controls row: Fill / Pencil mode toggles + Undo button + Get Hint button.
 *
 * Active mode button: solid black fill with white text.
 * Inactive mode button: mid-gray (#E0E0E0) fill with black text (CTRL-03).
 * Fill and Pencil buttons are enclosed in a shared 1dp black border frame (CTRL-04).
 * "Get Hint" renders as two centered lines: "Get" / "Hint" (CTRL-02).
 *
 * Implemented using Box+clickable with indication=null (no ripple per UI-02) for the mode
 * toggles, since ButtonMMD's `colors` parameter availability is unconfirmed at compile time.
 * The Undo and Get Hint buttons use ButtonMMD directly (no color customization needed).
 *
 * No ripple, no animation (UI-02). All elements >= 56dp height (UI-03).
 *
 * @param onHint          Called when the player taps the Get Hint button.
 * @param canRequestHint  When false, the Get Hint button is disabled (no valid hint targets).
 */
@Composable
fun ControlsRow(
    inputMode: InputMode,
    onToggleMode: () -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    canRequestHint: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Fill+Pencil group — takes 2 equal shares of 4 total (CTRL-04)
        // sizeIn on the outer Row sets the minimum height; inner Boxes match via their own sizeIn.
        Row(
            modifier = Modifier
                .weight(2f)
                .border(width = 1.dp, color = Color.Black, shape = RectangleShape)
        ) {
            // Fill mode toggle
            Box(
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 56.dp)
                    .background(if (inputMode == InputMode.FILL) Color.Black else Color(0xFFE0E0E0))
                    .clickable(
                        indication = null, // no ripple — E-ink ghosting prevention (UI-02)
                        interactionSource = remember { MutableInteractionSource() }
                    ) { if (inputMode != InputMode.FILL) onToggleMode() },
                contentAlignment = Alignment.Center
            ) {
                TextMMD(
                    text = "Fill",
                    color = if (inputMode == InputMode.FILL) Color.White else Color.Black
                )
            }

            // Pencil mode toggle
            Box(
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 56.dp)
                    .background(if (inputMode == InputMode.PENCIL) Color.Black else Color(0xFFE0E0E0))
                    .clickable(
                        indication = null, // no ripple — E-ink ghosting prevention (UI-02)
                        interactionSource = remember { MutableInteractionSource() }
                    ) { if (inputMode != InputMode.PENCIL) onToggleMode() },
                contentAlignment = Alignment.Center
            ) {
                TextMMD(
                    text = "Pencil",
                    color = if (inputMode == InputMode.PENCIL) Color.White else Color.Black
                )
            }
        }

        // Undo — no active/inactive state, standard ButtonMMD
        ButtonMMD(
            modifier = Modifier
                .weight(1f)
                .sizeIn(minHeight = 56.dp),
            onClick = onUndo
        ) {
            TextMMD(text = "Undo")
        }

        // Hint — two-line centered label (CTRL-02), disabled when no valid target
        ButtonMMD(
            modifier = Modifier
                .weight(1f)
                .sizeIn(minHeight = 56.dp),
            onClick = onHint,
            enabled = canRequestHint
        ) {
            TextMMD(text = "Get\nHint", textAlign = TextAlign.Center)
        }
    }
}
