---
phase: 08-controls-number-pad-fixes
plan: "01"
subsystem: ui
tags: [compose, controls, number-pad, e-ink, mmd, font]

requires:
  - phase: 03-core-game-ui
    provides: ControlsRow.kt and NumberPad.kt established with Box+clickable pattern

provides:
  - ControlsRow with 1dp black border frame grouping Fill+Pencil buttons (CTRL-04)
  - Inactive mode button mid-gray (#E0E0E0) background distinct from active black (CTRL-03)
  - Two-line centered "Get Hint" label (CTRL-02)
  - NumberPad digit buttons using sans-serif-condensed font for vertical centering (CTRL-01)

affects: [09-game-navigation]

tech-stack:
  added: []
  patterns:
    - "sizeIn(minHeight=56.dp) on inner Boxes instead of fillMaxHeight() — avoids unbounded height in Row without fixed max height (critical for Robolectric compatibility)"
    - "androidx.compose.ui.graphics.RectangleShape is the correct import (not foundation.shape)"
    - "TextMMD accepts fontFamily parameter — confirmed via AAR javap inspection"

key-files:
  created: []
  modified:
    - app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt
    - app/src/main/java/com/mudita/sudoku/ui/game/NumberPad.kt

key-decisions:
  - "sizeIn(minHeight=56.dp) on inner Boxes (not fillMaxHeight) — fillMaxHeight requires bounded parent height; Row with only sizeIn constraint is unbounded; Robolectric test showed number pad pushed off screen"
  - "RectangleShape import is androidx.compose.ui.graphics.RectangleShape not foundation.shape — plan code had wrong package"
  - "TextMMD fontFamily parameter confirmed working in MMD 1.0.1 — primary path succeeds; BasicText fallback not needed"
  - "Fill+Pencil border frame uses weight(2f) on outer group row + weight(1f) per box — preserves 4-equal-part width distribution"

patterns-established:
  - "Use sizeIn(minHeight=X) not fillMaxHeight() for Box elements inside Row without fixed height — prevents infinite constraint propagation in both Compose layout and Robolectric tests"
  - "TextMMD fontFamily parameter is available in MMD 1.0.1 AAR — document as confirmed convention"

requirements-completed: [CTRL-01, CTRL-02, CTRL-03, CTRL-04]

duration: 11min
completed: 2026-03-27
---

# Phase 08 Plan 01: Controls and Number Pad Visual Fixes Summary

**ControlsRow gets 1dp border frame grouping Fill/Pencil with mid-gray inactive state and two-line Get Hint; NumberPad digits use sans-serif-condensed (Roboto Condensed) for better vertical centering**

## Performance

- **Duration:** 11 min
- **Started:** 2026-03-27T01:25:00Z
- **Completed:** 2026-03-27T01:36:00Z
- **Tasks:** 2 (Tasks 1 and 2 committed together as one atomic fix)
- **Files modified:** 2

## Accomplishments

- ControlsRow: Fill and Pencil buttons now share a rectangular 1dp black border frame that visually separates them from Undo and Get Hint (CTRL-04)
- ControlsRow: Inactive mode button background changed from white to `Color(0xFFE0E0E0)` mid-gray; active remains solid black with white text (CTRL-03)
- ControlsRow: "Get Hint" label changed to `"Get\nHint"` with `textAlign = TextAlign.Center` for two-line centered rendering (CTRL-02)
- NumberPad: Digit buttons use `FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))` for taller/narrower Roboto Condensed glyphs that center better vertically (CTRL-01)
- All 207 tests pass; build compiles clean

## Task Commits

1. **Tasks 1+2: ControlsRow + NumberPad visual fixes** - `c3f1fcc` (fix)

## Files Created/Modified

- `app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt` - Added border frame for Fill+Pencil group, mid-gray inactive background, two-line Get Hint label
- `app/src/main/java/com/mudita/sudoku/ui/game/NumberPad.kt` - Added sans-serif-condensed FontFamily to TextMMD digit labels

## Decisions Made

- `sizeIn(minHeight=56.dp)` on inner Boxes instead of `fillMaxHeight()` — `fillMaxHeight()` requires the parent Row to have a bounded maximum height; a Row with only `sizeIn(minHeight=56.dp)` has no upper bound, causing infinite constraint propagation that pushed the NumberPad off screen in Robolectric tests
- `RectangleShape` is from `androidx.compose.ui.graphics.RectangleShape`, not `androidx.compose.foundation.shape` — confirmed via compile error; plan had wrong import package
- TextMMD `fontFamily` parameter is available in MMD 1.0.1 — confirmed via javap decompilation of `classes.jar` in AAR; primary path (TextMMD with fontFamily) works; BasicText fallback not needed

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed wrong RectangleShape import package**
- **Found during:** Task 1 (ControlsRow implementation)
- **Issue:** Plan specified `import androidx.compose.foundation.shape.RectangleShape` but Kotlin compiler reported "Unresolved reference: RectangleShape" — the correct package is `androidx.compose.ui.graphics`
- **Fix:** Changed import to `androidx.compose.ui.graphics.RectangleShape`
- **Files modified:** `app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt`
- **Verification:** Build compiles successfully
- **Committed in:** c3f1fcc (task commit)

**2. [Rule 1 - Bug] Replaced fillMaxHeight() with sizeIn(minHeight=56.dp) on inner Boxes**
- **Found during:** Task 1 (ControlsRow verification — GameScreenTest failure)
- **Issue:** Plan code used `fillMaxHeight()` on Boxes inside the bordered Row. The bordered Row had no bounded maximum height (only `sizeIn(minHeight=56.dp)`). When `fillMaxHeight()` is called in an unbounded-height parent, Compose Row becomes infinitely tall in Robolectric, pushing NumberPad below the visible area. `GameScreen shows number pad after puzzle loads` test asserted "is not displayed".
- **Fix:** Removed `fillMaxHeight()` from both inner Boxes; added `sizeIn(minHeight=56.dp)` directly to each Box instead. Removed `fillMaxHeight` import. The border frame on the outer Row still renders correctly since the Row expands to fit the tallest child.
- **Files modified:** `app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt`
- **Verification:** GameScreenTest passes; full 207-test suite green; build compiles
- **Committed in:** c3f1fcc (task commit)

---

**Total deviations:** 2 auto-fixed (2 Rule 1 bugs)
**Impact on plan:** Both auto-fixes necessary for correct compilation and layout behavior. No scope creep.

## Issues Encountered

- Local `local.properties` was missing from worktree (gitignored file not shared between worktrees) — copied from main repo to enable Gradle build. This is environment setup, not a code issue.

## Known Stubs

None — all four CTRL requirements are fully implemented with production-ready code.

## Next Phase Readiness

- Phase 09 (Game Navigation) is unblocked — ControlsRow and NumberPad are complete
- CLAUDE.md convention to document: `TextMMD fontFamily parameter works in MMD 1.0.1 (confirmed)` and `sizeIn(minHeight) preferred over fillMaxHeight() in Row without fixed height`

---
*Phase: 08-controls-number-pad-fixes*
*Completed: 2026-03-27*
