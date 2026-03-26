# Architecture

**Analysis Date:** 2026-03-25

## Pattern Overview

**Overall:** MVVM (Model-View-ViewModel) with unidirectional data flow

**Key Characteristics:**
- Single `GameViewModel` owns all mutable game state; UI composables are stateless receivers
- State exposed as `StateFlow<GameUiState>` (hot, always-current snapshot) and `SharedFlow<GameEvent>` (one-shot events)
- All persistence is interface-segregated behind `GameRepository` and `ScoreRepository`; concrete DataStore implementations are injected at the `MainActivity` level
- Navigation is enum-based (`Screen`) managed with a `remember { mutableStateOf }` in `MainActivity` — no Jetpack Navigation library
- No DI framework (Hilt/Koin); constructor injection is manual via `ViewModelProvider.Factory`

## Layers

**Puzzle Engine Layer:**
- Purpose: Generate, validate, and classify Sudoku puzzles — pure logic with no Android dependencies
- Location: `app/src/main/java/com/mudita/sudoku/puzzle/`
- Contains: `SudokuGenerator`, `SudokuValidator`, `UniquenessVerifier`, `DifficultyClassifier`, domain models (`SudokuPuzzle`, `Difficulty`, `DifficultyConfig`, `TechniqueTier`), `PuzzleGenerationException`
- Depends on: Sudoklify library (`dev.teogor.sudoklify`)
- Used by: `GameViewModel` via a `suspend (Difficulty) -> SudokuPuzzle` lambda (injectable for tests)

**Game Domain Layer:**
- Purpose: Game state machine, scoring, persistence contracts, and serialization DTO
- Location: `app/src/main/java/com/mudita/sudoku/game/`
- Contains: `GameViewModel`, `GameRepository` interface + `NoOpGameRepository`, `ScoreRepository` interface + `NoOpScoreRepository`, `DataStoreGameRepository`, `DataStoreScoreRepository`
- Sub-package `game/model/`: `GameUiState`, `GameAction`, `GameEvent`, `CompletionResult`, `InputMode`, `PersistedGameState`, `ScoreCalculation`
- Depends on: Puzzle Engine layer, DataStore, kotlinx.serialization
- Used by: UI layer (`GameScreen` collects `uiState` and `events`)

**UI Layer:**
- Purpose: Render state and emit user-intent callbacks; no business logic
- Location: `app/src/main/java/com/mudita/sudoku/ui/game/`
- Contains: `MenuScreen`, `DifficultyScreen`, `GameScreen`, `SummaryScreen`, `LeaderboardScreen`, `GameGrid`, `ControlsRow`, `NumberPad`
- Depends on: `GameViewModel` (only `GameScreen` takes the ViewModel directly; all other screens receive plain data and lambdas)
- Used by: `MainActivity`

**Entry Point / Navigation:**
- Purpose: Compose host, screen routing, repository construction, lifecycle save trigger
- Location: `app/src/main/java/com/mudita/sudoku/MainActivity.kt`
- Contains: `Screen` enum, `MainActivity`
- Depends on: all layers

## Data Flow

**New Game Flow:**

1. User taps "New Game" on `MenuScreen` → `onNewGame` callback fires
2. `MainActivity` sets `currentScreen = Screen.DIFFICULTY`
3. User selects difficulty on `DifficultyScreen` → `onDifficultySelected(difficulty)` fires
4. `MainActivity` calls `viewModel.startGame(difficulty)` then sets `currentScreen = Screen.GAME`
5. `GameViewModel.startGame()` emits `isLoading = true` then launches coroutine on `Dispatchers.Default` to call `SudokuGenerator.generatePuzzle(difficulty)`
6. On completion, `_uiState` is updated with fresh `GameUiState` (board, solution, givenMask)
7. `GameScreen` collects `uiState` via `collectAsStateWithLifecycle`; `GameGrid` renders board

**Player Move Flow:**

