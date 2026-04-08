package com.interstitial.sudoku.ui.newpuzzle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NewPuzzleScreen(
    isGenerating: Boolean,
    onDifficultySelected: (Difficulty) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBarMMD(
            title = { TextMMD("New puzzle", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                TextMMD(
                    text = "\u2190",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(16.dp)
                )
            }
        )

        if (isGenerating) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                TextMMD(
                    text = "Preparing puzzle\u2026",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            DifficultyRow(
                title = "Easy",
                subtitle = "More givens, shorter sessions",
                onClick = { onDifficultySelected(Difficulty.EASY) }
            )
            HorizontalDividerMMD()
            DifficultyRow(
                title = "Medium",
                subtitle = "Balanced deduction",
                onClick = { onDifficultySelected(Difficulty.MEDIUM) }
            )
            HorizontalDividerMMD()
            DifficultyRow(
                title = "Hard",
                subtitle = "Fewer givens, deeper focus",
                onClick = { onDifficultySelected(Difficulty.HARD) }
            )
            HorizontalDividerMMD()
        }
    }
}

@Composable
private fun DifficultyRow(
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
        TextMMD(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextMMD(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
