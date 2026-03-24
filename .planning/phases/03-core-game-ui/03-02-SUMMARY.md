---
phase: 03-core-game-ui
plan: "02"
subsystem: ui-game
tags: [compose, canvas, gamegrid, numberpad, controls, gamescreen, mmd, eink]
dependency_graph:
  requires: [03-01]
  provides: [GameGrid, NumberPad, ControlsRow, GameScreen-full]
  affects: [03-03, 03-04]
tech_stack:
  added: []
  patterns: [Canvas DrawScope, BoxWithConstraints, pointerInput detectTapGestures, collectAsStateWithLifecycle, Box+clickable indication=null]
key_files:
  created:
    - app/src/main/java/com/mudita/sudoku/ui/game/GameGrid.kt
    - app/src/main/java/com/mudita/sudoku/ui/game/NumberPad.kt
    - app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt
  modified:
    - app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt
decisions:
  - ControlsRow mode toggles use Box+clickable (indication=null) instead of ButtonMMD with colors param — ButtonMMD colors API unverifiable without MMD AAR; Box approach is guaranteed safe and explicitly noted in plan as fallback
  - Compilation verified structurally: all non-MMD Kotlin compiles cleanly; only MMD unresolved reference errors remain (same as Plan 01 baseline)
  - Grid lines drawn last in Canvas to prevent thick box borders being partially overwritten by adjacent cell background fills
metrics:
  duration: ~6 min
  completed: 2026-03-24
  tasks_completed: 2
  files_modified: 4
requirements: [UI-01, UI-02, UI-03]
---

# Phase 3 Plan 02: GameScreen UI — Canvas Grid, NumberPad, ControlsRow Summary

Complete GameScreen UI built with Canvas-based 9x9 grid (DrawScope extensions), 10-button horizontal NumberPad, Box+clickable mode toggle ControlsRow, and full GameViewModel wiring via collectAsStateWithLifecycle.

## What Was Built

### Task 1: GameGrid — Canvas-based 9x9 grid

**File:** `app/src/main/java/com/mudita/sudoku/ui/game/GameGrid.kt`

- `CellData` data class annotated with `@Stable` — prevents whole-grid recomposition on single-cell updates
- `isError` computed inline: `digit != 0 && !isGiven && digit != solution[index]`
- `GameGrid` composable using a single `Canvas` (no Box per cell)
- `BoxWithConstraints` for square grid sizing from available space
- `pointerInput + detectTapGestures` for cell selection: `(offset.x / cellSize).toInt().coerceIn(0,8)`
- `rememberTextMeasurer()` hoisted at composable level (cannot be called inside DrawScope)
- 5 text styles defined at composable level for digit rendering (given bold/player normal, black/white for selection)
- Canvas drawing order: selected fill → error indicators → digit text → pencil marks → grid lines
- `drawGridLines`: thick (2.5dp) at `i % 3 == 0`, thin (1dp) otherwise
- `drawSelectedCell`: solid black rect fill
- `drawErrorIndicator`: 1dp inset `Stroke` rect (D-04)
- `drawPencilMarks`: 3x3 mini-grid, digit n at subCell `((n-1)/3, (n-1)%3)`, 9sp font

### Task 2: NumberPad, ControlsRow, GameScreen

**NumberPad.kt** (`app/src/main/java/com/mudita/sudoku/ui/game/NumberPad.kt`):
- 10 `ButtonMMD` items in a horizontal `Row` with `Arrangement.spacedBy(2.dp)`
- Digits 1–9 + Erase (×, `\u00D7`) at end of row
- All buttons: `Modifier.weight(1f).sizeIn(minHeight = 56.dp)` (UI-03)
- `TextMMD` inside each button (UI-01)
- No ripple, no animation (UI-02)

**ControlsRow.kt** (`app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt`):
- Fill and Pencil mode toggles implemented as `Box + clickable(indication = null)` — no ripple (UI-02)
- Active state: `background(Color.Black)` with `TextMMD(color = Color.White)`
- Inactive state: `background(Color.White)` with `TextMMD(color = Color.Black)`
- Undo button uses `ButtonMMD` (no color customization needed)
- All elements: `Modifier.weight(1f).sizeIn(minHeight = 56.dp)` (UI-03)
- `MutableInteractionSource` supplied to `clickable` to satisfy Compose requirement

