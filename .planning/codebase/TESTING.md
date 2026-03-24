# Testing

> Last updated: 2026-03-24

## Strategy

All tests run on the JVM (no emulator required). Unit tests cover pure logic classes directly. ViewModel tests use Turbine + `runTest` + `MainDispatcherRule` to exercise the StateFlow layer. Integration tests run real puzzle generation in batches to validate stochastic output properties. No Compose UI tests exist yet; `androidTest/` is empty.

Dependencies are injected via constructor parameters, enabling test doubles without a DI framework. Mockk is available but not yet used — the codebase favors anonymous `open` subclasses for structural test doubles (e.g., `UniquenessVerifier`) and a hand-written `FakeGenerator` for deterministic ViewModel testing.

## Test Types

**Unit tests — puzzle engine (`app/src/test/java/com/mudita/sudoku/puzzle/`):**

| File | Tests | Scope |
|------|-------|-------|
| `SudokuValidatorTest.kt` | 5 | Pure function `isValidPlacement()` — row/col/box conflict |
| `UniquenessVerifierTest.kt` | 4 | Backtracking uniqueness — unique puzzle, multi-solution, abort-at-limit timing |
| `DifficultyClassifierTest.kt` | 5 | Technique tier classification with hardcoded reference boards |
| `SudokuGeneratorTest.kt` | 6 | Output shape, given-count ranges per difficulty, exception path |

**Integration tests — puzzle engine (`app/src/test/java/com/mudita/sudoku/puzzle/`):**

| File | Tests | Scope |
|------|-------|-------|
| `SudokuEngineIntegrationTest.kt` | 8 | 20-puzzle batches: uniqueness + technique compliance per difficulty; JVM timing proxy |

**Unit tests — game layer (`app/src/test/java/com/mudita/sudoku/game/`):**

| File | Tests | Scope |
|------|-------|-------|
| `GameUiStateTest.kt` | 20 | `GameUiState` default values, `equals`/`hashCode` for array fields, `GameAction`, `GameEvent`, `InputMode` enum |
| `GameViewModelTest.kt` | 32 | `GameViewModel` state machine — initial state, `startGame`, `selectCell`, `enterDigit`, `toggleInputMode`, pencil marks, undo, error tracking, completion detection, `GameEvent.Completed` |

**Total: 75 tests** (5 + 4 + 5 + 6 + 8 + 20 + 32)

**Not yet present:**
- Compose UI tests — `androidTest/` is empty; `createComposeRule` / Robolectric not yet invoked
- DataStore persistence tests
- Scoring/high-score tests
- Navigation integration tests

## Test Utilities

**`FakeGenerator` (`app/src/test/java/com/mudita/sudoku/game/FakeGenerator.kt`):**
- Deterministic puzzle generator — always returns the same known-valid 9×9 board regardless of requested difficulty
- 61 given cells, 20 empty cells — hardcoded in `BOARD` companion constant
- Provides test helpers:
  - `FakeGenerator.BOARD` — the puzzle board as `IntArray`
  - `FakeGenerator.SOLUTION` — the complete solution as `IntArray`
  - `FakeGenerator.emptyIndices()` — list of indices where `BOARD[i] == 0`
  - `FakeGenerator.correctDigitAt(index)` — `SOLUTION[index]`
  - `FakeGenerator.wrongDigitAt(index)` — first digit in `1..9` that is not `SOLUTION[index]`
- Injected into `GameViewModel` via the `generatePuzzle` lambda parameter:
  ```kotlin
  viewModel = GameViewModel(generatePuzzle = { difficulty ->
      FakeGenerator().generatePuzzle(difficulty)
  })
  ```

**`MainDispatcherRule` (`app/src/test/java/com/mudita/sudoku/game/MainDispatcherRule.kt`):**
- JUnit 4 `TestWatcher` that replaces `Dispatchers.Main` with `UnconfinedTestDispatcher` for each test
- Required for ViewModel tests on the JVM (no Android Looper)
- Applied as a JUnit rule: `@get:Rule val mainDispatcherRule = MainDispatcherRule()`
- Resets `Dispatchers.Main` after each test in `finished()`

