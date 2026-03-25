---
phase: 05-scoring-completion
plan: 02
subsystem: game
tags: [viewmodel, scoring, hints, stateflow, tdd, kotlin, coroutines]

requires:
  - phase: 05-01
    provides: ScoreRepository, FakeScoreRepository, CompletionResult, GameEvent.Completed (with full payload), ScoreCalculation, GameUiState.hintCount

provides:
  - GameViewModel.requestHint() — fills one random non-correct cell with solution value, increments hintCount, is permanently non-undoable
  - GameViewModel.handleCompletion() — computes score, checks/saves personal best, refreshes leaderboard, clears game, emits Completed event
  - GameViewModel.leaderboardScores StateFlow — per-difficulty best scores refreshed on init and after completion
  - ControlsRow with 4 buttons (Fill, Pencil, Undo, Get Hint) — Get Hint disabled when no valid hint targets
  - GameScreen.onCompleted callback — routes GameEvent.Completed to caller as CompletionResult
  - GameViewModelHintTest with 15 tests covering hint logic, score computation, personal best detection, non-undoable hints

affects:
  - 05-03 (SummaryScreen consumes CompletionResult from GameScreen.onCompleted)
  - 06 (navigation wires GameScreen.onCompleted to screen transition)

tech-stack:
  added: []
  patterns:
    - Injectable Random for deterministic hint cell selection in tests (Random(seed) vs Random.Default in production)
    - handleCompletion() ordering: saveBestScore -> refreshLeaderboard -> clearGame -> emit event (prevents stale leaderboard)
    - canRequestHint computed inline in Composable from uiState (no extra StateFlow needed)
    - Permanently non-undoable actions: not pushed to undoStack, documented with explicit comment

key-files:
  created:
    - app/src/test/java/com/mudita/sudoku/game/GameViewModelHintTest.kt
  modified:
    - app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt
    - app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt
    - app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt
    - app/src/test/java/com/mudita/sudoku/ui/game/ControlsRowTest.kt

key-decisions:
  - "Injectable Random via constructor (Random.Default in production, Random(seed) in tests) for deterministic hint cell selection"
  - "Hints are permanently non-undoable — not pushed to undoStack; undo() after requestHint() leaves hinted cell filled (locked product decision)"
  - "handleCompletion() ordering: saveBestScore BEFORE refreshLeaderboard BEFORE clearGame — ensures leaderboard reflects new score before game is wiped"
  - "canRequestHint computed as derived boolean in Composable from uiState — avoids extra StateFlow in ViewModel"
  - "ControlsRowTest updated to pass new onHint/canRequestHint params (Rule 3 fix for blocking compilation)"

patterns-established:
  - "Injectable Random pattern: add random: Random = Random.Default to ViewModel constructor for test-deterministic random selection"
  - "Non-undoable action pattern: skip undoStack push, document with explicit comment explaining locked product decision"

requirements-completed:
  - SCORE-03
  - HS-02

duration: 11min
completed: 2026-03-25
---

# Phase 5 Plan 02: Hint Logic + Completion Scoring Summary

**requestHint() with injectable Random fills any non-correct cell, handleCompletion() computes score + personal best via ScoreRepository, ControlsRow adds 4th Get Hint button, GameScreen wires onCompleted callback**

## Performance

- **Duration:** 11 min
- **Started:** 2026-03-25T03:19:39Z
- **Completed:** 2026-03-25T03:30:37Z
- **Tasks:** 2
- **Files modified:** 5 (1 created in production, 1 created in tests, 3 modified)

## Accomplishments

- GameViewModel gains `requestHint()` that fills one random non-correct cell (empty OR wrong-filled), increments hintCount, is permanently non-undoable by design
- `handleCompletion()` replaces the two ad-hoc launch blocks with ordered: saveBestScore -> refreshLeaderboard -> clearGame -> emit Completed event (score + isPersonalBest)
- `leaderboardScores: StateFlow<Map<Difficulty, Int?>>` gives the summary screen per-difficulty bests without additional round-trips
- ControlsRow extended from 3 to 4 buttons; Get Hint disables when all non-given cells are already correct
- GameScreen `onCompleted` replaces `// TODO Phase 5` with real CompletionResult construction from GameEvent.Completed
- 15 new unit tests covering all hint behaviors; 176 total tests pass with 0 failures

## Task Commits

1. **Task 1: GameViewModel hint + completion logic + leaderboardScores (TDD)** - `4faa007` (feat)
2. **Task 2: ControlsRow Hint button + GameScreen wiring** - `c971c03` (feat)

## Files Created/Modified

- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` — Added requestHint(), handleCompletion(), refreshLeaderboard(), leaderboardScores StateFlow, ScoreRepository + Random constructor params
- `app/src/test/java/com/mudita/sudoku/game/GameViewModelHintTest.kt` — 15 tests: hint fills cell, wrong-filled cell, hintCount increments, errorCount unchanged, non-undoable, no-op guards, last-cell completion, score computation, personal best detection, leaderboard update, deterministic random
- `app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt` — Added onHint and canRequestHint params; 4th ButtonMMD for Get Hint
- `app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt` — Added onCompleted callback, canRequestHint derivation, replaced TODO with CompletionResult construction, updated ControlsRow call
- `app/src/test/java/com/mudita/sudoku/ui/game/ControlsRowTest.kt` — Updated 8 ControlsRow instantiation sites with new onHint/canRequestHint params

## Decisions Made

- Injectable Random via constructor for test-deterministic hint selection — avoids test flakiness without compromising production randomness
- Hints permanently non-undoable (not pushed to undoStack) — reverting a hint while hintCount stays elevated would create inconsistent state; documented with explicit comment as locked product decision
- handleCompletion() ordering is strict: saveBestScore -> refreshLeaderboard -> clearGame -> emit event — leaderboard must reflect new score before game is cleared
- canRequestHint computed inline in Composable (not a ViewModel StateFlow) — it's a pure derivation of uiState, adding a StateFlow would duplicate state

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Updated ControlsRowTest to pass new required ControlsRow parameters**

- **Found during:** Task 2 (ControlsRow signature change)
- **Issue:** Existing `ControlsRowTest.kt` called `ControlsRow(...)` without the new `onHint` and `canRequestHint` parameters, causing compilation failure across the full test suite
- **Fix:** Updated all 8 ControlsRow instantiation sites in ControlsRowTest.kt to pass `onHint = {}` and `canRequestHint = true`
- **Files modified:** `app/src/test/java/com/mudita/sudoku/ui/game/ControlsRowTest.kt`
- **Verification:** Full test suite compiles and passes (176 tests, 0 failures)
- **Committed in:** c971c03 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Fix was necessary to compile; no scope creep. ControlsRowTest behavior unchanged.

## Issues Encountered

- Gradle wrapper script fails with "ClassNotFoundException" when Java home path contains spaces (`C:\Program Files\...`) under bash. Workaround: invoke `java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain` directly throughout all build commands.
- `advanceUntilIdle()` used in a plain suspend helper function outside a `TestScope` receiver caused compilation error. Fixed by restructuring the test helper to use Turbine `uiState.test { awaitItem()... }` pattern instead, consistent with existing test patterns.

## Next Phase Readiness

- Phase 5 Plan 03 (SummaryScreen) can now consume `onCompleted: (CompletionResult) -> Unit` from GameScreen
- `leaderboardScores: StateFlow<Map<Difficulty, Int?>>` is available for displaying historical bests on the summary screen
- All hint and scoring mechanics are fully tested and production-ready

---
*Phase: 05-scoring-completion*
*Completed: 2026-03-25*
