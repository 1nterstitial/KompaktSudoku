# Coding Conventions

**Analysis Date:** 2026-03-25

## Naming Patterns

**Files:**
- One top-level declaration per file; file name matches the declaration name exactly
- Screen composables: `PascalCaseScreen.kt` (e.g., `GameScreen.kt`, `MenuScreen.kt`, `SummaryScreen.kt`)
- ViewModel: `GameViewModel.kt` — single ViewModel for the entire game loop
- Repository interfaces and implementations: `[Name]Repository.kt` / `DataStore[Name]Repository.kt`
- Model types: `PascalCase.kt` (e.g., `GameUiState.kt`, `GameAction.kt`, `GameEvent.kt`)
- Engine classes: `PascalCase.kt` in `engine/` package (e.g., `SudokuGenerator.kt`, `DifficultyClassifier.kt`)
- Test files: mirror production file name with `Test` suffix (e.g., `GameViewModelTest.kt`, `SudokuValidatorTest.kt`)
- Test doubles: `Fake[Type].kt` (e.g., `FakeGenerator.kt`, `FakeGameRepository.kt`, `FakeScoreRepository.kt`)

**Functions:**
- camelCase throughout
- Public ViewModel functions are imperative verbs: `startGame()`, `selectCell()`, `enterDigit()`, `eraseCell()`, `requestHint()`, `undo()`, `resumeGame()`, `saveNow()`
- Private helpers are also camelCase verbs: `applyFill()`, `applyPencilMark()`, `handleCompletion()`, `refreshLeaderboard()`
- Top-level pure functions: camelCase (e.g., `calculateScore()`, `isValidPlacement()`, `difficultyConfigFor()`)
- Extension functions named as regular functions: `toPersistedState()`, `toUiState()`

**Variables/Properties:**
- camelCase
- Private mutable backing flows use `_` prefix: `_uiState`, `_events`, `_showResumeDialog`, `_leaderboardScores`
- Public exposed flows drop the `_` prefix: `uiState`, `events`, `showResumeDialog`, `leaderboardScores`
- DataStore keys use `SCREAMING_SNAKE_CASE`: `IN_PROGRESS_GAME_KEY`
- Local variables: camelCase descriptive names (`emptyIdx`, `newBoard`, `newMarks`, `candidates`)

**Types/Classes:**
- `PascalCase` throughout
- `data class` for pure model types: `GameUiState`, `CellData`, `DifficultyConfig`, `CompletionResult`, `PersistedGameState`
- `sealed class` for discriminated unions: `GameAction`, `GameEvent`
- `enum class` for finite sets: `Difficulty`, `InputMode`, `TechniqueTier`, `Screen`
- `interface` for repository contracts: `GameRepository`, `ScoreRepository`
- `class` for implementations: `DataStoreGameRepository`, `DataStoreScoreRepository`, `SudokuGenerator`, `DifficultyClassifier`

## Code Style

**Formatting:**
- No explicit formatter configuration file detected (no `.editorconfig`, no detekt config, no ktlint config)
- Indentation: 4 spaces
- Opening braces on same line as declaration
- Trailing commas used consistently in multi-parameter declarations
- Maximum line length: approximately 120–140 characters in practice

**Linting suppression:**
- `@Suppress("DEPRECATION")` used at call sites for `Json.decodeFromString` (deprecated API with no replacement available at API 31)
- `@Suppress("UNCHECKED_CAST")` in `MainActivity` for ViewModelProvider.Factory cast
- `@OptIn(ExperimentalSudoklifyApi::class)` on `SudokuGenerator` class
- `@OptIn(ExperimentalCoroutinesApi::class)` in test files using `advanceUntilIdle`

## Visibility and Encapsulation

**ViewModel state exposure pattern:**
```kotlin
// Private mutable — internal mutation only
private val _uiState = MutableStateFlow(GameUiState())
// Public immutable — exposed to UI
val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
```

**Companion object pattern in test doubles:**
```kotlin
companion object {
    val BOARD = IntArray(81) { ... }
    val SOLUTION = intArrayOf(...)
    fun emptyIndices(): List<Int> = ...
    fun correctDigitAt(index: Int): Int = SOLUTION[index]
    fun wrongDigitAt(index: Int): Int = ...
}
```

## KDoc Comments

**Coverage:**
- All public ViewModel methods have KDoc blocks
- All public data classes have `@param`-documented constructor parameters
- Classes have KDoc explaining purpose and key design decisions
- Private helpers have brief KDoc or inline comments explaining non-obvious logic
- Decision IDs referenced inline: `// per decision D-03`, `// SCORE-01 design`

