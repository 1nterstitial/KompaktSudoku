# Phase 4: Persistence - Research

**Researched:** 2026-03-24
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
| STATE-01 | User can pause a game mid-play; full grid state, pencil marks, error count, and hint count are persisted to device storage | DataStore `stringPreferencesKey` + `Json.encodeToString` on `PersistedGameState`; triggered in `Activity.onStop()` |
| STATE-02 | On app launch, if a paused game exists, user is prompted to resume it or start a new game | `GameRepository.loadGame()` returns `PersistedGameState?`; `GameViewModel` exposes `resumePromptVisible: Boolean` flag; `ResumeDialog` composable in `GameScreen` |
| STATE-03 | User can resume a paused game and continue exactly where they left off | `GameViewModel.resumeGame()` restores board, solution, givenMask, pencilMarks, difficulty, errorCount from persisted state; undo stack starts empty |
</phase_requirements>

---

## Summary

Phase 4 adds persistence to an already-working game. The entire tech stack needed is already declared in `app/build.gradle.kts` — DataStore Preferences 1.2.1 and kotlinx.serialization-json 1.8.0 are both present as `implementation` dependencies. No new Gradle additions are required.

The serialization challenge is that `GameUiState` uses primitive arrays (`IntArray`, `BooleanArray`, `Array<Set<Int>>`). These types are NOT directly annotatable with `@Serializable` because `IntArray` and `BooleanArray` are Kotlin primitive arrays rather than generic collections. The clean solution is a dedicated `PersistedGameState` data class that maps arrays to `List<Int>`, `List<Boolean>`, and `List<List<Int>>` — all of which serialize cleanly without custom serializers. This keeps the domain model (`GameUiState`) clean and lets the serialization concern live in its own class.

For lifecycle triggering, `Activity.onStop()` is the correct and reliable choice over `ProcessLifecycleOwner` for this use case. `onStop()` fires on a known single activity (MainActivity), runs on the main thread, and allows a coroutine launch on `lifecycleScope` that will complete before the process is killed. `ProcessLifecycleOwner` is more appropriate for multi-activity apps and adds a 700ms debounce delay that is unnecessary here.

**Primary recommendation:** Introduce `PersistedGameState` as the serialization DTO, a `GameRepository` interface that wraps DataStore read/write, inject it into `GameViewModel`, trigger save from `MainActivity.onStop()`, and check for saved state on `GameViewModel` init to expose the resume prompt flag.

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
│   ├── GameRepository.kt             # NEW: interface for save/load
│   ├── DataStoreGameRepository.kt    # NEW: DataStore implementation
│   └── model/
│       ├── GameUiState.kt            # Unchanged
│       ├── PersistedGameState.kt     # NEW: serialization DTO
│       └── ...
├── ui/game/
│   ├── GameScreen.kt                 # Modified: add ResumeDialog
│   └── ...
└── MainActivity.kt                   # Modified: add onStop save trigger
```

### Pattern 1: Serialization DTO Separation

**What:** `PersistedGameState` is a separate `@Serializable` data class with `List` fields that mirrors the relevant fields of `GameUiState`. Conversion functions map between the two.

**When to use:** Whenever the domain model uses types that don't map cleanly to `@Serializable` (primitive arrays, sealed classes).

**Why not annotate `GameUiState` directly:** `IntArray` and `BooleanArray` require `@Serializable` annotations that interact poorly with the custom `equals`/`hashCode` override on `GameUiState`. A DTO is cleaner and keeps the domain model independent of serialization concerns.

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
    val pencilMarks: List<List<Int>>, // Set<Int> per cell -> List<Int>
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
    pencilMarks = pencilMarks.map { it.toList() },
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

**What:** A `GameRepository` interface with `suspend fun saveGame(state: GameUiState)` and `suspend fun loadGame(): GameUiState?`. The DataStore implementation wraps the DataStore instance.

**When to use:** Always — allows `GameViewModel` to be tested with a `FakeGameRepository` without touching DataStore.

**Example:**
```kotlin
interface GameRepository {
    suspend fun saveGame(state: GameUiState)
    suspend fun loadGame(): GameUiState?
    suspend fun clearGame()
}

// DataStore key — matches CLAUDE.md spec
private val IN_PROGRESS_GAME_KEY = stringPreferencesKey("in_progress_game")

class DataStoreGameRepository(private val dataStore: DataStore<Preferences>) : GameRepository {

