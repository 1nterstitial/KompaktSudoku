# Phase 5: Scoring & Completion - Research

**Researched:** 2026-03-24
**Domain:** Android/Kotlin — hint logic, score calculation, DataStore persistence, Compose screen routing
**Confidence:** HIGH

## Summary

Phase 5 closes the game loop. It is an integration phase, not an infrastructure phase: the
foundational pieces (MVVM + StateFlow, DataStore, GameViewModel, MMD components, test harness)
are fully established from Phases 1–4. The work is surgical additions to existing code plus two
new leaf composables.

The five distinct concerns are: (1) hint logic in GameViewModel, (2) hintCount propagation through
the state model, (3) ScoreRepository interface + DataStoreScoreRepository, (4) SummaryScreen
composable, (5) LeaderboardScreen composable plus Screen-enum routing in MainActivity. All five
concerns have locked decisions in CONTEXT.md — there are no open design questions.

The riskiest sub-task is the Screen enum routing in MainActivity, which replaces the single
`GameScreen(viewModel)` call with a `when(screen)` dispatch that wires `GameEvent.Completed`
into a `GAME → SUMMARY` transition. The `GameEvent.Completed` event must also carry `hintCount`
(currently it only carries `errorCount`) and the `isPersonalBest` flag must be derived at
completion time in the ViewModel before the event fires.

**Primary recommendation:** Implement in five sequentially dependent tasks: state model additions
first, then ViewModel logic, then ScoreRepository, then SummaryScreen + LeaderboardScreen, then
MainActivity routing. Each task has a clear single-file or two-file scope. Tests follow
established patterns — `GameViewModelTest.kt` precedent for ViewModel tests,
`FakeGameRepository.kt` precedent for fake repository test doubles.

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Final score = `max(0, 100 - (errorCount × 10) - (hintCount × 5))`. Score floors at 0
  (never negative). Max possible score is 100 (perfect game: 0 errors, 0 hints). Non-negotiable.
- **D-02:** Hint button placed in `ControlsRow` as a fourth button:
  `[ Fill ] [ Pencil ] [ Undo ] [ Get Hint ]`. No additional row.
- **D-03:** Hint reveals exactly one unfilled, non-correct cell at random and fills it with its
  solution value. Cannot hint a cell that already contains the correct value.
- **D-04:** `hintCount` is a new field on `GameUiState` (Int, default 0). Must also be added to
  `PersistedGameState` for save/resume continuity. Must be included in `GameEvent.Completed`.
- **D-05:** A top-level `Screen` enum (`GAME`, `SUMMARY`, `LEADERBOARD`) drives which composable
  is active in `MainActivity`. `GameScreen` stays unchanged in structure. `GAME → SUMMARY`
  triggered by `GameEvent.Completed`. `SUMMARY → LEADERBOARD`: "View Leaderboard" button.
  `LEADERBOARD → GAME`: "New Game" button. Phase 6 replaces enum with NavHost.
- **D-06:** Completion summary screen shows: difficulty label, error count, hints used, final
  score, and (if new personal best) "New personal best!" notice. Two action buttons: "View
  Leaderboard" and "New Game".
- **D-07:** Leaderboard screen shows three rows — one per difficulty (Easy / Medium / Hard) —
  each displaying the stored best score (or "—" if no score recorded yet). One action button:
  "New Game" (returns to GAME screen and starts a new Easy game).
- **D-08:** High scores stored in DataStore keys: `high_score_easy`, `high_score_medium`,
  `high_score_hard` (each an `Int`). Single best score per difficulty.
- **D-09:** Separate `ScoreRepository` interface. `DataStoreScoreRepository` implements it.
  Both injected into `GameViewModel` for testability.
- **D-10:** After completion: read current best for difficulty → compare → if new score is
  higher, write it. `isPersonalBest` flag derived at this point and passed to `SummaryScreen`
  via `CompletionResult`.

### Claude's Discretion

- Exact random cell selection strategy for hints (random among all invalid unfilled cells is
  fine — no need to prefer any particular cell).
- `CompletionResult` data class shape — carries
  `(difficulty, errorCount, hintCount, finalScore, isPersonalBest)`.
