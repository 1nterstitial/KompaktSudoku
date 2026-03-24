# Conventions

> Last updated: 2026-03-24

## Naming

**Files:**
- One top-level class or sealed hierarchy per file, named to match the class: `GameViewModel.kt`, `GameUiState.kt`, `DifficultyClassifier.kt`
- Enum files named after the enum: `Difficulty.kt`, `InputMode.kt`
- Exception files named after the exception class: `PuzzleGenerationException.kt`
- `DifficultyConfig.kt` combines `TechniqueTier` enum, `DifficultyConfig` data class, and package-level config constants — acceptable when tightly coupled
- Test doubles prefixed with `Fake`: `FakeGenerator.kt`
- Test rules suffixed with `Rule`: `MainDispatcherRule.kt`

**Packages:**
- Feature-layer split: `com.mudita.sudoku.game`, `com.mudita.sudoku.game.model`, `com.mudita.sudoku.puzzle`, `com.mudita.sudoku.puzzle.engine`, `com.mudita.sudoku.puzzle.model`
- `model` sub-package: pure data types with no Android or framework imports
- `engine` sub-package: stateless logic classes

**Classes:**
- PascalCase: `GameViewModel`, `SudokuGenerator`, `DifficultyClassifier`, `UniquenessVerifier`

**Functions:**
- camelCase throughout
- Boolean-returning functions start with `is`/`has`/`meets`: `isValidPlacement`, `hasUniqueSolution`, `meetsRequirements`
- Action functions use verb phrases: `generatePuzzle`, `selectCell`, `enterDigit`, `toggleInputMode`, `applyFill`, `applyPencilMark`
- Private helpers named descriptively: `tryGenerateCandidate`, `pickSudoklifyDifficulty`, `candidatesFor`, `applyHiddenSingles`, `allUnits`

**Variables:**
- camelCase: `undoStack`, `givenMask`, `errorCount`, `lastRejectionReason`
- Backing StateFlow/SharedFlow prefixed with underscore: `_uiState`, `_events`
- Public read-only surfaces: `uiState`, `events` (no underscore)
- Top-level constants: `SCREAMING_SNAKE_CASE` — `EASY_CONFIG`, `MEDIUM_CONFIG`, `HARD_CONFIG`
- Test companion constants: `SCREAMING_SNAKE_CASE` — `FakeGenerator.BOARD`, `FakeGenerator.SOLUTION`

**Enums:**
- SCREAMING_SNAKE_CASE values: `InputMode.FILL`, `InputMode.PENCIL`, `Difficulty.EASY`, `TechniqueTier.NAKED_SINGLES_ONLY`

**Parameters:**
- Descriptive camelCase: `board`, `index`, `digit`, `difficulty`, `config`, `puzzle`
- Injected collaborators named after their type (lowercased): `verifier`, `classifier`

## Code Style

**Formatting:**
- 4-space indentation throughout
- No `.editorconfig`, no `ktlint`, no `spotless` config — style enforced by convention only
- Trailing commas on multi-line constructor params and function args

**Kotlin idioms in use:**
- `data class` for all value-holding models: `GameUiState`, `SudokuPuzzle`, `DifficultyConfig`, `GameAction.FillCell`, `GameAction.SetPencilMark`, `GameEvent.Completed`
- `sealed class` for discriminated unions: `GameAction`, `GameEvent`
- `enum class` for fixed sets: `Difficulty`, `InputMode`, `TechniqueTier`
- `when` over sealed classes and enums is always exhaustive — no `else` branch
- `_uiState.update { it.copy(...) }` for all StateFlow mutations — never `_uiState.value = ...`
- `ArrayDeque` for undo stack (Kotlin stdlib)
- `IntArray`/`BooleanArray`/`Array<Set<Int>>` for 81-cell board data — primitive arrays for performance
- Manual `equals`/`hashCode` overrides in `GameUiState` and `SudokuPuzzle` using `contentEquals`, `contentDeepEquals`, `contentHashCode`, `contentDeepHashCode` for array fields
- Default parameter injection for all dependencies: `SudokuGenerator(verifier, classifier, maxAttempts)`, `GameViewModel(generatePuzzle: suspend (Difficulty) -> SudokuPuzzle)`
- `repeat(n) { i -> ... }` for counted loops; `return@repeat` for early-continue inside lambdas
- `board.copyOf()` before every mutation to avoid side effects on caller's data
- `@OptIn(ExperimentalSudoklifyApi::class)` applied at class level for third-party experimental API
- Alias imports for name conflicts: `import dev.teogor.sudoklify.components.Difficulty as SudoklifyDifficulty`

