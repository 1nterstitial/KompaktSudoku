package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD

/**
 * Main menu screen — entry point of the app.
 *
 * Always shows "New Game" and "Best Scores" buttons. The "Resume" button is shown conditionally
 * when [hasSavedGame] is true; it is fully absent (not greyed out) when false (D-02, D-03).
 *
 * No BackHandler — default ComponentActivity back exits the app (D-11).
 * No AnimatedVisibility — E-ink display prohibits animations (UI-02).
 * All text uses TextMMD, all buttons use ButtonMMD (MMD compliance).
 * Layout: full-screen Column with system bar insets and 16dp horizontal padding.
 *
 * @param hasSavedGame  True when a saved in-progress game is available; shows Resume button.
 * @param onNewGame     Called when the player taps "New Game" — navigates to difficulty selection.
 * @param onResume      Called when the player taps "Resume" — restores saved game and goes to game.
 * @param onBestScores  Called when the player taps "Best Scores" — navigates to leaderboard.
 */
@Composable
fun MenuScreen(
    hasSavedGame: Boolean,
    onNewGame: () -> Unit,
    onResume: () -> Unit,
    onBestScores: () -> Unit
) {
    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp)
    ) {
        // 1. App title heading
        TextMMD(
            text = "Sudoku",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 24.dp)
        )

        // 2. New Game button — always visible
        ButtonMMD(
            onClick = onNewGame,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            TextMMD("New Game")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Resume button — only shown when a saved game exists (D-02)
        //    Absent entirely when false — not greyed out or disabled
        if (hasSavedGame) {
            ButtonMMD(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                TextMMD("Resume")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // 4. Best Scores button — always visible
        ButtonMMD(
            onClick = onBestScores,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            TextMMD("Best Scores")
        }

        // 5. Spacer — pushes button group to the top of the screen
        Spacer(modifier = Modifier.weight(1f))
    }
}