- Whether `ScoreRepository` is injected as a constructor param or passed differently — follow
  the existing injectable-lambda pattern for testability.

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SCORE-03 | User can request a hint; a single unfilled correct cell value is revealed; hint usage is counted | `requestHint()` action in GameViewModel; random selection of unfilled non-correct cells; `hintCount` field on `GameUiState` |
| SCORE-04 | Each hint used deducts a fixed penalty from the final score | Score formula D-01: hintCount × 5 penalty; computed in ViewModel at completion time |
| SCORE-05 | On completion, user sees a summary showing error count, hints used, and final score | `SummaryScreen` composable with `CompletionResult` parameter; Screen enum routing GAME→SUMMARY |
| SCORE-06 | Final score is error-based (fewer errors = higher score) with hint penalties applied | Score formula D-01: `max(0, 100 - errorCount×10 - hintCount×5)` |
| HS-01 | Per-difficulty high scores are stored persistently on device | `DataStoreScoreRepository` using keys `high_score_easy/medium/hard` (Int); DataStore already proven in Phase 4 |
| HS-02 | After game completion, user is informed if they achieved a new personal best | `isPersonalBest` flag computed in ViewModel at completion; passed via `CompletionResult` to `SummaryScreen` |
| HS-03 | User can view a leaderboard screen showing top scores per difficulty | `LeaderboardScreen` composable; Screen enum SUMMARY→LEADERBOARD transition; reads from `ScoreRepository` |
</phase_requirements>

---

## Standard Stack

All libraries are already declared in the project. Phase 5 adds no new dependencies.

### Core (already declared)
| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| DataStore Preferences | 1.2.1 (via `libs.androidx.datastore.preferences`) | `DataStoreScoreRepository` — persistent high score storage | Established in Phase 4; same pattern as `DataStoreGameRepository` |
| Kotlin Coroutines | 1.10.2 | Async DataStore reads/writes in ScoreRepository | Already in project; suspend functions dispatch to Dispatchers.IO |
| Jetpack ViewModel | via AndroidX BOM | GameViewModel extended with hint + score logic | Established in Phases 2–4 |
| Compose Material 3 | 1.4.0 (via BOM) | `Surface`, `BasicAlertDialog` — used in new screens | Established in Phase 3 |
| MMD 1.0.1 | 1.0.1 | `ButtonMMD`, `TextMMD` — all interactive elements | Mandatory per CLAUDE.md |
| Turbine | 1.2.0 | Testing SharedFlow events in ViewModel tests | Already declared; used in GameViewModelTest |
| Robolectric | 4.14.x | JVM-based ViewModel and Compose tests | Already configured in `build.gradle.kts` (isIncludeAndroidResources = true) |
| JUnit 4 | 4.13.2 | Test runner | Established; Compose test rule requires JUnit 4 |

### No New Dependencies Required

No `build.gradle.kts` changes are needed. All Phase 5 functionality is achievable with the
existing dependency set:
- `intPreferencesKey` (from `androidx.datastore.preferences.core`) for high score keys — already
  imported transitively via `libs.androidx.datastore.preferences`.
- `kotlinx.serialization` is NOT needed for ScoreRepository — high scores are raw `Int`
  preferences, not JSON-serialized objects. This is simpler than `DataStoreGameRepository`.

---

## Architecture Patterns

### Project Structure — Phase 5 Additions