## Patterns

**State management (MVVM + StateFlow):**
- `GameViewModel` holds all mutable state
- `_uiState: MutableStateFlow<GameUiState>` — private, mutated only inside the ViewModel via `.update { it.copy(...) }`
- `uiState: StateFlow<GameUiState>` — public surface exposed to UI via `.asStateFlow()`
- `_events: MutableSharedFlow<GameEvent>` with `replay=0, extraBufferCapacity=1` — one-shot events for navigation/dialogs
- `events: SharedFlow<GameEvent>` — public surface via `.asSharedFlow()`
- Undo stack (`ArrayDeque<GameAction>`) lives in the ViewModel, not in `GameUiState`, to keep the state snapshot immutable

**Immutable state:**
- `GameUiState` is a `data class` with only `val` properties
- Mutable arrays are always `.copyOf()` before modification, never mutated in place
- `init` blocks with `require()` enforce invariants: `SudokuPuzzle` validates board/solution size and givenCount minimum

**Dependency injection via constructor defaults:**
- No DI framework — collaborators injected as constructor parameters with sensible defaults
- Enables test doubles without mocking: `SudokuGenerator(verifier, classifier, maxAttempts)`, `GameViewModel(generatePuzzle)`
- `open class UniquenessVerifier` / `open fun hasUniqueSolution` — opened explicitly to allow anonymous subclass overrides in tests (documented with inline comment)
- `internal` visibility for methods that must be tested but should not form the public API: `classifyBoard`, `solveWithNakedSingles`, `solveWithHiddenSingles`, `countSolutions`

**Error handling:**
- Expected failures throw typed exceptions: `PuzzleGenerationException` (not generic `RuntimeException`)
- `try/catch` in `tryGenerateCandidate` returns `null` on Sudoklify failures; the caller retries
- Guard returns at the top of public functions for invalid input: index out of range, given cell, null selection
- `require()` in `init` for constructor preconditions with values embedded in the message

**KDoc and comments:**
- Every public class and public function has a KDoc block; `@param` and `@return` tags where the name alone is ambiguous
- Class-level KDoc explains algorithm design and deliberate exclusions
- Inline `//` comments on arithmetic index calculations and non-obvious decisions
- Section separator comments in longer files: `// ------------------------------------------------------------------ public actions`
- Design decision rationale inline: "errors are permanent once counted (SCORE-01 design)"
- Requirement IDs cross-referenced: `// Gate 1: uniqueness (PUZZ-01)`

**Coroutines:**
- CPU-bound work dispatched with `withContext(Dispatchers.Default)`: puzzle generation in `startGame`
- `viewModelScope.launch` for fire-and-forget coroutine work
- `SharedFlow.emit` wrapped in `viewModelScope.launch` for one-shot event emission

## Anti-patterns

- **No ripple effects** — E-ink display constraint; `ThemeMMD` disables ripple by default; never re-enable via `indication` override
- **No `dynamicColorScheme`** — device is monochromatic; only `eInkColorScheme` permitted
- **No `_uiState.value = ...` direct assignment** — always `_uiState.update { it.copy(...) }`
- **No mutable state in `GameUiState`** — undo stack is ViewModel-private, not in the state snapshot
- **No SharedPreferences** — DataStore only; SharedPreferences has synchronous write ANR risk
- **No Room** — overkill for 3 high scores + 1 game state
- **No JUnit 5** — `createComposeRule` depends on JUnit 4 lifecycle
- **No Mockito** — Mockk is the Kotlin-idiomatic mocking library for suspend functions and flows
- **No Gson** — `kotlinx.serialization` is compile-time null-safe; Gson relies on reflection
- **No `else` on exhaustive `when`** — sealed class and enum `when` blocks are intentionally exhaustive
- **No font size overrides below MMD defaults** — typography is tuned for E-ink readability
- **No `collectAsState()`** — use `collectAsStateWithLifecycle()` for lifecycle-aware collection
- **No animations** — E-ink display cannot render smooth transitions