**GameScreen.kt** (`app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt`) — replaces Plan 01 stub:
- `viewModel: GameViewModel = viewModel()` — lifecycle-aware ViewModel injection
- `collectAsStateWithLifecycle()` for UI state collection
- Two `LaunchedEffect(Unit)` blocks: one for event collection, one for `startGame(Difficulty.EASY)` auto-start
- Layout: `Column` with `windowInsetsPadding(WindowInsets.systemBars)` + `padding(horizontal = 8.dp)`
- Order: `DifficultyBar` → `GameGrid(weight=1f)` → spacer → `ControlsRow` → spacer → `NumberPad` → spacer
- `LoadingScreen`: centered static `TextMMD` only — no spinner, no animation (UI-02)
- `DifficultyBar`: centered `TextMMD` with difficulty name title-cased

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed invalid `weight` import in GameScreen.kt**
- **Found during:** Task 2 verification (compile check)
- **Issue:** `import androidx.compose.foundation.layout.weight` was included explicitly but `Modifier.weight` is a `ColumnScope` extension — not importable as a top-level function. Kotlin compiler rejected it with "Cannot access internal" error.
- **Fix:** Removed the import; `weight` is resolved implicitly within `Column` scope.
- **Files modified:** `GameScreen.kt`
- **Commit:** `202ebcf`

### Design Decisions Applied

**ControlsRow: Box+clickable fallback used for mode toggles (Plan-noted risk)**
- **Reason:** `ButtonMMD.colors` parameter availability is unconfirmed — the MMD AAR cannot be resolved without credentials so compile-time API verification is impossible.
- **Implementation:** `Box + clickable(indication = null, interactionSource = remember { MutableInteractionSource() })` with `background(Color.Black/White)` and `TextMMD(color = Color.White/Black)`.
- **Plan reference:** Plan 02 explicitly documented this fallback with identical code.
- **TextMMD color parameter:** Used as specified in plan fallback; likely valid since TextMMD wraps Material3 Text which accepts `color`. Cannot verify without AAR.

**Compilation verification method:**
- Gradle cannot resolve MMD for `compileDebugKotlin` even with `compileOnly` (classpath configuration requires the AAR to build `friendPathsSet`).
- Workaround: temporarily disabled `compileOnly(libs.mmd)` in main repo `build.gradle.kts` and ran compile.
- Result: ALL errors were MMD unresolved references only (`ButtonMMD`, `TextMMD`, `ThemeMMD`) — identical baseline to Plan 01. No non-MMD Kotlin syntax or type errors.
- `build.gradle.kts` restored to `compileOnly(libs.mmd)` after check.

## Known Stubs

| File | Location | Description | Plan to resolve |
|------|----------|-------------|-----------------|
| `GameScreen.kt` | `LaunchedEffect(Unit) { ... GameEvent.Completed ... }` | Completion event handler is a TODO comment | Phase 5 — Scoring & Completion |
| `GameScreen.kt` | `viewModel.startGame(Difficulty.EASY)` auto-start | Hardcoded EASY difficulty; no difficulty selection screen | Phase 6 — Menu & Navigation |

## ButtonMMD `colors` Parameter Outcome

Used Box+clickable fallback (plan-noted LOW confidence risk) — `ButtonMMD.colors` parameter was NOT tested directly because MMD AAR is unavailable. The fallback implementation achieves identical visual behavior (active=black fill/white text, inactive=white fill/black text) and is E-ink compliant (indication=null, no ripple).

## MMD Import Corrections

No corrections needed — import paths match Plan 01 established convention:
- `import com.mudita.mmd.ButtonMMD`
- `import com.mudita.mmd.TextMMD`

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| Task 1 (feat) | `f50a0d9` | implement GameGrid — Canvas-based 9x9 Sudoku grid |
| Task 2 (feat) | `202ebcf` | implement NumberPad, ControlsRow, and full GameScreen |

## Self-Check

### Files Exist
- `app/src/main/java/com/mudita/sudoku/ui/game/GameGrid.kt` — FOUND
- `app/src/main/java/com/mudita/sudoku/ui/game/NumberPad.kt` — FOUND
- `app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt` — FOUND
- `app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt` — FOUND (stub replaced)

### Commits Exist
- `f50a0d9` feat(03-02): implement GameGrid — FOUND
- `202ebcf` feat(03-02): implement NumberPad, ControlsRow, and full GameScreen — FOUND

## Self-Check: PASSED
