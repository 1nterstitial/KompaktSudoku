---
phase: 02-game-state-domain
plan: 01
subsystem: game-domain
tags: [kotlin, stateflow, mvvm, sealed-class, gamestate, unit-testing, coroutines-test]

# Dependency graph
requires:
  - phase: 01-puzzle-engine
    provides: Difficulty enum, SudokuPuzzle data class, SudokuGenerator

provides:
  - GameUiState data class — immutable snapshot of all game UI state
  - InputMode enum — FILL and PENCIL input modes
  - GameAction sealed class — undo-supporting action variants (FillCell, SetPencilMark)
  - GameEvent sealed class — one-shot completion event (Completed)
  - MainDispatcherRule — JUnit 4 rule for Dispatchers.Main replacement in ViewModel tests
  - FakeGenerator — deterministic puzzle generator for fast ViewModel and game-logic tests
  - GameUiStateTest — 22 passing unit tests covering equality, hashCode, and all model types

affects: [02-02, 02-03, 03-core-game-ui, 04-persistence, 05-scoring]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Data class with manual equals/hashCode using contentEquals/contentDeepEquals for IntArray/BooleanArray/Array fields"
    - "Sealed class for domain actions (GameAction) and one-shot events (GameEvent)"
    - "JUnit 4 TestWatcher rule pattern for Dispatchers.Main replacement"
    - "Companion object test helpers (emptyIndices, correctDigitAt, wrongDigitAt) for deterministic test data"

key-files:
  created:
    - app/src/main/java/com/mudita/sudoku/game/model/InputMode.kt
    - app/src/main/java/com/mudita/sudoku/game/model/GameAction.kt
    - app/src/main/java/com/mudita/sudoku/game/model/GameEvent.kt
    - app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt
    - app/src/test/java/com/mudita/sudoku/game/MainDispatcherRule.kt
    - app/src/test/java/com/mudita/sudoku/game/FakeGenerator.kt
    - app/src/test/java/com/mudita/sudoku/game/GameUiStateTest.kt
  modified: []

key-decisions:
  - "GameUiState stores undoStack outside the data class — undo history is mutable and belongs in GameViewModel, not in immutable UI state"
  - "FakeGenerator is a standalone class (not a subclass of SudokuGenerator) to avoid modifying Phase 1 sealed types; GameViewModel will accept a generator lambda in Plan 02"
  - "Array<Set<Int>> for pencilMarks requires contentDeepEquals/contentDeepHashCode — standard equals is reference-based for arrays"
  - "gradle-wrapper.jar was missing from worktree; copied from main repo — this is a worktree setup issue, no project files modified"

patterns-established:
  - "Array field equality pattern: board.contentEquals(other.board), pencilMarks.contentDeepEquals(other.pencilMarks)"
  - "Test helper companion objects: SOLUTION/BOARD constants + emptyIndices/correctDigitAt/wrongDigitAt helpers"

requirements-completed: [DIFF-01, INPUT-01, INPUT-03, INPUT-04, INPUT-05, SCORE-01, SCORE-02]

# Metrics
duration: 5min
completed: 2026-03-24
---

# Phase 2 Plan 1: Game State Domain Models Summary

**GameUiState data class with content-based array equality, InputMode/GameAction/GameEvent sealed types, and MainDispatcherRule + FakeGenerator test infrastructure — 22 tests green**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-24T21:39:55Z
- **Completed:** 2026-03-24T21:44:55Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- GameUiState data class with 10 fields and correct equals/hashCode using contentEquals/contentDeepEquals for IntArray, BooleanArray, and Array<Set<Int>> fields
- InputMode (FILL/PENCIL), GameAction (FillCell/SetPencilMark), GameEvent (Completed) sealed types establishing all contracts Plans 02 and 03 build against
- MainDispatcherRule and FakeGenerator providing test infrastructure for ViewModel tests in Plans 02 and 03

## Task Commits

Each task was committed atomically:

1. **Task 1: Create domain model files + GameUiStateTest** - `d3b9511` (feat)
2. **Task 2: Create test infrastructure (MainDispatcherRule, FakeGenerator)** - `12317ab` (chore)

