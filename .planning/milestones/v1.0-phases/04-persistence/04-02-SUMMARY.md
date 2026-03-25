---
phase: 04-persistence
plan: 02
subsystem: database
tags: [datastore, viewmodel, stateflow, repository-pattern, tdd, coroutines, persistence]

# Dependency graph
requires:
  - phase: 04-persistence-plan-01
    provides: GameRepository interface, NoOpGameRepository, FakeGameRepository, PersistedGameState DTO
  - phase: 02-game-state-domain
    provides: GameViewModel, GameUiState, applyFill completion detection

provides:
  - GameViewModel with GameRepository injection (constructor param with NoOpGameRepository default)
  - showResumeDialog StateFlow<Boolean> — true when saved game awaiting user decision
  - hasSavedGame() — returns true when pendingSavedState is non-null or dialog is showing
  - resumeGame() — restores saved state, clears undo stack, dismisses dialog
  - startNewGame() — clears saved state via repository, starts Easy game
  - saveNow() — persists current state with isLoading/isComplete/empty-board guards
  - repository.clearGame() called automatically when puzzle is completed
  - ioDispatcher injection for testable async I/O without real Dispatchers.IO

affects:
  - 04-03-PLAN (UI wiring — resume dialog, saveNow on pause, startNewGame on new game)

# Tech tracking
tech-stack:
  added:
    - ioDispatcher: CoroutineDispatcher constructor injection for testable IO
    - UnconfinedTestDispatcher injected in persistence tests for synchronous IO
  patterns:
    - Dispatcher injection pattern: ioDispatcher param with Dispatchers.IO default enables test doubles
    - TDD green: test file written first (RED compile failure), then production code (GREEN all pass)
    - Guard pattern in saveNow(): isLoading || isComplete || board.all{0} returns early

key-files:
  created:
    - app/src/test/java/com/mudita/sudoku/game/GameViewModelPersistenceTest.kt
  modified:
    - app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt

key-decisions:
  - "ioDispatcher injected into GameViewModel constructor (Dispatchers.IO default) — allows UnconfinedTestDispatcher in tests to make withContext(ioDispatcher) calls synchronous, avoiding advanceUntilIdle() races with real thread pools"
  - "Tests that need a loaded game use Turbine awaitItem() sequences (idle -> loading -> loaded) rather than advanceUntilIdle(), because Dispatchers.Default (used by startGame) is a real thread pool not controlled by test scheduler"
  - "hasSavedGame() checks both pendingSavedState and _showResumeDialog.value — handles edge case where dialog is showing but pendingSavedState was already consumed"
  - "saveNow() is a suspend fun (not fire-and-forget) — caller controls when persistence happens; matches lifecycle-aware usage in Plan 03"

patterns-established:
  - "Dispatcher injection: inject CoroutineDispatcher as constructor param with real dispatcher default; tests pass UnconfinedTestDispatcher to synchronize IO"
  - "TDD for ViewModel persistence: write tests against interface contracts first, verify RED (compile fail), then implement GREEN"

requirements-completed: [STATE-01, STATE-02, STATE-03]

# Metrics
duration: 9min
completed: 2026-03-25
---

# Phase 4 Plan 02: ViewModel Persistence Integration Summary

**GameViewModel persistence integration: repository injection, init-time saved state detection, resumeGame/startNewGame/saveNow/hasSavedGame methods, completion-triggered clearGame — 14 TDD persistence tests all passing**

## Performance

- **Duration:** ~9 min
- **Started:** 2026-03-25T01:59:19Z
- **Completed:** 2026-03-25T02:09:10Z
- **Tasks:** 1 (TDD: RED + GREEN)
- **Files modified:** 2

## Accomplishments

- GameViewModel now accepts `GameRepository` and `ioDispatcher` as constructor parameters (both defaulted for backward compatibility with existing callers)
- `showResumeDialog: StateFlow<Boolean>` exposes saved-game detection to the UI layer without leaking persistence implementation details
- `resumeGame()` atomically restores all saved fields (board, solution, givenMask, difficulty, selectedCellIndex, pencilMarks, errorCount) and clears the undo stack per decision D-05
- `saveNow()` is a suspend function with three guards: isLoading, isComplete, and empty board — prevents stale or incomplete state from being persisted
- Game completion automatically calls `repository.clearGame()` alongside the Completed event — no dangling save after the game ends
- 14 TDD persistence tests cover all behaviors; all 38+ pre-existing ViewModel tests still pass unchanged

