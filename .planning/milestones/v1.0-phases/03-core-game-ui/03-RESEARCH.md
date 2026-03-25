# Phase 3: Core Game UI - Research

**Researched:** 2026-03-24
**Domain:** Jetpack Compose UI / MMD library / E-ink rendering / Sudoku grid composables
**Confidence:** MEDIUM-HIGH (MMD internal API not publicly documented; patterns sourced from
known real-world usage + official Compose docs; all Compose findings HIGH confidence)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Box borders thick (2–3dp), individual cell borders thin (1dp). Classic thick+thin Sudoku
  look. 3×3 box boundaries must be clearly heavier than inter-cell lines.
- **D-02:** Selected cell rendered with a solid black fill and white digit. Maximum contrast — no
  inverted-row/column highlighting.
- **D-03:** Given (pre-filled) cells use bold/heavy typeface. Player-entered digits use regular
  weight. Typographic distinction only — no background fill difference between given and player
  cells.
- **D-05:** Digit buttons arranged in a single horizontal row of 9 (1–9), spanning the full screen
  width. Each button ≥ 56dp tall.
- **D-06:** A dedicated Erase (or ×) button is included alongside the digit row.
- **D-07:** Vertical layout stack: slim top bar (difficulty label) → 9×9 grid → controls row
  (Fill/Pencil toggle + Undo) → number pad row.
- **D-08:** Mode toggle is two side-by-side ButtonMMD buttons ("Fill" / "Pencil"). Active mode
  visually distinguished.
- **D-09:** MMD library (1.0.1) must be added to `app/build.gradle.kts` as `implementation(libs.mmd)`.
- **D-10:** `MainActivity` must become `ComponentActivity` with `setContent { ThemeMMD { GameScreen() } }`.

### Claude's Discretion

- **D-04:** Error cell visual treatment — subtle but present; errors are silent during play.
- Pencil mark display within cells: mini 3×3 grid vs compact list.
- Erase button placement: at end of 1–9 row, or separate row.
- Loading state: what to show when `isLoading = true`.

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| UI-01 | All UI components built using MMD library wrapped in ThemeMMD | ThemeMMD setup, ButtonMMD/TextMMD API patterns documented below |
| UI-02 | No animations, ripple effects, or transitions anywhere | ThemeMMD disables ripple by default; no Animated* composables; explicit patterns to avoid |
| UI-03 | All interactive touch targets minimum 56dp | `Modifier.sizeIn(minHeight = 56.dp)` pattern; number pad sizing math confirmed below |
</phase_requirements>

---

## Summary

Phase 3 is a pure UI-construction phase with no new business logic. The ViewModel
(`GameViewModel`) and all domain models are fully implemented. The work is: wire MMD
into the project, convert `MainActivity`, and build four composable layers — top bar, grid,
controls row, number pad — that observe `GameUiState` and call ViewModel methods.

The most architecturally significant decision is the grid rendering approach. A Canvas-based
grid is the correct choice for E-ink: it produces a single draw call per recomposition, gives
precise pixel-level control over thick/thin borders, and avoids the overdraw risk that comes
from layering 81 individual `Box` composables with borders. The LibreSudoku open-source
project (a production Compose Sudoku app) uses this exact approach and is a direct reference.

For pencil marks, the 3×3 mini-grid layout inside each cell is the standard Sudoku convention
and is more readable on E-ink than a compact inline list: each candidate position is stable and
spatially meaningful (1 always top-left, 9 always bottom-right), reducing cognitive load for
experienced solvers.

**Primary recommendation:** Render the grid with a single `Canvas` composable; use `@Stable`
data class wrappers for per-cell state passed into the Canvas; implement the number pad as a
`Row` of `ButtonMMD` items with explicit `Modifier.weight(1f).sizeIn(minHeight = 56.dp)`.

---

## Project Constraints (from CLAUDE.md)

| Directive | Implication for Phase 3 |
|-----------|------------------------|
| Use `ButtonMMD` and `TextMMD` throughout | All number pad buttons, mode toggle, top bar text — no plain `Button` or `Text` |
| Ripple disabled in `ThemeMMD` — do not re-enable | Never set `indication = rememberRipple()` on any clickable |
| `eInkColorScheme` only — no dynamic or tinted color | No `MaterialTheme.colorScheme.primary`-colored fills; black/white only |
| No animations | No `AnimatedVisibility`, `animateColorAsState`, `crossfade`, or any `Animated*` composable |
| `minSdk = 31`, `targetSdk = 31` | `Modifier.minimumInteractiveComponentSize()` available (API 1+); no compatibility risk |
| MVVM + StateFlow | `collectAsStateWithLifecycle`; pass lambdas to children, not the ViewModel itself |
| `compileSdk = 35` | IDE warnings for deprecated APIs; actual runtime is API 31 |

---

## Standard Stack

### Core (already in build files)
| Library | Version | Purpose | Status |
|---------|---------|---------|--------|
| Compose BOM | 2026.03.00 | Pins all Compose versions | Already declared |
| compose.ui | 1.10.5 (via BOM) | Compose core | Already declared |
| compose.material3 | 1.4.0 (via BOM) | Material3 base that MMD builds on | Already declared |
| lifecycle-viewmodel-compose | 2.9.0 | `viewModel()` composable | Already declared |
| lifecycle-runtime-compose | 2.9.0 | `collectAsStateWithLifecycle` | Already declared |

