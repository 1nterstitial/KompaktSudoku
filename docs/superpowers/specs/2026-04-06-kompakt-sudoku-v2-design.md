# KompaktSudoku v2 — Design Spec

Date: 2026-04-06

## Overview

A complete rebuild of KompaktSudoku for the Mudita Kompakt E-ink Android device. The app is a fully playable Sudoku game with three difficulty levels, session-centric records, rule-based conflict feedback, and a calm UI built entirely with MMD components.

The v1 puzzle engine (generator, validator, uniqueness verifier, difficulty classifier) is ported into the new project. Everything else — UI, ViewModel, persistence, scoring semantics — is built fresh.

### Core Decisions

| Decision | Choice |
|----------|--------|
| Scoring model | Session-centric records (no score formula) |
| Conflict feedback | Rule-based only (board legality, no solution leaking) |
| Project approach | Fresh project in `sudoku-v2/`, selective engine port |
| Architecture | Single ViewModel + StateFlow, sealed class routing |
| Navigation | No Jetpack Navigation — sealed class + when block |
| Puzzle engine | Port v1 custom engine behind clean interface |
| MMD consumption | JitPack, submodule, or AAR — whatever works |
| Accessibility | Deferred to follow-up |
| Build approach | Single spec, single build |

---

## Project Structure

**Package:** `com.interstitial.sudoku`

**Module:** Single `app` module.

**Root directory:** `D:\Development\KompaktSudoku\sudoku-v2`

**`.planning/` is gitignored.**

```
app/src/main/java/com/interstitial/sudoku/
├── MainActivity.kt
├── game/
│   ├── GameViewModel.kt
│   ├── GameRepository.kt
│   ├── DataStoreGameRepository.kt
│   ├── RecordsRepository.kt
│   ├── DataStoreRecordsRepository.kt
│   └── model/
│       ├── GameUiState.kt
│       ├── GameAction.kt
│       ├── GameEvent.kt
│       ├── InputMode.kt
│       ├── DifficultyRecord.kt
│       └── PersistedGameState.kt
├── puzzle/
│   ├── engine/
│   │   ├── SudokuGenerator.kt
│   │   ├── SudokuValidator.kt
│   │   ├── UniquenessVerifier.kt
│   │   ├── DifficultyClassifier.kt
│   │   └── ConflictDetector.kt
│   ├── model/
│   │   ├── Difficulty.kt
│   │   ├── DifficultyConfig.kt
│   │   └── SudokuPuzzle.kt
│   └── PuzzleGenerationException.kt
└── ui/
    ├── theme/
    │   └── Theme.kt
    ├── home/
    │   └── HomeScreen.kt
    ├── newpuzzle/
    │   └── NewPuzzleScreen.kt
    ├── game/
    │   ├── GameScreen.kt
    │   ├── SudokuGrid.kt
    │   ├── PuzzleTopBar.kt
    │   ├── PuzzleMetaStrip.kt
    │   ├── InputModeToggle.kt
    │   ├── PuzzleActionRow.kt
    │   ├── DigitPad.kt
    │   └── LeavePuzzleDialog.kt
    ├── summary/
    │   └── SummaryScreen.kt
    └── records/
        └── RecordsScreen.kt
```

---

## Architecture

**Navigation:** Sealed class `Route` with cases: `Home`, `NewPuzzle`, `Game(difficulty)`, `Summary`, `Records`. `MainActivity` holds `var currentRoute by remember` and renders with a `when` block. No transitions, no animation.

**State:** Single `GameViewModel` owns all game state via `StateFlow<GameUiState>`. Composables observe via `collectAsStateWithLifecycle`. User actions dispatched as `GameAction` sealed class instances.

**Persistence:** Two DataStore-backed repositories:
- `GameRepository` — save/load in-progress game
- `RecordsRepository` — per-difficulty completion stats and best times

**Puzzle engine:** Ported from v1. `SudokuGenerator`, `SudokuValidator`, `UniquenessVerifier`, `DifficultyClassifier` brought over with their existing tests. New `ConflictDetector` added for rule-based feedback.

---

## Data Models

### GameUiState

