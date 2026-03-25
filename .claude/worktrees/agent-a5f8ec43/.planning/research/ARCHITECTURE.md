# Architecture Patterns

**Domain:** Sudoku game app on Mudita Kompakt (E-ink Android, MMD + Jetpack Compose)
**Researched:** 2026-03-23
**Overall confidence:** HIGH (core Android patterns verified via official docs; MMD-specific details MEDIUM via community reference app)

---

## Recommended Architecture

**MVVM with Unidirectional Data Flow (UDF)**, structured in three layers: UI, ViewModel, and Data. This aligns with the project's stated constraint (MVVM + StateFlow) and with MMD's integration model, which follows standard Compose ViewModel conventions.

### Layer Overview

```
┌─────────────────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose + MMD components)            │
│  Screens: MenuScreen, GameScreen, SummaryScreen,        │
│           LeaderboardScreen                             │
│  Events flow UP ──► ViewModel                          │
│  State flows DOWN ◄── StateFlow                        │
└───────────────┬─────────────────────────────────────────┘
                │ collectAsState()
┌───────────────▼─────────────────────────────────────────┐
│  ViewModel Layer                                         │
│  GameViewModel (game logic, scoring, hint tracking)     │
│  MenuViewModel (difficulty selection, navigation)       │
│  LeaderboardViewModel (high score reads/writes)         │
└──────────┬──────────────────────────┬───────────────────┘
           │                          │
┌──────────▼──────────┐  ┌────────────▼───────────────────┐
│  Puzzle Engine       │  │  Data Layer                    │
│  (pure Kotlin,       │  │  GameStateRepository           │
│   no Android deps)   │  │  HighScoreRepository           │
│  Generator           │  │  DataStore (Proto or Prefs)    │
│  Validator           │  │  (local storage only)          │
└─────────────────────┘  └────────────────────────────────┘
```

---

## Component Boundaries

### 1. Puzzle Engine (Pure Kotlin, No Android Dependencies)

**Responsibility:** Generate valid Sudoku puzzles, validate moves, compute solutions, provide hints.

**Boundary:** No Android imports. No Compose. Pure Kotlin objects and data classes only. Testable with plain JUnit.

| Class | Responsibility |
|-------|---------------|
| `SudokuGenerator` | Generates a fully solved 9×9 grid via backtracking (fill diagonal boxes, then recurse remaining cells) |
| `SudokuPuzzleFactory` | Removes cells from the solved grid to produce a playable puzzle at the requested difficulty |
| `SudokuValidator` | Checks row, column, and 3×3 box constraints for a given board state |
| `SudokuSolver` | Backtracking solver used internally by generator and hint logic |
| `DifficultyConfig` | Data class encoding cells-revealed range and minimum solving technique required per difficulty (Easy, Medium, Hard) |

**Difficulty encoding (MEDIUM confidence — standard practice, not official specification):**
- Easy: ~46–51 givens; solvable by naked singles only
- Medium: ~36–45 givens; requires hidden singles / naked pairs
- Hard: ~27–35 givens; requires more advanced constraint techniques

**Key invariant:** `SudokuGenerator` guarantees unique solution. Generation rejects puzzles that admit multiple solutions by running `SudokuSolver` after removing cells and verifying exactly one solution exists.

---

### 2. Game State (Domain Model)

**Responsibility:** Represent the complete in-progress game as immutable data classes. No UI logic. No Android deps.

```kotlin
data class SudokuCell(
    val row: Int,
    val col: Int,
    val value: Int,         // 0 = empty
    val isGiven: Boolean,   // true = pre-filled, not editable
    val isError: Boolean    // set after game completion reveal only
)

data class SudokuBoard(
    val cells: List<List<SudokuCell>>,  // [row][col], 9×9
    val solution: List<List<Int>>       // reference solution for validation
)

data class GameState(
    val board: SudokuBoard,
    val selectedCell: Pair<Int, Int>?,  // null when nothing selected
    val difficulty: Difficulty,
    val errorCount: Int,                // incremented silently on wrong fill
    val hintsUsed: Int,
    val isComplete: Boolean,
    val isPaused: Boolean
)

enum class Difficulty { EASY, MEDIUM, HARD }
```