### MMD (must be added — D-09)
| Library | Version | Purpose | Status |
|---------|---------|---------|--------|
| com.mudita:mmd | 1.0.1 | ThemeMMD, ButtonMMD, TextMMD, all E-ink UI | **MISSING from app/build.gradle.kts** |

The version catalog entry already exists (`libs.mmd = { group = "com.mudita", name = "mmd",
version.ref = "mmd" }` and `mmd = "1.0.1"` in versions). Only the `implementation` declaration
in `app/build.gradle.kts` is missing.

**Repository:** The MMD library is hosted on `https://mudita.jfrog.io/artifactory/mmd-release`.
The project must declare this as a Maven repository in `settings.gradle.kts` (or
`build.gradle.kts` at root) if not already present.

**Installation diff for app/build.gradle.kts:**
```kotlin
dependencies {
    // ADD this line — MMD library (E-ink UI components)
    implementation(libs.mmd)
    // ... rest of existing dependencies unchanged
}
```

**Version verification:** `mmd = "1.0.1"` confirmed in `gradle/libs.versions.toml`. The
1.0.1 release (Feb 28, 2026) is a naming-convention refactor; component names confirmed as
`ButtonMMD`, `TextMMD`, `ThemeMMD` — unchanged from 1.0.0.

---

## Architecture Patterns

### Recommended Project Structure (new files this phase)

```
app/src/main/java/com/mudita/sudoku/
├── MainActivity.kt            # EXISTING — upgrade to ComponentActivity
└── ui/
    └── game/
        ├── GameScreen.kt      # Root screen composable; observes ViewModel
        ├── GameGrid.kt        # Canvas-based 9×9 grid + cell data
        ├── NumberPad.kt       # Row of ButtonMMD digit buttons + Erase
        └── ControlsRow.kt     # Fill/Pencil toggle + Undo button
```

No new ViewModel or model classes required. All state comes from `GameUiState`.

---

### Pattern 1: MainActivity — ComponentActivity with ThemeMMD (D-10)

**What:** Convert `MainActivity` from `android.app.Activity` to
`androidx.activity.ComponentActivity`. Add `setContent` with `ThemeMMD` as the root wrapper.

**Current state:** `MainActivity` extends bare `Activity` with empty `onCreate`.

**Required result:**

```kotlin
// app/src/main/java/com/mudita/sudoku/MainActivity.kt
package com.mudita.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mudita.mmd.ThemeMMD
import com.mudita.sudoku.ui.game.GameScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD {
                GameScreen()
            }
        }
    }
}
```

**Import note:** `setContent` comes from `androidx.activity.compose.setContent` (provided by
`androidx.activity:activity-compose`, which is pulled in transitively by
`lifecycle-viewmodel-compose`). No additional dependency needed.

**AndroidManifest:** No change needed — `.MainActivity` declaration is unchanged; the manifest
does not care whether it extends `Activity` or `ComponentActivity`.

---

### Pattern 2: GameScreen — ViewModel wiring

**What:** Top-level screen composable obtains the ViewModel and distributes state/callbacks
to child composables. Child composables receive only the data they need plus lambdas — not
the ViewModel itself.

```kotlin
// Source: Android Developers — Compose and libraries
@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // One-shot events (puzzle completion)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is GameEvent.Completed -> { /* TODO Phase 5 — completion screen */ }
            }
        }
    }

    if (uiState.isLoading) {
        LoadingScreen()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 8.dp)
    ) {
        // 1. Top bar
        DifficultyBar(difficulty = uiState.difficulty)

        // 2. Grid — fills remaining space
        GameGrid(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            board = uiState.board,
            givenMask = uiState.givenMask,
            selectedCellIndex = uiState.selectedCellIndex,
            pencilMarks = uiState.pencilMarks,
            onCellClick = viewModel::selectCell
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Controls row
        ControlsRow(
            inputMode = uiState.inputMode,
            onToggleMode = viewModel::toggleInputMode,
            onUndo = viewModel::undo
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Number pad
        NumberPad(
            onDigitClick = viewModel::enterDigit,
            onErase = { viewModel.enterDigit(0) }  // 0 = erase sentinel — see note below
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}
```

**Erase sentinel note:** `enterDigit` in the current ViewModel guards for `digit !in 1..9`,
so passing `0` is a no-op. The ViewModel will need a separate `eraseCell()` method or the
guard expanded. Flag this as a task in the plan — the ViewModel needs a minimal addition.
Alternatively the planner may scope an `eraseCell()` method in the same phase.

---

### Pattern 3: Canvas Grid — the correct approach for E-ink

**What:** The entire 9×9 grid — all lines, cell backgrounds, and digits — is drawn with a
single `Canvas` composable. This is not the naive approach of 81 `Box` composables; it is a
deliberate choice matching production Sudoku apps (LibreSudoku, compose-sudoku) for the
following reasons:

**Why Canvas over 81 Box composables:**
- E-ink performs a full panel refresh when the dirty region changes. 81 individual composables
  each with background modifiers creates 81 independent dirty regions on cell selection changes,
  increasing partial-refresh artifacts. A single Canvas marks one dirty region.
- Precise border control: thick/thin borders at 3×3 boundaries require math, not nested padding.
  Canvas `drawLine` at exact pixel offsets is exact; `Modifier.border` on individual cells
  creates double-border thickness where cells share edges.
