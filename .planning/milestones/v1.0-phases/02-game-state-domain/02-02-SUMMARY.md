---
phase: 02-game-state-domain
plan: 02
subsystem: game-logic
tags: [kotlin, viewmodel, stateflow, sharedflow, coroutines, turbine, robolectric, tdd]

# Dependency graph
requires:
  - phase: 02-01
    provides: GameUiState, InputMode, GameAction, GameEvent, FakeGenerator, MainDispatcherRule domain models

provides:
  - GameViewModel class with StateFlow<GameUiState> and SharedFlow<GameEvent>
  - startGame(difficulty) — puzzle generation on Dispatchers.Default with isLoading state transitions
  - selectCell(index) — updates selectedCellIndex in emitted state
  - enterDigit(digit) — fills cells in FILL mode, guards given cells via givenMask
  - toggleInputMode() — flips between FILL and PENCIL
  - undoStack (ArrayDeque<GameAction>) — captures FillCell actions for Plan 03 undo
  - GameViewModelTest covering DIFF-01, DIFF-02, INPUT-01, INPUT-02, INPUT-03

affects: [03-undo-pencil, 03-core-game-ui, 04-persistence, 05-scoring-completion]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "ViewModel accepts generatePuzzle suspend lambda for test injection — avoids SudokuGenerator coupling in tests"
    - "applyPencilMark as stub — deferred to Plan 03; Plan 02 implements FILL path fully"
    - "undoStack lives in ViewModel (not GameUiState) — keeps state immutable, recomposition-safe"
    - "Turbine test { ... } for StateFlow assertions — synchronous coroutine testing pattern"
    - "advanceUntilIdle pattern avoided — use polling loop on StateFlow.value for event-driven completion"

key-files:
  created:
    - app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt
    - app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt
  modified: []

key-decisions:
  - "GameViewModel constructor accepts suspend (Difficulty) -> SudokuPuzzle lambda — allows FakeGenerator injection without subclassing"
  - "applyPencilMark is a no-op stub in Plan 02 — pencil mark logic deferred to Plan 03 per plan spec"
  - "Completion test uses polling on StateFlow.value instead of nested Turbine test blocks — nested test { } blocks cause timeout due to coroutine dispatch ordering"

patterns-established:
  - "ViewModel test: create in @Before with FakeGenerator lambda, use Turbine test { awaitItem() } for each state emission"
  - "startGame pattern: emit isLoading=true synchronously, generate puzzle on Dispatchers.Default, emit fresh GameUiState"

requirements-completed: [DIFF-01, DIFF-02, INPUT-01, INPUT-02, INPUT-03]

# Metrics
duration: 4min
completed: 2026-03-24
---

# Phase 2 Plan 2: GameViewModel Core Actions Summary

**GameViewModel with StateFlow/SharedFlow, startGame/selectCell/enterDigit/toggleInputMode, and 19 passing Turbine-based unit tests covering DIFF-01, DIFF-02, INPUT-01, INPUT-02, INPUT-03**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-24T21:50:16Z
- **Completed:** 2026-03-24T21:54:22Z
- **Tasks:** 1 (TDD: RED + GREEN)
- **Files modified:** 2

## Accomplishments

- Implemented GameViewModel as the central game state machine with injectable puzzle generation lambda
- All 19 GameViewModelTest tests pass covering initial state, startGame with difficulty, selectCell, enterDigit (fill/given/no-selection guards), toggleInputMode, and completion event
- undoStack captures FillCell actions on overwrite for Plan 03 undo implementation
- applyPencilMark stubbed as no-op per plan spec (Plan 03 completes it)

## Task Commits

1. **Task 1: GameViewModel core actions (startGame, selectCell, enterDigit fill, toggleInputMode)** - `767bf05` (feat)

**Plan metadata:** _(see final commit below)_

## Files Created/Modified

- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` — ViewModel with StateFlow/SharedFlow, all core game actions
- `app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt` — 19 unit tests with Turbine and FakeGenerator

## Decisions Made

- GameViewModel constructor accepts `suspend (Difficulty) -> SudokuPuzzle` lambda — this allows `FakeGenerator().generatePuzzle` injection without requiring SudokuGenerator subclassing or Mockk
- `applyPencilMark` is a no-op stub — pencil mark logic is Plan 03's responsibility; stub keeps Plan 02 scope clean
- Completion test uses polling loop on `StateFlow.value` instead of nested Turbine `test { }` blocks — nested blocks caused `TurbineTimeoutCancellationException` due to coroutine dispatch ordering with `UnconfinedTestDispatcher`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed compilation error in completion test — deprecated coroutines `launch` and missing `advanceUntilIdle`**
- **Found during:** Task 1 (initial test compilation)
- **Issue:** Test used top-level `kotlinx.coroutines.launch` (deprecated in test context) and `kotlinx.coroutines.test.advanceUntilIdle` as an unresolved reference outside `runTest` scope
- **Fix:** Rewrote completion test to collect events via Turbine `test { }` block with polling loop on `StateFlow.value` to wait for puzzle load
- **Files modified:** app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt
- **Verification:** BUILD SUCCESSFUL, 19 tests pass
- **Committed in:** 767bf05 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Fix was necessary for compilation; no scope creep.

## Issues Encountered

- `gradlew` script failed with `ClassNotFoundException` — root cause: missing `local.properties` with `sdk.dir` in this worktree. Fixed by creating `local.properties` mirroring the sibling worktree.

## Next Phase Readiness

- GameViewModel is complete and tested; Plan 03 (undo + pencil marks) can add `applyPencilMark` and `undo()` without changing the public API
- All Plan 02 requirements (DIFF-01, DIFF-02, INPUT-01, INPUT-02, INPUT-03) satisfied
- No blockers for Plan 03

---
*Phase: 02-game-state-domain*
*Completed: 2026-03-24*
