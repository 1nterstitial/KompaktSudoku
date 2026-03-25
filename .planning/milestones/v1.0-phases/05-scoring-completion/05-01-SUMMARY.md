---
phase: 05-scoring-completion
plan: 01
subsystem: scoring
tags: [scoring, datastore, persistence, kotlin, serialization]

# Dependency graph
requires:
  - phase: 04-persistence
    provides: DataStoreGameRepository pattern, GameUiState/PersistedGameState models
  - phase: 02-game-state-domain
    provides: GameUiState, PersistedGameState, GameEvent, GameViewModel

provides:
  - hintCount field on GameUiState, PersistedGameState, GameEvent.Completed
  - CompletionResult data class (5 fields: difficulty, errorCount, hintCount, finalScore, isPersonalBest)
  - calculateScore top-level function: max(0, 100 - errorCount*10 - hintCount*5)
  - ScoreRepository interface + NoOpScoreRepository
  - DataStoreScoreRepository with separate score_state DataStore
  - FakeScoreRepository test double with preloadScore helper
  - ScoreCalculationTest: 8 edge cases for score formula
  - DataStoreScoreRepositoryTest: 4 Robolectric tests for persistence
  - Backward-compat: JSON without hintCount deserializes to hintCount=0

affects: [05-02-viewmodel-logic, 05-03-completion-ui, game-viewmodel]

# Tech tracking
tech-stack:
  added: [PreferenceDataStoreFactory (test), TemporaryFolder JUnit rule]
  patterns: [ScoreRepository mirrors GameRepository pattern, calculateScore as top-level function for testability]

key-files:
  created:
    - app/src/main/java/com/mudita/sudoku/game/model/CompletionResult.kt
    - app/src/main/java/com/mudita/sudoku/game/model/ScoreCalculation.kt
    - app/src/main/java/com/mudita/sudoku/game/ScoreRepository.kt
    - app/src/main/java/com/mudita/sudoku/game/DataStoreScoreRepository.kt
    - app/src/test/java/com/mudita/sudoku/game/FakeScoreRepository.kt
    - app/src/test/java/com/mudita/sudoku/game/DataStoreScoreRepositoryTest.kt
    - app/src/test/java/com/mudita/sudoku/game/ScoreCalculationTest.kt
  modified:
    - app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt
    - app/src/main/java/com/mudita/sudoku/game/model/PersistedGameState.kt
    - app/src/main/java/com/mudita/sudoku/game/model/GameEvent.kt
    - app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt
    - app/src/test/java/com/mudita/sudoku/game/PersistedGameStateTest.kt
    - app/src/test/java/com/mudita/sudoku/game/GameUiStateTest.kt

key-decisions:
  - "calculateScore as top-level function in ScoreCalculation.kt — independently testable and importable by GameViewModel"
  - "scoreDataStore separate from gameDataStore — one DataStore file per instance; mixing causes data corruption"
  - "hintCount default = 0 in PersistedGameState — backward-compatible deserialization of Phase 4 JSON"
  - "No Dispatchers.IO inside DataStoreScoreRepository — ViewModel caller dispatches (consistent with DataStoreGameRepository)"
  - "GameEvent.Completed extended to full payload — SummaryScreen gets all data without secondary state query"
  - "GameViewModel emits Completed with isPersonalBest=false placeholder — Plan 02 wires ScoreRepository to compute correctly"

patterns-established:
  - "Repository interface + NoOp + DataStore impl + Fake test double — established pattern (GameRepository, now ScoreRepository)"
  - "Top-level pure function for business logic (calculateScore) — testable without class instantiation"

requirements-completed: [SCORE-04, SCORE-06, HS-01]

# Metrics
duration: 4min
completed: 2026-03-24
---

# Phase 5 Plan 01: Scoring Data Contracts and Persistence Layer Summary

**Score formula, hintCount on all state models, CompletionResult type, and DataStore-backed ScoreRepository with per-difficulty Int keys**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-24T05:32:10Z
- **Completed:** 2026-03-24T05:36:30Z
- **Tasks:** 2
- **Files modified:** 13

## Accomplishments

- Added hintCount to GameUiState (with equals/hashCode), PersistedGameState (with converters and = 0 default for backward compat), and GameEvent.Completed (with full completion payload)
- Implemented calculateScore(errorCount, hintCount) as a testable top-level function with 8 edge-case tests confirming the max(0, 100 - errorCount*10 - hintCount*5) formula
- Created ScoreRepository interface + DataStoreScoreRepository using a separate "score_state" DataStore with intPreferencesKey per difficulty, plus FakeScoreRepository test double mirroring FakeGameRepository pattern
- All 4 DataStoreScoreRepositoryTest cases pass via Robolectric + PreferenceDataStoreFactory

## Task Commits

Each task was committed atomically:

