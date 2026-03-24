package com.mudita.sudoku.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.ButtonMMD
import com.mudita.mmd.TextMMD

/**
 * Horizontal row of 10 buttons: digits 1–9 plus an Erase (×) button.
 *
 * All buttons use ButtonMMD + TextMMD for E-ink compatibility (UI-01).
 * All buttons are ≥56dp height for adequate touch targets on the Kompakt display (UI-03).
 * No ripple, no animation (UI-02).
 */
@Composable
fun NumberPad(
    onDigitClick: (Int) -> Unit,
    onErase: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (digit in 1..9) {
            ButtonMMD(
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 56.dp),
                onClick = { onDigitClick(digit) }
            ) {
                TextMMD(text = digit.toString())
            }
        }
        // Erase button — × symbol; placed at end of row (D-06)
        ButtonMMD(
            modifier = Modifier
                .weight(1f)
                .sizeIn(minHeight = 56.dp),
            onClick = onErase
        ) {
            TextMMD(text = "\u00D7") // × (multiplication sign)
        }
    }
}