**Key design decisions:**
- `isError` is NOT computed during play — errors are counted silently and revealed at completion only (per project requirements)
- `solution` is carried with `SudokuBoard` to enable hint delivery and post-game error reveal without a second solver pass
- `selectedCell` lives in `GameState` (not local Compose state) so it survives configuration changes

---

### 3. ViewModel Layer

**Responsibility:** Mediate between UI events and domain logic. Own `GameState` via `StateFlow`. Coordinate with the data layer for persistence.

#### GameViewModel

Owns the active game's full lifecycle: start, cell selection, digit entry, hint request, pause, resume, and completion detection.

```kotlin
// Exposed state — single source of truth for GameScreen
val uiState: StateFlow<GameUiState>

// Sealed event type — UI calls these, never mutates state directly
sealed class GameEvent {
    data class SelectCell(val row: Int, val col: Int) : GameEvent()
    data class EnterDigit(val digit: Int) : GameEvent()
    object RequestHint : GameEvent()
    object PauseGame : GameEvent()
    object ResumeGame : GameEvent()
    object EraseCell : GameEvent()
}

fun onEvent(event: GameEvent)
```

`GameUiState` is a single data class (not a sealed class) because the game screen always has a board to show — loading and error states are transient and handled at navigation/launch time rather than as persistent screen states.

```kotlin
data class GameUiState(
    val board: SudokuBoard,
    val selectedCell: Pair<Int, Int>?,
    val difficulty: Difficulty,
    val errorCount: Int,
    val hintsUsed: Int,
    val isComplete: Boolean,
    val isPaused: Boolean,
    val hintCell: Pair<Int, Int>? = null  // briefly non-null to flash hint highlight
)
```

#### MenuViewModel

Manages difficulty selection. Reads persisted in-progress game flag to show "Resume" option.

```kotlin
val hasSavedGame: StateFlow<Boolean>
val selectedDifficulty: StateFlow<Difficulty>
```

#### LeaderboardViewModel

Reads and writes high score records per difficulty level.

```kotlin
val highScores: StateFlow<Map<Difficulty, HighScoreEntry>>
```

---

### 4. UI Layer (Compose + MMD)

**Responsibility:** Render `UiState`, forward user events to ViewModel. No business logic.

**Screens:**

| Screen | Composable | ViewModel |
|--------|-----------|-----------|
| Main Menu | `MenuScreen` | `MenuViewModel` |
| Active Game | `GameScreen` | `GameViewModel` |
| Completion Summary | `SummaryScreen` | `GameViewModel` (reads final state) |
| Leaderboard | `LeaderboardScreen` | `LeaderboardViewModel` |

**E-ink UI rules enforced at this layer:**
- All components are MMD variants (`ButtonMMD`, `TextMMD`, `TopAppBarMMD`, etc.)
- `ThemeMMD` wraps the entire composable tree at the root
- No animations, no ripple effects (MMD disables these by default via `eInkColorScheme`)
- No `AnimatedVisibility`, no `Crossfade`, no `animate*AsState`
- State updates cause instant recomposition — display refresh is handled by the OS/driver
- Large touch targets (minimum 48dp, prefer 56dp+ for grid cells on 800×480 display)
- High-contrast only: black grid lines on white cells, bold selection highlight via border weight not color

**SudokuGrid composable (custom, not an MMD primitive):**
- Draws the 9×9 grid using `Canvas` or nested `Box` composables
- Receives `board: SudokuBoard`, `selectedCell`, `hintCell` as parameters
- Emits `onCellTap(row, col)` lambda up to screen
- Heavier border weight on 3×3 box boundaries (standard Sudoku visual)

**NumberPad composable:**
- 9 digit buttons + Erase button
- Uses `ButtonMMD` grid layout
- Emits `onDigitSelected(digit)` and `onErase()` lambdas up

---

### 5. Data Layer (Persistence)

**Responsibility:** Read and write game state and high scores to local storage. Expose data as `Flow` to the ViewModel layer.

#### Recommended storage: DataStore Preferences (not Proto)