```
app/src/main/java/com/mudita/sudoku/
├── game/
│   ├── model/
│   │   ├── GameUiState.kt         MODIFY — add hintCount: Int = 0 field + equals/hashCode
│   │   ├── PersistedGameState.kt  MODIFY — add hintCount: Int = 0; update converters
│   │   ├── GameEvent.kt           MODIFY — Completed adds hintCount param
│   │   └── CompletionResult.kt    NEW — data class (difficulty, errorCount, hintCount,
│   │                                            finalScore, isPersonalBest)
│   ├── GameRepository.kt          NO CHANGE
│   ├── DataStoreGameRepository.kt NO CHANGE
│   ├── ScoreRepository.kt         NEW — interface + NoOpScoreRepository
│   ├── DataStoreScoreRepository.kt NEW — DataStore Int keys per difficulty
│   └── GameViewModel.kt           MODIFY — inject ScoreRepository, add requestHint(),
│                                           extend applyFill() to emit hintCount + score,
│                                           expose canRequestHint: StateFlow<Boolean>
├── ui/
│   ├── game/
│   │   ├── ControlsRow.kt         MODIFY — add onHint callback + Hint button (4th)
│   │   ├── GameScreen.kt          MODIFY — wire GameEvent.Completed to onCompleted callback
│   │   ├── SummaryScreen.kt       NEW — leaf composable receiving CompletionResult
│   │   └── LeaderboardScreen.kt   NEW — leaf composable receiving Map<Difficulty, Int?>
└── MainActivity.kt                MODIFY — Screen enum + when routing + ViewModel factory
                                            extended to inject DataStoreScoreRepository
```

### Pattern 1: ScoreRepository (mirrors GameRepository exactly)

**What:** Interface with `suspend` operations; DataStore-backed implementation; NoOp for default
injection. ScoreRepository is simpler than GameRepository because Int preferences require no JSON
serialization.

**When to use:** Any ViewModel that needs high score read/write access.

```kotlin
// ScoreRepository.kt
interface ScoreRepository {
    suspend fun getBestScore(difficulty: Difficulty): Int?
    suspend fun saveBestScore(difficulty: Difficulty, score: Int)
}

class NoOpScoreRepository : ScoreRepository {
    override suspend fun getBestScore(difficulty: Difficulty): Int? = null
    override suspend fun saveBestScore(difficulty: Difficulty, score: Int) {}
}
```

```kotlin
// DataStoreScoreRepository.kt — reference pattern from DataStoreGameRepository.kt
import androidx.datastore.preferences.core.intPreferencesKey

private val HIGH_SCORE_EASY   = intPreferencesKey("high_score_easy")
private val HIGH_SCORE_MEDIUM = intPreferencesKey("high_score_medium")
private val HIGH_SCORE_HARD   = intPreferencesKey("high_score_hard")

class DataStoreScoreRepository(
    private val dataStore: DataStore<Preferences>
) : ScoreRepository {
    override suspend fun getBestScore(difficulty: Difficulty): Int? =
        withContext(Dispatchers.IO) {
            dataStore.data.first()[difficulty.toPreferenceKey()]
        }

    override suspend fun saveBestScore(difficulty: Difficulty, score: Int) {
        withContext(Dispatchers.IO) {
            dataStore.edit { it[difficulty.toPreferenceKey()] = score }
        }
    }

    private fun Difficulty.toPreferenceKey() = when (this) {
        Difficulty.EASY   -> HIGH_SCORE_EASY
        Difficulty.MEDIUM -> HIGH_SCORE_MEDIUM
        Difficulty.HARD   -> HIGH_SCORE_HARD
    }
}
```

**Source:** Established DataStore pattern from `DataStoreGameRepository.kt` (project codebase);
`intPreferencesKey` is part of `androidx.datastore.preferences.core` which is already in the
dependency tree.

### Pattern 2: Hint Cell Selection

**What:** In `requestHint()`, collect all candidate cell indices, pick one at random, apply as a
fill that does NOT increment errorCount.

```kotlin
fun requestHint() {
    val state = _uiState.value
    if (state.isLoading || state.isComplete) return
    // Cells that are not given AND not already correct
    val candidates = (0..80).filter { i ->
        !state.givenMask[i] && state.board[i] != state.solution[i]
    }
    if (candidates.isEmpty()) return  // canRequestHint = false when this is empty
    val idx = candidates.random()
    val newBoard = state.board.copyOf()
    newBoard[idx] = state.solution[idx]
    val newMarks = state.pencilMarks.copyOf().also { it[idx] = emptySet() }
    val allCorrect = newBoard.indices.all { i -> newBoard[i] == state.solution[i] }
    _uiState.update {
        it.copy(
            board = newBoard,
            pencilMarks = newMarks,
            hintCount = it.hintCount + 1,
            isComplete = allCorrect
        )
    }
    if (allCorrect) handleCompletion(newBoard, state)
}
```

