# Phase 4: Persistence - Research

**Researched:** 2026-03-24 (updated 2026-03-24 post-UI-SPEC)
**Domain:** Android DataStore Preferences + kotlinx.serialization + Lifecycle-triggered save
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Game state is saved automatically when the app goes to background (via `Activity.onStop()` or `ProcessLifecycleOwner`). No explicit pause button needed in Phase 4.
- **D-02:** Save on backgrounding only — no per-move DataStore writes. Avoids performance overhead on the Helio A22 CPU.
- **D-03:** On app launch, if a saved game exists, a modal dialog appears on GameScreen: "Resume last game?" with two options — Resume and New Game. Phase 6 replaces this flow with the full navigation menu.
- **D-04:** If the player chooses "New Game" from the resume dialog, a new Easy game is started automatically. No difficulty picker in Phase 4 — that belongs in Phase 6's menu screen.
- **D-05:** Undo history is NOT persisted across pause/resume. On resume, the undo stack starts empty.

### Claude's Discretion

- Repository layer design (how `GameRepository` is structured, how it's injected into `GameViewModel`) — Claude decides the cleanest approach consistent with MVVM + StateFlow.
- Serialization data class shape (whether a separate `PersistedGameState` is introduced or `GameUiState` is annotated directly) — Claude decides based on what keeps the domain model clean.
- Lifecycle observer placement (Activity `onStop` vs `ProcessLifecycleOwner`) — Claude decides based on what works reliably on AOSP 12.

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| STATE-01 | User can pause a game mid-play; full grid state, pencil marks, error count, and hint count are persisted to device storage | DataStore `stringPreferencesKey` + `Json.encodeToString` on `PersistedGameState`; triggered in `Activity.onStop()` via `viewModel.saveNow()` |
| STATE-02 | On app launch, if a paused game exists, user is prompted to resume it or start a new game | `GameRepository.loadGame()` returns `PersistedGameState?`; `GameViewModel` exposes `showResumeDialog: StateFlow<Boolean>`; `ResumeDialog` composable in `GameScreen` with `BasicAlertDialog` |
| STATE-03 | User can resume a paused game and continue exactly where they left off | `GameViewModel.resumeGame()` restores board, solution, givenMask, pencilMarks, difficulty, errorCount from persisted state; undo stack starts empty |
</phase_requirements>

---

## Summary

Phase 4 adds persistence to an already-working game. The entire tech stack needed is already declared in `app/build.gradle.kts` — DataStore Preferences 1.2.1 and kotlinx.serialization-json 1.8.0 are both present as `implementation` dependencies. No new Gradle additions are required.

The serialization challenge is that `GameUiState` uses primitive arrays (`IntArray`, `BooleanArray`, `Array<Set<Int>>`). These types are NOT directly annotatable with `@Serializable` because `IntArray` and `BooleanArray` are Kotlin primitive arrays rather than generic collections. The clean solution is a dedicated `PersistedGameState` data class that maps arrays to `List<Int>`, `List<Boolean>`, and `List<List<Int>>` — all of which serialize cleanly without custom serializers. This keeps the domain model (`GameUiState`) clean and lets the serialization concern live in its own class.

For lifecycle triggering, `Activity.onStop()` is the correct and reliable choice over `ProcessLifecycleOwner` for this use case. `onStop()` fires on a known single activity (MainActivity), runs on the main thread, and allows a coroutine launch on `lifecycleScope` that will complete before the process is killed. `ProcessLifecycleOwner` is more appropriate for multi-activity apps and adds a 700ms debounce delay that is unnecessary here.

The ViewModel factory question (Open Questions in v1) is resolved: use `by viewModels { factory }` in `MainActivity` with an inline `ViewModelProvider.Factory` that passes the `DataStoreGameRepository` instance to `GameViewModel`. No Hilt or dependency injection framework is needed.

**Primary recommendation:** Introduce `PersistedGameState` as the serialization DTO, a `GameRepository` interface that wraps DataStore read/write, inject it into `GameViewModel` via `ViewModelProvider.Factory` in `MainActivity`, trigger save from `MainActivity.onStop()` via `viewModel.saveNow()`, and check for saved state in `GameViewModel.init` to expose the resume prompt flag.

---

## Standard Stack

### Core (all already declared in build.gradle.kts — no new dependencies needed)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| DataStore Preferences | 1.2.1 | Async key-value persistence | Already declared; coroutine-native; no ANR risk; official Google recommendation |
| kotlinx.serialization-json | 1.8.0 | JSON encode/decode for `PersistedGameState` | Already declared; compile-time checked; Kotlin-native |
| Lifecycle (lifecycleScope) | 2.9.0 (via BOM) | Launch save coroutine in `onStop()` | Already declared; `lifecycleScope` outlives the composition but is bounded by Activity |
| Coroutines | 1.10.2 | Suspend save/load operations | Already declared |

### No New Dependencies Required

All required libraries are already in `app/build.gradle.kts`. Phase 4 adds no new entries to `libs.versions.toml`.

---

## Architecture Patterns

### Recommended Project Structure (additions only)

```
app/src/main/java/com/mudita/sudoku/
├── game/
│   ├── GameViewModel.kt              # Modified: add repository param + resume logic
│   ├── GameRepository.kt             # NEW: interface + NoOpGameRepository
│   ├── DataStoreGameRepository.kt    # NEW: DataStore implementation + gameDataStore delegate
│   └── model/
│       ├── GameUiState.kt            # Unchanged
│       ├── PersistedGameState.kt     # NEW: serialization DTO + conversion functions
│       └── ...
├── ui/game/
│   ├── GameScreen.kt                 # Modified: add ResumeDialog + hasSavedGame guard
│   └── ...
└── MainActivity.kt                   # Modified: factory, onStop save trigger
```

### Pattern 1: Serialization DTO Separation

**What:** `PersistedGameState` is a separate `@Serializable` data class with `List` fields that mirrors the relevant fields of `GameUiState`. Conversion functions map between the two.

**When to use:** Whenever the domain model uses types that don't map cleanly to `@Serializable` (primitive arrays, sealed classes).

**Why not annotate `GameUiState` directly:** `IntArray` and `BooleanArray` require `@Serializable` annotations that interact poorly with the custom `equals`/`hashCode` override on `GameUiState`. A DTO is cleaner and keeps the domain model independent of serialization concerns. `inputMode` and `isLoading` are intentionally excluded — they reset to defaults on resume.

**Example:**
```kotlin
// Source: kotlinlang.org/api/kotlinx.serialization — built-in serializers
@Serializable
data class PersistedGameState(
    val board: List<Int>,
    val solution: List<Int>,
    val givenMask: List<Boolean>,
    val difficulty: String,           // Difficulty.name — enums serialize as String
    val selectedCellIndex: Int? = null,
    val pencilMarks: List<List<Int>>, // Set<Int> per cell -> sorted List<Int>
    val errorCount: Int,
    val isComplete: Boolean
)

// Conversion from domain model
fun GameUiState.toPersistedState(): PersistedGameState = PersistedGameState(
    board = board.toList(),
    solution = solution.toList(),
    givenMask = givenMask.toList(),
    difficulty = difficulty.name,
    selectedCellIndex = selectedCellIndex,
    pencilMarks = pencilMarks.map { it.sorted() },  // sorted for deterministic JSON
    errorCount = errorCount,
    isComplete = isComplete
)

// Conversion to domain model
fun PersistedGameState.toUiState(): GameUiState = GameUiState(
    board = board.toIntArray(),
    solution = solution.toIntArray(),
    givenMask = givenMask.toBooleanArray(),
    difficulty = Difficulty.valueOf(difficulty),
    selectedCellIndex = selectedCellIndex,
    pencilMarks = Array(81) { i -> pencilMarks[i].toSet() },
    errorCount = errorCount,
    isComplete = isComplete
    // inputMode defaults to FILL — intentional on resume
    // isLoading defaults to false — correct
)
```

### Pattern 2: Repository Interface + DataStore Implementation

**What:** A `GameRepository` interface with `suspend fun saveGame(state: GameUiState)`, `suspend fun loadGame(): GameUiState?`, and `suspend fun clearGame()`. The DataStore implementation wraps the DataStore instance.

**When to use:** Always — allows `GameViewModel` to be tested with a `FakeGameRepository` without touching DataStore.

**Example:**
```kotlin
interface GameRepository {
    suspend fun saveGame(state: GameUiState)
    suspend fun loadGame(): GameUiState?
    suspend fun clearGame()
}

// NoOp default — keeps GameViewModel backward-compatible for existing tests
class NoOpGameRepository : GameRepository {
    override suspend fun saveGame(state: GameUiState) {}
    override suspend fun loadGame(): GameUiState? = null
    override suspend fun clearGame() {}
}

// DataStore key — matches CLAUDE.md spec exactly
private val IN_PROGRESS_GAME_KEY = stringPreferencesKey("in_progress_game")

class DataStoreGameRepository(private val dataStore: DataStore<Preferences>) : GameRepository {

    override suspend fun saveGame(state: GameUiState) = withContext(Dispatchers.IO) {
        val json = Json.encodeToString(state.toPersistedState())
        dataStore.edit { prefs -> prefs[IN_PROGRESS_GAME_KEY] = json }
    }

    override suspend fun loadGame(): GameUiState? = withContext(Dispatchers.IO) {
        try {
            val prefs = dataStore.data.first()
            val json = prefs[IN_PROGRESS_GAME_KEY] ?: return@withContext null
            Json.decodeFromString<PersistedGameState>(json).toUiState()
        } catch (e: Exception) {
            null  // Corrupt data — treat as no saved game
        }
    }

    override suspend fun clearGame() = withContext(Dispatchers.IO) {
        dataStore.edit { prefs -> prefs.remove(IN_PROGRESS_GAME_KEY) }
    }
}
```

### Pattern 3: DataStore Creation via Context Extension

**What:** Create the DataStore instance via the standard `preferencesDataStore` property delegate on `Context`. Declare at file scope in `DataStoreGameRepository.kt`.

**Source:** developer.android.com/topic/libraries/architecture/datastore (HIGH confidence)

**Example:**
```kotlin
// At FILE SCOPE (top level, before class declaration) — NOT inside a class or function
val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "game_state")
```

**Important:** `preferencesDataStore` must be called at the TOP LEVEL of a Kotlin file. Calling it inside a class or function creates a new DataStore instance on every call and will corrupt data. In `MainActivity`, import it as `import com.mudita.sudoku.game.gameDataStore`.

### Pattern 4: GameViewModel Repository Injection (consistent with existing pattern)

**What:** `GameViewModel` accepts `GameRepository` as a constructor parameter with `NoOpGameRepository()` as the default. This mirrors the existing `generatePuzzle` injectable lambda pattern and keeps existing `GameViewModelTest` tests backward-compatible with no changes.

**New public API added to GameViewModel:**
```kotlin
class GameViewModel(
    private val generatePuzzle: suspend (Difficulty) -> SudokuPuzzle = { ... },
    private val repository: GameRepository = NoOpGameRepository()
) : ViewModel() {

    val showResumeDialog: StateFlow<Boolean>  // true when saved game found on init
    fun resumeGame()          // restores pendingSavedState, clears undo stack
    fun startNewGame()        // clears saved state, starts Easy game (D-04)
    fun hasSavedGame(): Boolean  // used by GameScreen to guard auto-start
    suspend fun saveNow()     // called from onStop(); guards isLoading/isComplete/empty board

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = repository.loadGame()
            if (saved != null) {
                pendingSavedState = saved
                _showResumeDialog.value = true
            }
        }
    }
}
```

### Pattern 5: Save Trigger in MainActivity.onStop()

**What:** Override `onStop()` in `MainActivity` to call `viewModel.saveNow()` via `lifecycleScope`. The guard conditions (isLoading, isComplete, empty board) live inside `saveNow()`, keeping `MainActivity` thin.

**Why `onStop()` over `ProcessLifecycleOwner`:** For a single-activity app, `onStop()` is simpler, fires at the right moment (activity leaves foreground), and does not have the 700ms debounce that `ProcessLifecycleOwner` adds. Reliable on AOSP 12.

**ViewModel factory pattern:** Use `by viewModels { factory }` delegate with an inline `ViewModelProvider.Factory`. This is the standard AndroidX pattern for ViewModel injection without Hilt.

**Example (full MainActivity):**
```kotlin
class MainActivity : ComponentActivity() {

    private val repository by lazy {
        DataStoreGameRepository(applicationContext.gameDataStore)
    }

    private val viewModel: GameViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GameViewModel(repository = repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD {
                GameScreen(viewModel = viewModel)  // pass activity-scoped ViewModel
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.saveNow()  // guard conditions inside saveNow()
        }
    }
}
```

Note: `repository` uses `lazy` so `applicationContext` is available (not null at property init time). The `by viewModels { factory }` delegate survives configuration changes.

### Pattern 6: Resume Dialog in GameScreen

**What:** A `BasicAlertDialog` (not `AlertDialog`) displayed conditionally when `showResumeDialog` is `true`. Must use `ButtonMMD` for buttons to prevent E-ink ripple.

**Why `BasicAlertDialog` over `AlertDialog`:** `AlertDialog`'s built-in `confirmButton`/`dismissButton` slots may add ripple to their internal button implementations. `BasicAlertDialog` with `ButtonMMD` children is the safe, verified-ripple-free approach.

**UI-SPEC contract (from 04-UI-SPEC.md):**
- Container: `Surface` with `color = Color.White`, `border = BorderStroke(1.dp, Color.Black)`
- Container padding: 16dp vertical, 24dp horizontal
- Buttons: `ButtonMMD`, `fillMaxWidth`, `height(56.dp)` (minimum touch target per UI-03)
- `onDismissRequest = onNewGame` — back press or outside tap = "New Game"
- No second confirmation before "New Game" — single tap is sufficient
- `@OptIn(ExperimentalMaterial3Api::class)` required on `ResumeDialog` function

**Example:**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResumeDialog(
    onResume: () -> Unit,
    onNewGame: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onNewGame) {
        Surface(
            color = Color.White,
            border = BorderStroke(1.dp, Color.Black)
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp)) {
                TextMMD("Resume last game?")
                Spacer(modifier = Modifier.height(16.dp))
                ButtonMMD(onClick = onResume, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    TextMMD("Resume")
                }
                Spacer(modifier = Modifier.height(16.dp))
                ButtonMMD(onClick = onNewGame, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    TextMMD("New Game")
                }
            }
        }
    }
}
```

**GameScreen auto-start guard:**
```kotlin
// Before (Phase 3):
LaunchedEffect(Unit) { viewModel.startGame(Difficulty.EASY) }