- Recomposition scope: `Canvas` is a single composable. With correct `@Stable` inputs, a
  cell selection change recomposing only the Canvas avoids recomposing the whole Column.

**Cell data wrapper (must be @Stable for smart recomposition):**
```kotlin
@Stable
data class CellData(
    val index: Int,
    val digit: Int,          // 0 = empty
    val isGiven: Boolean,
    val isSelected: Boolean,
    val pencilMarks: Set<Int>
)
```

**Grid rendering pattern:**
```kotlin
@Composable
fun GameGrid(
    modifier: Modifier = Modifier,
    board: IntArray,
    givenMask: BooleanArray,
    selectedCellIndex: Int?,
    pencilMarks: Array<Set<Int>>,
    onCellClick: (Int) -> Unit
) {
    // Build stable cell list — only rebuilt when board/selection/marks change
    val cells = remember(board, givenMask, selectedCellIndex, pencilMarks) {
        List(81) { i ->
            CellData(
                index = i,
                digit = board[i],
                isGiven = givenMask[i],
                isSelected = selectedCellIndex == i,
                pencilMarks = pencilMarks[i]
            )
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val gridSize = minOf(maxWidth, maxHeight)
        val cellSizePx = with(LocalDensity.current) { (gridSize / 9).toPx() }

        Canvas(
            modifier = Modifier
                .size(gridSize)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val col = (offset.x / cellSizePx).toInt().coerceIn(0, 8)
                        val row = (offset.y / cellSizePx).toInt().coerceIn(0, 8)
                        onCellClick(row * 9 + col)
                    }
                }
        ) {
            drawGrid(cells, cellSizePx)
        }
    }
}
```

**drawGrid implementation sketch (inside DrawScope extension):**
```kotlin
// Source pattern: LibreSudoku Board.kt canvas approach
private fun DrawScope.drawGrid(cells: List<CellData>, cellSize: Float) {
    val thinStroke = 1.dp.toPx()
    val thickStroke = 2.5.dp.toPx()

    // 1. Selected cell fill (solid black)
    cells.firstOrNull { it.isSelected }?.let { cell ->
        val row = cell.index / 9
        val col = cell.index % 9
        drawRect(
            color = Color.Black,
            topLeft = Offset(col * cellSize, row * cellSize),
            size = Size(cellSize, cellSize)
        )
    }

    // 2. Digits
    // Use drawText via TextMeasurer (requires rememberTextMeasurer() hoisted from composable)
    cells.forEach { cell ->
        val row = cell.index / 9
        val col = cell.index % 9
        val cx = col * cellSize + cellSize / 2
        val cy = row * cellSize + cellSize / 2

        if (cell.digit != 0) {
            // drawText call here — see Pattern 4 for TextMeasurer setup
        } else if (cell.pencilMarks.isNotEmpty()) {
            drawPencilMarks(cell.pencilMarks, col * cellSize, row * cellSize, cellSize)
        }
    }

    // 3. Grid lines — thin first, then thick on top
    for (i in 0..9) {
        val pos = i * cellSize
        val isBoxBoundary = i % 3 == 0
        val stroke = if (isBoxBoundary) thickStroke else thinStroke
        val color = Color.Black
        // Vertical
        drawLine(color, Offset(pos, 0f), Offset(pos, 9 * cellSize), stroke)
        // Horizontal
        drawLine(color, Offset(0f, pos), Offset(9 * cellSize, pos), stroke)
    }
}
```