1. Player taps a cell → `GameGrid.onCellClick(index)` → `viewModel.selectCell(index)` → `_uiState.update { copy(selectedCellIndex = index) }`
2. Player taps a digit on `NumberPad` → `viewModel.enterDigit(digit)`
3. `enterDigit` routes to `applyFill` or `applyPencilMark` based on `inputMode`
4. `applyFill` pushes `GameAction.FillCell` onto `undoStack`, updates board, increments `errorCount` if wrong
5. If all 81 cells match solution, `applyFill` calls `handleCompletion()`
6. `handleCompletion` computes score, checks/saves personal best, refreshes leaderboard, clears save, emits `GameEvent.Completed`
7. `GameScreen` collects the event via `LaunchedEffect` and calls `onCompleted(CompletionResult(...))`
8. `MainActivity` sets `completionResult` then `currentScreen = Screen.SUMMARY`

**Pause/Resume Flow:**

1. App goes to background → `MainActivity.onStop()` launches `viewModel.saveNow()` on `Dispatchers.IO`
2. `saveNow()` serializes `GameUiState` → `PersistedGameState` → JSON → DataStore key `in_progress_game`
3. App restarts → `GameViewModel.init` launches load: `repository.loadGame()` returns `GameUiState?`
4. If non-null, `pendingSavedState` is set and `_showResumeDialog` emits `true`
5. `MainActivity` collects `showResumeDialog`; `MenuScreen` shows "Resume" button when `hasSavedGame = true`
6. User taps "Resume" → `viewModel.resumeGame()` restores state, clears `pendingSavedState`, navigates to `Screen.GAME`

**State Management:**

- `_uiState: MutableStateFlow<GameUiState>` — primary game state, always-current snapshot; updated with `update { copy(...) }` for immutability
- `_events: MutableSharedFlow<GameEvent>(replay=0)` — one-shot navigation signals; consumed by `LaunchedEffect` in `GameScreen`
- `_showResumeDialog: MutableStateFlow<Boolean>` — controls "Resume" button visibility in `MenuScreen`
- `_leaderboardScores: MutableStateFlow<Map<Difficulty, Int?>>` — collected directly in `MainActivity` and passed as a parameter to `LeaderboardScreen`
- `undoStack: ArrayDeque<GameAction>` — lives in `GameViewModel`, not in `GameUiState` (prevents serialization of undo history)

## Key Abstractions

**GameUiState:**
- Purpose: Immutable snapshot of all live game state
- File: `app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt`
- Pattern: `data class` with manual `equals`/`hashCode` because `IntArray` and `BooleanArray` use referential equality by default
- Fields: `board: IntArray(81)`, `solution: IntArray(81)`, `givenMask: BooleanArray(81)`, `pencilMarks: Array<Set<Int>>(81)`, `difficulty`, `selectedCellIndex`, `inputMode`, `errorCount`, `hintCount`, `isComplete`, `isLoading`

**GameAction (sealed class):**
- Purpose: Captures enough pre-action state to allow undo
- File: `app/src/main/java/com/mudita/sudoku/game/model/GameAction.kt`
- Variants: `FillCell(cellIndex, previousValue, previousPencilMarks)`, `SetPencilMark(cellIndex, digit, wasAdded)`
- Note: Hint fills are intentionally NOT pushed to `undoStack` (non-undoable by product decision)

**GameRepository / ScoreRepository (interfaces):**
- Purpose: Decouple persistence mechanism from ViewModel; enables test doubles
- Files: `app/src/main/java/com/mudita/sudoku/game/GameRepository.kt`, `ScoreRepository.kt`
- Each has a `NoOp` default implementation for callers that don't require persistence
- Concrete: `DataStoreGameRepository` (JSON via `PersistedGameState`), `DataStoreScoreRepository` (Int preferences)

**PersistedGameState:**
- Purpose: Serialization DTO bridging `GameUiState` ↔ DataStore JSON
- File: `app/src/main/java/com/mudita/sudoku/game/model/PersistedGameState.kt`
- Uses `List<T>` fields (not arrays) because kotlinx.serialization handles generic `List<T>` natively
- Extension functions `GameUiState.toPersistedState()` and `PersistedGameState.toUiState()` handle mapping
- Fields NOT persisted (reset on resume): `inputMode` → `FILL`, `isLoading` → `false`, `undoStack` → cleared