1. **Task 1: State model additions + CompletionResult + score formula tests** - `10e21d1` (feat)
2. **Task 2: ScoreRepository interface + DataStore implementation + FakeScoreRepository + tests** - `1e2337c` (feat)

## Files Created/Modified

- `app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt` - Added hintCount field with equals/hashCode
- `app/src/main/java/com/mudita/sudoku/game/model/PersistedGameState.kt` - Added hintCount = 0 (backward compat) with converters
- `app/src/main/java/com/mudita/sudoku/game/model/GameEvent.kt` - Extended Completed with hintCount, score, difficulty, isPersonalBest
- `app/src/main/java/com/mudita/sudoku/game/model/CompletionResult.kt` - NEW: data class with 5 fields for summary screen
- `app/src/main/java/com/mudita/sudoku/game/model/ScoreCalculation.kt` - NEW: calculateScore top-level function
- `app/src/main/java/com/mudita/sudoku/game/ScoreRepository.kt` - NEW: interface + NoOpScoreRepository
- `app/src/main/java/com/mudita/sudoku/game/DataStoreScoreRepository.kt` - NEW: separate score_state DataStore with intPreferencesKey per difficulty
- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` - Updated Completed emission with full payload using calculateScore
- `app/src/test/java/com/mudita/sudoku/game/ScoreCalculationTest.kt` - NEW: 8 score formula edge cases
- `app/src/test/java/com/mudita/sudoku/game/DataStoreScoreRepositoryTest.kt` - NEW: 4 Robolectric tests
- `app/src/test/java/com/mudita/sudoku/game/FakeScoreRepository.kt` - NEW: in-memory test double with saveCallCount/preloadScore
- `app/src/test/java/com/mudita/sudoku/game/PersistedGameStateTest.kt` - Added 2 backward-compat hintCount tests
- `app/src/test/java/com/mudita/sudoku/game/GameUiStateTest.kt` - Updated Completed construction to new 5-param signature

## Decisions Made

- calculateScore as top-level function — mirrors isValidPlacement pattern from Phase 1; eliminates class instantiation for a pure formula
- scoreDataStore separate from gameDataStore — DataStore spec mandates one instance per file name per process; mixing would corrupt both stores
- hintCount = 0 default in PersistedGameState — essential for backward-compatible deserialization of Phase 4 JSON saved games
- GameEvent.Completed extended to full payload — SummaryScreen in Plan 03 needs all data at once to avoid a secondary state flow read
- GameViewModel emits isPersonalBest=false as placeholder — Plan 02 wires ScoreRepository to compute this correctly

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed GameViewModel compilation break from GameEvent.Completed signature change**
- **Found during:** Task 1 (after extending Completed to 5-param constructor)
- **Issue:** Existing GameViewModel code called `GameEvent.Completed(newErrorCount)` — single param constructor no longer exists
- **Fix:** Updated emission to provide all 5 fields using available state data; added `calculateScore` import; isPersonalBest=false placeholder until Plan 02 wires ScoreRepository
- **Files modified:** app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt
- **Verification:** `./gradlew :app:testDebugUnitTest --tests "com.mudita.sudoku.game.*"` all pass
- **Committed in:** 10e21d1 (Task 1 commit)

**2. [Rule 1 - Bug] Fixed GameUiStateTest compilation break from Completed signature change**
- **Found during:** Task 1 (test compilation failure)
- **Issue:** Two tests constructed `GameEvent.Completed(errorCount = 3)` with old single-param form
- **Fix:** Updated both test cases to provide all 5 required fields
- **Files modified:** app/src/test/java/com/mudita/sudoku/game/GameUiStateTest.kt
- **Committed in:** 10e21d1 (Task 1 commit)

---

**Total deviations:** 2 auto-fixed (Rule 1 - compilation breaks from signature evolution)
**Impact on plan:** Both fixes necessary for compilation correctness. No scope creep — all fixes are direct consequences of the planned Completed signature change.

## Issues Encountered

- Gradle wrapper invocation via `./gradlew` failed with ClassNotFoundException in the bash environment on Windows; resolved by calling the Gradle binary directly from the cached distribution at `~/.gradle/wrapper/dists/gradle-8.11.1-bin/`.

## Known Stubs

- **GameViewModel.kt line ~258**: `isPersonalBest = false` hardcoded — Plan 02 will inject ScoreRepository and compute the real value via `getBestScore()` comparison.

## Next Phase Readiness

- All type contracts for Plan 02 (ViewModel logic) and Plan 03 (UI screens) are in place
- ScoreRepository + FakeScoreRepository ready for GameViewModel injection in Plan 02
- calculateScore ready for direct use in GameViewModel completion logic
- No blockers

## Self-Check: PASSED

All created files exist on disk. Task commits 10e21d1 and 1e2337c verified in git log.

---
*Phase: 05-scoring-completion*
*Completed: 2026-03-24*
