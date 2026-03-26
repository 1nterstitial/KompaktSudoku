# Testing Patterns

**Analysis Date:** 2026-03-25

## Test Framework

**Runner:**
- JUnit 4 via `@RunWith(RobolectricTestRunner::class)` for all tests requiring Android context
- Plain JUnit 4 (no `@RunWith`) for pure Kotlin tests (`ScoreCalculationTest`, `GameUiStateTest`, `PersistedGameStateTest`)
- Config: `app/build.gradle.kts` with `testOptions { unitTests { isIncludeAndroidResources = true } }`

**Assertion Library:**
- `org.junit.Assert.*` (JUnit 4 static assertions)
- `app.cash.turbine` for Flow/StateFlow emission testing

**Test Libraries:**
- Robolectric 4.14.x — runs Compose UI tests on JVM without device
- Turbine 1.2.0 — `flow.test { awaitItem() }` syntax for StateFlow
- Mockk 1.13.x — available but not used in v1.0 tests (FakeX pattern preferred over mocking)
- `kotlinx.coroutines.test` — `runTest`, `UnconfinedTestDispatcher`, `advanceUntilIdle`

**Run Commands:**
```bash
./gradlew test              # Run all unit tests
./gradlew testDebugUnitTest # Run debug variant tests with output
```

**Compose UI Test Config:**
- `@Config(sdk = [31])` required on all Robolectric Compose tests — matches device `targetSdk`
- `createComposeRule()` used for all UI tests

## Test File Organization

**Location:** `app/src/test/java/com/mudita/sudoku/` — all tests in test source set, mirroring main package structure.

**Structure:**
```
test/
├── game/
│   ├── DataStoreScoreRepositoryTest.kt   — Real DataStore with TemporaryFolder
│   ├── FakeGameRepository.kt             — Test double
│   ├── FakeGenerator.kt                  — Deterministic puzzle generator
│   ├── FakeScoreRepository.kt            — Test double with call tracking
│   ├── GameUiStateTest.kt                — Data class + equals/hashCode
│   ├── GameViewModelEraseTest.kt         — eraseCell() behavior
│   ├── GameViewModelHintTest.kt          — hint, score, personal best
│   ├── GameViewModelPersistenceTest.kt   — save/load/resume lifecycle
│   ├── GameViewModelTest.kt              — Core game loop (startGame, enterDigit, undo, etc.)
│   ├── MainDispatcherRule.kt             — Shared JUnit4 TestWatcher
│   ├── PersistedGameStateTest.kt         — JSON round-trip serialization
│   └── ScoreCalculationTest.kt           — Pure formula tests
├── puzzle/
│   ├── DifficultyClassifierTest.kt       — Technique tier classification
│   ├── SudokuEngineIntegrationTest.kt    — End-to-end generation (20 puzzles × 3 difficulties)
│   ├── SudokuGeneratorTest.kt            — Given-count ranges, exception on failure
│   ├── SudokuValidatorTest.kt            — isValidPlacement row/col/box logic
│   └── UniquenessVerifierTest.kt         — Unique solution detection
└── ui/game/
    ├── ControlsRowTest.kt                — Mode toggle, undo, touch targets
    ├── DifficultyScreenTest.kt           — Difficulty selection callbacks
    ├── GameGridTest.kt                   — Canvas tap-to-index mapping
    ├── GameScreenTest.kt                 — Smoke test (render + number pad visible)
    ├── LeaderboardScreenTest.kt          — Score display, em-dash for nulls
    ├── MenuScreenTest.kt                 — Resume button conditional, callbacks
    ├── NumberPadTest.kt                  — Digit dispatch, erase, touch targets
    └── SummaryScreenTest.kt              — Stat display, personal best, callbacks
```

**Naming Convention:**
- Test functions use backtick strings with descriptive natural language
- Format: `` `[subject] [condition] [expected outcome]` ``
- Requirement IDs appended when relevant: `` `startGame EASY - emits loading then loaded state - DIFF-01` ``
- Section dividers using `// ---` banners group related tests within a file

## Test Infrastructure

