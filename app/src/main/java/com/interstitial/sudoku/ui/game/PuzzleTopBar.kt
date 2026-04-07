package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PuzzleTopBar(
    difficulty: Difficulty,
    onBack: () -> Unit
) {
    val title = when (difficulty) {
        Difficulty.EASY -> "Easy puzzle"
        Difficulty.MEDIUM -> "Medium puzzle"
        Difficulty.HARD -> "Hard puzzle"
    }
    TopAppBarMMD(
        title = { TextMMD(title) },
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
}
