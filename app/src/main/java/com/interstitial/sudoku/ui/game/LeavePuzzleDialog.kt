package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.text.TextMMD

@Composable
fun LeavePuzzleDialog(
    onKeepForLater: () -> Unit,
    onDiscard: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            TextMMD(
                text = "Leave puzzle?",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            ButtonMMD(
                onClick = onKeepForLater,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { TextMMD("Keep for later") }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButtonMMD(
                onClick = onDiscard,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { TextMMD("Discard puzzle") }
        }
    }
}
