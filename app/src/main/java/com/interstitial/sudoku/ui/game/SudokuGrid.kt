package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.interstitial.sudoku.game.model.GameUiState

@OptIn(ExperimentalTextApi::class)
@Composable
fun SudokuGrid(
    state: GameUiState,
    onCellTap: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val textMeasurer = rememberTextMeasurer()
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime > 200) {
                        lastTapTime = now
                        val gridSide = minOf(size.width, size.height).toFloat()
                        val offsetX = (size.width - gridSide) / 2f
                        val cellSize = gridSide / 9f
                        val col = ((offset.x - offsetX) / cellSize).toInt().coerceIn(0, 8)
                        val row = (offset.y / cellSize).toInt().coerceIn(0, 8)
                        onCellTap(row, col)
                    }
                }
            }
    ) {
        val gridSide = minOf(size.width, size.height)
        val offsetX = (size.width - gridSide) / 2f
        val cellSize = gridSide / 9f
        val givenStyle = TextStyle(fontSize = (cellSize * 0.55f).toSp(), fontWeight = FontWeight.Bold, color = onSurface)
        val playerStyle = TextStyle(fontSize = (cellSize * 0.55f).toSp(), color = onSurface)
        val selectedStyle = TextStyle(fontSize = (cellSize * 0.55f).toSp(), fontWeight = FontWeight.Bold, color = surface)
        val noteStyle = TextStyle(fontSize = (cellSize * 0.2f).toSp(), color = onSurfaceVariant)
        val selectedNoteStyle = TextStyle(fontSize = (cellSize * 0.2f).toSp(), color = surface)

        // 1. Draw cell backgrounds
        for (i in 0 until 81) {
            val r = i / 9
            val c = i % 9
            val x = offsetX + c * cellSize
            val y = r * cellSize

            if (state.selectedCell == i) {
                drawRect(onSurface, Offset(x, y), Size(cellSize, cellSize))
            }
        }

        // 2. Draw grid lines
        for (i in 0..9) {
            val pos = i * cellSize
            drawLine(outlineVariant, Offset(offsetX + pos, 0f), Offset(offsetX + pos, gridSide), strokeWidth = 1f)
            drawLine(outlineVariant, Offset(offsetX, pos), Offset(offsetX + gridSide, pos), strokeWidth = 1f)
        }
        for (i in 0..3) {
            val pos = i * 3 * cellSize
            drawLine(onSurface, Offset(offsetX + pos, 0f), Offset(offsetX + pos, gridSide), strokeWidth = 3f)
            drawLine(onSurface, Offset(offsetX, pos), Offset(offsetX + gridSide, pos), strokeWidth = 3f)
        }

        // 3. Draw digits and notes
        for (i in 0 until 81) {
            val r = i / 9
            val c = i % 9
            val x = offsetX + c * cellSize
            val y = r * cellSize
            val isSelected = state.selectedCell == i
            val value = state.board[i]

            if (value != 0) {
                val style = when {
                    isSelected -> selectedStyle
                    state.givens[i] || i in state.hintedCells -> givenStyle
                    else -> playerStyle
                }
                val measured = textMeasurer.measure(value.toString(), style)
                drawText(
                    measured,
                    topLeft = Offset(
                        x + (cellSize - measured.size.width) / 2f,
                        y + (cellSize - measured.size.height) / 2f
                    )
                )
            } else {
                val notes = state.notes[i]
                if (notes.isNotEmpty()) {
                    val noteSize = cellSize / 3f
                    for (d in notes) {
                        val nr = (d - 1) / 3
                        val nc = (d - 1) % 3
                        val style = if (isSelected) selectedNoteStyle else noteStyle
                        val measured = textMeasurer.measure(d.toString(), style)
                        drawText(
                            measured,
                            topLeft = Offset(
                                x + nc * noteSize + (noteSize - measured.size.width) / 2f,
                                y + nr * noteSize + (noteSize - measured.size.height) / 2f
                            )
                        )
                    }
                }
            }

            // 4. Conflict indicators (inset corner marks)
            if (state.conflictMask[i] && value != 0) {
                val inset = cellSize * 0.1f
                val markLen = cellSize * 0.2f
                val markColor = if (isSelected) surface else onSurface
                drawLine(markColor, Offset(x + inset, y + inset), Offset(x + inset + markLen, y + inset), strokeWidth = 2f)
                drawLine(markColor, Offset(x + inset, y + inset), Offset(x + inset, y + inset + markLen), strokeWidth = 2f)
                val bx = x + cellSize - inset
                val by = y + cellSize - inset
                drawLine(markColor, Offset(bx - markLen, by), Offset(bx, by), strokeWidth = 2f)
                drawLine(markColor, Offset(bx, by - markLen), Offset(bx, by), strokeWidth = 2f)
            }
        }
    }
}

private fun Float.toSp(): androidx.compose.ui.unit.TextUnit = (this / 3f).sp
