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
import com.mudita.sudoku.game.model.CompletionResult

/**
 * Completion summary screen shown after a puzzle is solved.
 *
 * Displays difficulty, error count, hints used, and final score.
 * Conditionally shows a "New personal best!" notice when [result.isPersonalBest] is true.
 * Provides navigation to the leaderboard or a fresh game.
 *
 * No animations (UI-02). All text uses TextMMD, all buttons use ButtonMMD (MMD compliance).
 * Layout: full-screen Column with system bar insets and 16dp horizontal padding.
 *
 * @param result            Full completion data from [GameEvent.Completed] — never null when shown.
 * @param onViewLeaderboard Called when the player taps "View Leaderboard".
 * @param onBackToMenu      Called when the player taps "Back to Menu" or presses the system back button.
 */
@Composable
fun SummaryScreen(
    result: CompletionResult,
    onViewLeaderboard: () -> Unit,
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
            text = "Puzzle Complete",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 24.dp)
        )

        // 2. Difficulty label
        TextMMD(
            text = result.difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Stats panel
        Surface(
            color = Color.White,
            border = BorderStroke(1.dp, Color.Black)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ScoreRow(label = "Errors", value = result.errorCount.toString())
                ScoreRow(label = "Hints", value = result.hintCount.toString())
                HorizontalDivider(color = Color.Black, thickness = 1.dp)
                ScoreRow(
                    label = "Score",
                    value = result.finalScore.toString(),
                    valueFontWeight = FontWeight.Bold
                )
            }
        }

        // 4. Personal best notice — conditional, no AnimatedVisibility (UI-02)
        if (result.isPersonalBest) {
            TextMMD(
                text = "New personal best!",
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }

        // 5. Spacer — pushes action buttons to the bottom of the screen
        Spacer(modifier = Modifier.weight(1f))

        // 6. View Leaderboard button
        ButtonMMD(
            onClick = onViewLeaderboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            TextMMD(text = "View Leaderboard")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 7. Back to Menu button
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
 * A single stat row with label on the left and value on the right.
 *
 * Used for Errors, Hints, and Score rows in the stats panel.
 * The Score row passes [FontWeight.Bold] for its value to distinguish the final score.
 */
@Composable
private fun ScoreRow(
    label: String,
    value: String,
    valueFontWeight: FontWeight = FontWeight.Normal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextMMD(text = label)
        TextMMD(text = value, fontWeight = valueFontWeight)
    }
}
