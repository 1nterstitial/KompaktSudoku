package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.game.model.InputMode
import com.mudita.mmd.components.text.TextMMD

@Composable
fun InputModeToggle(
    currentMode: InputMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(48.dp)
            .border(2.dp, onSurface)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (currentMode == InputMode.FILL) onSurface else surface)
                .clickable(onClick = { if (currentMode != InputMode.FILL) onToggle() }),
            contentAlignment = Alignment.Center
        ) {
            TextMMD(
                text = "Fill",
                style = MaterialTheme.typography.labelLarge,
                color = if (currentMode == InputMode.FILL) surface else onSurface
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (currentMode == InputMode.NOTES) onSurface else surface)
                .clickable(onClick = { if (currentMode != InputMode.NOTES) onToggle() }),
            contentAlignment = Alignment.Center
        ) {
            TextMMD(
                text = "Notes",
                style = MaterialTheme.typography.labelLarge,
                color = if (currentMode == InputMode.NOTES) surface else onSurface
            )
        }
    }
}
