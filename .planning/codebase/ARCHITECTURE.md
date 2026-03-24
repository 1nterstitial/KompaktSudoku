# Architecture

> Last updated: 2026-03-24

## Pattern

**MVVM with StateFlow** — a single `GameViewModel` owns all game state, exposes it as an immutable `StateFlow<GameUiState>`, and emits one-shot outcomes via `SharedFlow<GameEvent>`. The UI layer (not yet implemented) will observe both flows and call public ViewModel methods in response to user input. The puzzle engine (domain layer) is pure Kotlin with no Android dependencies and is injected into the ViewModel as a suspend lambda for testability.

## Layers

### Domain / Puzzle Engine
- Location: `app/src/main/java/com/mudita/sudoku/puzzle/`
- Purpose: Generate valid, uniquely-solvable puzzles at a specified difficulty. Pure Kotlin — no Android dependencies.
- Depends on: Sudoklify library (`dev.teogor.sudoklify`) for raw seeded puzzle generation
- Used by: `GameViewModel` (via injected `generatePuzzle` lambda)

Key files:
- `puzzle/model/Difficulty.kt` — enum: `EASY`, `MEDIUM`, `HARD`
- `puzzle/model/DifficultyConfig.kt` — `TechniqueTier` enum + `DifficultyConfig` data class + three top-level config constants (`EASY_CONFIG`, `MEDIUM_CONFIG`, `HARD_CONFIG`) + `difficultyConfigFor()` mapping function
- `puzzle/model/SudokuPuzzle.kt` — immutable `data class`: 81-element `board` IntArray, `solution` IntArray, `difficulty`, derived `givenCount`; custom `equals`/`hashCode` for IntArray correctness
- `puzzle/engine/SudokuGenerator.kt` — wraps Sudoklify; runs three acceptance gates (uniqueness → given-count range → technique tier); up to `maxAttempts=50` retries; `verifier` and `classifier` are constructor-injected for testability
- `puzzle/engine/UniquenessVerifier.kt` — backtracking solver that aborts on the second solution; `open class` to allow subclass test doubles
- `puzzle/engine/DifficultyClassifier.kt` — human-style constraint-propagation solver; classifies minimum `TechniqueTier` (naked singles → hidden singles → advanced)
- `puzzle/engine/SudokuValidator.kt` — top-level `isValidPlacement(board, index, digit)` function + thin `SudokuValidator` class wrapper
- `puzzle/PuzzleGenerationException.kt` — thrown when `SudokuGenerator` exhausts `maxAttempts`

### Game State (ViewModel + Models)
- Location: `app/src/main/java/com/mudita/sudoku/game/`
- Purpose: Own all mutable game state; implement the full game loop (digit entry, pencil marks, undo, error tracking, completion detection).
- Depends on: Domain layer (`SudokuGenerator`, `SudokuPuzzle`, `Difficulty`)
- Used by: Presentation layer (not yet implemented)

Key files:
- `game/GameViewModel.kt` — `ViewModel` subclass; owns `MutableStateFlow<GameUiState>` and `MutableSharedFlow<GameEvent>`; holds `undoStack: ArrayDeque<GameAction>` outside `GameUiState` to keep state immutable
- `game/model/GameUiState.kt` — immutable `data class` snapshot of all UI-visible state; manual `equals`/`hashCode` for `IntArray` and `Array<Set<Int>>` fields
- `game/model/GameAction.kt` — `sealed class` for undoable actions: `FillCell(cellIndex, previousValue, previousPencilMarks)` and `SetPencilMark(cellIndex, digit, wasAdded)`
- `game/model/GameEvent.kt` — `sealed class` for one-shot events: `Completed(errorCount)`
- `game/model/InputMode.kt` — enum: `FILL`, `PENCIL`

### Presentation (UI)
- Location: `app/src/main/java/com/mudita/sudoku/MainActivity.kt`
- Status: Stub only — `MainActivity` extends `Activity` with an empty `onCreate`. No Compose UI is implemented yet. This layer is the target of phase 03.

### Persistence (Planned)
- Location: Not yet created
- Purpose: Pause/resume game state and per-difficulty high scores via DataStore Preferences
- Storage keys: `in_progress_game` (JSON-serialized `GameState`), `high_score_easy`, `high_score_medium`, `high_score_hard`

## State Management

State flows in one direction:

1. `GameViewModel` holds `_uiState: MutableStateFlow<GameUiState>` (private); exposes `uiState: StateFlow<GameUiState>` (read-only).
2. Every user action calls a ViewModel public method (`startGame`, `selectCell`, `enterDigit`, `toggleInputMode`, `undo`).
3. Each method produces a new `GameUiState` via `.copy()` and calls `_uiState.update { ... }` — no in-place mutation.
4. Compose UI will observe `uiState` via `collectAsStateWithLifecycle()` and recompose on each emission.
5. One-shot outcomes (puzzle completed) are emitted via `_events: MutableSharedFlow<GameEvent>` with `replay=0` — not replayed on recomposition.

