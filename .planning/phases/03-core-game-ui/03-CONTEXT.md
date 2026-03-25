# Phase 3: Core Game UI - Context

**Gathered:** 2026-03-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Build the playable game screen — the 9×9 Sudoku grid, number pad, mode controls, and undo button — wired to GameViewModel and rendered correctly on the Mudita Kompakt E-ink display with no ghosting, full MMD compliance, and ≥ 56dp touch targets.

This phase does NOT include: pause/resume persistence, scoring summary, hint logic, navigation between screens, or the main menu. Those belong in later phases.

</domain>

<decisions>
## Implementation Decisions

### Grid & Cell Visuals

- **D-01:** Box borders thick (2–3dp), individual cell borders thin (1dp). Classic thick+thin Sudoku look. 3×3 box boundaries must be clearly heavier than inter-cell lines.
- **D-02:** Selected cell rendered with a solid black fill and white digit. Maximum contrast — no inverted-row/column highlighting (too complex, risks E-ink partial-refresh artifacts).
- **D-03:** Given (pre-filled) cells use bold/heavy typeface. Player-entered digits use regular weight. Typographic distinction only — no background fill difference between given and player cells.
- **D-04:** Error cells (wrong digit) — Claude's discretion on treatment; keep it subtle given that errors are silent during play per SCORE-01.

### Number Pad

- **D-05:** Digit buttons arranged in a single horizontal row of 9 (1–9), spanning the full screen width. Each button ≥ 56dp tall.
- **D-06:** A dedicated Erase (or ×) button is included alongside the digit row to clear the selected cell's digit or pencil marks.

### Screen Composition

- **D-07:** Vertical layout stack (top to bottom):
  1. Slim top bar — difficulty label
  2. 9×9 grid — largest area, fills remaining space
  3. Controls row — Fill/Pencil mode toggle + Undo button
  4. Number pad row — 1–9 + Erase

  Navigation bar padding (~20dp) is accounted for at the bottom.

- **D-08:** Mode toggle is two side-by-side ButtonMMD buttons ("Fill" / "Pencil"). The active mode is visually distinguished (inverted or bordered) so current mode is always scannable at a glance.

### MMD Integration

- **D-09:** MMD library (version 1.0.1) must be added to `app/build.gradle.kts` as an `implementation` dependency — it is in the version catalog (`libs.mmd`) but not yet declared in the app module. This must happen before any UI work.
- **D-10:** `MainActivity` must be upgraded from a bare `Activity` to a `ComponentActivity` with `setContent { ThemeMMD { GameScreen() } }`. All Compose content lives inside `ThemeMMD`.

### Claude's Discretion

- Pencil mark display within cells (mini 3×3 grid vs compact list) — user deferred; Claude decides the most readable approach for the E-ink display.
- Erase button placement (at end of pad row, or separate row) — Claude decides based on 56dp constraint fit.
- Loading state (when `isLoading = true`) — show a centered TextMMD "Generating puzzle…" or similar placeholder while puzzle generation completes.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### MMD Library
- `https://github.com/mudita/MMD` — MMD component list, ThemeMMD usage, ButtonMMD, TextMMD API. Verify component names against 1.0.1 release.

