package com.interstitial.sudoku.ui.records

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
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect


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

        for (difficulty in Difficulty.entries) {
            val record = records[difficulty] ?: DifficultyRecord()
            DifficultySection(difficulty, record)
        }
    }
}

@Composable
private fun DifficultySection(difficulty: Difficulty, record: DifficultyRecord) {
    TextMMD(
        text = difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
    )
    HorizontalDividerMMD(
        thickness = 2.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    RecordRow("Completed", if (record.completedCount > 0) record.completedCount.toString() else "\u2014")
    DottedDivider(modifier = Modifier.padding(start = 32.dp, end = 16.dp))
    RecordRow("Best time", record.bestTimeMs?.let { formatTime(it) } ?: "\u2014")
    DottedDivider(modifier = Modifier.padding(start = 32.dp, end = 16.dp))
    RecordRow("Best no-hint", record.bestNoHintTimeMs?.let { formatTime(it) } ?: "\u2014")
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun RecordRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextMMD(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        TextMMD(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DottedDivider(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