**Rationale:** The game state model is simple enough that Proto DataStore's protobuf schema overhead is not warranted. Preferences DataStore with `kotlinx.serialization` JSON encoding handles the `GameState` data class cleanly without a `.proto` file dependency.

**HIGH confidence** — Official Android documentation confirms DataStore supports custom serialization via `kotlinx.serialization`.

```kotlin
// GameStateRepository
interface GameStateRepository {
    val savedGameState: Flow<GameState?>
    suspend fun saveGameState(state: GameState)
    suspend fun clearGameState()
}

// HighScoreRepository
interface HighScoreRepository {
    val highScores: Flow<Map<Difficulty, HighScoreEntry>>
    suspend fun submitScore(difficulty: Difficulty, score: Int)
}

data class HighScoreEntry(
    val score: Int,
    val errorCount: Int,
    val hintsUsed: Int,
    val date: Long  // epoch millis
)
```

**Serialization approach:** Annotate `GameState`, `SudokuBoard`, `SudokuCell`, `HighScoreEntry` with `@Serializable` from `kotlinx.serialization`. Serialize to JSON string stored in Preferences DataStore under named keys. Deserialize on read with null-safe handling for missing/corrupt data.

**Why not Room:** A relational database is unnecessary for this workload. The game has at most one in-progress save slot and a handful of high score records per difficulty. Room's schema migration overhead and boilerplate do not pay off.

---

## Data Flow: Input → State → E-ink Display

```
1. USER TAPS CELL (row=4, col=7)
   └─► GameScreen.SudokuGrid.onCellTap(4, 7)
       └─► GameViewModel.onEvent(SelectCell(4, 7))
           └─► _uiState.update { it.copy(selectedCell = 4 to 7) }
               └─► StateFlow emits new GameUiState
                   └─► collectAsState() triggers recomposition
                       └─► SudokuGrid redraws with cell (4,7) highlighted
                           └─► E-ink driver performs partial refresh on changed region

2. USER TAPS DIGIT (5)
   └─► NumberPad.onDigitSelected(5)
       └─► GameViewModel.onEvent(EnterDigit(5))
           ├─► SudokuValidator.isValidPlacement(board, row=4, col=7, digit=5)
           │   (result stored internally, NOT shown to user yet)
           ├─► If digit != solution[4][7]: errorCount++
           ├─► board updated: cells[4][7].value = 5
           ├─► SudokuValidator.isBoardComplete(board) → check for completion
           └─► _uiState.update { updated board + possible isComplete=true }

3. GAME COMPLETE
   └─► GameViewModel detects isBoardComplete == true
       ├─► Reveals errors: sets isError=true on all wrong cells
       ├─► Computes score: baseScore - (errorCount * errorPenalty) - (hintsUsed * hintPenalty)
       ├─► HighScoreRepository.submitScore(difficulty, score)  [suspend, coroutine]
       ├─► GameStateRepository.clearGameState()               [suspend, coroutine]
       └─► _uiState.update { isComplete = true }
           └─► Navigation event emitted → navigate to SummaryScreen

4. USER PAUSES
   └─► GameViewModel.onEvent(PauseGame)
       ├─► GameStateRepository.saveGameState(currentGameState)  [suspend, coroutine]
       └─► _uiState.update { isPaused = true }
           └─► GameScreen shows pause overlay / navigation to Menu
```

**E-ink display implication:** Steps 1–3 each trigger a recomposition that changes only the affected composables. Because no animations run and MMD disables ripple, the delta between frames is minimal — only the changed cell(s) and digit pad update, reducing partial-refresh area and avoiding the full-screen flash of a global refresh.

---

## Pause/Resume State Serialization

**Flow:**

```
Pause
  GameViewModel.onEvent(PauseGame)
  └─► coroutineScope.launch {
        val json = Json.encodeToString(currentGameState)
        dataStore.edit { prefs -> prefs[GAME_STATE_KEY] = json }
      }

Resume (app relaunch or back from Menu)
  GameViewModel.init block (or loadSavedGame() called from MenuViewModel)
  └─► gameStateRepository.savedGameState
        .filterNotNull()
        .first()
        ?.let { savedState ->
            _uiState.update { GameUiState.fromGameState(savedState) }
          }
```

