---
phase: 02-game-state-domain
plan: 03
subsystem: game
tags: [kotlin, viewmodel, stateflow, turbine, tdd, undo, pencil-marks, error-tracking, completion]

# Dependency graph
requires:
  - phase: 02-game-state-domain
    plan: 01
    provides: GameUiState, GameAction, GameEvent domain models
  - phase: 02-game-state-domain
    plan: 02
    provides: GameViewModel with startGame, selectCell, enterDigit, toggleInputMode, applyFill stub

provides:
  - GameViewModel.undo() with LIFO ArrayDeque-backed undo stack supporting FillCell and SetPencilMark actions
  - GameViewModel.applyPencilMark() with toggle semantics (add if absent, remove if present)
  - Comprehensive test suite: 20 new tests covering INPUT-04, INPUT-05, SCORE-01, SCORE-02
  - Full Phase 2 test suite (all 9 requirements tested and passing)

affects: [03-core-game-ui, 05-scoring-completion]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "TDD: RED (failing test compile error) -> GREEN (minimal impl) -> tests pass"
    - "Undo stack as ArrayDeque<GameAction> in ViewModel only (not in immutable state)"
    - "wasAdded flag on SetPencilMark encodes toggle direction for O(1) undo"
    - "allCorrect recheck in undo FillCell branch maintains isComplete invariant"
    - "errorCount never decremented on undo (permanent silent tracking per SCORE-01)"

key-files:
  created: []
  modified:
    - app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt
    - app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt

key-decisions:
  - "applyPencilMark wasAdded=true when digit NOT in set (gets added) — undo removes it; wasAdded=false when digit already present (gets removed) — undo adds it back"
  - "undo FillCell rechecks allCorrect to restore isComplete=false when last correct fill is undone"
  - "errorCount is permanent on undo: errors are counted once, never decremented (SCORE-01 design)"
  - "applyFill already pushed to undoStack in Plan 02; Plan 03 only adds undo() consumer and pencil mark producer"

patterns-established:
  - "Toggle undo: store wasAdded flag at action time, reverse it on undo — no need to re-read set"
  - "Completion recheck on undo: board.indices.all { i -> board[i] == solution[i] } is cheap (81 elements)"

requirements-completed: [INPUT-04, INPUT-05, SCORE-01, SCORE-02]

# Metrics
duration: 8min
completed: 2026-03-24
---

# Phase 2 Plan 03: Pencil Marks, Undo, and Completion Tests Summary

**Pencil mark toggling, LIFO undo stack (FillCell + SetPencilMark), silent error counting, and automatic completion detection — all Phase 2 requirements tested and passing**

## Performance

- **Duration:** 8 min
- **Started:** 2026-03-24T21:56:34Z
- **Completed:** 2026-03-24T22:04:00Z
- **Tasks:** 1
- **Files modified:** 2

## Accomplishments

- Implemented `applyPencilMark` with toggle semantics: adds digit if absent, removes if present, pushes `SetPencilMark` onto undo stack
- Implemented `undo()` as a public method with LIFO `ArrayDeque.removeLast()` behavior for both `FillCell` and `SetPencilMark` actions
- Added 20 new tests covering all Phase 2 requirements (pencil marks, undo, error tracking, completion)
- Full test suite (Phase 1 + Phase 2, all tests) passes: `BUILD SUCCESSFUL`

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement applyPencilMark and undo, add comprehensive tests** - `59d5f2a` (feat)

**Plan metadata:** (to be recorded in final commit)

## Files Created/Modified

- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` - Added `fun undo()` and replaced stub `applyPencilMark` with full implementation
- `app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt` - Added 20 tests: pencilMark (6), undo (7), errorTracking (4), completion (3)

## Decisions Made

- `wasAdded=true` when digit was NOT in set (it gets added; undo should remove it); `wasAdded=false` when digit WAS present (it gets removed; undo should add back) — encoding in the action at write time avoids re-reading state on undo
- FillCell undo rechecks `allCorrect` via full board scan (81 elements, cheap) to properly reset `isComplete=false` after undoing the last correct fill
- errorCount is permanently incremented on wrong fills and never decremented on undo — this is intentional per SCORE-01 (silent error tracking counts mistakes made, not current board state)
- `applyFill` was already pushing to `undoStack` in Plan 02 (the comment said "Plan 03"); Plan 03 only adds the `undo()` consumer and pencil mark producer

## Deviations from Plan

None - plan executed exactly as written. TDD RED phase confirmed compilation failure (unresolved reference `undo`). GREEN phase passed all 27 tests on first attempt.

## Issues Encountered

- Gradle wrapper JAR was missing from worktree (worktrees don't automatically copy the binary JAR). Copied from main repo to enable builds. Local.properties also needed to be created with Android SDK path.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 2 is complete. All 9 requirements (DIFF-01, DIFF-02, INPUT-01 through INPUT-05, SCORE-01, SCORE-02) have tests and passing implementation.
- Phase 3 (Core Game UI) can proceed: GameViewModel has all public methods (startGame, selectCell, enterDigit, toggleInputMode, undo) needed by the Compose UI layer.
- No blockers.

---
## Self-Check: PASSED

- FOUND: app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt
- FOUND: app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt
- FOUND: .planning/phases/02-game-state-domain/02-03-SUMMARY.md
- FOUND: commit 59d5f2a (feat(02-03))
- Full test suite: BUILD SUCCESSFUL

---
*Phase: 02-game-state-domain*
*Completed: 2026-03-24*
