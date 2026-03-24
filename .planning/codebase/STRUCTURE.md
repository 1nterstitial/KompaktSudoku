# Structure

> Last updated: 2026-03-24

## Directory Layout

```
KompaktSudoku/
├── app/
│   ├── build.gradle.kts                    # App module: dependencies, SDK levels, Compose + serialization plugins
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/mudita/sudoku/
│       │   │   ├── MainActivity.kt                    # Entry point (stub — no Compose content yet)
│       │   │   ├── game/                              # MVVM game layer (phase 02)
│       │   │   │   ├── GameViewModel.kt               # State machine; StateFlow + SharedFlow
│       │   │   │   └── model/
│       │   │   │       ├── GameAction.kt              # Sealed: FillCell, SetPencilMark (undo records)
│       │   │   │       ├── GameEvent.kt               # Sealed: Completed(errorCount) one-shot event
│       │   │   │       ├── GameUiState.kt             # Immutable UI state snapshot (data class)
│       │   │   │       └── InputMode.kt               # Enum: FILL, PENCIL
│       │   │   └── puzzle/                            # Puzzle engine (phase 01)
│       │   │       ├── PuzzleGenerationException.kt   # Thrown after maxAttempts exhausted
│       │   │       ├── engine/
│       │   │       │   ├── DifficultyClassifier.kt    # Technique-tier classifier (constraint propagation)
│       │   │       │   ├── SudokuGenerator.kt         # Sudoklify wrapper + 3-gate acceptance loop
│       │   │       │   ├── SudokuValidator.kt         # isValidPlacement() top-level fn + class wrapper
│       │   │       │   └── UniquenessVerifier.kt      # Abort-on-second-solution backtracking solver
│       │   │       └── model/
│       │   │           ├── Difficulty.kt              # Enum: EASY, MEDIUM, HARD
│       │   │           ├── DifficultyConfig.kt        # TechniqueTier enum, DifficultyConfig data class,
│       │   │           │                              # EASY/MEDIUM/HARD_CONFIG constants, difficultyConfigFor()
│       │   │           └── SudokuPuzzle.kt            # Immutable puzzle value object
│       │   └── res/
│       │       └── values/
│       │           ├── strings.xml
│       │           └── themes.xml
│       └── test/
│           └── java/com/mudita/sudoku/
│               ├── game/
│               │   ├── FakeGenerator.kt               # Deterministic puzzle stub (known board + solution)
│               │   ├── GameUiStateTest.kt             # Equals/hashCode tests for GameUiState
│               │   ├── GameViewModelTest.kt           # Full ViewModel test suite (Robolectric + Turbine)
│               │   └── MainDispatcherRule.kt          # JUnit rule: replaces Main dispatcher with test dispatcher
│               └── puzzle/
│                   ├── DifficultyClassifierTest.kt    # Technique-tier classification unit tests
│                   ├── SudokuEngineIntegrationTest.kt # Batch integration tests (60 puzzles)
│                   ├── SudokuGeneratorTest.kt         # Generator acceptance gate unit tests
│                   ├── SudokuValidatorTest.kt         # isValidPlacement unit tests
│                   └── UniquenessVerifierTest.kt      # Solution-counting unit tests
├── gradle/
│   ├── libs.versions.toml                  # Version catalog — single source of truth for all dependency versions
│   └── wrapper/
│       └── gradle-wrapper.properties       # Gradle 8.11.1
├── build.gradle.kts                        # Root build: plugin declarations only
├── settings.gradle.kts                     # Module registration: includes :app
├── gradle.properties                       # JVM args, AndroidX flag
├── local.properties                        # SDK path (not committed)
└── .planning/                              # GSD workflow artifacts (not shipped with APK)
    ├── PROJECT.md                          # Project charter and constraints
    ├── REQUIREMENTS.md                     # All v1 requirements
    ├── ROADMAP.md                          # Phase definitions and success criteria
    ├── STATE.md                            # Current phase position and key decisions
    ├── codebase/                           # Codebase maps (this file)
    ├── research/                           # Pre-project research artifacts
    └── phases/
        ├── 01-puzzle-engine/               # Phase 01 plans and verification
        ├── 02-game-state-domain/           # Phase 02 plans and verification
        └── 03-core-game-ui/               # Phase 03 context (in progress)
```

## Key Files