**MainDispatcherRule** (`app/src/test/java/com/mudita/sudoku/game/MainDispatcherRule.kt`):
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) { Dispatchers.setMain(testDispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}
```
- Applied as `@get:Rule val mainDispatcherRule = MainDispatcherRule()` in all ViewModel tests

## Test Doubles

**FakeGenerator** (`app/src/test/java/com/mudita/sudoku/game/FakeGenerator.kt`):
- Returns a fixed known-valid 81-cell board (the canonical Wikipedia Sudoku) regardless of difficulty
- 20 empty cells at deterministic indices for fill/pencil/undo testing
- Companion object exposes `BOARD`, `SOLUTION`, `emptyIndices()`, `correctDigitAt(Int)`, `wrongDigitAt(Int)`
- Used by injecting `generatePuzzle = { FakeGenerator().generatePuzzle(it) }` into `GameViewModel`

**FakeGameRepository** (`app/src/test/java/com/mudita/sudoku/game/FakeGameRepository.kt`):
- In-memory implementation of `GameRepository`
- Tracks `saveCallCount` and `clearCallCount` for assertion
- Constructor takes optional `savedState: GameUiState?` for pre-loading saved game scenarios

**FakeScoreRepository** (`app/src/test/java/com/mudita/sudoku/game/FakeScoreRepository.kt`):
- In-memory implementation of `ScoreRepository`
- Tracks `saveCallCount`; provides `preloadScore(difficulty, score)` test helper that does NOT increment saveCallCount

## ViewModel Test Patterns

**Standard ViewModel test setup:**
```kotlin
@RunWith(RobolectricTestRunner::class)
class GameViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        viewModel = GameViewModel(generatePuzzle = { difficulty ->
            FakeGenerator().generatePuzzle(difficulty)
        })
    }
}
```

**StateFlow observation with Turbine:**
```kotlin
viewModel.uiState.test {
    awaitItem()                    // consume initial idle state
    viewModel.startGame(Difficulty.EASY)
    val loadingState = awaitItem() // isLoading = true
    val loadedState = awaitItem()  // isLoading = false
    cancelAndIgnoreRemainingEvents()
}
```

**Asserting no state change (no-op guard tests):**
```kotlin
viewModel.enterDigit(5)  // no cell selected — should be no-op
expectNoEvents()
```

**SharedFlow event testing:**
```kotlin
viewModel.events.test {
    // trigger completion...
    val event = awaitItem()
    assertTrue("Expected Completed event", event is GameEvent.Completed)
    cancelAndIgnoreRemainingEvents()
}
```

**Persistence test setup with injectable dispatcher:**
```kotlin
private fun makeViewModel(
    savedState: GameUiState? = null,
    repository: FakeGameRepository = FakeGameRepository(savedState)
): Pair<GameViewModel, FakeGameRepository> {
    val vm = GameViewModel(
        generatePuzzle = fakeGenerate,
        repository = repository,
        ioDispatcher = UnconfinedTestDispatcher()  // synchronous I/O in tests
    )
    return vm to repository
}
```

**`advanceUntilIdle` pattern (when Turbine cannot be used):**
```kotlin
vm.startGame(Difficulty.EASY)
advanceUntilIdle()  // let all coroutines complete
assertTrue(vm.uiState.value.isComplete)
```

## Compose UI Test Patterns

**Standard Compose test class:**
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class MenuScreenTest {
    @get:Rule
    val composeRule = createComposeRule()
}
```

**Rendering a screen:**
```kotlin
composeRule.setContent {
    ThemeMMD {
        MenuScreen(hasSavedGame = false, onNewGame = {}, onResume = {}, onBestScores = {})
    }
}
```

**Assertion patterns:**
```kotlin
composeRule.onNodeWithText("Sudoku").assertIsDisplayed()
composeRule.onNodeWithText("Resume").assertDoesNotExist()
composeRule.onNodeWithText("1").assertHeightIsAtLeast(56.dp)
```

**Callback verification:**
```kotlin
var callCount = 0
composeRule.setContent {
    ThemeMMD {
        MenuScreen(hasSavedGame = false, onNewGame = { callCount++ }, ...)
    }
}
composeRule.onNodeWithText("New Game").performClick()
assertEquals(1, callCount)
```