Key constraint: hint fills do NOT increment `errorCount`. This is correct by design — a hint is
not an error; it carries its own separate penalty in the score formula.

### Pattern 3: Completion with Score and Personal Best

**What:** `applyFill()` already calls `handleCompletion` on `allCorrect`. That helper needs to
compute the score, read the current best, derive `isPersonalBest`, optionally write the new best,
then emit `GameEvent.Completed`.

**Important:** DataStore reads/writes are `suspend`; the completion path is already inside a
`viewModelScope.launch` block, so `withContext(ioDispatcher)` is straightforward.

```kotlin
private fun handleCompletion(finalState: GameUiState) {
    viewModelScope.launch {
        val score = maxOf(0, 100 - finalState.errorCount * 10 - finalState.hintCount * 5)
        val currentBest = withContext(ioDispatcher) {
            scoreRepository.getBestScore(finalState.difficulty)
        }
        val isPersonalBest = currentBest == null || score > currentBest
        if (isPersonalBest) {
            withContext(ioDispatcher) {
                scoreRepository.saveBestScore(finalState.difficulty, score)
            }
        }
        withContext(ioDispatcher) { repository.clearGame() }
        _events.emit(
            GameEvent.Completed(
                errorCount = finalState.errorCount,
                hintCount  = finalState.hintCount,
                score      = score,
                difficulty = finalState.difficulty,
                isPersonalBest = isPersonalBest
            )
        )
    }
}
```

### Pattern 4: Screen Enum Routing in MainActivity

**What:** Replace the single `GameScreen(viewModel)` call with a `MutableState<Screen>` + `when`
dispatch. The `CompletionResult` must be threaded from the event into `SummaryScreen`.

```kotlin
// MainActivity.kt additions
enum class Screen { GAME, SUMMARY, LEADERBOARD }

// Inside setContent { ThemeMMD { ... } }:
var currentScreen by remember { mutableStateOf(Screen.GAME) }
var completionResult by remember { mutableStateOf<CompletionResult?>(null) }

when (currentScreen) {
    Screen.GAME -> GameScreen(
        viewModel = viewModel,
        onCompleted = { result ->
            completionResult = result
            currentScreen = Screen.SUMMARY
        }
    )
    Screen.SUMMARY -> SummaryScreen(
        result = completionResult!!,
        onViewLeaderboard = { currentScreen = Screen.LEADERBOARD },
        onNewGame = {
            viewModel.startNewGame()
            currentScreen = Screen.GAME
        }
    )
    Screen.LEADERBOARD -> LeaderboardScreen(
        scores = /* read from viewModel */ ,
        onNewGame = {
            viewModel.startNewGame()
            currentScreen = Screen.GAME
        }
    )
}
```

**Note on GameScreen signature change:** `GameScreen` must accept an `onCompleted: (CompletionResult) -> Unit`
callback. The existing `LaunchedEffect` that collects `viewModel.events` calls this callback when
`GameEvent.Completed` arrives.

### Pattern 5: Leaderboard Data Flow

**What:** `LeaderboardScreen` needs all three difficulty scores. The cleanest approach is to
expose a `StateFlow<Map<Difficulty, Int?>>` from `GameViewModel` that reflects current stored
scores. The ViewModel loads scores on init (or on-demand) from `ScoreRepository`.

Alternatively, read the three scores in MainActivity from `ScoreRepository` directly. Given that
`GameViewModel` already holds a reference to `scoreRepository`, exposing the scores from the
ViewModel as a `StateFlow` is the cleaner approach and matches MVVM discipline.

```kotlin
// In GameViewModel — add alongside uiState
private val _leaderboardScores = MutableStateFlow<Map<Difficulty, Int?>>(emptyMap())
val leaderboardScores: StateFlow<Map<Difficulty, Int?>> = _leaderboardScores.asStateFlow()

private suspend fun refreshLeaderboard() {
    _leaderboardScores.value = mapOf(
        Difficulty.EASY   to scoreRepository.getBestScore(Difficulty.EASY),
        Difficulty.MEDIUM to scoreRepository.getBestScore(Difficulty.MEDIUM),
        Difficulty.HARD   to scoreRepository.getBestScore(Difficulty.HARD)
    )
}
```