**Plan metadata:** (docs commit after SUMMARY + STATE update)

_Note: Task 1 followed TDD — test file written before production files, then GREEN phase verified with `./gradlew :app:testDebugUnitTest --tests "*.GameUiStateTest"`_

## Files Created/Modified

- `app/src/main/java/com/mudita/sudoku/game/model/InputMode.kt` — Enum with FILL and PENCIL values
- `app/src/main/java/com/mudita/sudoku/game/model/GameAction.kt` — Sealed class for undo actions (FillCell, SetPencilMark)
- `app/src/main/java/com/mudita/sudoku/game/model/GameEvent.kt` — Sealed class for one-shot events (Completed)
- `app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt` — Immutable game state data class with content-based equals/hashCode
- `app/src/test/java/com/mudita/sudoku/game/MainDispatcherRule.kt` — JUnit 4 TestWatcher for Dispatchers.Main replacement
- `app/src/test/java/com/mudita/sudoku/game/FakeGenerator.kt` — Deterministic puzzle generator with 61 givens, 20 empty cells, and test helpers
- `app/src/test/java/com/mudita/sudoku/game/GameUiStateTest.kt` — 22 unit tests for GameUiState equality, defaults, and model contracts

## Decisions Made

- GameUiState does not store undoStack — undo history is mutable and belongs in GameViewModel, not in immutable UI state. This was already specified in the plan.
- FakeGenerator is a standalone class rather than a SudokuGenerator subclass — avoids modifying Phase 1 production code. GameViewModel will accept a generator function type in Plan 02 to enable injection.
- Array<Set<Int>> for pencilMarks requires contentDeepEquals/contentDeepHashCode for correct structural equality — documented in GameUiState.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] gradle-wrapper.jar missing from worktree**
- **Found during:** Task 1 verification (running tests)
- **Issue:** The worktree was created without gradle/wrapper/gradle-wrapper.jar — gradlew script cannot execute without it
- **Fix:** Copied gradle-wrapper.jar from the main repo at /D/Development/Claude/gradle/wrapper/gradle-wrapper.jar
- **Files modified:** gradle/wrapper/gradle-wrapper.jar (not tracked in git — already in .gitignore)
- **Verification:** `./gradlew :app:testDebugUnitTest` succeeded after copy
- **Committed in:** Not committed (generated file, part of .gitignore)

Also needed local.properties (sdk.dir) — copied from main repo, also not tracked.

---

**Total deviations:** 1 environment setup (blocking)
**Impact on plan:** Environment-only fix; no production or test source code changes outside the plan.

## Issues Encountered

- Worktree was missing gradle-wrapper.jar and local.properties — resolved by copying from main repo. All test runs succeeded after this.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- All 4 domain model contracts (GameUiState, InputMode, GameAction, GameEvent) are defined with exact field signatures
- MainDispatcherRule and FakeGenerator provide test infrastructure for Plans 02 and 03
- Plan 02 can proceed to implement GameViewModel against these contracts
- FakeGenerator injection into GameViewModel (lambda or interface) will be decided in Plan 02

## Self-Check: PASSED

All expected files exist and both commits are present in git log.

- FOUND: app/src/main/java/com/mudita/sudoku/game/model/InputMode.kt
- FOUND: app/src/main/java/com/mudita/sudoku/game/model/GameAction.kt
- FOUND: app/src/main/java/com/mudita/sudoku/game/model/GameEvent.kt
- FOUND: app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt
- FOUND: app/src/test/java/com/mudita/sudoku/game/MainDispatcherRule.kt
- FOUND: app/src/test/java/com/mudita/sudoku/game/FakeGenerator.kt
- FOUND: app/src/test/java/com/mudita/sudoku/game/GameUiStateTest.kt
- COMMIT d3b9511: feat(02-01): add domain model contracts and GameUiState unit tests
- COMMIT 12317ab: chore(02-01): add test infrastructure (MainDispatcherRule, FakeGenerator)

---
*Phase: 02-game-state-domain*
*Completed: 2026-03-24*