**Serialization contract:**
- `GameState` is the serialized unit (not `GameUiState` — UI state like `hintCell` highlight is transient and not persisted)
- `solution: List<List<Int>>` is included in the serialized board so the resumed game can still deliver hints and detect completion without regenerating
- `isGiven: Boolean` per cell is serialized so the board correctly prevents editing of pre-filled cells after resume

**Null/corrupt handling:** If DataStore returns null or deserialization fails, the app treats it as no saved game and presents a fresh menu. Never crash on corrupt persistence data.

---

## Suggested Build Order (Phase Dependencies)

```
Phase 1: Puzzle Engine (no dependencies)
  ├─► SudokuGenerator + SudokuSolver (backtracking)
  ├─► SudokuValidator
  ├─► DifficultyConfig
  └─► Pure unit tests — verifiable without Android

Phase 2: Game State + ViewModel skeleton (depends on Phase 1)
  ├─► GameState / SudokuBoard / SudokuCell data classes + @Serializable
  ├─► GameViewModel with StateFlow<GameUiState>
  ├─► GameEvent sealed class
  └─► Wiring: onEvent() → SudokuValidator → state updates

Phase 3: Core Game UI (depends on Phase 2)
  ├─► ThemeMMD root wrapper
  ├─► SudokuGrid custom composable (Canvas-based 9×9)
  ├─► NumberPad composable (ButtonMMD grid)
  ├─► GameScreen assembling Grid + NumberPad + ViewModel
  └─► Playable end-to-end on device (no persistence yet)

Phase 4: Persistence (depends on Phase 2)
  ├─► DataStore setup + GameStateRepository implementation
  ├─► Pause/resume serialization (JSON via kotlinx.serialization)
  ├─► HighScoreRepository
  └─► LeaderboardViewModel

Phase 5: Scoring + Completion (depends on Phases 2, 4)
  ├─► Error counting logic in GameViewModel
  ├─► Hint logic in GameViewModel + SudokuSolver hint extraction
  ├─► Post-game error reveal + score computation
  ├─► SummaryScreen composable
  └─► LeaderboardScreen composable

Phase 6: Menu + Navigation (depends on Phases 3, 4, 5)
  ├─► Compose Navigation graph
  ├─► MenuScreen (difficulty picker, resume option)
  ├─► MenuViewModel
  └─► Full app flow: Menu → Game → Summary → Leaderboard
```

**Rationale for this order:**
- Puzzle Engine first: it has no dependencies and is the riskiest logic to get right (generation algorithm must guarantee unique solutions). Getting it wrong late is expensive.
- Game state before UI: the ViewModel contract defines what the UI consumes. Building UI against a defined state shape avoids rework.
- Core UI before persistence: playability on device validates the touch/grid UX before investing in save/restore plumbing.
- Persistence before scoring: completion flow needs the score written to the leaderboard atomically.
- Menu last: navigation is easier to assemble once all destination screens exist.

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: Mutable State in Composables
**What:** Using `remember { mutableStateOf(...) }` for game board state inside `GameScreen` or `SudokuGrid`.
**Why bad:** State is lost on configuration changes (rotation, back-stack). Game progress disappears.
**Instead:** All game state lives in `GameViewModel` backed by `StateFlow`.

### Anti-Pattern 2: Calling Suspend Functions Directly in Composables
**What:** Invoking `gameStateRepository.saveGameState()` from a `LaunchedEffect` or `onClick` inside a composable.
**Why bad:** Couples the UI layer to the data layer, breaking separation of concerns and making testing harder.
**Instead:** Composables emit events to `GameViewModel.onEvent()`. The ViewModel launches coroutines in `viewModelScope`.

### Anti-Pattern 3: Animating State Changes
**What:** Using `AnimatedVisibility`, `animateColorAsState`, `Crossfade`, or any `animate*` API for cell highlighting, digit entry feedback, or screen transitions.
**Why bad:** E-ink display redraws produce ghosting artifacts when animations produce intermediate frames. Display latency means animations don't render correctly anyway.
**Instead:** All state changes are instant. Selected cell highlighted via static border weight change. Screen transitions are direct navigation replacements (no slide/fade).