Call `refreshLeaderboard()` in `init` and after every score write so the leaderboard is always
current.

### Pattern 6: canRequestHint Derived State

**What:** `ControlsRow` receives `canRequestHint: Boolean` and passes it to the hint button's
`enabled` parameter. This is cleanest as a derived property on `GameUiState` or as a separate
`StateFlow<Boolean>` from the ViewModel.

The simplest approach: expose `canRequestHint` as a `val` computed by the existing
`uiState.collectAsStateWithLifecycle()`. Since `GameScreen` already collects `uiState`, adding
a derived expression is zero overhead:

```kotlin
// In GameScreen.kt:
val canRequestHint = !uiState.isComplete && !uiState.isLoading &&
    (0..80).any { i -> !uiState.givenMask[i] && uiState.board[i] != uiState.solution[i] }
```

This avoids a second StateFlow. Correctness: a hint is invalid if the game is over, still loading,
or all unfilled cells already have the correct value.

### Anti-Patterns to Avoid

- **Emitting `GameEvent.Completed` before the DataStore write:** The score write must complete
  before the event fires, otherwise `SummaryScreen` could show `isPersonalBest=true` for a score
  that was not actually saved (e.g., if the process dies between the event and the write).
  Solution: `saveBestScore()` is called inside the same `viewModelScope.launch` block, before
  `_events.emit(...)`.

- **Decrementing errorCount on hint:** A hint fill is not an error. `errorCount` must NOT change
  when `requestHint()` applies the solution value to a cell. Only `hintCount` increments.

- **Undo-able hints:** The `requestHint()` action must NOT push an undo action onto the undo
  stack. Hints are permanent reveals, not moves that can be undone. (CONTEXT.md does not mention
  this explicitly, but it is consistent with the hint-as-permanent-reveal contract in D-03.)

- **Using `animateContentSize` or `AnimatedVisibility` for the personal best notice:**
  The "New personal best!" text must be conditionally shown/hidden via plain `if (isPersonalBest)`
  — never via `AnimatedVisibility` or `animateContentSize` (UI-02 E-ink prohibition).

- **Sharing the same DataStore instance for game state and high scores:** The project already
  separates them at the DataStore level. `gameDataStore` is the game persistence store.
  `DataStoreScoreRepository` needs its own `DataStore<Preferences>` instance scoped under a
  different name (e.g., `"score_state"`). Create a `Context.scoreDataStore` extension property
  mirroring `Context.gameDataStore`.

- **Missing `@Serializable` on `CompletionResult`:** `CompletionResult` is NOT persisted to
  DataStore — it is only passed in-memory from ViewModel to UI via `GameEvent.Completed`. There
  is no need for `@Serializable`. Adding it would be unnecessary noise.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Int preference storage | Custom serialized object or Room | `intPreferencesKey` + DataStore | Three Int keys — trivially expressible as native DataStore preferences; no schema migration complexity |
| Random hint cell selection | Weighted selection, hint strategy engine | `kotlin.random.Random` / `List.random()` | Decision D-03 explicitly approves random selection; no strategy preference needed |
| Score persistence comparison | Cache-then-compare in memory | DataStore read → compare → write in suspension | Coroutine-safe; no race conditions; one suspend chain |
| Disabled button state | Custom drawable or opacity modifier | `ButtonMMD(enabled = canRequestHint)` | MMD renders disabled state internally; no custom appearance needed (UI-SPEC confirmed) |

---

## Common Pitfalls

### Pitfall 1: `PersistedGameState` Backward Compatibility
**What goes wrong:** Adding `hintCount: Int` to `PersistedGameState` without a default value
causes deserialization failures for any game state serialized under Phase 4 (which has no
`hintCount` key in the JSON). The app would crash or return null for any resumed game.
**Why it happens:** `kotlinx.serialization` requires all non-optional fields to be present in
the JSON. Missing fields without defaults throw `SerializationException`.
**How to avoid:** Declare `hintCount: Int = 0` — the `= 0` makes it optional in the JSON
decoder. Same pattern was used for `selectedCellIndex: Int? = null` in the existing
`PersistedGameState`.
**Warning signs:** Test `PersistedGameStateTest.kt` with a JSON string that lacks `hintCount`
— should deserialize to `hintCount = 0` without throwing.