    override suspend fun saveGame(state: GameUiState) {
        val json = Json.encodeToString(state.toPersistedState())
        dataStore.edit { prefs ->
            prefs[IN_PROGRESS_GAME_KEY] = json
        }
    }

    override suspend fun loadGame(): GameUiState? {
        return try {
            val prefs = dataStore.data.first()
            val json = prefs[IN_PROGRESS_GAME_KEY] ?: return null
            Json.decodeFromString<PersistedGameState>(json).toUiState()
        } catch (e: Exception) {
            null  // Corrupt data — treat as no saved game
        }
    }

    override suspend fun clearGame() {
        dataStore.edit { prefs ->
            prefs.remove(IN_PROGRESS_GAME_KEY)
        }
    }
}
```

### Pattern 3: DataStore Creation via Context Extension

**What:** Create the DataStore instance via the standard `preferencesDataStore` property delegate on `Context`. Pass the DataStore to the repository constructor.

**Source:** developer.android.com/topic/libraries/architecture/datastore (HIGH confidence)

**Example:**
```kotlin
// Application-scoped DataStore — create once at application level
val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "game_state")

// In MainActivity — pass to repository
val repository = DataStoreGameRepository(applicationContext.gameDataStore)
```

**Important:** `preferencesDataStore` must be called at the TOP LEVEL of a Kotlin file (package level), not inside a class. Calling it inside a class or function creates a new DataStore instance on every call and will corrupt data.

### Pattern 4: GameViewModel Repository Injection (consistent with existing pattern)

**What:** `GameViewModel` accepts `GameRepository` as a constructor parameter, mirroring the existing `generatePuzzle` injectable lambda pattern.

**Example:**
```kotlin
class GameViewModel(
    private val generatePuzzle: suspend (Difficulty) -> SudokuPuzzle = { ... },
    private val repository: GameRepository = NoOpGameRepository()
) : ViewModel() {

    // On init: check for saved game, expose resume prompt
    private val _showResumeDialog = MutableStateFlow(false)
    val showResumeDialog: StateFlow<Boolean> = _showResumeDialog.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repository.loadGame()
            if (saved != null) {
                _pendingSavedState = saved
                _showResumeDialog.update { true }
            }
        }
    }
