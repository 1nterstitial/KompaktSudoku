# Phase 7: Grid Rendering Fixes - Context

**Gathered:** 2026-03-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Fix two pencil mark rendering bugs in `GameGrid.kt` and enforce a cap in `GameViewModel.kt`:
1. Pencil mark digits are invisible on selected cells (black text on black background) — fix by using white color when selected (GRID-01)
2. Pencil marks are too small to read because the font is hardcoded and the layout tries to fit 9 marks — fix by capping at 4 marks per cell in a 2×2 layout with a dynamic font size (GRID-02)

No new gameplay features. No new screens. No changes outside `GameGrid.kt` and `GameViewModel.kt`.

</domain>

<decisions>
## Implementation Decisions

### GRID-01: Pencil mark color on selected cells
- **D-01:** Pencil mark digits display in `Color.White` when `isSelected == true`, `Color.Black` otherwise.
- **D-02:** The `drawPencilMarks` function must receive the cell's `isSelected` state and choose the appropriate color. Do NOT use a single shared `pencilStyle` TextStyle — define `pencilStyleNormal` (black) and `pencilStyleSelected` (white) at the composable level, pass the correct one per cell.
- **D-03:** This applies ONLY to the selected cell (the one with a solid black background). No other cells are affected.

### GRID-02: Pencil mark layout — 2×2 cap, dynamic font size
- **D-04:** Maximum 4 pencil marks per cell (changed from unlimited up to 9). The layout inside the cell is a 2×2 grid (2 columns, 2 rows).
- **D-05:** Cap enforcement lives in `GameViewModel.applyPencilMark()`. If `pencilMarks[index].size >= 4` AND the digit is NOT already in the set → no-op (silent block, no state change). Toggle-off (digit already in set → remove it) is ALWAYS allowed regardless of cap.
- **D-06:** The 4 displayed marks are sorted by digit value (ascending). Position mapping: sorted[0] = top-left, sorted[1] = top-right, sorted[2] = bottom-left, sorted[3] = bottom-right.
- **D-07:** Font size is computed dynamically from `cellSizePx`: target `(cellSizePx / 2f) * 0.60f` as initial value (fills ~60% of each 2×2 slot). This replaces the hardcoded `9.sp`. The font size is passed as `fontSize = dynamicPencilFontSp.sp` into the TextStyle where `dynamicPencilFontSp = (cellSizePx / 2f * 0.60f) / density` (convert px → sp).
- **D-08:** The `drawPencilMarks` function signature changes to accept `cellSize: Float` (to compute font size) and remove the `style: TextStyle` parameter — it now builds its own style internally from `cellSize` and `isSelected`.

### Claude's Discretion
- Exact multiplier tuning (0.60f is the target — if marks clip on device, reduce to 0.55f).
- Whether to add a Compose Preview showing the 2×2 layout for visual verification.
- Test coverage strategy for the 4-mark cap in `GameViewModelTest`.

</decisions>

<specifics>
## Specific Ideas

- "The 3×3 board layout stays the same (9×9 grid, 9 cells per box) — only the pencil marks within each individual cell change from 3×3 to 2×2."
- The user's intent: pencil marks were too small because the old code tried to squeeze 9 marks. By capping at 4, each mark gets a much larger slot (half the cell dimension per axis), making them readable.
- The 5th-mark block is completely silent — no toast, no vibration, no visual feedback. The mark simply doesn't appear.

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` — GRID-01 (pencil mark color) and GRID-02 (font size cap) definitions

### Roadmap
- `.planning/ROADMAP.md` — Phase 7 goal, success criteria, and requirement IDs

### Files being modified
- `app/src/main/java/com/mudita/sudoku/ui/game/GameGrid.kt` — Canvas rendering: `drawPencilMarks()`, `pencilStyle` TextStyle, `CellData` usage
- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` — `applyPencilMark()` function where the 4-mark cap must be enforced

### Test file to update
- `app/src/test/java/com/mudita/sudoku/ui/game/GameGridTest.kt` — Compose UI tests for grid rendering
- `app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt` — Unit tests for pencil mark cap behavior

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `CellData.isSelected: Boolean` — already carries selection state into the Canvas; no ViewModel changes needed to pass selection into rendering
- `pencilMarks: Set<Int>` in `CellData` — already carries the full mark set; cap enforcement changes which marks reach the set, not how `CellData` is structured

### Established Patterns
- TextStyle defined at composable level, captured by Canvas lambda (established in Phase 3): follow same pattern for `pencilStyleNormal` and `pencilStyleSelected`
- Font size via `TextStyle(fontSize = N.sp)` — use `(dynamicPx / density).sp` to convert runtime pixel value to SP units
- `_uiState.update { it.copy(pencilMarks = newMarks) }` pattern with `newMarks.copyOf()` before mutation — mandatory for immutable state updates

### Integration Points
- `GameViewModel.applyPencilMark()` is the sole write path for pencil mark state — cap logic goes here only
- `drawPencilMarks()` is a private `DrawScope` extension function — self-contained change, no callers to update beyond the single call site in the Canvas block
- `pencilStyle` TextStyle defined at line 96 in `GameGrid.kt` — replace with two styles or move font computation inside `drawPencilMarks`

</code_context>

<deferred>
## Deferred Ideas

- Showing which marks are "impossible" given the current board state — new capability, separate phase
- Pencil mark auto-clear when a digit is filled — separate feature request

</deferred>

---

*Phase: 07-grid-rendering-fixes*
*Context gathered: 2026-03-26*