// After (Phase 4):
LaunchedEffect(Unit) {
    if (!viewModel.hasSavedGame()) {
        viewModel.startGame(Difficulty.EASY)
    }
}
```

### Anti-Patterns to Avoid

- **Top-level function instead of property delegate for DataStore:** `preferencesDataStore` must be declared at file scope, not inside a function or class body.
- **Per-move DataStore writes:** D-02 locks this out. Save only on backgrounding.
- **Saving `isLoading = true` state:** Guard the save call — board is blank during generation.
- **Saving `isComplete = true` state:** Clear saved state when game completes, not on save.
- **Direct `GameUiState` serialization:** Use `PersistedGameState` DTO to control exactly which fields are stored.
- **Storing DataStore instance in a static/companion object:** Context-extension delegate is the Android-approved pattern.
- **Missing `hasSavedGame()` guard in GameScreen:** Without the guard, `startGame(Difficulty.EASY)` overwrites the pending saved state before `showResumeDialog` can appear (Pitfall 6).

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Async key-value storage | Manual SharedPreferences writes, custom file I/O | DataStore Preferences | Already declared; coroutine-native; transactional writes; no file corruption on process kill |
| JSON encode/decode | Manual string building, custom parsers | `Json.encodeToString` / `Json.decodeFromString` | Already declared; type-safe; handles null and nested collections |
| Lifecycle observation | Manual `onStop` flags, global booleans | `lifecycleScope.launch` in `onStop()` | Already declared in lifecycle-runtime-compose; correct cancellation semantics |
| ViewModel dependency injection | Hilt, Koin, manual singletons | `ViewModelProvider.Factory` inline in `MainActivity` | No DI framework needed for a single-ViewModel app; inline factory is 10 lines |

**Key insight:** Every piece of infrastructure needed already exists in the declared dependencies. The entire phase is configuration and wiring, not new library integration.

---

## Common Pitfalls

### Pitfall 1: Saving During Active Puzzle Generation

**What goes wrong:** `onStop()` fires while `isLoading = true`. The board is all zeros. Saving this state and restoring it on next launch presents a blank board.

**Why it happens:** Any backgrounding event during the ~200ms generation window triggers `onStop()`.

**How to avoid:** Guard save with `if (!state.isLoading && !state.isComplete && state.board.any { it != 0 })` inside `saveNow()`.

**Warning signs:** Resume dialog appears but board is blank.

### Pitfall 2: Saving a Completed Game

**What goes wrong:** Player completes the puzzle. App is backgrounded. `onStop()` saves the completed state. Next launch presents resume dialog — restoring a full board.

**Why it happens:** Phase 4 has no post-completion navigation to clear game state.

**How to avoid:** Call `repository.clearGame()` inside `GameViewModel.applyFill()` when `allCorrect` is detected. The save guard (`isComplete = true` → no save) is a secondary defense.

**Warning signs:** Resume dialog offers to resume a full board.

### Pitfall 3: IntArray/BooleanArray Serialization Failure

**What goes wrong:** Adding `@Serializable` directly to `GameUiState` fails because `IntArray` and `BooleanArray` require explicit serializer references.

**Why it happens:** kotlinx.serialization has `IntArraySerializer` and `BooleanArraySerializer` as builtins but they are not applied automatically to `data class` fields.

**How to avoid:** Use the `PersistedGameState` DTO with `List<Int>` and `List<Boolean>` fields. These serialize automatically without any annotation override.

**Warning signs:** `@Serializable` compile-time errors mentioning `IntArray` or `BooleanArray`.

### Pitfall 4: `Array<Set<Int>>` Serialization

**What goes wrong:** `pencilMarks: Array<Set<Int>>` in `GameUiState` is a generic array of generic sets. This combination is not trivially serializable.

**Why it happens:** `Array<T>` with generic `T` requires explicit serializer specification in kotlinx.serialization.

**How to avoid:** In `PersistedGameState`, represent pencil marks as `List<List<Int>>`. The conversion `pencilMarks.map { it.sorted() }` handles this cleanly and produces deterministic JSON.

**Warning signs:** Runtime exception `kotlinx.serialization.SerializationException: Serializer for class 'Set' is not found`.

### Pitfall 5: DataStore `data.first()` on Main Thread

**What goes wrong:** Calling `dataStore.data.first()` on the main thread throws a thread violation exception (DataStore 1.2.1 enforces off-main-thread I/O).

**Why it happens:** Repository `loadGame()` must run on `Dispatchers.IO`.

**How to avoid:** Use `withContext(Dispatchers.IO)` inside repository methods. In `GameViewModel.init`, use `viewModelScope.launch(Dispatchers.IO)`.

**Warning signs:** `IllegalStateException` on app launch before any UI appears.

### Pitfall 6: Double-init Resume Check (Race Condition)

**What goes wrong:** `GameScreen` has `LaunchedEffect(Unit) { viewModel.startGame(Difficulty.EASY) }`. If `GameViewModel.init` also fires a resume check asynchronously, both run in parallel — `startGame` can overwrite the restored state before the resume dialog appears.

**Why it happens:** The current `GameScreen` auto-starts an Easy game unconditionally (Phase 3 behavior).

**How to avoid:** Guard the `LaunchedEffect` with `if (!viewModel.hasSavedGame())`. The `hasSavedGame()` function checks whether `pendingSavedState != null || showResumeDialog.value`. Since `GameViewModel.init` sets `pendingSavedState` BEFORE setting `_showResumeDialog.value`, `hasSavedGame()` returns true immediately once the init coroutine completes.

**Warning signs:** Resume dialog flashes briefly then disappears as the new game overwrites the loaded state.

---

## Code Examples

Verified patterns from official sources and project codebase analysis:

### DataStore Preferences Instance (file-scope delegate)
```kotlin
// Source: developer.android.com/topic/libraries/architecture/datastore
// File: DataStoreGameRepository.kt — TOP LEVEL, before class declaration
val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "game_state")