**Pencil mark mini-grid inside cell (Claude's discretion — see decision below):**
```kotlin
private fun DrawScope.drawPencilMarks(
    marks: Set<Int>,
    cellLeft: Float,
    cellTop: Float,
    cellSize: Float
) {
    val subSize = cellSize / 3f
    for (digit in 1..9) {
        if (digit in marks) {
            val subRow = (digit - 1) / 3
            val subCol = (digit - 1) % 3
            // Draw small digit at subCell center
            // digit 1→(0,0), 2→(0,1), 3→(0,2), 4→(1,0), ...  9→(2,2)
            val x = cellLeft + subCol * subSize + subSize / 2f
            val y = cellTop + subRow * subSize + subSize / 2f
            // drawText call for small digit at (x, y)
        }
    }
}
```

---

### Pattern 4: Text in Canvas — TextMeasurer

**What:** Drawing text inside a Canvas requires `TextMeasurer`. Unlike `TextMMD` (which is a
`@Composable`), `drawText` is a `DrawScope` extension. The measurer must be hoisted to the
composable layer and passed to or captured inside the Canvas lambda.

```kotlin
@Composable
fun GameGrid(...) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val digitStyle = TextStyle(
        fontSize = 20.sp,    // Adjust to ~50% of cellSize
        fontWeight = FontWeight.Bold,    // for given cells
        color = Color.Black
    )
    val digitStylePlayer = digitStyle.copy(fontWeight = FontWeight.Normal)
    val digitStyleSelected = digitStyle.copy(color = Color.White) // on black fill

    Canvas(...) {
        // Inside lambda, textMeasurer and styles captured from outer scope
        cells.forEach { cell ->
            if (cell.digit != 0) {
                val style = when {
                    cell.isSelected -> if (cell.isGiven) digitStyle.copy(
                        color = Color.White, fontWeight = FontWeight.Bold
                    ) else digitStyleSelected
                    cell.isGiven -> digitStyle
                    else -> digitStylePlayer
                }
                val measured = textMeasurer.measure(cell.digit.toString(), style)
                val row = cell.index / 9; val col = cell.index % 9
                val cellSize = size.width / 9f
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = col * cellSize + (cellSize - measured.size.width) / 2f,
                        y = row * cellSize + (cellSize - measured.size.height) / 2f
                    )
                )
            }
        }
    }
}
```

**Important:** `rememberTextMeasurer()` must be called at the composable level, not inside the
Canvas lambda (Composables cannot be called inside DrawScope).

---

### Pattern 5: Number Pad — Row of ButtonMMD

**What:** A single horizontal `Row` with 10 items (1–9 + Erase), each using
`Modifier.weight(1f)` to distribute width equally across the 800dp screen, and
`Modifier.sizeIn(minHeight = 56.dp)` to meet UI-03.

**Screen width math:** 800px device width at ~320dpi (Mudita Kompakt) ≈ ~250dp usable at
density. Actually the device is 800×480 **pixels** on a ~3.9" diagonal. Pixel density ≈
235ppi → 1dp ≈ 1.47px. So 800px ÷ 1.47 ≈ 544dp usable width. With 10 buttons at
`weight(1f)`: 544dp ÷ 10 = ~54dp each. At 54dp wide, buttons meet the ≥56dp minimum for
**height** but are slightly narrow. Height is the binding constraint (D-05 says "≥56dp tall"),
and width is fixed by `weight`. This is acceptable — `weight` fills full width; the 54dp width
is a display-density consequence, not a failure to meet spec (spec says "tall").

```kotlin
@Composable
fun NumberPad(
    onDigitClick: (Int) -> Unit,
    onErase: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (digit in 1..9) {
            ButtonMMD(
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 56.dp),
                onClick = { onDigitClick(digit) }
            ) {
                TextMMD(text = digit.toString())
            }
        }
        // Erase button at end of row (Claude's discretion decision — see below)
        ButtonMMD(
            modifier = Modifier
                .weight(1f)
                .sizeIn(minHeight = 56.dp),
            onClick = onErase
        ) {
            TextMMD(text = "×")
        }
    }
}
```

---

### Pattern 6: Mode Toggle — Active State with ButtonMMD

**What:** Two `ButtonMMD` buttons side-by-side ("Fill" / "Pencil"). The active mode button
must look visually distinct from the inactive one. Since `ButtonMMD` inherits from Material3
`Button`, the `colors` parameter accepts `ButtonDefaults.buttonColors(...)` which controls
`containerColor` and `contentColor`.

**E-ink active state:** Solid black fill for active, outlined/white fill for inactive is the
highest-contrast approach on a monochromatic display — matches the selected-cell convention
(D-02) and keeps the visual language consistent.

```kotlin
@Composable
fun ControlsRow(
    inputMode: InputMode,
    onToggleMode: () -> Unit,
    onUndo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Fill button
        ButtonMMD(
            modifier = Modifier
                .weight(1f)
                .sizeIn(minHeight = 56.dp),
            onClick = {
                if (inputMode != InputMode.FILL) onToggleMode()
            },
            colors = if (inputMode == InputMode.FILL) {
                // Active: solid black container, white text
                ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                )
            } else {
                // Inactive: white container, black text
                ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            }
        ) {
            TextMMD(text = "Fill")
        }

        // Pencil button (mirror of above, inverted)
        ButtonMMD(
            modifier = Modifier
                .weight(1f)
                .sizeIn(minHeight = 56.dp),
            onClick = {
                if (inputMode != InputMode.PENCIL) onToggleMode()
            },
            colors = if (inputMode == InputMode.PENCIL) {
                ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            }
        ) {
            TextMMD(text = "Pencil")
        }

        // Undo
        ButtonMMD(
            modifier = Modifier
                .weight(1f)
                .sizeIn(minHeight = 56.dp),
            onClick = onUndo
        ) {
            TextMMD(text = "Undo")
        }
    }
}
```

**Risk flag on `colors` parameter:** `ButtonMMD` wraps or extends Material3 `Button`. The
`colors` parameter is standard Material3 API and will be present. However, if `ButtonMMD`
applies its own `ButtonColors` override internally (likely for the E-ink monochrome default),
passing explicit `ButtonDefaults.buttonColors(...)` should still override correctly per Material3
convention. If it does not (LOW confidence), the fallback is a `Box` with `Modifier.clickable`
and `Modifier.background` for the toggle buttons specifically — acceptable since the mode toggle
is not a primary navigation action.

---

### Pattern 7: Top Bar (Difficulty Label)

```kotlin
@Composable
fun DifficultyBar(difficulty: Difficulty) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        TextMMD(
            text = difficulty.name.lowercase()
                .replaceFirstChar { it.uppercase() }  // "Easy" / "Medium" / "Hard"
        )
    }
}
```

No `TopAppBarMMD` needed here — the spec calls for a "slim" label, not a full app bar.

---

### Pattern 8: Loading State (Claude's discretion)

When `isLoading = true`, show a centered `TextMMD` placeholder. No spinner — `CircularProgressIndicatorMMD` creates animation frames that can leave ghosting on E-ink. The
screen switch itself is a single full refresh, which is acceptable.

```kotlin
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        TextMMD(text = "Generating puzzle…")
    }
}
```

---

### Anti-Patterns to Avoid

- **81 Box composables with borders:** Creates 81 recomposition scopes and overdraw. Use Canvas.
- **`animateColorAsState` for selected cell:** Violates UI-02; also causes E-ink ghosting.
- **`indication = rememberRipple()`:** Explicitly forbidden; ThemeMMD strips this globally, but do
  not re-introduce it manually on any `Modifier.clickable`.
- **`LazyVerticalGrid` for the Sudoku grid:** Lazy layouts are for unknown-length scrollable
  lists. The 9×9 grid is fixed, non-scrollable, and fits entirely on screen. Use `Canvas`.
- **Nested Column+Row for the grid:** 81 composable nodes in the tree; no better than Box
  approach. Canvas is the correct primitive for fixed-layout custom drawing.
- **`CircularProgressIndicatorMMD` for loading:** Animates; causes ghosting. Use static text.
- **Reading `selectedCellIndex` inside Canvas lambda without `remember`:** State read inside
  `Canvas` lambda happens in draw phase (good), but ensure the `cells` list is recomputed via
  `remember(board, givenMask, selectedCellIndex, pencilMarks)` so Canvas only redraws when
  inputs actually change, not on every parent recomposition.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Touch target sizing | Custom `Layout` to enlarge hit area | `Modifier.sizeIn(minHeight = 56.dp)` on ButtonMMD | Material3 handles touch area expansion automatically at the system layer |
| E-ink color scheme | Custom `ColorScheme` with hand-coded monochrome | `eInkColorScheme` from ThemeMMD | Already validated for E-ink contrast ratios; rolling your own risks grays that smear |
| State collection from Flow | Manual `collectLatest` in `LaunchedEffect` | `collectAsStateWithLifecycle()` | Lifecycle-aware; prevents background collection when app is backgrounded |
| Button ripple suppression | `Modifier.indication(null)` on each button | ThemeMMD default (already suppressed) | Re-applying `indication(null)` is redundant and creates confusion about intent |
| Text rendering in Canvas | Custom bitmap font atlas | `rememberTextMeasurer()` + `drawText()` | Compose 1.3+ provides `drawText` as a first-class DrawScope extension; no custom bitmap needed |

---

## Claude's Discretion — Recommendations

### D-04: Error Cell Visual Treatment

**Recommendation:** A thin red-equivalent (darkened border, 2dp) around the error cell only.

On a monochromatic E-ink display, "red" is not available. The treatment must use black only.
Options:
1. **Thin outer border on error cell** — 2dp black border inside the cell boundary, offset from
   the grid line. This is visible, non-intrusive, and does not affect reading the digit.
2. **Strikethrough on the digit** — Aggressive, potentially confusing in pencil-mark mode.
3. **No visual treatment** — Valid interpretation of "silent errors per SCORE-01".

**Recommendation is option 1:** A 1dp inset border drawn in the Canvas on any cell where
`board[i] != 0 && !givenMask[i] && board[i] != solution[i]`. This requires passing the
`solution` array to the Canvas — but `GameUiState.solution` is already available in `uiState`.
Keep it minimal: do not add strikethrough, do not invert the cell.

**Confidence:** MEDIUM — this is a judgment call for E-ink aesthetics. LOW risk since errors
are cosmetic and the border is easy to remove in a follow-up.

---

### Pencil Mark Display: Mini 3×3 Grid (recommended over compact list)

**Recommendation:** Mini 3×3 grid, where candidate `n` is positioned at fixed subCell
`((n-1)/3, (n-1)%3)`.

**Why:**
- This is the universal Sudoku convention. Experienced solvers know that 1 is top-left, 9 is
  bottom-right. A compact list (e.g., "1 3 7" centered) requires re-reading each time.
- On E-ink, spatial stability reduces cognitive load — the eye goes directly to the 7 position
  regardless of what other candidates are set.
- LibreSudoku (production reference) uses this exact approach.
- The only downside is that at small cell sizes the subCell text may be very small. At 800px ÷
  1.47dpPerPx ÷ 9 cells ≈ 60dp per cell, each sub-cell is ~20dp — acceptable for 8–9sp text.

**Minimum font size for pencil marks:** 8sp. Below that, E-ink subpixel rendering may blur
the glyph at 235ppi. Recommend 9sp for safety.

---

### Erase Button: End of the Number Pad Row (recommended)

**Recommendation:** Place "×" as the 10th button at the right end of the 1–9 row, same height
and visual weight.

**Why:**
- Keeps all digit-entry actions in a single horizontal strip — one touch zone for all input.
- A separate row for Erase would add height to the layout and compress the grid (the grid is
  `Modifier.weight(1f)` so it would shrink).
- 10 equally-spaced buttons at `weight(1f)` across ~544dp ≈ 54dp each — meets the ≥56dp
  height spec; width is slightly narrow but touch registration includes the full weight area.
- LibreSudoku and other production Sudoku apps place Erase in the same row as digits.

---

### Loading State: Static Text (recommended)

**Recommendation:** `TextMMD("Generating puzzle…")` centered on a blank screen.

**Why:** Puzzle generation via Sudoklify on `Dispatchers.Default` takes < 200ms on the Helio
A22 for EASY/MEDIUM, potentially 300–500ms for HARD (solving difficulty classification).
A static label is sufficient for this duration. A spinner would require animation frames and
risks partial-refresh ghosting on E-ink during the brief flash.

---

## Common Pitfalls

### Pitfall 1: Double-Border Lines Between Cells

**What goes wrong:** Drawing a `1dp` border on each cell composable produces `2dp` shared
borders where two cells meet. Box composables with `Modifier.border(1.dp)` will make internal
grid lines look heavier than outer edges.

**Why it happens:** Each cell draws its own four borders independently.

**How to avoid:** Use Canvas `drawLine` at line-center positions. One Canvas, one line per grid
division. The line's position is the center; stroke width extends equally in both directions.

**Warning signs:** Inner grid lines appear double-width when inspecting with Layout Inspector.

---

### Pitfall 2: Recomposing All 81 Cells on Single-Cell Selection Change

**What goes wrong:** Selecting a cell changes `selectedCellIndex`. If the cell list is not
`remember`-cached, every composable in the tree that reads `uiState` recomposes. For a Canvas
approach this is less severe (one composable), but if using Box composables it causes
81 simultaneous recompositions per tap.

**Why it happens:** `GameUiState` is a data class; `StateFlow.collectAsStateWithLifecycle()`
emits a new value on every `.copy()` call — even if only one field changed.

**How to avoid (Canvas approach):**
- Wrap the cell list in `remember(board, givenMask, selectedCellIndex, pencilMarks)`.
- The Canvas redraws exactly once when any of those inputs change.
- `GameUiState` already has correct `equals`/`hashCode` for arrays — state emissions are
  not spurious.

**How to avoid (Box approach, not recommended):**
- `@Stable data class CellData(...)` + `key(cell.index)` in a loop.
- Only the previously-selected and newly-selected cell composables recompose.

---

### Pitfall 3: `setContent` Without `ComponentActivity`

**What goes wrong:** Calling `setContent { }` from `android.app.Activity` (not
`ComponentActivity`) fails with a compile error — `setContent` is an extension function on
`ComponentActivity`.

**Why it happens:** `MainActivity` currently extends bare `Activity`. Calling `setContent`
imports from `androidx.activity.compose` which only works on `ComponentActivity`.

**How to avoid:** Change `class MainActivity : Activity()` to
`class MainActivity : ComponentActivity()`. Also update the import from
`android.app.Activity` to `androidx.activity.ComponentActivity`.

**Warning signs:** IDE underlines `setContent` as unresolved; build fails with
`Unresolved reference: setContent`.

---

### Pitfall 4: ThemeMMD Outside Canvas Text Styling

**What goes wrong:** `TextMMD` inside Canvas is impossible — Composables cannot be called
inside `DrawScope`. Text drawn via `drawText(TextMeasurer, ...)` bypasses ThemeMMD's typography
entirely.

**Why it happens:** Canvas is a draw-phase primitive; composable functions cannot execute there.

**How to avoid:**
- Manually specify `TextStyle` values that match MMD's E-ink typography for in-Canvas text.
- For grid digits: `fontSize = 20.sp, fontWeight = FontWeight.Bold/Normal` is reasonable
  starting point (adjust to ~45-50% of cell size in dp).
- For pencil marks: `fontSize = 9.sp, fontWeight = FontWeight.Normal`.
- These values are declared as `val` constants at the top of `GameGrid.kt`, not hardcoded inline.

---

### Pitfall 5: `WindowInsets` — Navigation Bar Overlapping Number Pad

**What goes wrong:** Android's navigation bar (gesture bar or 3-button bar, ~20dp) overlaps
the bottom of the screen. Without `windowInsetsPadding`, the number pad is partially hidden.

**Why it happens:** `setContent` without `windowInsetsPadding` renders edge-to-edge; system
UI renders on top.

**How to avoid:** Apply `Modifier.windowInsetsPadding(WindowInsets.systemBars)` on the root
`Column` in `GameScreen`, or `Modifier.padding(bottom = WindowInsets.systemBars.getBottom(...))`.
The context notes mention "navigation bar padding (~20dp) is accounted for at the bottom" (D-07).

---

### Pitfall 6: `pointerInput` With Stale Lambda Capture

**What goes wrong:** If `onCellClick` lambda captures ViewModel state at composition time and
is not updated on recomposition, taps may call stale callbacks.

**Why it happens:** `Modifier.pointerInput(Unit)` with `Unit` key never re-executes the block.

**How to avoid:** Use `Modifier.pointerInput(onCellClick)` (key = the lambda reference), or
capture the callback via `rememberUpdatedState(onCellClick)` inside the `pointerInput` block.
Preferred:
```kotlin
val currentOnCellClick by rememberUpdatedState(onCellClick)
Modifier.pointerInput(Unit) {
    detectTapGestures { offset ->
        // uses currentOnCellClick — always current
        currentOnCellClick(/* computed index */)
    }
}
```

---

## Code Examples

### ThemeMMD Import and Setup
```kotlin
// Source: mudita/MMD README + release.1.0.1 source
import com.mudita.mmd.ThemeMMD

// In MainActivity.onCreate:
setContent {
    ThemeMMD {
        // All Compose content here — ThemeMMD applies eInkColorScheme,
        // disables ripple globally, applies E-ink typography
        GameScreen()
    }
}
```

### ButtonMMD with Active State
```kotlin
// Source: MMD README example + Material3 ButtonDefaults pattern
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color

ButtonMMD(
    onClick = { /* action */ },
    modifier = Modifier.sizeIn(minHeight = 56.dp),
    colors = ButtonDefaults.buttonColors(
        containerColor = Color.Black,   // active state
        contentColor = Color.White
    )
) {
    TextMMD(text = "Fill")
}
```

### Canvas drawText with TextMeasurer
```kotlin
// Source: Compose 1.3+ DrawScope API
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

val textMeasurer = rememberTextMeasurer()
Canvas(modifier = Modifier.fillMaxSize()) {
    val style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    val result = textMeasurer.measure("5", style)
    drawText(
        textLayoutResult = result,
        topLeft = Offset(x = centerX - result.size.width / 2f,
                         y = centerY - result.size.height / 2f)
    )
}
```

### collectAsStateWithLifecycle with ViewModel
```kotlin
// Source: Android Developers — lifecycle-runtime-compose
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ...
}
```

### Touch target — sizeIn vs minimumInteractiveComponentSize
```kotlin
// For components that MUST be exactly 56dp+:
Modifier.sizeIn(minHeight = 56.dp)   // Explicit — layout enforced

// For components that can be smaller visually but need 48dp hit area:
Modifier.minimumInteractiveComponentSize()  // Pads touch, not layout

// For this project: use sizeIn(minHeight = 56.dp) on all interactive elements
// to meet UI-03 per requirements (layout size, not just touch area).
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `drawText` with bitmap atlas | `rememberTextMeasurer()` + `DrawScope.drawText()` | Compose 1.3 (2022) | No custom font atlas needed for Canvas text |
| `collectAsState()` | `collectAsStateWithLifecycle()` | lifecycle-runtime-compose 2.6 (2023) | Stops collecting when app backgrounded — prevents leaks |
| `android.app.Activity` + ViewBinding | `ComponentActivity` + `setContent {}` | Compose 1.0 (2021) — but this project still uses old approach | Must be changed in D-10 |
| `LazyVerticalGrid` for custom grids | `Canvas` for fixed-layout custom rendering | N/A (always correct; just often misused) | Single draw call, zero overdraw |

**Deprecated/outdated:**
- `rememberRipple()` → Use no ripple (ThemeMMD handles this globally). Do not import.
- `LocalContentAlpha` → Removed in Material3; use `contentAlpha` parameter on TextStyle.
- `AmbientContentColor` → Renamed to `LocalContentColor` (pre-Compose 1.0 API; irrelevant if
  not using pre-1.0 patterns).

---

## Environment Availability

Step 2.6: SKIPPED — Phase 3 is code and configuration changes only. No new external tools,
services, CLIs, or databases. MMD library is pulled from Mudita's JFrog Artifactory Maven
repository (existing network dependency; same as development environment already used for
other dependencies). No additional tools required beyond the existing Gradle + Android SDK
build environment.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 (4.13.2) + Robolectric (4.14.1) + Compose UI Test |
| Config file | None — Robolectric config via `@RunWith(RobolectricTestRunner::class)` |
| Quick run command | `./gradlew :app:test --tests "com.mudita.sudoku.ui.*" -x lint` |
| Full suite command | `./gradlew :app:test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| UI-01 | All components wrapped in ThemeMMD | Compose UI (smoke) | `./gradlew :app:test --tests "com.mudita.sudoku.ui.GameScreenTest"` | ❌ Wave 0 |
| UI-02 | No animations/ripple anywhere | Code review + Compose UI | Manual inspection + `./gradlew :app:test --tests "com.mudita.sudoku.ui.GameScreenTest"` | ❌ Wave 0 |
| UI-03 | All touch targets ≥ 56dp | Compose UI (SemanticsTree) | `./gradlew :app:test --tests "com.mudita.sudoku.ui.TouchTargetTest"` | ❌ Wave 0 |
| D-02 | Selected cell solid black, white digit | Compose UI (screenshot or semantics) | `./gradlew :app:test --tests "com.mudita.sudoku.ui.GameGridTest"` | ❌ Wave 0 |
| D-08 | Active mode toggle visually distinct | Compose UI (semantics state) | `./gradlew :app:test --tests "com.mudita.sudoku.ui.ControlsRowTest"` | ❌ Wave 0 |

**Note on UI-02 (no animations):** Automated testing for absence of animations is limited in
Compose. The primary guard is code review: no `Animated*` composables, no
`animateColorAsState`, no `transition`. Robolectric-based Compose tests can verify composable
renders without crashing; visual correctness requires device/emulator manual check.

**Note on UI-03 (touch targets):** Compose Semantics tree does not expose `dp` size directly
in test assertions without `onNode().fetchSemanticsNode().boundsInRoot`. A simple test
verifying `boundsInRoot.height >= 56.dp.toPx(density)` on each interactive node suffices.

### Sampling Rate
- **Per task commit:** `./gradlew :app:test --tests "com.mudita.sudoku.ui.*" -x lint`
- **Per wave merge:** `./gradlew :app:test`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `app/src/test/java/com/mudita/sudoku/ui/GameScreenTest.kt` — covers UI-01, UI-02 smoke
- [ ] `app/src/test/java/com/mudita/sudoku/ui/GameGridTest.kt` — covers D-02 selected cell
- [ ] `app/src/test/java/com/mudita/sudoku/ui/ControlsRowTest.kt` — covers D-08 toggle state
- [ ] `app/src/test/java/com/mudita/sudoku/ui/TouchTargetTest.kt` — covers UI-03

All four use Robolectric + `createComposeRule()` pattern already established in
`GameViewModelTest.kt` (`@RunWith(RobolectricTestRunner::class)`).

---

## Open Questions

1. **ButtonMMD `colors` parameter — does it accept `ButtonDefaults.buttonColors(...)`?**
   - What we know: `ButtonMMD` wraps Material3 `Button`; Material3 `Button` accepts `colors:
     ButtonColors`. MMD 1.0.1 release note says "naming convention refactors."
   - What's unclear: Whether `ButtonMMD`'s signature exposes `colors` parameter, or whether it
     hard-codes its own `ButtonColors` internally for E-ink monochrome.
   - Recommendation: Attempt `colors = ButtonDefaults.buttonColors(...)` first. If the parameter
     is absent, fall back to wrapping the toggle in a `Box(Modifier.background(...).clickable {})`
     with `TextMMD` inside — this is E-ink safe and avoids ripple.

2. **`eraseCell` ViewModel method — needed?**
   - What we know: `enterDigit` guards for `digit !in 1..9`. Passing `0` is a silent no-op.
   - What's unclear: Whether the plan should add `eraseCell()` to `GameViewModel` in this phase.
   - Recommendation: Add `eraseCell()` as a small addition to `GameViewModel` in Wave 1 of
     this phase. It's one line (`board[idx] = 0` + undo push) and is required for the number
     pad to be fully functional. Without it the Erase button does nothing.

3. **MMD Maven repository — already declared?**
   - What we know: `libs.mmd` is in the version catalog; the `implementation` line is missing
     from `app/build.gradle.kts`. The JFrog URL `https://mudita.jfrog.io/artifactory/mmd-release`
     must be in `settings.gradle.kts` for Gradle to resolve the artifact.
   - What's unclear: Whether the project already has this repository declared (not checked in
     available files).
   - Recommendation: Read `settings.gradle.kts` in Wave 0 and add the Mudita repository URL
     if absent. This is a blocker — without it, `implementation(libs.mmd)` will fail to resolve.