**Style:**
```kotlin
/**
 * One-sentence summary ending with period.
 *
 * Longer explanation when needed. Design decisions called out explicitly.
 *
 * Guards:
 * - Condition 1: what happens
 * - Condition 2: what happens
 *
 * @param paramName Description of param.
 */
```

**Inline comments:**
- Used for drawing order in Canvas (labelled a/b/c/d/e)
- Used for pitfall references: `// Pitfall 3 avoidance`
- Used for requirement IDs: `// PUZZ-01`, `// UI-02`, `// D-05`

## Error Handling

**Patterns:**
- Repository implementations catch all exceptions silently when the safe fallback is well-defined:
  ```kotlin
  } catch (e: Exception) {
      null  // Corrupt or unreadable data — treat as no saved game
  }
  ```
- `PuzzleGenerationException` is thrown (not caught) from `SudokuGenerator` when generation fails after `maxAttempts`; it extends `RuntimeException`
- Guard clauses at the top of ViewModel functions for no-ops (`if (index !in 0..80) return`)
- No `try-catch` in ViewModel — exceptions from puzzle generation propagate to `viewModelScope`'s uncaught exception handler

## State Mutation Pattern

**Immutable copy pattern (mandatory):**
```kotlin
// Always copy arrays before mutation — never mutate in-place
val newBoard = state.board.copyOf()
newBoard[idx] = digit
_uiState.update {
    it.copy(board = newBoard, errorCount = newErrorCount)
}
```

**StateFlow update:**
- Use `_uiState.update { it.copy(...) }` for state derived from current state
- Use `_uiState.value = newState` for wholesale replacement (e.g., `resumeGame()`)

## Compose UI Conventions

**MMD compliance (mandatory):**
- All user-facing text uses `TextMMD` — never plain `Text`
- All buttons use `ButtonMMD` — never plain `Button`
- `ThemeMMD` wraps the entire composition in `MainActivity.setContent { ThemeMMD { ... } }`
- No `AnimatedVisibility`, no `Animated*` composables anywhere
- No ripple: `indication = null` + `MutableInteractionSource()` when using `clickable`
- Conditional UI uses plain `if`: `if (hasSavedGame) { ButtonMMD(...) }`

**Composable structure:**
- Screen-level composables are public, top-level `@Composable fun`
- Private sub-composables (used only within one file) are `private fun` at file scope
- `modifier: Modifier = Modifier` as last parameter on all public composables
- Screen composables receive callbacks as lambdas, never the ViewModel directly (exception: `GameScreen` receives `viewModel` explicitly)

**Touch target minimum: 56dp height enforced on all interactive elements:**
```kotlin
.sizeIn(minHeight = 56.dp)
// or
.height(56.dp)  // for full-width buttons
```

**Canvas drawing pattern:**
- Drawing order: backgrounds → error indicators → digit text → pencil marks → grid lines (last, on top)
- Text measured via `rememberTextMeasurer()` hoisted to composable level; used inside `drawText()`

## Coroutine Conventions

**Dispatcher usage:**
- `Dispatchers.Default` for CPU-bound work (puzzle generation in `withContext`)
- `Dispatchers.IO` for disk I/O (DataStore reads/writes via `withContext(ioDispatcher)`)
- `ioDispatcher` is injectable in `GameViewModel` constructor — pass `UnconfinedTestDispatcher()` in tests
- `viewModelScope.launch` for all ViewModel-initiated coroutines

**Suspend functions:**
- Repository methods are `suspend fun` — no callback-style I/O
- `saveNow()` is `suspend fun` to allow structured concurrency from `onStop`

## Module Organization

**Package structure:**
```
com.mudita.sudoku
├── (root)                     — MainActivity, Screen enum
├── game/                      — ViewModel, repositories, interfaces
│   └── model/                 — Data types: GameUiState, GameAction, GameEvent, etc.
├── puzzle/                    — PuzzleGenerationException
│   ├── engine/                — SudokuGenerator, SudokuValidator, DifficultyClassifier, UniquenessVerifier
│   └── model/                 — Difficulty, DifficultyConfig, SudokuPuzzle, TechniqueTier
└── ui/
    └── game/                  — All @Composable screens and components
```

**No barrel files** — each class imported directly by full path.

---

*Convention analysis: 2026-03-25*