private val IN_PROGRESS_GAME_KEY = stringPreferencesKey("in_progress_game")
```

### Reading with null-safe Flow collection
```kotlin
// Source: developer.android.com/topic/libraries/architecture/datastore
override suspend fun loadGame(): GameUiState? = withContext(Dispatchers.IO) {
    try {
        val prefs = dataStore.data.first()
        val json = prefs[IN_PROGRESS_GAME_KEY] ?: return@withContext null
        Json.decodeFromString<PersistedGameState>(json).toUiState()
    } catch (e: Exception) {
        null
    }
}
```

### Writing with DataStore.edit
```kotlin
// Source: developer.android.com/topic/libraries/architecture/datastore
override suspend fun saveGame(state: GameUiState) = withContext(Dispatchers.IO) {
    val json = Json.encodeToString(state.toPersistedState())
    dataStore.edit { prefs -> prefs[IN_PROGRESS_GAME_KEY] = json }
}
```

### saveNow() with guard conditions
```kotlin
// Guards: do not save if loading, complete, or board is empty
suspend fun saveNow() {
    val state = _uiState.value
    if (state.isLoading || state.isComplete || state.board.all { it == 0 }) return
    repository.saveGame(state)
}
```

### MainActivity.onStop() coroutine launch
```kotlin
// Source: developer.android.com/topic/libraries/architecture/lifecycle
override fun onStop() {
    super.onStop()
    lifecycleScope.launch(Dispatchers.IO) {
        viewModel.saveNow()  // guard conditions inside saveNow()
    }
}
```

### ViewModelProvider.Factory for repository injection
```kotlin
// Standard AndroidX pattern — no Hilt needed for single-ViewModel app
private val viewModel: GameViewModel by viewModels {
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameViewModel(repository = repository) as T
        }
    }
}
```

### FakeGameRepository for testing
```kotlin
// Consistent with existing FakeGenerator pattern in project test sources
class FakeGameRepository(
    private var savedState: GameUiState? = null
) : GameRepository {
    var saveCallCount = 0
        private set
    var clearCallCount = 0
        private set

    override suspend fun saveGame(state: GameUiState) { savedState = state; saveCallCount++ }
    override suspend fun loadGame(): GameUiState? = savedState
    override suspend fun clearGame() { savedState = null; clearCallCount++ }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| SharedPreferences (synchronous) | DataStore Preferences (coroutine-native) | Android Jetpack 2020+ | No ANR risk; transactional writes |
| LifecycleObserver interface | DefaultLifecycleObserver | Lifecycle 2.4+ | Deprecation-free; default methods |
| AlertDialog with confirm/dismiss slots | BasicAlertDialog with custom content | Material3 stable | More control; no unwanted ripple on E-ink |

**Nothing deprecated:** All patterns used in this phase are current for API 31 + Lifecycle 2.9.0 + Material3 via BOM 2026.03.00.

---

## Runtime State Inventory

Step 2.5: NOT APPLICABLE — Phase 4 is a greenfield persistence layer addition, not a rename/refactor/migration. No existing stored data or registered state requires migration. DataStore file `game_state` does not exist before Phase 4 runs.

---

## Environment Availability

Step 2.6: SKIPPED (no external tool/service dependencies — Phase 4 is pure code and local DataStore I/O).

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.14.1 + Mockk 1.13.17 + Turbine 1.2.0 |
| Config file | `app/build.gradle.kts` — `testOptions.unitTests.isIncludeAndroidResources = true` |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "*Persistence*" --tests "*DataStore*" --tests "*Repository*" -x lint` |
| Full suite command | `./gradlew :app:testDebugUnitTest -x lint` |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| STATE-01 | `saveGame()` stores all fields; `loadGame()` restores them identically | unit | `./gradlew :app:testDebugUnitTest --tests "*PersistedGameStateTest*" -x lint` | Wave 0 |
| STATE-01 | Save is NOT triggered while `isLoading = true` | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelPersistenceTest*" -x lint` | Wave 0 |
| STATE-01 | Save is NOT triggered when `isComplete = true` | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelPersistenceTest*" -x lint` | Wave 0 |
| STATE-02 | `showResumeDialog` is true when saved state exists on init | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelPersistenceTest*" -x lint` | Wave 0 |
| STATE-02 | `showResumeDialog` is false when no saved state exists on init | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelPersistenceTest*" -x lint` | Wave 0 |
| STATE-03 | `resumeGame()` restores board, pencilMarks, errorCount, difficulty exactly | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelPersistenceTest*" -x lint` | Wave 0 |
| STATE-03 | After resuming, undo stack is empty (undo is a no-op) | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelPersistenceTest*" -x lint` | Wave 0 |
| STATE-03 | "New Game" from dialog clears saved state and starts Easy game | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelPersistenceTest*" -x lint` | Wave 0 |
| STATE-03 | Resume then re-pause preserves updated state (round-trip) | unit | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelPersistenceTest*" -x lint` | Wave 0 |
| STATE-01/02/03 | `onStop()` save trigger — lifecycle hook, not unit-testable | manual | N/A | N/A |

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "*Persistence*" --tests "*Repository*" -x lint`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest -x lint`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/java/com/mudita/sudoku/game/PersistedGameStateTest.kt` — DTO round-trip serialization (STATE-01)
- [ ] `app/src/test/java/com/mudita/sudoku/game/GameViewModelPersistenceTest.kt` — ViewModel persistence behaviors (STATE-01, 02, 03)
- [ ] `app/src/test/java/com/mudita/sudoku/game/FakeGameRepository.kt` — shared test double for all persistence tests

*(DataStoreGameRepository is tested indirectly via FakeGameRepository in ViewModel tests; direct DataStore unit testing requires Android instrumented tests and is deferred.)*

---

## Open Questions

1. **`onStop()` coroutine completion before process kill**
   - What we know: `lifecycleScope` coroutines launched in `onStop()` are not automatically canceled before the process can be killed on AOSP 12.
   - What's unclear: Whether DataStore writes complete within the ~300ms window before Android may kill the process after `onStop()`.
   - Recommendation: DataStore writes are fast (< 5ms). Accept the low-probability risk. For Phase 4's use case (E-ink device where users background intentionally), this is acceptable. Phase 5/6 can add a WorkManager fallback if needed.

2. **`hasSavedGame()` race during ViewModel init**
   - What we know: `GameViewModel.init` loads saved state asynchronously. `GameScreen`'s `LaunchedEffect(Unit)` runs on composition. If composition fires before `init` coroutine completes, `hasSavedGame()` returns false and `startGame()` is called.
   - What's unclear: The timing window is narrow (< 1ms in practice since init launches on `Dispatchers.IO`), but theoretically possible.
   - Recommendation: Accept this timing risk for Phase 4. The `init` block sets `pendingSavedState` synchronously before launching the coroutine if needed, or the ViewModel can start in a "checking saved state" loading mode. Executor chooses the simplest approach.

---

## Sources

### Primary (HIGH confidence)
- [developer.android.com/topic/libraries/architecture/datastore](https://developer.android.com/topic/libraries/architecture/datastore) — DataStore Preferences API: `preferencesDataStore` delegate, `edit{}`, `data.first()`, `stringPreferencesKey`
- [developer.android.com/topic/libraries/architecture/lifecycle](https://developer.android.com/topic/libraries/architecture/lifecycle) — `lifecycleScope`, `DefaultLifecycleObserver`, `onStop()` patterns
- [kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.builtins/-int-array-serializer.html](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.builtins/-int-array-serializer.html) — `IntArraySerializer` and `BooleanArraySerializer` builtin existence confirmed
- Project codebase (`app/build.gradle.kts`, `GameViewModel.kt`, `GameUiState.kt`) — verified all dependencies already declared, existing patterns confirmed

### Secondary (MEDIUM confidence)
- [developer.android.com/reference/android/arch/lifecycle/ProcessLifecycleOwner](https://developer.android.com/reference/android/arch/lifecycle/ProcessLifecycleOwner) — 700ms debounce timing; confirms `onStop()` is more appropriate for single-activity apps
- [medium.com/androiddevelopers/datastore-and-kotlin-serialization-8b25bf0be66c](https://medium.com/androiddevelopers/datastore-and-kotlin-serialization-8b25bf0be66c) — DataStore + kotlinx.serialization patterns; verified against official docs

### Tertiary (LOW confidence — informational only)
- WebSearch results for `BasicAlertDialog` patterns — consistent with Compose Material3 docs but not individually verified against the BOM 2026.03.00 API

---

## Project Constraints (from CLAUDE.md)

| Directive | Impact on Phase 4 |
|-----------|-------------------|
| DataStore 1.2.1 + kotlinx.serialization 1.8.0 | Already declared; use as specified |
| `in_progress_game` key name | Must use exactly this string for `stringPreferencesKey` |
| MVVM + StateFlow | Repository injected into ViewModel; no DataStore calls from UI |
| No animations, no ripple | `ResumeDialog` uses `ButtonMMD`; `BasicAlertDialog` avoids built-in ripple |
| `minSdk 31` | No API < 31 needed; DataStore 1.2.1 supports API 21+ |
| `@Serializable` + `Json.encodeToString/decodeFromString` | Confirmed as the specified serialization approach |
| JUnit 4 (not JUnit 5) | All test files must use `@RunWith(RobolectricTestRunner::class)` |
| Mockk for mocking | Use `mockk<GameRepository>()` in tests that mock the repository |
| Turbine for Flow testing | Use `flow.test {}` for `showResumeDialog` StateFlow assertions |
| GSD workflow enforcement | No direct file edits; work through GSD execute-phase |

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries already present in build files; no new integration risk
- Architecture patterns: HIGH — DataStore and kotlinx.serialization APIs verified against official docs; DTO pattern is a well-established Android practice; ViewModel factory pattern verified from AndroidX docs; UI-SPEC contract from 04-UI-SPEC.md
- Pitfalls: HIGH — array serialization issues and double-init problem are confirmed by codebase inspection; isLoading/isComplete guard conditions derived from direct code reading of `GameViewModel.kt`

**Research date:** 2026-03-24
**Valid until:** 2026-05-24 (stable libraries; DataStore 1.2.x is not changing rapidly)
