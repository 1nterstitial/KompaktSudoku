---
phase: 07-grid-rendering-fixes
plan: 01
subsystem: ui
tags: [canvas, pencil-marks, game-grid, text-rendering, e-ink]

# Dependency graph
requires:
  - phase: 03-core-game-ui
    provides: GameGrid.kt Canvas-based rendering with CellData
  - phase: 02-game-state-domain
    provides: GameViewModel.applyPencilMark pencil mark write path

provides:
  - Pencil marks render white on selected (black background) cells — GRID-01
  - Pencil marks capped at 4 per cell in 2x2 sorted layout with dynamic font — GRID-02
  - 4-mark cap enforced in GameViewModel silently (toggle-off always allowed)

affects: [08-controls-number-pad-fixes, 09-game-navigation]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Dynamic px-to-sp: (cellSizePx / 2f * factor) / density converts runtime pixel to sp for TextStyle"
    - "drawPencilMarks builds TextStyle internally (isSelected + density) instead of accepting external style"
    - "4-mark cap guard: if (wasAdded && currentSet.size >= 4) return before undo push"

key-files:
  created: []
  modified:
    - app/src/main/java/com/mudita/sudoku/ui/game/GameGrid.kt
    - app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt
    - app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt

key-decisions:
  - "[07-01] drawPencilMarks builds TextStyle internally from isSelected+density (D-08) — removes style param, encapsulates all pencil style logic in the function"
  - "[07-01] Dynamic pencil font: (cellSize/2 * 0.60f) / density sp — fills ~60% of each 2x2 slot, device-density-aware"
  - "[07-01] 4-mark cap guard placed before undoStack.addLast — blocked adds leave undo history clean"
  - "[07-01] marks.sorted().forEachIndexed for 2x2 layout — slot index (0-3) maps to positions via i/2 (row) and i%2 (col)"

patterns-established:
  - "Dynamic font from canvas px: capture LocalDensity.current.density in BoxWithConstraints, pass to DrawScope extension; use (px / density).sp for TextStyle"

requirements-completed: [GRID-01, GRID-02]

# Metrics
duration: 7min
completed: 2026-03-27
---

# Phase 7 Plan 01: Grid Rendering Fixes Summary

**White pencil marks on selected cells + 2x2 dynamic layout capped at 4 marks per cell, fixing black-on-black invisibility and 3x3 illegibility on E-ink.**

## Performance

- **Duration:** 7 min
- **Started:** 2026-03-26T23:57:02Z
- **Completed:** 2026-03-27T00:03:33Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- GRID-01: Pencil marks now render Color.White on selected (black-background) cells; Color.Black on all others
- GRID-02: Font size computed dynamically from cellSize (no more hardcoded 9.sp); 2x2 sorted layout (not 3x3) fits readable marks in each slot
- 4-mark cap enforced silently in GameViewModel.applyPencilMark; toggle-off and undo unaffected
- 5 new unit tests covering cap behavior (blocked add, toggle-off at cap, undo after blocked add)

## Task Commits

1. **Task 1: Enforce 4-mark cap in GameViewModel.applyPencilMark** - `aba9c22` (feat)
2. **Task 2: Fix pencil mark rendering - white on selected + 2x2 dynamic layout** - `137ddbf` (fix)

## Files Created/Modified

- `app/src/main/java/com/mudita/sudoku/ui/game/GameGrid.kt` — Removed pencilStyle, added density capture, updated drawPencilMarks call site, replaced drawPencilMarks with 2x2 dynamic implementation
- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` — Added 4-mark cap guard in applyPencilMark
- `app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt` — Added 5 new pencil mark cap tests (GRID-02)

## Decisions Made

- Dynamic pencil font uses `(cellSize / 2f * 0.60f) / density` converting runtime pixels to sp. The 0.60f multiplier fills ~60% of each 2x2 slot; tunable on device (reduce to 0.55f if marks clip).
- `drawPencilMarks` now owns its style completely — callers pass `isSelected` and `density`, function builds `TextStyle` internally. Aligns with D-08 and removes ambiguity about which style is active.
- `marks.sorted()` with `forEachIndexed` renders only marks present in sorted order (max 4 after cap); slot position is index-based not digit-based.

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None — both rendering and cap enforcement are fully wired.

## Self-Check: PASSED

Files exist:
- `app/src/main/java/com/mudita/sudoku/ui/game/GameGrid.kt` — FOUND
- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` — FOUND
- `app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt` — FOUND

Commits exist:
- `aba9c22` feat(07-01): enforce 4-mark cap — FOUND
- `137ddbf` fix(07-01): fix pencil mark rendering — FOUND