```kotlin
data class GameUiState(
    val board: IntArray = IntArray(81),
    val solution: IntArray = IntArray(81),
    val givens: BooleanArray = BooleanArray(81),
    val notes: Array<Set<Int>> = Array(81) { emptySet() },
    val conflictMask: BooleanArray = BooleanArray(81),
    val selectedCell: Int? = null,
    val inputMode: InputMode = InputMode.FILL,
    val difficulty: Difficulty = Difficulty.EASY,
    val cellsRemaining: Int = 0,
    val elapsedMs: Long = 0,
    val hasUndo: Boolean = false,
    val digitCounts: IntArray = IntArray(9),
    val isComplete: Boolean = false,
    val isPaused: Boolean = false,
    val hintsUsed: Int = 0,
    val hintedCells: Set<Int> = emptySet(),
    val isGenerating: Boolean = false,
)
```

Key differences from v1:
- `notes` uses `Set<Int>` per cell (full 1-9, was capped at 4)
- `conflictMask` computed from board legality, not solution comparison
- `digitCounts` drives digit pad disabled state
- `elapsedMs` tracked silently, shown in summary/records
- No `errorCount`, no `score`

### GameAction

```kotlin
sealed class GameAction {
    data class SelectCell(val index: Int) : GameAction()
    data class PlaceDigit(val digit: Int) : GameAction()
    data class ToggleNote(val digit: Int) : GameAction()
    object Erase : GameAction()
    object Undo : GameAction()
    object Hint : GameAction()
    object ToggleInputMode : GameAction()
    data class NewGame(val difficulty: Difficulty) : GameAction()
    object ResumeGame : GameAction()
    object PausePuzzle : GameAction()
    object KeepForLater : GameAction()
    object DiscardPuzzle : GameAction()
}
```

### DifficultyRecord

```kotlin
@Serializable
data class DifficultyRecord(
    val completedCount: Int = 0,
    val bestTimeMs: Long? = null,
    val bestNoHintTimeMs: Long? = null,
    val lastCompletedEpochMs: Long? = null,
)
```

### PersistedGameState

```kotlin
@Serializable
data class PersistedGameState(
    val schemaVersion: Int = 1,
    val board: List<Int>,
    val solution: List<Int>,
    val givens: List<Boolean>,
    val notes: List<List<Int>>,
    val difficulty: String,
    val elapsedMs: Long,
    val hintsUsed: Int,
    val hintedCells: List<Int>,
)
```

---

## Screens

### Home Screen

List rows with secondary descriptions using `LazyColumnMMD`:
- **Continue puzzle** — shown only when a saved game exists. Secondary: `Medium · 31 cells left`
- **New puzzle** — secondary: `Choose easy, medium, or hard`
- **Records** — secondary: `Completed puzzles and best times`

Rows separated by `HorizontalDividerMMD`. Tapping a row navigates directly. Title "Sudoku" at top, left-aligned, `headlineLarge`.

### New Puzzle Screen

`TopAppBarMMD` with back arrow, title "New puzzle". Three difficulty rows:
- **Easy** — `More givens, shorter sessions`
- **Medium** — `Balanced deduction`
- **Hard** — `Fewer givens, deeper focus`

On tap: puzzle generation starts off-main-thread. Screen shows static text "Preparing puzzle..." while generating. No spinner.

### Game Screen

Top-to-bottom layout within 800×480 portrait:

| Section | Height | Component |
|---------|--------|-----------|
| Top app bar | ~56dp | `TopAppBarMMD` — back arrow (opens Leave dialog), title: `Easy/Medium/Hard puzzle` |
| Meta strip | ~32dp | `31 cells left` — recomposes only when count changes |
| Grid | ~396dp | Single `Canvas`, 9×9, `aspectRatio(1f)` |
| Mode toggle | ~48dp | Two-segment `Fill` / `Notes` — selected = black fill, white text |
| Action row | ~48dp | Three `ButtonMMD`: Undo, Erase, Hint — text labels, disabled when unavailable |
| Digit pad | ~168dp | 3×3 grid of `ButtonMMD`, digits 1-9, ≥56dp height, disabled at 9 placements |

**Grid rendering (Canvas):**
- Box borders: 3dp `onSurface` (black)
- Cell borders: 1dp `outlineVariant` (gray)
- Given digits: Bold, `onSurface`
- Player digits: Regular, `onSurface`
- Selected cell: solid `onSurface` fill, `surface` text (black/white inversion)
- Pencil marks: 3×3 micro-grid per cell, `onSurfaceVariant`, dynamically scaled small text
- Conflict indicator: inset corner marks, `onSurface`
- 200ms touch debounce via `System.currentTimeMillis()` delta

