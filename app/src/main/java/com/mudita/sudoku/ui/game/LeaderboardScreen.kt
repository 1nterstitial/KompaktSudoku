package com.mudita.sudoku.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.sudoku.puzzle.model.Difficulty

/**
 * Best scores screen — shows one stored score per difficulty (Easy / Medium / Hard).
 *
 * Heading is "Best Scores" (not "Leaderboard") because only one score per difficulty is stored
 * (D-08). The word "leaderboard" appears only in route/code identifiers, not in user-facing copy.
 *
 * No animations (UI-02). All text uses TextMMD, all buttons use ButtonMMD (MMD compliance).
 * Layout: full-screen Column with system bar insets and 16dp horizontal padding.
 *
 * @param scores        Map from [Difficulty] to the stored best score (null if no score recorded).
 * @param onBackToMenu  Called when the player taps "Back to Menu" or presses the system back button.
 */
@Composable
fun LeaderboardScreen(
    scores: Map<Difficulty, Int?>,
    onBackToMenu: () -> Unit
) {
    // System back press returns to MENU (D-12)
    BackHandler { onBackToMenu() }

    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp)
    ) {
        // 1. Screen heading
        TextMMD(
            text = "Best Scores",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 24.dp)
        )

        // 2. Scores panel — one row per difficulty; em-dash when no score recorded
        Surface(
            color = Color.White,
            border = BorderStroke(1.dp, Color.Black)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ScoreRow(
                    label = "Easy",
                    value = scores[Difficulty.EASY]?.toString() ?: "\u2014"
                )
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
                ScoreRow(
                    label = "Medium",
                    value = scores[Difficulty.MEDIUM]?.toString() ?: "\u2014"
                )
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
                ScoreRow(
                    label = "Hard",
                    value = scores[Difficulty.HARD]?.toString() ?: "\u2014"
                )
            }
        }

        // 3. Spacer — pushes action button to the bottom of the screen
        Spacer(modifier = Modifier.weight(1f))

        // 4. Back to Menu button
        ButtonMMD(
            onClick = onBackToMenu,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            TextMMD(text = "Back to Menu")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/**
 * A single score row with difficulty label on the left and score value on the right.
 *
 * Reused for all three difficulty rows in the scores panel.
 * Both label and value use standard (non-bold) weight — no distinction needed here.
 */
@Composable
private fun ScoreRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextMMD(text = label)
        TextMMD(text = value)
    }
}