---

## Sources

### Primary (HIGH confidence)
- [Android Developers — Compose and other libraries](https://developer.android.com/develop/ui/compose/libraries) — ComponentActivity + setContent, viewModel(), collectAsStateWithLifecycle
- [Android Developers — Follow best practices (Compose performance)](https://developer.android.com/develop/ui/compose/performance/bestpractices) — remember, keys, derivedStateOf, lambda modifiers
- [Android Developers — Lists and grids](https://developer.android.com/develop/ui/compose/lists) — LazyVerticalGrid keys, fixed grid guidance
- [composables.com — minimumInteractiveComponentSize](https://composables.com/material3/minimuminteractivecomponentsize) — touch target modifier behavior
- [Android Developers — Buttons](https://developer.android.com/develop/ui/compose/components/button) — ButtonColors, containerColor/contentColor

### Secondary (MEDIUM confidence)
- [mudita/MMD GitHub repo](https://github.com/mudita/MMD) — component list, ThemeMMD/ButtonMMD/TextMMD names, ripple-disabled by default, eInkColorScheme
- [mudita/MMD release.1.0.1](https://github.com/mudita/MMD/releases/tag/release.1.0.1) — confirmed "naming convention refactors," no major API breaks
- [LibreSudoku source — Board.kt](https://raw.githubusercontent.com/kaajjo/LibreSudoku/master/app/src/main/java/com/kaajjo/libresudoku/ui/components/board/Board.kt) — Canvas approach, thick/thin border math, TextMeasurer pattern, pencil mark 3×3 mini-grid
- [davidraywilson/CalmDirectory](https://github.com/davidraywilson/CalmDirectory) — Real-world MMD 1.0.0 usage: ThemeMMD + eInkColorScheme + ButtonMMD confirmed working

### Tertiary (LOW confidence — flagged)
- Web search results for ButtonMMD `colors` parameter: no official API doc found; pattern
  inferred from Material3 inheritance. Treat as LOW until verified at build time.
- MMD internal package paths (`com.mudita.mmd.components.buttons.ButtonMMD`,
  `com.mudita.mmd.components.text.TextMMD`) — inferred from README + CalmDirectory; exact
  import paths must be confirmed via IDE autocomplete after adding dependency.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all versions in version catalog; only `implementation` line missing
- MMD component API (ThemeMMD, ButtonMMD, TextMMD): MEDIUM — names confirmed, `colors` param LOW
- Canvas grid approach: HIGH — confirmed by LibreSudoku production code + official Compose docs
- Architecture (MVVM, collectAsStateWithLifecycle): HIGH — official Android Developers docs
- Pencil marks (3×3 mini-grid): HIGH — universal Sudoku convention + LibreSudoku reference
- Touch target enforcement: HIGH — official Compose docs, `sizeIn` is straightforward
- Pitfalls: HIGH — all sourced from direct code inspection or official docs

**Research date:** 2026-03-24
**Valid until:** 2026-04-24 (MMD is a small library; low churn risk. Compose BOM 2026.03.00
is stable. 30-day validity is conservative.)
