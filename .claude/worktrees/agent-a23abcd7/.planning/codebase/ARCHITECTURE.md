# Architecture

**Analysis Date:** 2026-03-24

## Pattern Overview

**Overall:** MVVM with StateFlow (mandated by project constraints)

**Key Characteristics:**
- Single Android module, single-activity app (`MainActivity`)
- Puzzle generation engine is pure Kotlin — no Android dependencies, fully unit-testable on JVM
- Presentation layer (ViewModels, UI State, Composables) is not yet implemented; only the domain/engine layer exists as of Phase 1
- All persistence to be done via DataStore Preferences (async, coroutine-native)

## Layers

**Domain / Engine:**
- Purpose: Puzzle generation, validation, classification, and domain model definitions
- Location: `app/src/main/java/com/mudita/sudoku/puzzle/`
- Contains: Domain models (`model/`), engine services (`engine/`), and exceptions
- Depends on: Sudoklify library (`dev.teogor.sudoklify`) for raw puzzle seeding
- Used by: Presentation layer (ViewModel) — not yet implemented

**Presentation (planned — Phase 2+):**
- Purpose: MVVM ViewModel contract, UI state holder, Composable screens
- Location: `app/src/main/java/com/mudita/sudoku/` (future packages: `ui/`, `viewmodel/`)
- Contains: ViewModels exposing StateFlow<UiState>, Composable screens using MMD components
- Depends on: Domain layer, DataStore (Phase 4)
- Used by: `MainActivity`

**Persistence (planned — Phase 4):**
- Purpose: Pause/resume state and per-difficulty high scores
- Location: Not yet created
- Contains: DataStore Preferences repository
- Storage keys: `in_progress_game` (JSON string), `high_score_easy`, `high_score_medium`, `high_score_hard`

## Data Flow

**Puzzle Generation Flow (implemented):**

1. Caller (future ViewModel) invokes `SudokuGenerator.generatePuzzle(difficulty: Difficulty)`
2. `SudokuGenerator` calls `tryGenerateCandidate()` which delegates to `SudoklifyArchitect` with a random seed
3. Raw board and solution grids are flattened from `List<List<Int>>` to `IntArray(81)` (index = `row*9+col`)
4. Gate 1: `UniquenessVerifier.hasUniqueSolution()` runs abort-on-second-solution backtracking — rejects if not unique
5. Gate 2: Given-count range check against `DifficultyConfig` — rejects if outside `minGivens..maxGivens`
6. Gate 3: `DifficultyClassifier.meetsRequirements()` runs human-style constraint propagation — rejects if technique tier doesn't match
7. Accepted candidate is returned as an immutable `SudokuPuzzle`; on exhausting `maxAttempts`, throws `PuzzleGenerationException`

**Game Play Flow (planned — Phase 2–3):**

1. User selects difficulty on menu screen
2. ViewModel calls `SudokuGenerator.generatePuzzle(difficulty)`
3. ViewModel emits `GameUiState` via `StateFlow`
4. Composable collects state via `collectAsStateWithLifecycle()`
5. User taps cell → ViewModel updates selected cell in state
6. User taps digit → ViewModel validates against `SudokuPuzzle.solution`, increments silent error count if wrong
7. On pause: ViewModel serializes `GameState` to JSON via `kotlinx.serialization`, writes to DataStore
8. On all 81 cells filled correctly: ViewModel triggers completion, computes score

**State Management:**
- StateFlow held in ViewModel, collected in Composables via `collectAsStateWithLifecycle()`
- Game state serialized as JSON string to DataStore for persistence across app lifecycle

## Key Abstractions

**SudokuPuzzle:**
- Purpose: Immutable value object representing a generated puzzle
- File: `app/src/main/java/com/mudita/sudoku/puzzle/model/SudokuPuzzle.kt`
- Pattern: Kotlin `data class` with custom `equals`/`hashCode` due to `IntArray` fields
- Board encoding: 81-element `IntArray`, index `= row*9+col`, `0` = empty, `1–9` = given digit

**Difficulty:**
- Purpose: Enum of the three selectable difficulty levels
- File: `app/src/main/java/com/mudita/sudoku/puzzle/model/Difficulty.kt`
- Values: `EASY`, `MEDIUM`, `HARD`

**DifficultyConfig:**
- Purpose: Per-difficulty constraint parameters (given-count range, required technique tier)
- File: `app/src/main/java/com/mudita/sudoku/puzzle/model/DifficultyConfig.kt`
- Access: `difficultyConfigFor(difficulty)` factory function
- Concrete configs: `EASY_CONFIG` (36–45 givens), `MEDIUM_CONFIG` (27–35 givens), `HARD_CONFIG` (22–27 givens)

**TechniqueTier:**
- Purpose: Enum classifying puzzle solving complexity for the difficulty gate
- File: `app/src/main/java/com/mudita/sudoku/puzzle/model/DifficultyConfig.kt`
- Values: `NAKED_SINGLES_ONLY`, `HIDDEN_PAIRS`, `ADVANCED`

**SudokuGenerator:**
- Purpose: Orchestrates puzzle generation with three acceptance gates
- File: `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt`
- Pattern: Constructor injection of `UniquenessVerifier` and `DifficultyClassifier` for testability; retry loop up to `maxAttempts` (default 50)

**UniquenessVerifier:**
- Purpose: Verifies exactly one solution exists via backtracking with early abort
- File: `app/src/main/java/com/mudita/sudoku/puzzle/engine/UniquenessVerifier.kt`
- Pattern: `open class` to allow test doubles without a full mocking framework

**DifficultyClassifier:**
- Purpose: Determines minimum technique tier needed to solve a puzzle without guessing
- File: `app/src/main/java/com/mudita/sudoku/puzzle/engine/DifficultyClassifier.kt`
- Pattern: Attempt progressively harder solving passes; first pass that fully solves the board determines the tier

**SudokuValidator / isValidPlacement:**
- Purpose: Checks a digit placement for row/column/box conflicts
- File: `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuValidator.kt`
- Pattern: Top-level function `isValidPlacement(board, index, digit)` plus a thin `SudokuValidator` class wrapper for injection

## Entry Points

**Application Entry:**
- Location: `app/src/main/java/com/mudita/sudoku/MainActivity.kt`
- Pattern: Minimal `Activity` subclass; Compose `setContent` and ViewModel wiring to be added in Phase 2–3

**Puzzle Engine Entry:**
- Location: `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt`
- Triggers: Called by ViewModel (future) to obtain a `SudokuPuzzle` on game start

## Error Handling

**Strategy:** Exception-based for generation failures; silent tracking for gameplay errors

**Patterns:**
- `PuzzleGenerationException` (checked equivalent) thrown when `SudokuGenerator` exhausts all retry attempts — caller (ViewModel) must handle and show error UI
- Sudoklify library errors caught internally in `tryGenerateCandidate()` and treated as retry signals (return `null`)
- Gameplay errors (wrong cell fill) tracked silently as a counter; not surfaced until game completion (requirement SCORE-01)

## Cross-Cutting Concerns

**Logging:** None currently; `println`/`Log` not used in production code
**Validation:** `init` blocks in `SudokuPuzzle` assert invariants (board size = 81, solution has no zeros, givenCount ≥ 17)
**Authentication:** Not applicable (offline-only app)
**Serialization:** `kotlinx.serialization` with `@Serializable` on domain models for DataStore persistence (Phase 4)
**Threading:** Puzzle generation is synchronous pure Kotlin; ViewModel will wrap in `viewModelScope` coroutine (Phase 2)

---

*Architecture analysis: 2026-03-24*
