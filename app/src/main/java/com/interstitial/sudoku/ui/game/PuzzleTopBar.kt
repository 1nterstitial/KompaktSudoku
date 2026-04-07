package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.mudita.mmd.components.text.TextMMD

@Composable
fun PuzzleTopBar(
    difficulty: Difficulty,
    cellsRemaining: Int,
    onBack: () -> Unit
) {
    val title = when (difficulty) {
        Difficulty.EASY -> "Easy"
        Difficulty.MEDIUM -> "Medium"
        Difficulty.HARD -> "Hard"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextMMD(
            text = "\u2190",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(8.dp)
        )
        TextMMD(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        TextMMD(
            text = "$cellsRemaining left",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}
