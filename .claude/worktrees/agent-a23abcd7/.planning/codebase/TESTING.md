# Testing

## Test Structure

All tests live in `app/src/test/java/com/mudita/sudoku/puzzle/` — the mirror of the production puzzle package. `androidTest/` is currently empty (no Compose UI tests yet).

5 test files, 28 total tests:

| File | Tests | Scope |
|------|-------|-------|
| `SudokuValidatorTest.kt` | 5 | Unit — `isValidPlacement()` row/col/box conflict logic |
| `UniquenessVerifierTest.kt` | 4 | Unit — uniqueness backtracking, abort-on-second-solution |
| `DifficultyClassifierTest.kt` | 5 | Unit — technique-tier classification with real puzzle boards |
| `SudokuGeneratorTest.kt` | 6 | Unit — generator output shape, given-count ranges, exception path |
| `SudokuEngineIntegrationTest.kt` | 8 | Integration — 20-puzzle batches for uniqueness + technique + timing |

## Frameworks Used

| Framework | Declared | Actually Used |
|-----------|----------|---------------|
| JUnit 4 (`junit:junit:4.13.2`) | Yes | Yes — all test classes use `@Test`, `Assert.*` |
| Mockk (`io.mockk:mockk:1.13.x`) | Yes | **No** — zero `mockk {}`/`every {}` usage; replaced by anonymous subclass pattern |
| Turbine (`app.cash.turbine:turbine:1.2.0`) | Yes | **No** — no StateFlow/ViewModel tests yet (Phase 2+) |
| Robolectric (`org.robolectric:robolectric:4.14.x`) | Yes | **No** — no Compose UI tests yet (Phase 3+) |
| Compose UI Test | Yes (via BOM) | **No** — `androidTest/` is empty |
| Kotlinx Coroutines Test | Yes | **No** — no ViewModel tests yet |

## Test Patterns

**Backtick test names** — all test methods use Kotlin backtick syntax for readable names:
```kotlin
@Test fun `20 easy puzzles have givenCount in 36 to 45`() { ... }
```

**Statistical batch testing** — `repeat(N)` for stochastic properties:
```kotlin
repeat(20) { i ->
    val puzzle = generator.generatePuzzle(Difficulty.EASY)
    assertTrue("Easy puzzle #$i failed...", verifier.hasUniqueSolution(puzzle.board))
}
```

**Anonymous open subclass for test doubles** — used instead of Mockk (zero deps):
```kotlin
val alwaysRejectVerifier = object : UniquenessVerifier() {
    override fun hasUniqueSolution(puzzle: IntArray) = false
}
```
This pattern required `UniquenessVerifier` and `hasUniqueSolution` to be declared `open` in production code.

**Wall-clock timing assertions** — JVM proxy for device performance requirement:
```kotlin
val start = System.currentTimeMillis()
generator.generatePuzzle(Difficulty.HARD)
val elapsed = System.currentTimeMillis() - start
assertTrue("Hard generation took ${elapsed}ms, expected < 2000ms", elapsed < 2000)
```

**Manual try/catch for exception testing** — not using JUnit's `@Test(expected=...)`:
```kotlin
try {
    strictGenerator.generatePuzzle(Difficulty.EASY)
    fail("Expected PuzzleGenerationException")
} catch (e: PuzzleGenerationException) {
    assertTrue(e.message!!.contains("3 attempts"))
}
```

**Hardcoded reference boards** — real Sudoku boards embedded in tests for deterministic behavior:
- `UniquenessVerifierTest`: uses the first published 17-clue puzzle (Gordon Royle's list)
- `DifficultyClassifierTest`: uses the Wikipedia canonical easy puzzle

## Coverage Areas

**Covered:**
- `SudokuValidator.isValidPlacement()` — row, column, 3×3 box conflict detection (5 cases)
- `UniquenessVerifier.hasUniqueSolution()` — unique puzzle, multi-solution board, full board (3 cases)
- `UniquenessVerifier.countSolutions()` — abort-at-limit performance (1 case)
- `DifficultyClassifier.classifyTechniqueTier()` — naked singles board, hard board (3 cases)
- `DifficultyClassifier.meetsRequirements()` — exact-tier matching (2 cases)
- `SudokuGenerator.generatePuzzle()` — board size, solution completeness, given-count ranges (6 cases)
- `SudokuGenerator` — exception after maxAttempts (1 case)
- Integration: uniqueness across 60 puzzles (3 difficulties × 20), technique compliance for Easy/Medium/Hard, JVM timing proxy (2 cases)

**Not yet covered:**
- ViewModel unit tests (Phase 2 — Mockk, Turbine, `runTest` not yet used)
- Compose UI tests (Phase 3 — Robolectric, `createComposeRule` not yet used)
- DataStore persistence tests (Phase 4)
- Scoring logic tests (Phase 5)
- Navigation tests (Phase 6)
- `androidTest/` integration tests on device (empty, requires physical Kompakt)

## Running Tests

```bash
# Run all unit tests
./gradlew :app:testDebugUnitTest

# Run puzzle package only
./gradlew :app:testDebugUnitTest --tests "com.mudita.sudoku.puzzle.*"
```

**Build environment requirement:** JDK 17, Android SDK (compileSdk 35), Gradle 8.11.1.
28 tests, 0 failures confirmed by plan executor (01-04-SUMMARY.md). Requires re-verification on machine with full SDK.
