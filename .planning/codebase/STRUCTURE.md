# Codebase Structure

**Analysis Date:** 2026-03-25

## Directory Layout

```
KompaktSudoku/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/mudita/sudoku/
│       │   │   ├── MainActivity.kt            # Activity, Screen enum, nav host
│       │   │   ├── game/                      # ViewModel, repositories, persistence
│       │   │   │   ├── GameViewModel.kt
│       │   │   │   ├── GameRepository.kt      # Interface + NoOpGameRepository
│       │   │   │   ├── ScoreRepository.kt     # Interface + NoOpScoreRepository
│       │   │   │   ├── DataStoreGameRepository.kt
│       │   │   │   ├── DataStoreScoreRepository.kt
│       │   │   │   └── model/                 # Pure domain model types
│       │   │   │       ├── GameUiState.kt
│       │   │   │       ├── GameAction.kt
│       │   │   │       ├── GameEvent.kt
│       │   │   │       ├── CompletionResult.kt
│       │   │   │       ├── InputMode.kt
│       │   │   │       ├── PersistedGameState.kt
│       │   │   │       └── ScoreCalculation.kt
│       │   │   ├── puzzle/                    # Puzzle engine — no Android deps
│       │   │   │   ├── PuzzleGenerationException.kt
│       │   │   │   ├── engine/                # Generation, validation, classification
│       │   │   │   │   ├── SudokuGenerator.kt
│       │   │   │   │   ├── SudokuValidator.kt
│       │   │   │   │   ├── UniquenessVerifier.kt
│       │   │   │   │   └── DifficultyClassifier.kt
│       │   │   │   └── model/                 # Puzzle domain types
│       │   │   │       ├── SudokuPuzzle.kt
│       │   │   │       ├── Difficulty.kt
│       │   │   │       └── DifficultyConfig.kt  # TechniqueTier enum + configs
│       │   │   └── ui/
│       │   │       └── game/                  # All screen composables + sub-components
│       │   │           ├── GameScreen.kt      # Root game composable; wires ViewModel
│       │   │           ├── GameGrid.kt        # Canvas-based 9×9 grid
│       │   │           ├── ControlsRow.kt     # Fill/Pencil/Undo/Hint buttons
│       │   │           ├── NumberPad.kt       # Digits 1–9 + Erase
│       │   │           ├── MenuScreen.kt
│       │   │           ├── DifficultyScreen.kt
│       │   │           ├── SummaryScreen.kt
│       │   │           └── LeaderboardScreen.kt
│       │   ├── res/                           # Android resources (standard)
│       │   └── AndroidManifest.xml
│       └── test/
│           └── java/com/mudita/sudoku/
│               ├── game/                      # ViewModel + repository tests + fakes
│               │   ├── GameViewModelTest.kt
│               │   ├── GameViewModelEraseTest.kt
│               │   ├── GameViewModelHintTest.kt
│               │   ├── GameViewModelPersistenceTest.kt
│               │   ├── GameUiStateTest.kt
│               │   ├── PersistedGameStateTest.kt
│               │   ├── ScoreCalculationTest.kt
│               │   ├── DataStoreScoreRepositoryTest.kt
│               │   ├── FakeGameRepository.kt
│               │   ├── FakeScoreRepository.kt
│               │   ├── FakeGenerator.kt
│               │   └── MainDispatcherRule.kt
│               ├── puzzle/                    # Engine unit + integration tests
│               │   ├── SudokuGeneratorTest.kt
│               │   ├── SudokuValidatorTest.kt
│               │   ├── UniquenessVerifierTest.kt
│               │   ├── DifficultyClassifierTest.kt
│               │   └── SudokuEngineIntegrationTest.kt
│               └── ui/game/                   # Compose UI tests (Robolectric)
│                   ├── GameScreenTest.kt
│                   ├── GameGridTest.kt
│                   ├── ControlsRowTest.kt
│                   ├── NumberPadTest.kt
│                   ├── MenuScreenTest.kt
│                   ├── DifficultyScreenTest.kt
│                   ├── SummaryScreenTest.kt
│                   └── LeaderboardScreenTest.kt
├── gradle/
│   └── libs.versions.toml                     # Version catalog for all dependencies
├── build.gradle.kts                           # Root build script
└── app/build.gradle.kts                       # App module build script
```

## Directory Purposes

**`game/`:**
- Purpose: Everything required to run the game loop — ViewModel, persistence contracts, DataStore implementations
- Key files: `GameViewModel.kt` (state machine), `DataStoreGameRepository.kt` (JSON save/load), `DataStoreScoreRepository.kt` (high score storage)

**`game/model/`:**
- Purpose: Pure data types with no Android or framework dependencies
- Key files: `GameUiState.kt` (primary state type), `PersistedGameState.kt` (serialization bridge), `ScoreCalculation.kt` (pure score formula)

**`puzzle/`:**
- Purpose: Self-contained puzzle engine — can be extracted as a library module without modification
- Contains: Generation, validation, uniqueness verification, difficulty classification, and all puzzle domain types

**`puzzle/engine/`:**
- Purpose: Algorithmic logic for generating and verifying puzzles
- Key files: `SudokuGenerator.kt` (entry point; 50-attempt retry loop with 3 acceptance gates), `DifficultyClassifier.kt` (technique-tier analysis), `UniquenessVerifier.kt` (backtracking abort-on-second)

**`puzzle/model/`:**
- Purpose: Domain types used by engine and consumed by ViewModel
- Key files: `DifficultyConfig.kt` defines `TechniqueTier` enum and the three `DifficultyConfig` constants (`EASY_CONFIG`, `MEDIUM_CONFIG`, `HARD_CONFIG`)