### Phase 2 Domain Models (integration points)
- `app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt` — All state the UI reads (board, givenMask, selectedCellIndex, inputMode, pencilMarks, errorCount, isComplete, isLoading)
- `app/src/main/java/com/mudita/sudoku/game/model/InputMode.kt` — FILL / PENCIL enum
- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` — startGame, selectCell, enterDigit, toggleInputMode, applyPencilMark, undo

### Project Constraints
- `CLAUDE.md` §Technology Stack — MMD 1.0.1, Compose BOM 2026.03.00, ripple disabled in ThemeMMD (do not re-enable), eInkColorScheme only, ButtonMMD/TextMMD throughout
- `.planning/REQUIREMENTS.md` — UI-01 (MMD only), UI-02 (no animations/ripple), UI-03 (≥56dp touch targets)

### Phase 3 Success Criteria
- `.planning/ROADMAP.md` §Phase 3 — 4 success criteria: MMD-only components, no animations on hardware, ≥56dp targets, no ghosting after 30+ interactions

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `GameViewModel`: Fully implemented — UI observes `uiState: StateFlow<GameUiState>` and collects `events: SharedFlow<GameEvent>`. Wire via `collectAsStateWithLifecycle`.
- `GameUiState`: All fields needed for rendering are present (board, givenMask, selectedCellIndex, inputMode, pencilMarks, errorCount, isComplete, isLoading).

### Established Patterns
- MVVM + StateFlow pattern is locked in. Screen composable observes ViewModel state; user actions call ViewModel methods.
- `@Stable` annotation on cell data classes + `key(cellIndex)` in grid loop — required to prevent whole-grid recomposition on every cell selection change (noted risk in STATE.md).

### Integration Points
- `MainActivity.kt` — currently a bare `android.app.Activity`. Must become `ComponentActivity` with Compose content wired in this phase.
- MMD not yet in `app/build.gradle.kts` — `implementation(libs.mmd)` must be added.

</code_context>

<specifics>
## Specific Ideas

- The thick+thin border grid style was explicitly selected by user via visual preview — the ASCII mockup shown was accepted as the target aesthetic.
- Solid black fill for selected cells was explicitly chosen — not a subtle highlight.
- Row-of-9 number pad layout was chosen via visual preview — single horizontal strip, not 3×3 phone-style numpad.
- Screen stacking order confirmed via mockup: difficulty label → grid → controls row (Fill/Pencil + Undo) → number pad.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

<design_clarifications>
## Design Clarifications (Post-Review)

Added 2026-03-25 in response to cross-AI plan review feedback.

### Canvas Compliance Clarification (UI-01)

**Review concern:** "Canvas-based 9x9 grid may conflict with success criterion 1 if cells are rendered via custom drawing rather than MMD components."

**Resolution:** The Canvas-based grid IS compliant with UI-01. The requirement "All UI components are built using the MMD library wrapped in ThemeMMD" means:
- All Compose content lives inside a `ThemeMMD { }` wrapper (verified: MainActivity.kt)
- Interactive controls (buttons, toggles, text) use `ButtonMMD` and `TextMMD` (verified: NumberPad.kt, ControlsRow.kt)
- The grid is rendered via a `Canvas` composable inside a ThemeMMD-scoped screen. Canvas is a standard Compose primitive for custom drawing — it inherits the ThemeMMD color scheme and typography context. There is no `ButtonMMD`-equivalent for a 9x9 game grid; using 81 individual ButtonMMD composables would be a performance anti-pattern on E-ink hardware (81 recompositions per selection change).
- The "MMD-only" requirement prohibits non-MMD themed widgets (e.g., Material3 `Button` instead of `ButtonMMD`, Material3 `Text` instead of `TextMMD`). It does not prohibit standard Compose layout and drawing primitives (`Canvas`, `Box`, `Column`, `Row`) which have no MMD equivalent and are required for any Compose UI.

### 56dp Touch Target Scope Clarification (UI-03)

**Review concern:** "56dp touch targets for 81 grid cells physically impossible on 800x480 display."

**Resolution:** UI-03 ("All interactive touch targets are at minimum 56dp") applies to **interactive controls**: digit buttons (1-9), Erase button, Fill/Pencil mode toggle, Undo button, and any future menu buttons. These are all verified at >=56dp via `assertHeightIsAtLeast(56.dp)` tests.

Grid cell tap targets are **physically constrained** by the device dimensions and the 9x9 board geometry:
- 800x480 display at ~160dpi = 500x300dp available (minus system bars and padding)
- Square grid constrained by shorter dimension: ~280dp per side
- Cell size: ~280dp / 9 = ~31dp per cell

31dp per cell is the physical maximum for a 9x9 grid on this display. This is a fundamental geometric constraint, not a design choice. The 56dp minimum applies to discrete interactive controls where sizing is a design decision, not to game board cells where sizing is dictated by the board geometry and display dimensions.

All Sudoku apps on constrained displays (including LibreSudoku, the reference Compose Sudoku app) use similar cell sizes. The E-ink display's capacitive touch layer is calibrated for this scale.

</design_clarifications>

---

*Phase: 03-core-game-ui*
*Context gathered: 2026-03-24*
