package com.mudita.sudoku.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Stable per-cell data wrapper. Stability annotation tells Compose that recomposition
 * only occurs when a cell's own data changes — prevents whole-grid redraws on single-cell updates.
 */
@Stable
data class CellData(
    val index: Int,
    val digit: Int,          // 0 = empty
    val isGiven: Boolean,
    val isSelected: Boolean,
    val isError: Boolean,    // non-given digit present that != solution
    val pencilMarks: Set<Int>
)

/**
 * Canvas-based 9×9 Sudoku grid.
 *
 * Draws all 81 cells in a single Canvas composable — no Box per cell. Grid lines drawn last
 * so thick box borders are not partially overwritten by adjacent cell fills.
 *
 * No ripple, no animation, no Animated* composable (UI-02).
 */
@Composable
fun GameGrid(
    modifier: Modifier = Modifier,
    board: IntArray,
    solution: IntArray,
    givenMask: BooleanArray,
    selectedCellIndex: Int?,
    pencilMarks: Array<Set<Int>>,
    onCellClick: (Int) -> Unit
) {
    // Build cell list; recompute only when meaningful state changes
    val cells = remember(board, givenMask, selectedCellIndex, pencilMarks) {
        List(81) { i ->
            CellData(
                index = i,
                digit = board[i],
                isGiven = givenMask[i],
                isSelected = i == selectedCellIndex,
                isError = board[i] != 0 && !givenMask[i] && board[i] != solution[i],
                pencilMarks = pencilMarks[i]
            )
        }
    }

    // TextMeasurer must be hoisted at composable level — cannot be called inside DrawScope
    val textMeasurer = rememberTextMeasurer()

    // Text styles defined at composable level so Canvas lambda can capture them
    val digitStyleGiven = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )
    val digitStylePlayer = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Black
    )
    val digitStyleSelectedGiven = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    val digitStyleSelectedPlayer = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        color = Color.White
    )
    BoxWithConstraints(modifier = modifier) {
        val gridSizeDp = minOf(maxWidth, maxHeight)
        val gridSizePx = with(LocalDensity.current) { gridSizeDp.toPx() }
        val cellSizePx = gridSizePx / 9f
        val density = LocalDensity.current.density

        Canvas(
            modifier = Modifier
                .fillMaxSize()  // gives the Canvas a layout size for pointer-input hit testing
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val col = (offset.x / cellSizePx).toInt().coerceIn(0, 8)
                        val row = (offset.y / cellSizePx).toInt().coerceIn(0, 8)
                        onCellClick(row * 9 + col)
                    }
                }
        ) {
            // Drawing order:
            // a) Selected cell background (solid black rect)
            // b) Error cell indicator (1dp inset border)
            // c) Digit text
            // d) Pencil mark mini-grid
            // e) Grid lines (drawn last — on top of all cell content)

            // a) Selected cell background
            cells.filter { it.isSelected }.forEach { cell ->
                drawSelectedCell(cell, cellSizePx)
            }

            // b) Error indicators — skip selected cell (solid black fill already there)
            cells.filter { it.isError && !it.isSelected }.forEach { cell ->
                drawErrorIndicator(cell, cellSizePx)
            }

            // c) Digit text
            cells.filter { it.digit != 0 }.forEach { cell ->
                val row = cell.index / 9
                val col = cell.index % 9
                val style = when {
                    cell.isSelected && cell.isGiven -> digitStyleSelectedGiven
                    cell.isSelected -> digitStyleSelectedPlayer
                    cell.isGiven -> digitStyleGiven
                    else -> digitStylePlayer
                }
                val measured = textMeasurer.measure(cell.digit.toString(), style)
                val x = col * cellSizePx + (cellSizePx - measured.size.width) / 2f
                val y = row * cellSizePx + (cellSizePx - measured.size.height) / 2f
                drawText(textLayoutResult = measured, topLeft = Offset(x, y))
            }

            // d) Pencil marks (only for empty cells)
            cells.filter { it.digit == 0 && it.pencilMarks.isNotEmpty() }.forEach { cell ->
                val row = cell.index / 9
                val col = cell.index % 9
                drawPencilMarks(
                    marks = cell.pencilMarks,
                    cellLeft = col * cellSizePx,
                    cellTop = row * cellSizePx,
                    cellSize = cellSizePx,
                    textMeasurer = textMeasurer,
                    isSelected = cell.isSelected,
                    density = density
                )
            }

            // e) Grid lines — drawn last so thick box borders appear on top of cell fills
            drawGridLines(cellSizePx)
        }
    }
}

