package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun PuzzleActionRow(
    hasUndo: Boolean,
    canErase: Boolean,
    canHint: Boolean,
    onUndo: () -> Unit,
    onErase: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ButtonMMD(
            onClick = onUndo,
            enabled = hasUndo,
            modifier = Modifier.weight(1f).height(36.dp)
        ) { TextMMD("Undo") }

        ButtonMMD(
            onClick = onErase,
            enabled = canErase,
            modifier = Modifier.weight(1f).height(36.dp)
        ) { TextMMD("Erase") }

        ButtonMMD(
            onClick = onHint,
            enabled = canHint,
            modifier = Modifier.weight(1f).height(36.dp)
        ) { TextMMD("Hint") }
    }
}
