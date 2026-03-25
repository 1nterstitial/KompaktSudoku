---
phase: 04-persistence
plan: 01
subsystem: database
tags: [datastore, kotlinx-serialization, json, persistence, repository-pattern]

# Dependency graph
requires:
  - phase: 02-game-state-domain
    provides: GameUiState domain model with board/solution/givenMask/pencilMarks/errorCount fields
  - phase: 01-puzzle-engine
    provides: Difficulty enum used in PersistedGameState

provides:
  - PersistedGameState @Serializable DTO with List-based fields for all persistable game state
  - toPersistedState() / toUiState() extension functions for GameUiState <-> PersistedGameState conversion
  - GameRepository interface (saveGame/loadGame/clearGame) for ViewModel injection
  - NoOpGameRepository default implementation for backward compatibility
  - DataStoreGameRepository backed by DataStore Preferences + JSON serialization
  - gameDataStore Context extension property for DI
  - FakeGameRepository in-memory test double with saveCallCount/clearCallCount

affects:
  - 04-02-PLAN (ViewModel integration — will inject GameRepository into GameViewModel)
  - 04-03-PLAN (UI wiring — resume dialog depends on loadGame returning saved state)

# Tech tracking
tech-stack:
  added:
    - kotlinx.serialization @Serializable annotation on PersistedGameState DTO
    - DataStore Preferences (already in deps) wired via preferencesDataStore extension
  patterns:
    - Repository pattern: GameRepository interface injected into ViewModel (mirrors generatePuzzle lambda pattern)
    - DTO separation: PersistedGameState is a separate class from GameUiState — domain model stays clean
    - List-based serialization: IntArray/BooleanArray/Array<Set<Int>> converted to List<> for kotlinx.serialization compatibility
    - Corrupt-data resilience: loadGame() returns null on any Exception (never crashes on bad DataStore data)
    - Sorted pencil marks: sets stored as sorted lists for deterministic JSON

key-files:
  created:
    - app/src/main/java/com/mudita/sudoku/game/model/PersistedGameState.kt
    - app/src/main/java/com/mudita/sudoku/game/GameRepository.kt
    - app/src/main/java/com/mudita/sudoku/game/DataStoreGameRepository.kt
    - app/src/test/java/com/mudita/sudoku/game/FakeGameRepository.kt
    - app/src/test/java/com/mudita/sudoku/game/PersistedGameStateTest.kt
  modified: []

key-decisions:
  - "PersistedGameState is a separate DTO class — GameUiState is not annotated directly, keeping domain model free of serialization concerns"
  - "inputMode and isLoading excluded from persistence — inputMode resets to FILL on resume, isLoading always false on restore"
  - "Pencil marks stored as sorted List<Int> — deterministic JSON for diffing and debugging"
  - "DataStoreGameRepository catches all Exception in loadGame() and returns null — corrupt data treated as no saved game, never crashes"
  - "gameDataStore is a Context extension at file scope — matches official DataStore documentation pattern for dependency injection"

patterns-established:
  - "Repository injection pattern: GameRepository interface injected via constructor, same approach as generatePuzzle lambda in GameViewModel"
  - "Array-to-List conversion pattern: IntArray.toList(), BooleanArray.toList(), Array<Set<Int>>.map { it.sorted() } for DataStore JSON"

requirements-completed: [STATE-01]

# Metrics
duration: 25min
completed: 2026-03-24
---

# Phase 4 Plan 01: Persistence Data Layer Summary

**PersistedGameState @Serializable DTO with DataStore JSON persistence, GameRepository interface injectable into ViewModel, and FakeGameRepository in-memory test double for Plan 02 tests**

## Performance

- **Duration:** ~25 min
- **Started:** 2026-03-24T21:50:00Z
- **Completed:** 2026-03-24T22:15:00Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- PersistedGameState DTO serializes all persistable game state fields (board, solution, givenMask, difficulty, selectedCellIndex, pencilMarks, errorCount, isComplete) as JSON-safe List types
- GameRepository interface with NoOpGameRepository enables ViewModel injection without requiring a real DataStore in tests
- DataStoreGameRepository implements full async save/load/clear on Dispatchers.IO with corrupt-data resilience
- 14 unit tests covering round-trip conversion, JSON encoding, pencil mark sorting, null selectedCellIndex, and intentional non-persistence of inputMode/isLoading

## Task Commits

Each task was committed atomically:

1. **Task 1: PersistedGameState DTO + serialization round-trip tests** - `2e239d2` (feat — TDD green)
2. **Task 2: GameRepository interface + DataStoreGameRepository + FakeGameRepository** - `414c770` (feat)

## Files Created/Modified

- `app/src/main/java/com/mudita/sudoku/game/model/PersistedGameState.kt` - @Serializable DTO + toPersistedState() + toUiState() extension functions
- `app/src/main/java/com/mudita/sudoku/game/GameRepository.kt` - GameRepository interface + NoOpGameRepository
- `app/src/main/java/com/mudita/sudoku/game/DataStoreGameRepository.kt` - DataStore + JSON implementation + gameDataStore Context extension
- `app/src/test/java/com/mudita/sudoku/game/FakeGameRepository.kt` - In-memory test double with call count tracking
- `app/src/test/java/com/mudita/sudoku/game/PersistedGameStateTest.kt` - 14 round-trip and serialization tests

## Decisions Made

- PersistedGameState as a separate DTO — GameUiState stays free of @Serializable and kotlinx.serialization imports, keeping the domain model clean
- inputMode excluded from persistence: always resets to FILL on resume per decision D-05 design intent
- isLoading excluded from persistence: always false on resume (puzzle is already loaded)
- loadGame() returns null on any Exception rather than propagating — corrupt DataStore data treated as "no saved game"
- gameDataStore extension at file scope (not class member) matches the official DataStore documentation pattern and enables easy `context.gameDataStore` injection

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

- Gradle wrapper script could not be invoked directly from the shell in the worktree (Java classpath error). Resolved by invoking Gradle directly from the cached distribution at `~/.gradle/wrapper/dists/gradle-8.11.1-bin/`. Tests ran successfully.
- `local.properties` was missing in the worktree (needed for Android SDK path). Copied from the main repo.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Plan 02 (ViewModel integration) can now inject `GameRepository` into `GameViewModel` via constructor parameter
- `FakeGameRepository` is ready for Plan 02 ViewModel tests
- `DataStoreGameRepository` is ready for Plan 03 UI wiring with `context.gameDataStore`
- No blockers

---
*Phase: 04-persistence*
*Completed: 2026-03-24*