**Undo stack** (`ArrayDeque<GameAction>`) lives directly on `GameViewModel` (not inside `GameUiState`) so that `GameUiState` stays a pure immutable value.

**Error count is permanent** — `errorCount` increments on a wrong fill and is never decremented on undo (design rule SCORE-01).

## Key Data Flows

### Puzzle Generation Flow
1. UI (future) calls `viewModel.startGame(difficulty)`.
2. `_uiState.update { it.copy(isLoading = true) }` emitted immediately on the main thread.
3. `viewModelScope.launch` runs; `generatePuzzle(difficulty)` dispatched on `Dispatchers.Default` (CPU-bound).
4. `SudokuGenerator.generatePuzzle()` calls `SudoklifyArchitect` with a random seed, then gates the result:
   - Gate 1: `UniquenessVerifier.hasUniqueSolution()` — abort-on-second-solution backtracking
   - Gate 2: Given-count range check against `DifficultyConfig.minGivens..maxGivens`
   - Gate 3: `DifficultyClassifier.meetsRequirements()` — technique-tier constraint propagation
5. On acceptance, `undoStack.clear()` and a full `GameUiState` is constructed from `puzzle.board`, `puzzle.solution`, and a computed `givenMask` (`BooleanArray(81) { puzzle.board[i] != 0 }`).
6. `_uiState.update { ... }` emits the loaded state with `isLoading = false`.

### Digit Entry Flow (FILL mode)
1. UI calls `viewModel.selectCell(index)` → `_uiState.update { it.copy(selectedCellIndex = index) }`.
2. UI calls `viewModel.enterDigit(digit)`.
3. Guards: digit in 1..9, cell selected, cell not flagged in `givenMask`. Silent no-op if any guard fails.
4. `applyFill(idx, digit, state)`: pushes `GameAction.FillCell` onto `undoStack`; copies board; places digit; checks `digit != solution[idx]` → increments `errorCount`; clears `pencilMarks[idx]`; checks all-correct for completion.
5. `_uiState.update { ... }` emits new state; if `isComplete`, emits `GameEvent.Completed(errorCount)` on `_events`.

### Digit Entry Flow (PENCIL mode)
1. Same guards as FILL mode.
2. `applyPencilMark(idx, digit, state)`: toggles `digit` in `pencilMarks[idx]` set; pushes `GameAction.SetPencilMark(idx, digit, wasAdded)` onto `undoStack`.
3. `_uiState.update { it.copy(pencilMarks = newMarks) }`.

### Undo Flow
1. UI calls `viewModel.undo()`.
2. If `undoStack` is empty: no-op.
3. `undoStack.removeLast()` pops the most recent `GameAction`.
4. `FillCell`: restores `board[cellIndex]` to `previousValue` and `pencilMarks[cellIndex]` to `previousPencilMarks`; rechecks completion; does NOT decrement `errorCount`.
5. `SetPencilMark`: reverses the toggle based on `wasAdded` flag.
6. `_uiState.update { ... }` emits new state.

## Entry Points

**`MainActivity`** (`app/src/main/java/com/mudita/sudoku/MainActivity.kt`):
- Stub `Activity` subclass; Compose `setContent` and ViewModel wiring to be added in phase 03.

**`GameViewModel`** (`app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt`):
- Created via Compose `viewModel()` factory or constructor-injected in tests.
- Public API: `startGame(Difficulty)`, `selectCell(Int)`, `enterDigit(Int)`, `toggleInputMode()`, `undo()`.
- Exposes: `uiState: StateFlow<GameUiState>`, `events: SharedFlow<GameEvent>`.
- Injected dependency: `generatePuzzle: suspend (Difficulty) -> SudokuPuzzle` — defaults to `SudokuGenerator().generatePuzzle(difficulty)`.

## Error Handling

- `SudokuGenerator` throws `PuzzleGenerationException` after `maxAttempts` failed candidates. Currently not caught at the ViewModel call site — will require a try/catch and error UI state in phase 03.
- Sudoklify library errors are caught inside `tryGenerateCandidate()` and treated as retry signals (return `null`).
- ViewModel action guards (`selectCell`, `enterDigit`, `undo`) use silent early-return no-ops rather than exceptions.

## Cross-Cutting Concerns

**Threading:** Puzzle generation dispatched on `Dispatchers.Default` via `withContext` in `GameViewModel.startGame`. All state updates happen on the main thread via `_uiState.update`.

**Immutability:** `GameUiState` is a `data class` updated exclusively via `.copy()`. Array fields (`board`, `solution`, `givenMask`, `pencilMarks`) are always copied (`copyOf()`) before modification.

**Testability:** `SudokuGenerator` accepts `UniquenessVerifier` and `DifficultyClassifier` as constructor parameters. `GameViewModel` accepts a `generatePuzzle` lambda — `FakeGenerator` is injected in tests without a mocking framework.

**No persistence yet:** DataStore is declared as a dependency in `app/build.gradle.kts` but has not been integrated into the codebase.

**No UI yet:** The Compose layer does not exist. `MainActivity` is a bare stub.
