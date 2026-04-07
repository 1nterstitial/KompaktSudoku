package com.interstitial.sudoku.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.game.model.DifficultyRecord
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    records: Map<Difficulty, DifficultyRecord>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBarMMD(
            title = { TextMMD("Records") },
            navigationIcon = {
                TextMMD(
                    text = "\u2190",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable(onClick = onBack).padding(16.dp)
                )
            }
        )

        LazyColumnMMD {
            for (difficulty in Difficulty.entries) {
                val record = records[difficulty] ?: DifficultyRecord()
                item {
                    TextMMD(
                        text = difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDividerMMD()
                    RecordRow("Completed", if (record.completedCount > 0) record.completedCount.toString() else "\u2014")
                    HorizontalDividerMMD()
                    RecordRow("Best time", record.bestTimeMs?.let { formatTime(it) } ?: "\u2014")
                    HorizontalDividerMMD()
                    RecordRow("Best no-hint", record.bestNoHintTimeMs?.let { formatTime(it) } ?: "\u2014")
                    HorizontalDividerMMD(thickness = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun RecordRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextMMD(text = label, style = MaterialTheme.typography.bodyMedium)
        TextMMD(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