```

### Pattern 5: Save Trigger in MainActivity.onStop()

**What:** Override `onStop()` in `MainActivity` to call a `suspend` save function via `lifecycleScope`. The coroutine runs on `Dispatchers.IO` for the DataStore write.

**Why `onStop()` over `ProcessLifecycleOwner`:** For a single-activity app, `onStop()` is simpler, fires at the right moment (activity leaves foreground), and does not have the 700ms debounce that `ProcessLifecycleOwner` adds. Reliable on AOSP 12.

**Example:**
```kotlin
// In MainActivity
override fun onStop() {
    super.onStop()
    val currentState = viewModel.uiState.value
    // Only save if a real game is in progress (not loading, not on default empty state)
    if (!currentState.isLoading && currentState.board.any { it != 0 }) {
        lifecycleScope.launch(Dispatchers.IO) {
            repository.saveGame(currentState)
        }
    }
}
```

Note: `lifecycleScope` coroutines launched in `onStop()` are not automatically canceled before the process can be killed. DataStore writes are fast (< 5ms on DataStore's internal Dispatchers.IO executor). The risk of incomplete write before kill is low, but the save must be non-blocking on the main thread.

### Pattern 6: Resume Dialog in GameScreen

**What:** A Material3 `AlertDialog` displayed conditionally when `showResumeDialog` is `true`. Must use `indication = null` on buttons to prevent E-ink ripple (consistent with rest of UI).

**The MMD constraint:** Per CLAUDE.md and Phase 3 findings, `ButtonMMD` must be used rather than plain `Button`. The resume dialog buttons should match the established pattern. If `AlertDialog`'s built-in confirm/dismiss buttons add ripple, use `BasicAlertDialog` with `ButtonMMD` content instead.

**Example:**
```kotlin
@Composable
fun ResumeDialog(
    onResume: () -> Unit,
    onNewGame: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onNewGame) {
        Column(...) {
            TextMMD("Resume last game?")
            ButtonMMD(onClick = onResume) { TextMMD("Resume") }
            ButtonMMD(onClick = onNewGame) { TextMMD("New Game") }
        }
    }
}
```

### Anti-Patterns to Avoid

- **Top-level function instead of property delegate for DataStore:** `preferencesDataStore` must be declared at file scope, not inside a function or class body. Otherwise a new DataStore file handle is opened on every call.
- **Per-move DataStore writes:** D-02 locks this out. DataStore writes are async but the serialization overhead on Helio A22 makes per-move writes measurable. Save only on backgrounding.
- **Saving `isLoading = true` state:** Guard the save call — if `isLoading` is true, the board is blank or mid-generation. Saving that state would cause a corrupt restore on next launch.
- **Saving `isComplete = true` state:** A completed game should not be restored as "in progress". Clear the saved state when the game completes (in `applyFill` when `allCorrect` is detected), or guard the save call.
- **Direct `GameUiState` serialization:** `GameUiState.isLoading` and `GameUiState.inputMode` should not be persisted (they reset naturally). Using `PersistedGameState` as a DTO explicitly controls which fields are stored.
- **Storing DataStore instance in a static/companion object:** The Context-extension delegate is the Android-approved pattern. Static instances can leak context.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Async key-value storage | Manual SharedPreferences writes, custom file I/O | DataStore Preferences | Already declared; coroutine-native; transactional writes; no file corruption on process kill |
| JSON encode/decode | Manual string building, custom parsers | `Json.encodeToString` / `Json.decodeFromString` | Already declared; type-safe; handles null and nested collections |
| Lifecycle observation | Manual `onStop` flags, global booleans | `lifecycleScope.launch` in `onStop()` | Already declared in lifecycle-runtime-compose; correct cancellation semantics |

**Key insight:** Every piece of infrastructure needed already exists in the declared dependencies. The entire phase is configuration and wiring, not new library integration.

---

## Common Pitfalls

### Pitfall 1: Saving During Active Puzzle Generation

**What goes wrong:** `onStop()` fires while `isLoading = true` (puzzle is being generated on `Dispatchers.Default`). The board is all zeros. Saving this state, then restoring it on next launch, presents a blank board with no given cells — an unplayable state.

**Why it happens:** Configuration changes (rotation, though E-ink devices rarely rotate) or background-then-foreground during generation can trigger `onStop()`.

**How to avoid:** Guard save with `if (!currentState.isLoading && currentState.board.any { it != 0 })`.

**Warning signs:** Resume dialog appears but board is blank.

### Pitfall 2: Saving a Completed Game

**What goes wrong:** Player completes the puzzle. Before Phase 5's completion screen exists, the game sits in `isComplete = true`. If the player backgrounds the app, `onStop()` saves this completed state. Next launch presents the resume dialog — resuming loads a completed board.

**Why it happens:** Phase 4 has no post-completion navigation to clear game state.

**How to avoid:** Clear saved game state when `isComplete` transitions to `true` (add `repository.clearGame()` call in `GameViewModel.applyFill` when `allCorrect`).

**Warning signs:** Resume dialog offers to resume a full board.

### Pitfall 3: IntArray/BooleanArray Serialization Failure

**What goes wrong:** Adding `@Serializable` directly to `GameUiState` fails because `IntArray` and `BooleanArray` require `@Serializable(with = IntArraySerializer::class)` annotations. The K2 compiler error is not always obvious.

**Why it happens:** kotlinx.serialization does have `IntArraySerializer` and `BooleanArraySerializer` as builtins, but they are not applied automatically — they must be referenced explicitly OR the type must be converted to `List` first.

**How to avoid:** Use the `PersistedGameState` DTO with `List<Int>` and `List<Boolean>` fields. These serialize automatically without any annotation override.

**Warning signs:** `@Serializable` compile-time errors mentioning `IntArray` or `BooleanArray`.

### Pitfall 4: `Array<Set<Int>>` Serialization

**What goes wrong:** `pencilMarks: Array<Set<Int>>` in `GameUiState` is a generic array of generic sets. This combination is not trivially serializable.

**Why it happens:** `Array<T>` with generic `T` requires explicit serializer specification in kotlinx.serialization.

**How to avoid:** In `PersistedGameState`, represent pencil marks as `List<List<Int>>`. The conversion `pencilMarks.map { it.toList() }` handles this cleanly.

**Warning signs:** Runtime exception `kotlinx.serialization.SerializationException: Serializer for class 'Set' is not found`.

### Pitfall 5: DataStore `data.first()` on Main Thread

**What goes wrong:** Calling `dataStore.data.first()` on the main thread throws `NetworkOnMainThreadException` (DataStore 1.2.1 still enforces off-main-thread for I/O).

**Why it happens:** Repository `loadGame()` must run on `Dispatchers.IO`.

**How to avoid:** Always call `loadGame()` from a coroutine launched on `Dispatchers.IO`, or use `withContext(Dispatchers.IO)` inside the repository implementation. In `GameViewModel.init`, `viewModelScope.launch(Dispatchers.IO)` is the correct scope.

**Warning signs:** `IllegalStateException: Cannot access database on the main thread`.

### Pitfall 6: Double-init Resume Check

**What goes wrong:** `GameScreen` has a `LaunchedEffect(Unit) { viewModel.startGame(Difficulty.EASY) }`. If `GameViewModel.init` also fires a resume check, both run in parallel — `startGame` can overwrite the restored state before the resume dialog appears.

**Why it happens:** The current `GameScreen` auto-starts an Easy game unconditionally.

**How to avoid:** `GameScreen` must check `showResumeDialog` BEFORE calling `startGame`. If a resume prompt is pending, do not call `startGame`. The LaunchedEffect condition should be: `LaunchedEffect(Unit) { if (!viewModel.hasSavedGame()) viewModel.startGame(Difficulty.EASY) }`. Alternatively, `GameViewModel.init` can auto-start a new game only when no saved state exists.

**Warning signs:** Resume dialog flashes briefly then disappears because `startGame` reset the state.

---

## Code Examples

Verified patterns from official sources and project codebase analysis:

### DataStore Preferences Instance (file-scope delegate)
```kotlin
// Source: developer.android.com/topic/libraries/architecture/datastore
// File: DataStoreGameRepository.kt (top level, before class declaration)
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
    dataStore.edit { prefs ->
        prefs[IN_PROGRESS_GAME_KEY] = json
    }
}
```

### MainActivity.onStop() coroutine launch
```kotlin
// Source: developer.android.com/topic/libraries/architecture/lifecycle
override fun onStop() {
    super.onStop()
    val state = viewModel.uiState.value
    if (!state.isLoading && !state.isComplete && state.board.any { it != 0 }) {
        lifecycleScope.launch(Dispatchers.IO) {
            repository.saveGame(state)
        }
    }
}
```

### @Serializable DTO (clean List-based fields)
```kotlin
// Source: kotlinlang.org/api/kotlinx.serialization
@Serializable
data class PersistedGameState(
    val board: List<Int>,
    val solution: List<Int>,
    val givenMask: List<Boolean>,
    val difficulty: String,
    val selectedCellIndex: Int?,
    val pencilMarks: List<List<Int>>,
    val errorCount: Int,
    val isComplete: Boolean
)
```

### FakeGameRepository for testing
```kotlin
// Consistent with existing FakeGenerator pattern in project test sources
class FakeGameRepository(
    private var savedState: GameUiState? = null
) : GameRepository {
    override suspend fun saveGame(state: GameUiState) { savedState = state }
    override suspend fun loadGame(): GameUiState? = savedState
    override suspend fun clearGame() { savedState = null }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| SharedPreferences (synchronous) | DataStore Preferences (coroutine-native) | Android Jetpack 2020+ | No ANR risk; transactional writes |
| LifecycleObserver interface | DefaultLifecycleObserver | Lifecycle 2.4+ | Deprecation-free; default methods |

**Nothing deprecated:** All patterns used in this phase are current for API 31 + Lifecycle 2.9.0.

---

## Environment Availability

Step 2.6: SKIPPED (no external tool/service dependencies — Phase 4 is pure code and local DataStore I/O).

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.14.1 |
| Config file | `app/build.gradle.kts` — `testOptions.unitTests.isIncludeAndroidResources = true` |
| Quick run command | `./gradlew test --tests "com.mudita.sudoku.game.*" -x lint` |
| Full suite command | `./gradlew test -x lint` |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| STATE-01 | `saveGame()` stores all fields; restored state equals original on `loadGame()` | unit | `./gradlew test --tests "*.GameRepositoryTest" -x lint` | Wave 0 |
| STATE-01 | Save is NOT triggered while `isLoading = true` | unit | `./gradlew test --tests "*.GameViewModelPersistenceTest" -x lint` | Wave 0 |
| STATE-01 | Save is NOT triggered when game `isComplete = true` | unit | `./gradlew test --tests "*.GameViewModelPersistenceTest" -x lint` | Wave 0 |
| STATE-02 | `showResumeDialog` is true when saved state exists on init | unit | `./gradlew test --tests "*.GameViewModelPersistenceTest" -x lint` | Wave 0 |
| STATE-02 | `showResumeDialog` is false when no saved state exists on init | unit | `./gradlew test --tests "*.GameViewModelPersistenceTest" -x lint` | Wave 0 |
| STATE-02 | Resume dialog renders over GameScreen when showResumeDialog=true | unit (Compose) | `./gradlew test --tests "*.GameScreenPersistenceTest" -x lint` | Wave 0 |
| STATE-03 | `resumeGame()` restores board, pencilMarks, errorCount, difficulty exactly | unit | `./gradlew test --tests "*.GameViewModelPersistenceTest" -x lint` | Wave 0 |
| STATE-03 | After resuming, undo stack is empty | unit | `./gradlew test --tests "*.GameViewModelPersistenceTest" -x lint` | Wave 0 |
| STATE-03 | "New Game" from dialog starts Easy game and clears saved state | unit | `./gradlew test --tests "*.GameViewModelPersistenceTest" -x lint` | Wave 0 |
| STATE-03 | Resume then re-pause preserves updated state correctly (round-trip) | unit | `./gradlew test --tests "*.GameViewModelPersistenceTest" -x lint` | Wave 0 |

### Sampling Rate

- **Per task commit:** `./gradlew test --tests "com.mudita.sudoku.game.*" -x lint`
- **Per wave merge:** `./gradlew test -x lint`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/java/com/mudita/sudoku/game/GameRepositoryTest.kt` — covers STATE-01 (DataStore round-trip for all PersistedGameState fields)
- [ ] `app/src/test/java/com/mudita/sudoku/game/GameViewModelPersistenceTest.kt` — covers STATE-01 guard conditions, STATE-02 dialog flag, STATE-03 restore and round-trip
- [ ] `app/src/test/java/com/mudita/sudoku/ui/game/GameScreenPersistenceTest.kt` — covers STATE-02 dialog rendering
- [ ] `app/src/test/java/com/mudita/sudoku/game/FakeGameRepository.kt` — shared test double for all persistence tests

---

## Open Questions

1. **ViewModel factory for `DataStoreGameRepository` injection**
   - What we know: `GameViewModel` currently uses the default `viewModel()` factory in `GameScreen`, which calls the zero-arg constructor. Adding `repository: GameRepository` as a constructor parameter requires a custom `ViewModelProvider.Factory` or a factory lambda pattern.
   - What's unclear: The cleanest approach without introducing Hilt (out of scope) is to use a `ViewModelProvider.Factory` created in `MainActivity` where the DataStore context is available, and pass it into `GameScreen`. Alternatively, `GameScreen` can accept a pre-built `GameViewModel` parameter (already the case — `viewModel: GameViewModel = viewModel()`).
   - Recommendation: Pass a custom factory to `viewModel(factory = ...)` in `GameScreen`, constructing the factory in `MainActivity` with the DataStore instance. This avoids Hilt and keeps injection at the composition root.

2. **MainActivity holds both ViewModel reference and Repository reference**
   - What we know: `onStop()` needs to call `repository.saveGame(viewModel.uiState.value)`. MainActivity needs both references.
   - What's unclear: Whether the repository reference should live in the ViewModel (accessed via `viewModel.saveNow()`) or be a separate field in `MainActivity`.
   - Recommendation: `GameViewModel` exposes a `suspend fun saveNow()` that delegates to the repository. `MainActivity.onStop()` calls `lifecycleScope.launch { viewModel.saveNow() }`. This keeps `MainActivity` thin and tests `saveNow()` via `GameViewModelPersistenceTest`.

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
- WebSearch results for `AlertDialog` / `BasicAlertDialog` patterns — consistent with Compose Material3 docs but not individually verified against the BOM 2026.03.00 API

---

## Project Constraints (from CLAUDE.md)

| Directive | Impact on Phase 4 |
|-----------|-------------------|
| DataStore 1.2.1 + kotlinx.serialization 1.8.0 | Already declared; use as specified |
| `in_progress_game` key name | Must use exactly this string for `stringPreferencesKey` |
| MVVM + StateFlow | Repository injected into ViewModel; no DataStore calls from UI |
| No animations, no ripple | `ResumeDialog` buttons must use `indication = null` or `ButtonMMD` |
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
- Architecture patterns: HIGH — DataStore and kotlinx.serialization APIs verified against official docs; DTO pattern is a well-established Android practice
- Pitfalls: HIGH — array serialization issues and double-init problem are confirmed by codebase inspection; isLoading/isComplete guard conditions are derived from direct code reading of `GameViewModel.kt`

**Research date:** 2026-03-24
**Valid until:** 2026-05-24 (stable libraries; DataStore 1.2.x is not changing rapidly)