### Pitfall 2: `GameEvent.Completed` Payload Missing Before Routing
**What goes wrong:** `GameScreen` calls `onCompleted(result)` and the `Screen` transitions to
`SUMMARY` before `completionResult` is stored in `MainActivity`'s state. `SummaryScreen`
receives a null `CompletionResult`.
**Why it happens:** Recomposition can happen between state writes if `completionResult` and
`currentScreen` are updated in two separate state operations.
**How to avoid:** Update `completionResult` first, then `currentScreen` — both in the same
callback lambda. Kotlin guarantees sequential execution within a lambda, and Compose batches
state updates within a single frame when they occur in the same event handler.

### Pitfall 3: Score Written After `clearGame()`
**What goes wrong:** If `repository.clearGame()` executes before `scoreRepository.saveBestScore()`,
the high score is lost if the process is killed between the two operations.
**Why it happens:** Two separate `withContext(ioDispatcher)` blocks in the same launch could
theoretically be interrupted.
**How to avoid:** Write the score before clearing the game state. Score persistence is more
important than clearing the in-progress game (a cleared game just means the player starts fresh
on next launch — acceptable; a lost personal best is frustrating). Use explicit ordering:
`saveBestScore` → `clearGame` → `_events.emit`.

### Pitfall 4: `requestHint()` on a Complete Board
**What goes wrong:** If `requestHint()` is called on a board where all non-given cells already
equal the solution (possible race condition in a test, or if the hint button state is stale),
the `candidates` list is empty and `candidates.random()` throws `NoSuchElementException`.
**Why it happens:** `List.random()` on an empty list throws rather than returning null.
**How to avoid:** Early return if `candidates.isEmpty()` — this is the same guard that sets
`canRequestHint = false` for the button's disabled state.

### Pitfall 5: DataStore Instance Sharing Between Game and Score
**What goes wrong:** If both `DataStoreGameRepository` and `DataStoreScoreRepository` use the
same `Context.gameDataStore` DataStore instance, key namespace collisions are theoretically
possible (and the code is semantically wrong even if no collision exists today).
**Why it happens:** Developer reuses the existing `gameDataStore` extension property for
convenience.
**How to avoid:** Create `val Context.scoreDataStore: DataStore<Preferences> by preferencesDataStore(name = "score_state")`
as a new extension property. Inject this into `DataStoreScoreRepository` in `MainActivity`.

### Pitfall 6: `leaderboardScores` StateFlow Not Refreshed After Score Write
**What goes wrong:** Player completes a game, scores a personal best. `DataStoreScoreRepository`
writes the new score. Player taps "View Leaderboard" — but the leaderboard shows the old score
(or no score) because `_leaderboardScores` was only loaded at ViewModel init.
**Why it happens:** The StateFlow snapshot is stale; it was populated at init from DataStore but
not refreshed after the write.
**How to avoid:** Call `refreshLeaderboard()` after every `saveBestScore()` call within the
completion handler, before emitting `GameEvent.Completed`.

---

## Code Examples

Verified patterns from the project codebase:

### DataStore intPreferencesKey Usage
```kotlin
// Pattern from DataStoreGameRepository.kt — adapt for Int keys
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

private val HIGH_SCORE_EASY = intPreferencesKey("high_score_easy")

// Read
val score: Int? = dataStore.data.first()[HIGH_SCORE_EASY]

// Write
dataStore.edit { prefs -> prefs[HIGH_SCORE_EASY] = score }
```
**Source:** `DataStoreGameRepository.kt` (project codebase) — same `dataStore.data.first()` and
`dataStore.edit { }` pattern; key type changes from `stringPreferencesKey` to `intPreferencesKey`.

