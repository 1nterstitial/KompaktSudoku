package com.mudita.sudoku.ui.game

import androidx.activity.compose.BackHandler
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
import com.mudita.sudoku.puzzle.model.Difficulty

/**
 * Difficulty selection screen — shown after player taps "New Game" from the main menu.
 *
 * Offers Easy, Medium, and Hard difficulty options. A "Back" button (and system back press)
 * both return to the menu without starting a game (D-12).
 *
 * No AnimatedVisibility — E-ink display prohibits animations (UI-02).
 * All text uses TextMMD, all buttons use ButtonMMD (MMD compliance).
 * Layout: full-screen Column with system bar insets and 16dp horizontal padding.
 * Difficulty buttons are grouped at the top; "Back" button is anchored to the bottom.
 *
 * @param onDifficultySelected  Called with the chosen [Difficulty] when player taps Easy/Medium/Hard.
 * @param onBack                Called when player taps "Back" or presses the system back button.
 */
@Composable
fun DifficultyScreen(
    onDifficultySelected: (Difficulty) -> Unit,
    onBack: () -> Unit
) {
    // System back press returns to MENU (D-12)
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp)
    ) {
        // 1. Screen heading
        TextMMD(
            text = "Select Difficulty",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 24.dp)
        )

        // 2. Easy button
        ButtonMMD(
            onClick = { onDifficultySelected(Difficulty.EASY) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            TextMMD("Easy")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Medium button
        ButtonMMD(
            onClick = { onDifficultySelected(Difficulty.MEDIUM) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            TextMMD("Medium")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Hard button
        ButtonMMD(
            onClick = { onDifficultySelected(Difficulty.HARD) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            TextMMD("Hard")
        }

        // 5. Spacer — pushes "Back" button to the bottom of the screen
        Spacer(modifier = Modifier.weight(1f))

        // 6. Back button — anchored at the bottom (mirrors SummaryScreen / LeaderboardScreen pattern)
        ButtonMMD(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            TextMMD("Back")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