## Task Commits

Each task was committed atomically:

1. **Task 1: GameViewModel persistence integration + tests (TDD green)** - `3e7dce7` (feat)

## Files Created/Modified

- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` - Added `repository: GameRepository`, `ioDispatcher`, `showResumeDialog`, `pendingSavedState`, `init{}` block, `hasSavedGame()`, `resumeGame()`, `startNewGame()`, `saveNow()`, clearGame on completion
- `app/src/test/java/com/mudita/sudoku/game/GameViewModelPersistenceTest.kt` - 14 persistence behavior tests using FakeGameRepository + UnconfinedTestDispatcher

## Decisions Made

- `ioDispatcher: CoroutineDispatcher = Dispatchers.IO` injected into constructor — the init block and all repository calls use `withContext(ioDispatcher)`. Tests pass `UnconfinedTestDispatcher()` so IO operations complete synchronously within runTest scope, avoiding races with real Dispatchers.IO thread pool.
- Tests that start a game use Turbine `awaitItem()` chains (idle → loading → loaded) rather than `advanceUntilIdle()`. `startGame()` dispatches puzzle generation to `Dispatchers.Default` (real thread pool, not test-controlled), so `advanceUntilIdle()` returns before the board is populated.
- `hasSavedGame()` checks `pendingSavedState != null || _showResumeDialog.value` rather than just the state field — future-proofs against edge cases where the dialog is visible but `pendingSavedState` was already consumed by another path.
- `saveNow()` kept as a `suspend fun` rather than a fire-and-forget launch — the caller (lifecycle observer in Plan 03) can `coroutineScope { saveNow() }` and guarantee completion before the process dies.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Injected ioDispatcher parameter into GameViewModel**
- **Found during:** Task 1 (Green phase — test execution)
- **Issue:** Plan specified `withContext(Dispatchers.IO)` directly in init block and methods. With `UnconfinedTestDispatcher` as Main, `withContext(Dispatchers.IO)` switches to a real thread pool that `advanceUntilIdle()` cannot control. Tests for `showResumeDialog` (init coroutine) and `hasSavedGame()` failed because the init block never completed within the test scheduler.
- **Fix:** Added `ioDispatcher: CoroutineDispatcher = Dispatchers.IO` parameter. All `withContext(Dispatchers.IO)` calls replaced with `withContext(ioDispatcher)`. Tests pass `UnconfinedTestDispatcher()` as `ioDispatcher`.
- **Files modified:** `GameViewModel.kt`, `GameViewModelPersistenceTest.kt`
- **Verification:** All 14 persistence tests pass; all existing tests pass unchanged.
- **Committed in:** `3e7dce7` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (Rule 2 — missing critical: dispatcher injection for testability)
**Impact on plan:** Required for test determinism. No scope creep — production behavior unchanged; `Dispatchers.IO` is still the default.

## Issues Encountered

- Tests checking `startGame`-dependent state (saveNow with loaded board, game completion) initially used `advanceUntilIdle()`. This doesn't work because `startGame` uses `Dispatchers.Default` (real thread pool). Resolved by switching those tests to Turbine `awaitItem()` sequences that naturally block until state emissions arrive.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Plan 03 (UI wiring) can call `viewModel.showResumeDialog.collectAsStateWithLifecycle()` to drive the resume dialog
- Plan 03 can call `viewModel.resumeGame()` or `viewModel.startNewGame()` from dialog button handlers
- Plan 03 can call `viewModel.saveNow()` from a `LifecycleObserver` `onStop` callback
- `DataStoreGameRepository(context.gameDataStore)` is ready to inject in place of `NoOpGameRepository` from Plan 01
- No blockers

---
*Phase: 04-persistence*
*Completed: 2026-03-25*