### FakeScoreRepository for Tests (mirrors FakeGameRepository pattern)
```kotlin
// Pattern from FakeGameRepository.kt
class FakeScoreRepository : ScoreRepository {
    private val scores = mutableMapOf<Difficulty, Int>()
    var saveCallCount = 0; private set

    override suspend fun getBestScore(difficulty: Difficulty): Int? = scores[difficulty]
    override suspend fun saveBestScore(difficulty: Difficulty, score: Int) {
        scores[difficulty] = score
        saveCallCount++
    }
}
```
**Source:** `FakeGameRepository.kt` (project codebase) — structural pattern.

### Conditional "New personal best!" Display
```kotlin
// UI-02 compliant: no AnimatedVisibility
if (result.isPersonalBest) {
    TextMMD(
        text = "New personal best!",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        textAlign = TextAlign.Center
    )
}
```
**Source:** UI-SPEC §SummaryScreen — conditional rendering, no animation.

### ControlsRow Extension (4th button)
```kotlin
// Add after the Undo ButtonMMD block in ControlsRow.kt
ButtonMMD(
    modifier = Modifier
        .weight(1f)
        .sizeIn(minHeight = 56.dp),
    onClick = onHint,
    enabled = canRequestHint
) {
    TextMMD(text = "Get Hint")
}
```
**Source:** `ControlsRow.kt` (project codebase) — Undo button is the direct precedent;
UI-SPEC §Hint Button confirms label "Get Hint" and `enabled` state.

### Score Row Pattern (SummaryScreen + LeaderboardScreen)
```kotlin
// UI-SPEC §Score Row Pattern
Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween
) {
    TextMMD(text = label)
    TextMMD(text = value)
}
```
**Source:** `05-UI-SPEC.md` §Score Row Pattern (project codebase).

---

## State of the Art

No new libraries or external APIs are introduced in this phase. All patterns are established
from prior phases.

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `GameEvent.Completed(errorCount: Int)` | `GameEvent.Completed(errorCount, hintCount, score, difficulty, isPersonalBest)` | Phase 5 | Caller (GameScreen) must pass through all fields to CompletionResult |
| `ControlsRow(inputMode, onToggleMode, onUndo)` | Adds `onHint: () -> Unit, canRequestHint: Boolean` | Phase 5 | All callers of ControlsRow must provide the two new params |
| `MainActivity` — single `GameScreen(viewModel)` | `Screen` enum + `when` dispatch | Phase 5 | Phase 6 NavHost replaces; leaf composables designed nav-unaware |

---

## Open Questions

1. **`GameEvent.Completed` vs `CompletionResult` data class**
   - What we know: CONTEXT.md says `GameEvent.Completed` must include `hintCount`. The score
     and `isPersonalBest` are derived in the ViewModel before the event fires.
   - What's unclear: Should `GameEvent.Completed` carry the full `CompletionResult` as a field,
     or should the five fields be inline on the event class?
   - Recommendation: Inline fields on `GameEvent.Completed` are the simplest approach that
     avoids a circular dependency (`GameEvent.kt` importing `CompletionResult`). The planner
     should define `CompletionResult` as a plain data class in `game/model/` and have
     `GameEvent.Completed` carry a single `result: CompletionResult` field — or flatten inline.
     Both are acceptable; inline is marginally simpler.

2. **`requestHint()` and the undo stack**
   - What we know: CONTEXT.md D-03 says hints permanently reveal a cell. No mention of undo.
   - What's unclear: Should `requestHint()` push a `FillCell` undo action, allowing the player
     to undo a hint (but `hintCount` would NOT decrement — inconsistent)?
   - Recommendation: Do NOT push to undo stack. Hints are permanent. Attempting to undo a hint
     would leave `hintCount` elevated while the cell is restored — a confusing state. Treat hint
     as an non-undoable reveal.

---

## Environment Availability

