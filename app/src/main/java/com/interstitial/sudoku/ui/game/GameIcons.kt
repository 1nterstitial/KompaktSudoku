package com.interstitial.sudoku.ui.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Undo: counter-clockwise arrow
val UndoIcon: ImageVector by lazy {
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.5f,
            fill = null
        ) {
            // Arrow arc (counter-clockwise from right)
            moveTo(7f, 11f)
            lineTo(3f, 7f)
            lineTo(7f, 3f)
            moveTo(3f, 7f)
            horizontalLineTo(15f)
            arcTo(7f, 7f, 0f, true, true, 15f, 21f)
            horizontalLineTo(11f)
        }
    }.build()
}

// Erase: backspace/delete shape
val EraseIcon: ImageVector by lazy {
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.5f,
            fill = null
        ) {
            // Backspace outline
            moveTo(9f, 4f)
            horizontalLineTo(20f)
            arcTo(1f, 1f, 0f, false, true, 21f, 5f)
            verticalLineTo(19f)
            arcTo(1f, 1f, 0f, false, true, 20f, 20f)
            horizontalLineTo(9f)
            lineTo(3f, 12f)
            close()
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.5f,
            fill = null
        ) {
            // X inside
            moveTo(10f, 9f)
            lineTo(16f, 15f)
            moveTo(16f, 9f)
            lineTo(10f, 15f)
        }
    }.build()
}

// Hint: lightbulb
val HintIcon: ImageVector by lazy {
    ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.5f,
            fill = null
        ) {
            // Bulb
            moveTo(9f, 18f)
            verticalLineTo(16.5f)
            arcTo(6f, 6f, 0f, true, true, 15f, 16.5f)
            verticalLineTo(18f)
            // Base lines
            moveTo(9f, 18f)
            horizontalLineTo(15f)
            moveTo(10f, 21f)
            horizontalLineTo(14f)
        }
    }.build()
}