**`ui/game/`:**
- Purpose: All Compose screens and sub-components; all composables are stateless
- Only `GameScreen` accepts the `GameViewModel` directly; all other screens receive plain types and lambdas

**`test/game/`:**
- Purpose: Unit tests for ViewModel, repositories, and domain model
- Contains test doubles: `FakeGameRepository`, `FakeScoreRepository`, `FakeGenerator`; `MainDispatcherRule` for coroutine dispatcher substitution

**`test/ui/game/`:**
- Purpose: Compose UI tests run on Robolectric (JVM, no emulator required)

## Key File Locations

**Entry Points:**
- `app/src/main/java/com/mudita/sudoku/MainActivity.kt` — Activity, Screen enum, nav routing, repository construction, lifecycle save

**Primary State:**
- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` — all game logic, state machine, undo, hint, persistence triggers

**UI State Contract:**
- `app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt` — complete UI state snapshot
- `app/src/main/java/com/mudita/sudoku/game/model/GameEvent.kt` — one-shot events (completion)
- `app/src/main/java/com/mudita/sudoku/game/model/GameAction.kt` — undo action types

**Persistence:**
- `app/src/main/java/com/mudita/sudoku/game/DataStoreGameRepository.kt` — save/load in-progress game as JSON
- `app/src/main/java/com/mudita/sudoku/game/DataStoreScoreRepository.kt` — read/write high scores as Int preferences
- `app/src/main/java/com/mudita/sudoku/game/model/PersistedGameState.kt` — JSON DTO + mapping functions

**Puzzle Engine:**
- `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt` — public API for puzzle generation
- `app/src/main/java/com/mudita/sudoku/puzzle/model/DifficultyConfig.kt` — difficulty parameters (given-count ranges, technique tiers)

**Score:**
- `app/src/main/java/com/mudita/sudoku/game/model/ScoreCalculation.kt` — `fun calculateScore(errorCount, hintCount)` → `max(0, 100 - errorCount*10 - hintCount*5)`

**Grid Rendering:**
- `app/src/main/java/com/mudita/sudoku/ui/game/GameGrid.kt` — Canvas-based grid with `CellData` wrapper

## Naming Conventions

**Files:**
- PascalCase matching the primary class or interface declared inside: `GameViewModel.kt`, `SudokuGenerator.kt`, `DifficultyScreen.kt`
- Interface + NoOp default in the same file: `GameRepository.kt` contains `GameRepository` interface and `NoOpGameRepository`
- Configuration constants in model files: `DifficultyConfig.kt` contains `TechniqueTier`, `DifficultyConfig`, and top-level `val` constants

**Packages:**
- Grouped by responsibility domain, not by layer: `game/` contains ViewModel + repositories + model; `puzzle/` contains engine + model; `ui/game/` contains all screens
- Sub-packages use singular nouns: `engine/`, `model/`

**Classes and interfaces:**
- Interfaces: plain noun (`GameRepository`, `ScoreRepository`)
- Implementations: `DataStore` prefix for DataStore-backed classes (`DataStoreGameRepository`)
- No-op defaults: `NoOp` prefix (`NoOpGameRepository`, `NoOpScoreRepository`)
- Screens: `Screen` suffix (`MenuScreen`, `GameScreen`, `SummaryScreen`)
- ViewModel: `ViewModel` suffix (`GameViewModel`)
- Exceptions: `Exception` suffix (`PuzzleGenerationException`)

**DataStore keys:**
- Snake case strings: `"in_progress_game"`, `"high_score_easy"`, `"high_score_medium"`, `"high_score_hard"`
- DataStore file names: `"game_state"`, `"score_state"`

## Where to Add New Code

**New game action (e.g. auto-fill pencil marks):**
- Add variant to `app/src/main/java/com/mudita/sudoku/game/model/GameAction.kt`
- Implement handler in `GameViewModel.kt`
- If the action adds UI controls, add to `ControlsRow.kt`

**New screen:**
- Add `Screen.NEW_SCREEN` to the `Screen` enum in `MainActivity.kt`
- Create `app/src/main/java/com/mudita/sudoku/ui/game/NewScreen.kt` — stateless composable with data params and callbacks
- Add `when` branch in `MainActivity.kt` `setContent` block
- Add corresponding test at `app/src/test/java/com/mudita/sudoku/ui/game/NewScreenTest.kt`

**New persistence field:**
- If part of game save state: add to `GameUiState.kt`, update `PersistedGameState.kt` and both mapping functions
- If a new score/preference: add a new `Preferences.Key` in `DataStoreScoreRepository.kt` and add a method to `ScoreRepository`

**New puzzle difficulty:**
- Add value to `Difficulty.kt` enum
- Add `DifficultyConfig` constant in `DifficultyConfig.kt`
- Update `difficultyConfigFor()` `when` expression
- Update `DataStoreScoreRepository.toPreferenceKey()` and add key constant
- Update `GameViewModel.refreshLeaderboard()` to include new difficulty
- Update `LeaderboardScreen.kt` to render the new row

**New engine component:**
- Place in `app/src/main/java/com/mudita/sudoku/puzzle/engine/`
- Inject into `SudokuGenerator` constructor (follow existing `verifier`/`classifier` pattern)

**Utilities / pure functions:**
- Domain-layer pure functions: `app/src/main/java/com/mudita/sudoku/game/model/` (e.g., `ScoreCalculation.kt`)
- Puzzle-engine pure functions: top-level in relevant engine file (e.g., `isValidPlacement` in `SudokuValidator.kt`)

---

*Structure analysis: 2026-03-25*