Step 2.6: SKIPPED (no external dependencies — phase is pure Kotlin/Compose code changes with
existing library set; no new CLIs, services, or runtimes required).

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 (4.13.2) + Robolectric (4.14.x) + Turbine (1.2.0) |
| Config file | `app/build.gradle.kts` — `testOptions.unitTests.isIncludeAndroidResources = true` |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.mudita.sudoku.game.*" -x lint` |
| Full suite command | `./gradlew :app:testDebugUnitTest -x lint` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SCORE-03 | requestHint() fills one unfilled cell; hintCount increments | unit | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelHintTest*" -x lint` | ❌ Wave 0 |
| SCORE-03 | requestHint() no-ops when canRequestHint = false (no candidates) | unit | same | ❌ Wave 0 |
| SCORE-04 | Score formula: hint penalty deducted per hint used | unit | `./gradlew :app:testDebugUnitTest --tests "*.ScoreCalculationTest*" -x lint` | ❌ Wave 0 |
| SCORE-05 | SummaryScreen shows errorCount, hintCount, finalScore from CompletionResult | unit (Compose) | `./gradlew :app:testDebugUnitTest --tests "*.SummaryScreenTest*" -x lint` | ❌ Wave 0 |
| SCORE-06 | Score = max(0, 100 - errors×10 - hints×5); floors at 0 | unit | same as SCORE-04 | ❌ Wave 0 |
| HS-01 | DataStoreScoreRepository writes and reads Int per difficulty | unit | `./gradlew :app:testDebugUnitTest --tests "*.DataStoreScoreRepositoryTest*" -x lint` | ❌ Wave 0 |
| HS-02 | isPersonalBest = true when new score > stored score | unit | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelHintTest*" -x lint` | ❌ Wave 0 |
| HS-03 | LeaderboardScreen displays stored scores per difficulty; "—" when null | unit (Compose) | `./gradlew :app:testDebugUnitTest --tests "*.LeaderboardScreenTest*" -x lint` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "com.mudita.sudoku.game.*" -x lint`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest -x lint`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `app/src/test/java/com/mudita/sudoku/game/GameViewModelHintTest.kt` — covers SCORE-03, HS-02
- [ ] `app/src/test/java/com/mudita/sudoku/game/ScoreCalculationTest.kt` — covers SCORE-04, SCORE-06
- [ ] `app/src/test/java/com/mudita/sudoku/game/FakeScoreRepository.kt` — test double for HS-01, HS-02 ViewModel tests
- [ ] `app/src/test/java/com/mudita/sudoku/game/DataStoreScoreRepositoryTest.kt` — covers HS-01
- [ ] `app/src/test/java/com/mudita/sudoku/ui/game/SummaryScreenTest.kt` — covers SCORE-05
- [ ] `app/src/test/java/com/mudita/sudoku/ui/game/LeaderboardScreenTest.kt` — covers HS-03

---

## Sources

### Primary (HIGH confidence)
- Project codebase (`GameViewModel.kt`, `DataStoreGameRepository.kt`, `GameRepository.kt`,
  `ControlsRow.kt`, `GameScreen.kt`, `GameUiState.kt`, `PersistedGameState.kt`, `GameEvent.kt`,
  `MainActivity.kt`, `FakeGameRepository.kt`, `MainDispatcherRule.kt`) — direct inspection;
  all integration patterns derived from existing code
- `05-CONTEXT.md` — all locked decisions (D-01 through D-10)
- `05-UI-SPEC.md` — screen layout contracts, copywriting, spacing, component choices
- `CLAUDE.md` — DataStore key names (`high_score_easy/medium/hard`), library versions, E-ink
  constraints
- `app/build.gradle.kts` — confirmed dependency set; no new dependencies required

### Secondary (MEDIUM confidence)
- `REQUIREMENTS.md` — requirement text for SCORE-03 through SCORE-06, HS-01 through HS-03
- `STATE.md` — accumulated architectural decisions from Phases 1–4

### Tertiary (LOW confidence)
- None — all findings are grounded in the project codebase or locked CONTEXT.md decisions.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries already in project; no new dependencies
- Architecture patterns: HIGH — all patterns derived from existing codebase by direct inspection
- Don't hand-roll: HIGH — DataStore Int preference API is proven in DataStoreGameRepository
- Pitfalls: HIGH — backward-compat pitfall confirmed by existing `PersistedGameState` design;
  others derived from Kotlin/coroutine semantics
- Test map: HIGH — test infrastructure mirrors Phase 4 test files exactly

**Research date:** 2026-03-24
**Valid until:** 2026-04-24 (stable stack — no fast-moving external dependencies)