**SudokuPuzzle:**
- Purpose: Immutable puzzle output from the engine layer
- File: `app/src/main/java/com/mudita/sudoku/puzzle/model/SudokuPuzzle.kt`
- Fields: `board: IntArray(81)` (0=empty, 1-9=given), `solution: IntArray(81)` (complete), `difficulty`, `givenCount`
- `init` block enforces invariants: size=81, solution fully filled, givenCount ≥ 17

**SudokuGenerator:**
- Purpose: Produces valid, uniquely-solvable puzzles at a target difficulty via a retry loop with three acceptance gates
- File: `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt`
- Gates: (1) `UniquenessVerifier.hasUniqueSolution`, (2) given-count range from `DifficultyConfig`, (3) `DifficultyClassifier.meetsRequirements`
- Maximum 50 attempts before throwing `PuzzleGenerationException`

**DifficultyClassifier:**
- Purpose: Classifies minimum solving technique required (naked singles / hidden singles / advanced)
- File: `app/src/main/java/com/mudita/sudoku/puzzle/engine/DifficultyClassifier.kt`
- Returns `TechniqueTier`: `NAKED_SINGLES_ONLY`, `HIDDEN_PAIRS`, or `ADVANCED`
- Used by `SudokuGenerator` Gate 3 to enforce difficulty accuracy

**GameGrid (Canvas-based):**
- Purpose: Renders the 9×9 board as a single `Canvas` composable — no per-cell `Box` layout
- File: `app/src/main/java/com/mudita/sudoku/ui/game/GameGrid.kt`
- Drawing order: selected cell background → error indicators → digits → pencil marks → grid lines
- Touch hit-testing: `detectTapGestures` maps `offset.x/cellSizePx` → column, `offset.y/cellSizePx` → row → calls `onCellClick(row*9+col)`
- `CellData` is annotated `@Stable` so Compose skips recomposition for unchanged cells

## Entry Points

**MainActivity:**
- Location: `app/src/main/java/com/mudita/sudoku/MainActivity.kt`
- Triggers: Android system (app launch)
- Responsibilities: Constructs `DataStoreGameRepository` and `DataStoreScoreRepository` using lazy delegates with `applicationContext`; creates `GameViewModel` via manual factory; hosts `ThemeMMD` wrapper; owns `currentScreen` and `completionResult` state; triggers `viewModel.saveNow()` in `onStop()`

**ThemeMMD:**
- Wraps the entire Compose content tree; provides monochromatic E-ink color scheme and MMD typography; ripple effects disabled by default

## Error Handling

**Strategy:** Errors are silently counted (not surfaced to the player during play) and deducted from score at completion.

**Patterns:**
- `DataStoreGameRepository.loadGame()` catches all exceptions and returns `null` — corrupt data treated as "no saved game"
- `SudokuGenerator.generatePuzzle()` throws `PuzzleGenerationException` after `maxAttempts` exhausted — not caught by ViewModel (generation is expected to always succeed within 50 tries under normal conditions)
- Guard conditions in ViewModel actions: `if (digit !in 1..9) return`, `if (state.givenMask[idx]) return`, `if (state.isLoading || state.isComplete) return` — silent no-ops

## Cross-Cutting Concerns

**Logging:** None — no logging framework used; no `Log.d` calls in production code.
**Validation:** Input guards live in `GameViewModel` action methods, not in composables.
**Authentication:** Not applicable — local-only app, no network, no user accounts.
**E-ink constraints:** No ripple (`indication = null` on all custom clickables), no `AnimatedVisibility`, no spinners, no `Animated*` composables. Loading state shows static text only.
**Dispatcher discipline:** CPU-bound work (puzzle generation) dispatches on `Dispatchers.Default`; I/O-bound work (DataStore) dispatches on `Dispatchers.IO`; ViewModel state updates on `Dispatchers.Main` (default for `viewModelScope`).

---

*Architecture analysis: 2026-03-25*