**Anonymous open subclasses for engine test doubles:**
- `UniquenessVerifier` is declared `open` with `open fun hasUniqueSolution` to allow override
- Used in `SudokuGeneratorTest` to force the exception path:
  ```kotlin
  val alwaysRejectVerifier = object : UniquenessVerifier() {
      override fun hasUniqueSolution(puzzle: IntArray) = false
  }
  ```

**Hardcoded reference boards in tests:**
- `UniquenessVerifierTest`: first published 17-clue puzzle (Gordon Royle's list) and a known multi-solution board
- `DifficultyClassifierTest`: Wikipedia canonical easy puzzle for `NAKED_SINGLES_ONLY` classification

## Coverage

**Covered:**
- `isValidPlacement()` — all three conflict types (row, column, 3×3 box) and no-conflict case
- `UniquenessVerifier.hasUniqueSolution()` and `countSolutions()` including abort-at-limit timing
- `DifficultyClassifier` — both classification and `meetsRequirements()` with real boards
- `SudokuGenerator` — output correctness, given-count ranges for all three difficulties, exception path
- `SudokuEngineIntegrationTest` — 60 puzzles (3 × 20) for uniqueness; 60 puzzles for technique compliance; JVM timing proxy for Easy and Hard
- `GameUiState` — all default values, `equals`/`hashCode` for `IntArray`/`BooleanArray`/`Array<Set<Int>>` fields, `isLoading`, `isComplete`, `difficulty`
- `GameAction.FillCell`, `GameAction.SetPencilMark`, `GameEvent.Completed` — data class field storage
- `InputMode` enum — value set exhaustiveness
- `GameViewModel.startGame` — loading/loaded state sequence, board non-empty, givenMask alignment, error count reset on new game, undo stack cleared on new game
- `GameViewModel.selectCell` — empty cell, given cell, replacement of previous selection, no-op on out-of-range
- `GameViewModel.enterDigit` FILL mode — fills board, no-op on given cell, no-op with no selection, overwrites existing fill, error count increment on wrong digit, no error increment on correct digit
- `GameViewModel.toggleInputMode` — FILL→PENCIL and PENCIL→FILL
- `GameViewModel.enterDigit` PENCIL mode — add mark, toggle off, multiple marks coexist, no-op on given cell, no-op with no selection, fill clears pencil marks
- `GameViewModel.undo` — restore fill, restore pencil marks cleared by fill, reverse pencil mark add, reverse pencil mark remove, no-op on empty stack, LIFO multi-step order
- Error tracking — correct digit no increment, wrong digit +1, accumulation, undo does not decrement errors (permanent)
- Completion detection — `isComplete` true on last correct fill, `isComplete` false when any cell wrong, `isComplete` reset to false on undo, `GameEvent.Completed` emitted with correct `errorCount`

**Not covered:**
- DataStore read/write (no tests exist for `in_progress_game`, `high_score_*` keys)
- High-score save/load logic (not yet implemented)
- Compose UI rendering and touch interaction (no `androidTest/` tests)
- `MainActivity.onCreate` (stub only)
- `SudokuGenerator.tryGenerateCandidate` null return path (Sudoklify throwing internally) — covered indirectly by the `alwaysRejectVerifier` path but not by a Sudoklify-exception path

## Running Tests

```bash
# Run all unit tests
./gradlew :app:testDebugUnitTest

# Run puzzle engine tests only
./gradlew :app:testDebugUnitTest --tests "com.mudita.sudoku.puzzle.*"

# Run game layer tests only
./gradlew :app:testDebugUnitTest --tests "com.mudita.sudoku.game.*"

# Run a specific test class
./gradlew :app:testDebugUnitTest --tests "com.mudita.sudoku.game.GameViewModelTest"
```

**Requirements:** JDK 17, Android SDK with compileSdk 35, Gradle 8.11.1.

`GameViewModelTest` uses `@RunWith(RobolectricTestRunner::class)` — Robolectric is required for ViewModel coroutine tests that touch Android infrastructure.