| File | Purpose |
|------|---------|
| `gradle/libs.versions.toml` | Single source of truth for all library versions and plugin aliases |
| `app/build.gradle.kts` | Module dependencies, SDK levels (min=31, target=31, compile=35), Kotlin compiler options |
| `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` | Central game state machine — all gameplay logic lives here |
| `app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt` | Complete UI state snapshot; 10 fields covering board, selection, pencil marks, errors, completion |
| `app/src/main/java/com/mudita/sudoku/game/model/GameAction.kt` | Undo record types: `FillCell` and `SetPencilMark` |
| `app/src/main/java/com/mudita/sudoku/game/model/GameEvent.kt` | One-shot event for puzzle completion with final error count |
| `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt` | Puzzle generation: Sudoklify wrapper + uniqueness + given-count + technique-tier gates |
| `app/src/main/java/com/mudita/sudoku/puzzle/model/DifficultyConfig.kt` | Difficulty parameters: given-count ranges and technique-tier requirements per level |
| `app/src/main/java/com/mudita/sudoku/puzzle/model/SudokuPuzzle.kt` | Immutable puzzle domain model with init-block invariant assertions |
| `app/src/test/java/com/mudita/sudoku/game/FakeGenerator.kt` | Deterministic test double: fixed 81-cell board with 20 known empty cells |
| `app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt` | Comprehensive ViewModel test suite: ~40 tests covering all game actions |
| `app/src/test/java/com/mudita/sudoku/puzzle/SudokuEngineIntegrationTest.kt` | Batch integration tests: 60 puzzles across difficulty levels |
| `.planning/ROADMAP.md` | Phase sequence, goals, and success criteria for all 6 planned phases |
| `.planning/STATE.md` | Current active phase, completed plans, accumulated decisions |

## Package Organization

Root package: `com.mudita.sudoku`

| Package | Contents | Layer |
|---------|----------|-------|
| `com.mudita.sudoku` | `MainActivity` | Entry point (stub) |
| `com.mudita.sudoku.game` | `GameViewModel` | MVVM — ViewModel |
| `com.mudita.sudoku.game.model` | `GameUiState`, `GameAction`, `GameEvent`, `InputMode` | MVVM — state models |
| `com.mudita.sudoku.puzzle` | `PuzzleGenerationException` | Domain — exceptions |
| `com.mudita.sudoku.puzzle.engine` | `SudokuGenerator`, `UniquenessVerifier`, `DifficultyClassifier`, `SudokuValidator` | Domain — engine logic |
| `com.mudita.sudoku.puzzle.model` | `Difficulty`, `DifficultyConfig`, `SudokuPuzzle` | Domain — value objects |

Packages not yet created (planned):
- `com.mudita.sudoku.ui` — Compose screens and components (phase 03)
- `com.mudita.sudoku.ui.navigation` — Navigation graph (phase 06)
- `com.mudita.sudoku.data` — DataStore repository (phase 04)

## Module Boundaries

**`:app` is the only Gradle module.** There is no multi-module structure. All code lives under `app/src/`.

The domain (`puzzle/`) and game-state (`game/`) packages are separated by convention only, not module boundaries. The key rule: `puzzle/` has no Android imports; `game/` imports `androidx.lifecycle.ViewModel` and Kotlin coroutines, but nothing from the UI layer.

**Dependency direction (enforced by convention):**
```
UI (not yet built)
    ↓
game/ (GameViewModel, GameUiState)
    ↓
puzzle/ (SudokuGenerator, SudokuPuzzle, Difficulty)
    ↓
Sudoklify library
```

## Naming Conventions

**Source files:** `PascalCase.kt`, one primary class/object per file matching the filename.

**Test files:** `{SourceFileName}Test.kt` in a mirrored package under `src/test/`. Example: `SudokuGenerator.kt` → `SudokuGeneratorTest.kt`.

**Test support files:** Descriptive names without `Test` suffix — `FakeGenerator.kt`, `MainDispatcherRule.kt`.

## Where to Add New Code

**New Compose UI screen:**
- Create under `app/src/main/java/com/mudita/sudoku/ui/`
- Example: `app/src/main/java/com/mudita/sudoku/ui/GameScreen.kt`
- Wire into `MainActivity.kt` via `setContent { ThemeMMD { GameScreen(...) } }`

**New ViewModel method or state field:**
- Add to `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt`
- Add corresponding field to `app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt`
- Add test coverage in `app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt`

**New undoable action type:**
- Add a new subclass to `app/src/main/java/com/mudita/sudoku/game/model/GameAction.kt`
- Handle the new case in `GameViewModel.undo()`

**New one-shot event:**
- Add a new subclass to `app/src/main/java/com/mudita/sudoku/game/model/GameEvent.kt`
- Emit via `_events.emit(...)` in `GameViewModel`

**DataStore persistence (phase 04):**
- Create `app/src/main/java/com/mudita/sudoku/data/GameRepository.kt`
- Inject into `GameViewModel` as a constructor parameter

**New puzzle engine component:**
- Pure Kotlin: place in `app/src/main/java/com/mudita/sudoku/puzzle/engine/`
- With corresponding test in `app/src/test/java/com/mudita/sudoku/puzzle/`