**Canvas tap testing (GameGrid):**
```kotlin
composeRule.onRoot().performTouchInput {
    val gridSize = minOf(width, height).toFloat()
    val cellSize = gridSize / 9f
    // Tap center of cell (row=0, col=0)
    click(Offset(cellSize * 0.5f, cellSize * 0.5f))
}
assertEquals(0, clicked)
```
- Grid set to `Modifier.fillMaxSize()` to give Canvas a layout size for hit testing
- Cell index formula: `row * 9 + col`

## DataStore Integration Tests

**Pattern using TemporaryFolder:**
```kotlin
@get:Rule
val tempFolder = TemporaryFolder()

private fun createRepository(): DataStoreScoreRepository {
    val dataStore = PreferenceDataStoreFactory.create {
        tempFolder.newFile("test_score_state.preferences_pb")
    }
    return DataStoreScoreRepository(dataStore)
}
```
- `PreferenceDataStoreFactory.create` used instead of `Context.gameDataStore` — no Android context needed
- Each test gets a fresh file via `tempFolder.newFile()`

## Puzzle Engine Tests

**Integration tests (`SudokuEngineIntegrationTest.kt`):**
- `repeat(20)` loop for statistical confidence on all three difficulties
- Tests PUZZ-01 (unique solutions), PUZZ-02 (technique tiers), generation performance (<2000ms on JVM)
- Uses real `SudokuGenerator`, not `FakeGenerator` — these are slow tests (~seconds total)

**Unit tests for engine components:**
- `SudokuValidatorTest`: covers row, column, box conflict detection on `isValidPlacement`
- `DifficultyClassifierTest`: uses embedded known-good boards (Wikipedia canonical easy board)
- `UniquenessVerifierTest`: verifies backtracking solver detects multi-solution and no-solution cases
- `SudokuGeneratorTest`: verifies given-count ranges and `PuzzleGenerationException` on exhausted attempts

## Pure Unit Tests (No Android, No Coroutines)

**`ScoreCalculationTest`** — tests for `calculateScore(errorCount, hintCount)`:
- Perfect game (0,0) = 100; floor at 0 (never negative); 10 errors = 0

**`GameUiStateTest`** — tests for `GameUiState` data class:
- Default field values; `equals`/`hashCode` contract for array fields (`IntArray`, `BooleanArray`, `Array<Set<Int>>`)
- Tests for `GameAction.FillCell`, `GameAction.SetPencilMark`, `GameEvent.Completed` data classes

**`PersistedGameStateTest`** — JSON serialization round-trip:
- Per-field preservation: `board`, `solution`, `givenMask`, `difficulty`, `selectedCellIndex`, `errorCount`, `isComplete`
- Fields that reset on resume: `inputMode` → `FILL`, `isLoading` → `false`
- JSON missing `hintCount` deserializes to `0` (backward compatibility)
- Pencil marks stored sorted, restored as `Set<Int>`

## Coverage Gaps

**DataStoreGameRepository not directly tested:**
- `saveGame()`, `loadGame()`, and `clearGame()` have no integration tests (only ViewModel-level coverage via `FakeGameRepository`)
- Corrupt data handling path (catch block returning null) is untested
- Priority: Medium — `PersistedGameStateTest` validates the round-trip logic; DataStore integration follows the same pattern as the tested `DataStoreScoreRepository`

**MainActivity navigation flow not tested:**
- Screen routing (`when (currentScreen)`) and `onStop` auto-save trigger have no tests
- Priority: Low — screens are individually tested; routing is simple enum `when` expressions

**GameGrid error state rendering not tested:**
- The inset error indicator border (`drawErrorIndicator()`) has no visual assertion
- Functional error-tracking logic is fully covered in `GameViewModelTest`
- Priority: Medium

**Device-side tests pending for all 6 phases:**
- All of the following require a physical Mudita Kompakt and cannot be automated with Robolectric:
  - Full test suite performance on Helio A22 CPU — Phase 1
  - `ResumeDialog` E-ink rendering — Phase 4
  - Full save-on-background round-trip (Home → force stop → cold start → resume) — Phase 4
  - `SummaryScreen` E-ink rendering — Phase 5
  - Full navigation flow end-to-end on device — Phase 5/6
  - No ghosting across full navigation flow — Phase 6
- Priority: High for any future release

---

*Testing analysis: 2026-03-25*