### Summary Screen

Title: "Puzzle complete" (the emotional headline, not a score).

Label-value rows separated by `HorizontalDividerMMD`:
- Difficulty — `Hard`
- Time — `45:12`
- Hints used — `1`
- "New personal record" — shown only when applicable

Three actions:
- **New puzzle** — primary `ButtonMMD` (inverted: black bg, white text)
- **Back to menu** — secondary `ButtonMMD` (bordered)
- **View records** — tertiary `ButtonMMD` (lighter border)

### Records Screen

`TopAppBarMMD` with back arrow, title "Records".

Sectioned by difficulty using `LazyColumnMMD`. Per section:
- Completed count
- Best time
- Best no-hint time

Em dash `—` for missing records. Section headers use `surfaceVariant` background. Rows separated by `HorizontalDividerMMD`.

### Leave Puzzle Dialog

Centered overlay composable (not `AlertDialog`):
- Title: `Leave puzzle?`
- Primary: `Keep for later` (inverted `ButtonMMD`)
- Secondary: `Discard puzzle` (bordered `ButtonMMD`)
- Timer pauses when shown
- No scrim-dismiss

---

## Game Logic

### Puzzle lifecycle

1. User taps difficulty → ViewModel launches coroutine → `SudokuGenerator.generate(difficulty)` off main thread → `isGenerating = true` → on completion, board/solution/givens populated, `isGenerating = false`
2. During play: `GameAction` dispatched → ViewModel updates state → Compose redraws
3. Completion: `cellsRemaining == 0 && conflictMask.none { it }` — all filled, no rule violations
4. On completion: elapsed time stops, records updated, route transitions to Summary
5. On back press: Leave Puzzle dialog shown, timer pauses

### Cell interaction

- Tap cell → selects it (black fill, white text)
- Tap selected cell again → deselects
- Tap digit in Fill mode → places digit (if not a given or hinted cell)
- Tap digit in Notes mode → toggles candidate in cell's note set
- Placing a digit clears that cell's notes
- Placing a digit triggers `ConflictDetector.buildConflictMask()` recompute

### Undo

Stack of `(cellIndex, previousValue, previousNotes)` tuples held in ViewModel, outside `GameUiState`. `PlaceDigit`, `Erase`, and `ToggleNote` push to stack. `Hint` does not push (non-undoable). Stack not serialized — undo resets on resume.

### Hints — selected-cell-first

- If selected cell is empty or player-filled → reveal solution value, mark cell as non-editable
- If no valid target selected → snackbar via `SnackbarHostStateMMD`: "Select a cell to reveal"
- Increments `hintsUsed`, clears notes on revealed cell

### Conflict detection

`ConflictDetector.buildConflictMask(board: IntArray): BooleanArray` — marks any cell whose non-zero value duplicates in its row, column, or 3×3 box. Recomputed after every digit placement or erase. No solution comparison.

### Elapsed time

- `accumulatedMs` persisted + `SystemClock.elapsedRealtime()` delta while playing
- Pauses when: Leave dialog open, app backgrounded, puzzle completed
- Not displayed during play — only in Summary and Records

### Persistence

Auto-save on `onStop` lifecycle. `PersistedGameState` serialized as JSON string in DataStore Preferences. On resume: deserialize, rebuild `conflictMask` and `digitCounts` from board state. Schema version field for future-proofing.

### Records update on completion

- Increment `completedCount` for difficulty
- If `elapsedMs < bestTimeMs` (or first completion): update `bestTimeMs`
- If `hintsUsed == 0 && elapsedMs < bestNoHintTimeMs` (or first no-hint): update `bestNoHintTimeMs`
- Set `lastCompletedEpochMs` to current time

---

## Theme & E-ink Rules

### Theme

```kotlin
@Composable
fun KompaktSudokuTheme(content: @Composable () -> Unit) {
    ThemeMMD(
        colorScheme = eInkColorScheme,
        typography = eInkTypography,
        content = content
    )
}
```

No customizations beyond MMD defaults.

### MMD component rules

- `TextMMD` everywhere (no `Text`)
- `ButtonMMD` everywhere (no `Button`)
- `TopAppBarMMD` on all screens with navigation
- `HorizontalDividerMMD` between all list items and sections
- `LazyColumnMMD` for scrollable lists
- `SnackbarHostStateMMD` for hint feedback
- No `Card`, no elevation, no shadow
- No `AlertDialog` — Leave Puzzle is a custom overlay composable