/**
 * Draws a solid black fill for the selected cell.
 */
private fun DrawScope.drawSelectedCell(cell: CellData, cellSize: Float) {
    val row = cell.index / 9
    val col = cell.index % 9
    drawRect(
        color = Color.Black,
        topLeft = Offset(col * cellSize, row * cellSize),
        size = Size(cellSize, cellSize)
    )
}

/**
 * Draws a 1dp inset border indicator for error cells (wrong player digit).
 * D-04: Error feedback via subtle inset border, not color (E-ink is monochromatic).
 */
private fun DrawScope.drawErrorIndicator(cell: CellData, cellSize: Float) {
    val row = cell.index / 9
    val col = cell.index % 9
    val inset = 2.dp.toPx()
    drawRect(
        color = Color.Black,
        topLeft = Offset(col * cellSize + inset, row * cellSize + inset),
        size = Size(cellSize - inset * 2, cellSize - inset * 2),
        style = Stroke(width = 1.dp.toPx())
    )
}

/**
 * Draws pencil mark candidates in a 2x2 grid inside the cell (max 4 marks).
 * Marks are sorted ascending: sorted[0]=top-left, sorted[1]=top-right,
 * sorted[2]=bottom-left, sorted[3]=bottom-right.
 *
 * Per D-01/D-02: uses Color.White when isSelected (black background), Color.Black otherwise.
 * Per D-07: font size computed dynamically from cellSize — (cellSize/2 * 0.60) px converted to sp.
 * Per D-08: builds TextStyle internally from cellSize and isSelected (no external style param).
 */
private fun DrawScope.drawPencilMarks(
    marks: Set<Int>,
    cellLeft: Float,
    cellTop: Float,
    cellSize: Float,
    textMeasurer: TextMeasurer,
    isSelected: Boolean,
    density: Float
) {
    val dynamicPencilFontSp = (cellSize / 2f * 0.60f) / density
    val color = if (isSelected) Color.White else Color.Black
    val style = TextStyle(
        fontSize = dynamicPencilFontSp.sp,
        fontWeight = FontWeight.Normal,
        color = color
    )
    val sorted = marks.sorted()
    val slotSize = cellSize / 2f
    sorted.forEachIndexed { i, digit ->
        val slotRow = i / 2   // 0,1 -> row 0; 2,3 -> row 1
        val slotCol = i % 2   // 0,2 -> col 0; 1,3 -> col 1
        val measured = textMeasurer.measure(digit.toString(), style)
        val x = cellLeft + slotCol * slotSize + (slotSize - measured.size.width) / 2f
        val y = cellTop + slotRow * slotSize + (slotSize - measured.size.height) / 2f
        drawText(textLayoutResult = measured, topLeft = Offset(x, y))
    }
}

/**
 * Draws the full 9×9 grid lines.
 * Lines at i % 3 == 0 are thick (2.5dp box borders); others are thin (1dp cell borders).
 * D-01: thick box borders separate 3×3 boxes; thin lines divide individual cells.
 */
private fun DrawScope.drawGridLines(cellSize: Float) {
    val thinStroke = 1.dp.toPx()
    val thickStroke = 2.5.dp.toPx()
    for (i in 0..9) {
        val pos = i * cellSize
        val stroke = if (i % 3 == 0) thickStroke else thinStroke
        drawLine(Color.Black, Offset(pos, 0f), Offset(pos, 9 * cellSize), stroke)
        drawLine(Color.Black, Offset(0f, pos), Offset(9 * cellSize, pos), stroke)
    }
}