### Anti-Pattern 4: Storing the Full Solution Grid Separately from the Board
**What:** Keeping `solution` only in `SudokuGenerator` or `GameViewModel` as a separate non-persisted field.
**Why bad:** After pause/resume, the solution is lost. Hint delivery and post-game error reveal require the solution.
**Instead:** Embed `solution` in `SudokuBoard` and include it in the serialized `GameState`.

### Anti-Pattern 5: Per-Cell Error Highlighting During Play
**What:** Marking `isError = true` on `SudokuCell` immediately when the player enters a wrong digit.
**Why bad:** Contradicts the project requirement for silent error tracking. Also eliminates the tension that makes Sudoku engaging.
**Instead:** Track `errorCount: Int` in `GameState`. Reveal `isError` on all cells only at game completion before showing `SummaryScreen`.

### Anti-Pattern 6: Using Room for This Workload
**What:** Adding Room database dependency to persist game state and scores.
**Why bad:** Schema migrations, DAOs, Entity annotations, and a Kapt/KSP dependency are all overhead for a dataset of one game state row and three high-score rows.
**Instead:** DataStore Preferences with `kotlinx.serialization` JSON encoding. Simpler, fewer dependencies, fully async.

---

## Scalability Considerations

This is a single-player offline game. Scalability concerns are device-local:

| Concern | Now (v1) | Future consideration |
|---------|----------|---------------------|
| Puzzle variety | Generated on-the-fly | Seed-based generation allows deterministic replay |
| High score storage | 3 entries (one per difficulty) | If expanded to top-N per difficulty, keep DataStore (still tiny dataset) |
| Multiple save slots | Single slot (paused game) | Multiple slots would need a lightweight index; DataStore JSON list still viable |
| Puzzle difficulty tuning | Static `DifficultyConfig` | Configurable at runtime if user feedback demands it |

---

## Sources

- [Compose UI Architecture — Android Developers (official)](https://developer.android.com/develop/ui/compose/architecture) — HIGH confidence
- [ViewModel and State in Compose — Android Developers codelab](https://developer.android.com/codelabs/basic-android-kotlin-compose-viewmodel-and-state) — HIGH confidence
- [DataStore — Android Developers (official)](https://developer.android.com/topic/libraries/architecture/datastore) — HIGH confidence
- [Modern Android App Architecture in 2025: MVVM, MVI, Clean Architecture](https://medium.com/@androidlab/modern-android-app-architecture-in-2025-mvvm-mvi-and-clean-architecture-with-jetpack-compose-c0df3c727334) — MEDIUM confidence
- [MMD GitHub Repository — mudita/MMD](https://github.com/mudita/MMD) — MEDIUM confidence (component names confirmed, full API not publicly documented)
- [CalmDirectory — MMD reference app](https://github.com/davidraywilson/CalmDirectory) — MEDIUM confidence (real MMD integration example: LazyColumnMMD, ButtonMMD, TopAppBarMMD, SwitchMMD confirmed)
- [How to Generate and Validate a Sudoku Puzzle in Kotlin](https://phelela.com/2025/06/24/how-i-handle-puzzle-solving-and-validation-generating-a-sudoku-grid-in-kotlin/) — MEDIUM confidence (backtracking generation pattern)
- [Sudoku Hero — MVVM Compose Sudoku reference](https://github.com/self-taught-software-developers/SudokuSolver) — MEDIUM confidence
- [LibreSudoku — Compose Sudoku reference](https://github.com/kaajjo/LibreSudoku) — MEDIUM confidence (architecture inferred, source not fully read)
- [Structuring Clean MVVM Architecture with StateFlow and Jetpack Compose](https://medium.com/kotlin-android-chronicle/structuring-clean-mvvm-architecture-with-stateflow-and-jetpack-compose-072c3b0abf7c) — MEDIUM confidence
- [Difficulty Rating of Sudoku Puzzles: An Overview](https://www.fi.muni.cz/~xpelanek/publications/sudoku-arxiv.pdf) — MEDIUM confidence (difficulty vs. givens count relationship)