### E-ink absolutes

- Zero animations — no `animateXAsState`, no `AnimatedVisibility`, no `Crossfade`, no transitions
- Zero ripple (enforced by `ThemeMMD`, verify no re-enabling)
- 200ms touch debounce on grid taps
- Tabular/monospace numerals for stable positioning
- Full-width elements to minimize partial screen refreshes
- Deterministic draw order in Canvas

### Spacing

8dp small gaps, 12dp row padding, 16dp page padding, 24dp section spacing.

---

## Copy

| Context | Text |
|---------|------|
| Fill mode label | `Fill` |
| Notes mode label | `Notes` |
| Leave dialog title | `Leave puzzle?` |
| Save action | `Keep for later` |
| Discard action | `Discard puzzle` |
| Summary headline | `Puzzle complete` |
| Record notification | `New personal record` |
| Hint unavailable | `Select a cell to reveal` |
| Loading | `Preparing puzzle...` |
| Records screen title | `Records` |
| Empty record value | `—` (em dash) |

---

## Testing

### Unit tests (JUnit 4 + Coroutines Test + Turbine)

| Area | Tests |
|------|-------|
| ConflictDetector | Row/column/box duplicates, overlapping conflicts, cleared after erase/replace, no conflicts on empty cells |
| GameViewModel — digits | Place digit updates board, clears notes, recomputes conflicts, decrements cellsRemaining, pushes undo |
| GameViewModel — notes | Toggle adds/removes candidate, full 1-9 support, placing digit clears notes |
| GameViewModel — undo | Restores previous value + notes, empty stack disables button, hint not undoable |
| GameViewModel — hint | Reveals selected cell, no valid target shows snackbar, increments hintsUsed, hinted cell non-editable |
| GameViewModel — completion | Detected at cellsRemaining == 0 with no conflicts, records updated, route transitions |
| GameViewModel — erase | Clears player digit and notes, no-op on givens, pushes undo |
| Elapsed time | Accumulates while playing, pauses on dialog/background, persists across save/resume |
| Serialization | `PersistedGameState` round-trips with full notes, elapsed time, all fields |
| Records | Best time updates, no-hint tracking, completed count increments, first completion edge case |
| Digit counts | Tracks per digit, disabled at 9, re-enabled on erase/undo |

### Puzzle engine (ported from v1 with existing tests)

| Area | Tests |
|------|-------|
| SudokuGenerator | Produces valid board, solution matches |
| SudokuValidator | Validates complete boards, rejects invalid |
| UniquenessVerifier | Confirms unique solution |
| DifficultyClassifier | Correct classification per config |

### UI tests (Compose UI Test + Robolectric)

| Area | Tests |
|------|-------|
| Grid tap → selection | Tap coordinates, verify selectedCell updates |
| Digit placement | Place digit, verify grid redraws |
| Navigation | Home → New Puzzle → Game → Summary → Home |
| Continue puzzle | Save, return to Home, "Continue puzzle" row visible |

### Deferred

- Accessibility content descriptions
- On-device E-ink QA
- Performance/recomposition profiling

---

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Kotlin | 2.3.20 | Primary language |
| Jetpack Compose BOM | 2026.03.00 | UI framework |
| Compose Material 3 | 1.4.0 (via BOM) | Base layer for MMD |
| MMD | 1.0.1 | All UI components |
| Android Gradle Plugin | 8.9.0 | Build tooling |
| Gradle | 8.11.1 | Build system |
| DataStore Preferences | 1.2.1 | Persistence |
| kotlinx.serialization | 1.8.0 | JSON serialization |
| Kotlin Coroutines | 1.10.2 | Async, Flow |
| JUnit 4 | 4.13.2 | Test runner |
| Mockk | 1.13.x | Mocking |
| Turbine | 1.2.0 | Flow testing |
| Robolectric | 4.14.x | JVM Compose UI tests |
| minSdk | 31 | Mudita Kompakt AOSP 12 |
| targetSdk | 31 | Match device OS |
| compileSdk | 35 | IDE warnings |

---

## Out of Scope

- Accessibility (deferred)
- Network features
- Multiple user profiles
- Puzzle sharing
- Custom themes or color schemes
- Timer displayed during play
- Score formula or scoring of any kind
