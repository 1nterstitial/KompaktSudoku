package com.interstitial.sudoku.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    difficulty: Difficulty,
    elapsedMs: Long,
    hintsUsed: Int,
    isPersonalBest: Boolean,
    onNewPuzzle: () -> Unit,
    onBackToMenu: () -> Unit,
    onViewRecords: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBarMMD(
            title = { TextMMD("Puzzle complete", fontWeight = FontWeight.Bold) }
        )

        StatRow("Difficulty", difficulty.name.lowercase().replaceFirstChar { it.uppercase() })
        HorizontalDividerMMD()
        StatRow("Time", formatTime(elapsedMs))
        HorizontalDividerMMD()
        StatRow("Hints used", hintsUsed.toString())
        HorizontalDividerMMD()

        if (isPersonalBest) {
            TextMMD(
                text = "New personal record",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
            HorizontalDividerMMD(thickness = 2.dp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        ButtonMMD(
            onClick = onNewPuzzle,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp)
        ) { TextMMD("New puzzle") }

        Spacer(modifier = Modifier.height(8.dp))

        ButtonMMD(
            onClick = onBackToMenu,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp)
        ) { TextMMD("Back to menu") }

        Spacer(modifier = Modifier.height(8.dp))

        ButtonMMD(
            onClick = onViewRecords,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp)
        ) { TextMMD("View records") }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextMMD(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        TextMMD(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
