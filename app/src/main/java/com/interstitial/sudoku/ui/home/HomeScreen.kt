package com.interstitial.sudoku.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun HomeScreen(
    hasSavedGame: Boolean,
    savedGameDifficulty: String,
    savedGameCellsLeft: Int,
    onContinue: () -> Unit,
    onNewPuzzle: () -> Unit,
    onRecords: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(top = 32.dp)) {
        TextMMD(
            text = "Sudoku",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
        HorizontalDividerMMD(thickness = 2.dp)

        if (hasSavedGame) {
            MenuRow(
                title = "Continue puzzle",
                subtitle = "$savedGameDifficulty \u00B7 $savedGameCellsLeft cells left",
                onClick = onContinue
            )
            HorizontalDividerMMD()
        }

        MenuRow(
            title = "New puzzle",
            subtitle = "Choose easy, medium, or hard",
            onClick = onNewPuzzle
        )
        HorizontalDividerMMD()

        MenuRow(
            title = "Records",
            subtitle = "Completed puzzles and best times",
            onClick = onRecords
        )
        HorizontalDividerMMD()
    }
}

@Composable
private fun MenuRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        TextMMD(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        TextMMD(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
