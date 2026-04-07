package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalTextApi::class)
@Composable
fun DigitPad(
    digitCounts: IntArray,
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val textMeasurer = rememberTextMeasurer()
    var pressedDigit by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .padding(horizontal = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cellW = size.width / 3f
                    val cellH = size.height / 3f
                    val col = (offset.x / cellW).toInt().coerceIn(0, 2)
                    val row = (offset.y / cellH).toInt().coerceIn(0, 2)
                    val digit = row * 3 + col + 1
                    if (digitCounts[digit - 1] < 9) {
                        pressedDigit = digit
                        onDigit(digit)
                        scope.launch {
                            delay(150)
                            pressedDigit = 0
                        }
                    }
                }
            }
    ) {
        val cellW = size.width / 3f
        val cellH = size.height / 3f
        val lineWidth = 3.dp.toPx()
        val digitStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = onSurface)
        val pressedStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = surface)
        val disabledStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = outlineVariant)

        // Draw pressed cell background
        if (pressedDigit in 1..9) {
            val pr = (pressedDigit - 1) / 3
            val pc = (pressedDigit - 1) % 3
            drawRect(onSurface, Offset(pc * cellW, pr * cellH), Size(cellW, cellH))
        }

        // Draw # lines — 2 vertical, 2 horizontal
        for (i in 1..2) {
            val x = i * cellW
            drawLine(onSurface, Offset(x, 0f), Offset(x, size.height), strokeWidth = lineWidth)
        }
        for (i in 1..2) {
            val y = i * cellH
            drawLine(onSurface, Offset(0f, y), Offset(size.width, y), strokeWidth = lineWidth)
        }

        // Draw digits centered in each cell
        for (d in 1..9) {
            val row = (d - 1) / 3
            val col = (d - 1) % 3
            val isDisabled = digitCounts[d - 1] >= 9
            val isPressed = d == pressedDigit
            val style = when {
                isPressed -> pressedStyle
                isDisabled -> disabledStyle
                else -> digitStyle
            }
            val measured = textMeasurer.measure(d.toString(), style)
            drawText(
                measured,
                topLeft = Offset(
                    col * cellW + (cellW - measured.size.width) / 2f,
                    row * cellH + (cellH - measured.size.height) / 2f
                )
            )
        }
    }
}
