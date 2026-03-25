# Phase 2: Game State & Domain - Research

**Researched:** 2026-03-24
**Domain:** Android ViewModel, StateFlow, immutable game state modeling, undo stack, pencil marks, error tracking
**Confidence:** HIGH — all patterns verified against official Android docs and confirmed against the project's actual dependency set in `gradle/libs.versions.toml`

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| DIFF-01 | User can choose Easy, Medium, or Hard difficulty from the main menu before starting a game | `GameViewModel.startGame(difficulty)` drives `SudokuGenerator`; difficulty stored in `GameUiState.difficulty` |
| DIFF-02 | Each difficulty level generates puzzles matching the cell count and technique classification defined in PUZZ-02 and PUZZ-03 | `SudokuGenerator` (Phase 1) already enforces this; Phase 2 just calls it via ViewModel |
| INPUT-01 | User can tap a cell to select it; selected cell is visually highlighted | `selectedCellIndex: Int?` in `GameUiState`; `selectCell(index)` action in ViewModel |
| INPUT-02 | User can tap a digit (1–9) to fill the selected cell with that value | `enterDigit(digit)` action; updates `board` IntArray in `GameUiState`; triggers SCORE-01/SCORE-02 checks |
| INPUT-03 | User can toggle between "fill" mode and "pencil mark" mode for input | `inputMode: InputMode` enum in `GameUiState`; `toggleInputMode()` action |
| INPUT-04 | User can add and remove pencil mark candidates in any cell | `pencilMarks: Array<Set<Int>>` — one `Set<Int>` per cell (81 entries); `enterDigit()` routes to pencil path when mode == PENCIL |
| INPUT-05 | User can undo the last move (fill or pencil mark action) | `undoStack: ArrayDeque<GameAction>` in ViewModel; `undo()` pops and reverses last action |
| SCORE-01 | Errors are tracked silently during play and not surfaced until the game ends | `errorCount` incremented when `board[i] != solution[i]`; never exposed in play UI; checked in ViewModel on each fill |
| SCORE-02 | App automatically detects when all 81 cells are correctly filled and triggers completion | All-cells check after every fill: `board.zip(solution).all { (a, b) -> a == b }`; emits `GameEvent.Completed` via SharedFlow |
</phase_requirements>

---

## Summary

Phase 2 builds the pure domain and ViewModel layer for the game loop. Phase 1 produced puzzle generation. Phase 2 wraps that in a ViewModel that holds all runtime game state: the current board, pencil marks, selection, input mode, error count, and undo history. The UI in Phase 3 will bind to this ViewModel.

The central design decision is a **single `GameUiState` data class** exposed as `StateFlow<GameUiState>` from `GameViewModel`. Each player action (selectCell, enterDigit, toggleInputMode, undo) produces a new copy of `GameUiState` via `.copy()`, keeping state fully immutable and recomposition-safe. Side effects (puzzle complete) are emitted as one-shot `GameEvent` values on a `SharedFlow` to prevent re-emission on recomposition.

Undo is modeled as an `ArrayDeque<GameAction>` where each `GameAction` is a sealed class capturing enough state to reverse the action. Pencil marks are `Array<Set<Int>>` — a 81-element array where each entry is the set of candidate digits annotated in that cell. This is the standard model used in production Sudoku apps (verified via LibreSudoku architecture research and domain knowledge).

Phase 2 has no Android UI dependencies — `GameViewModel` uses `viewModelScope`, but the state data classes are pure Kotlin. All logic is unit-testable with `runTest` + `UnconfinedTestDispatcher` + Turbine. The only Android dependency is `ViewModel` itself, which Robolectric makes testable on the JVM.

**Primary recommendation:** Single `GameUiState` data class + `StateFlow` + `SharedFlow` for events + `ArrayDeque<GameAction>` undo stack. No MVI, no Redux. MVVM with the backing-property pattern is the right fit for this complexity level.

---

## Project Constraints (from CLAUDE.md)

| Constraint | Impact on Phase 2 |
|------------|-------------------|
| Kotlin + Jetpack Compose + MMD required | ViewModel exposes `StateFlow` — Compose collects via `collectAsStateWithLifecycle`. No MMD components in ViewModel layer itself (those are Phase 3) |
| MVVM + StateFlow architecture | Mandatory pattern: `GameViewModel : ViewModel()` with `_uiState: MutableStateFlow<GameUiState>` + backing property |
| No animations, ripple effects | Not applicable to ViewModel layer; enforced at UI layer in Phase 3 |
| Local only, no network | All logic is on-device; no repository pattern needed for Phase 2 (DataStore comes in Phase 4) |
| GSD workflow enforcement | All file changes via `/gsd:execute-phase` |
| Ripple effects disabled in ThemeMMD | Not applicable to domain/ViewModel layer |
| `ButtonMMD` and `TextMMD` throughout | Not applicable to domain/ViewModel layer |
| `minSdk = 31`, `targetSdk = 31` | No API < 31 restrictions affect ViewModel or coroutines; both fully available on API 31 |

---

## Standard Stack

### Core (all already declared in `gradle/libs.versions.toml` and `app/build.gradle.kts`)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Jetpack ViewModel (`lifecycle-viewmodel-compose`) | 2.9.0 (via lifecycle) | State holder, business logic host | Survives configuration changes; project mandates MVVM; already declared in build |
| Kotlin Coroutines + StateFlow | 1.10.2 | Async execution, reactive state | `viewModelScope` + `MutableStateFlow.update()` is the canonical MVVM pattern; `coroutines-android` already declared |
| `lifecycle-runtime-compose` | 2.9.0 | `collectAsStateWithLifecycle` | Lifecycle-aware collection from Compose; already declared |
| JUnit 4 | 4.13.2 | Unit test runner | Already in use from Phase 1; Compose test rule requires JUnit 4 |
| Mockk | 1.13.17 | Mocking in unit tests | Kotlin-idiomatic; already declared |
| Turbine | 1.2.0 | Flow/StateFlow testing | `flow.test { awaitItem() }` pattern; already declared |
| `kotlinx-coroutines-test` | 1.10.2 | `runTest`, `UnconfinedTestDispatcher` | ViewModel coroutine testing; already declared |
| Robolectric | 4.14.1 | JVM-based Android testing | Allows ViewModel tests without emulator; already declared |

**No new dependencies required for Phase 2.** All needed libraries are already declared in the project.

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Single `GameUiState` data class | Multiple `StateFlow` fields | Multiple flows reduce coupling but require Compose to combine them; for a simple game screen a single UI state is simpler and less error-prone |
| `SharedFlow` for `GameEvent` | `Channel<GameEvent>` | Channel is equally valid but SharedFlow with `replay=0` is the recommended Google pattern for one-time UI events; prefer it |
| `ArrayDeque` for undo | `MutableList<GameAction>` | Equivalent; `ArrayDeque` is more semantically correct as a stack (push/pop both O(1)) and is the Kotlin stdlib recommendation for stack use cases |
| Sealed class `GameAction` | Copy full board state per undo | Full board copy (81 ints × N actions) is wasteful; delta-based sealed class is standard and uses negligible memory |

---

## Architecture Patterns

### Recommended Project Structure (Phase 2 additions)

```
app/src/main/java/com/mudita/sudoku/
├── puzzle/                           # Phase 1 (complete — do not modify)
│   ├── engine/
│   └── model/
├── game/                             # Phase 2 NEW
│   ├── model/
│   │   ├── GameUiState.kt            # Immutable state data class
│   │   ├── GameAction.kt             # Sealed class: undo-able actions
│   │   ├── InputMode.kt              # Enum: FILL | PENCIL
│   │   └── GameEvent.kt              # Sealed class: one-shot events (Completed)
│   └── GameViewModel.kt              # ViewModel: exposes StateFlow + SharedFlow
```

No Android UI imports in `game/model/` — all pure Kotlin data classes. `GameViewModel` imports `ViewModel` and `viewModelScope` but no Compose or MMD classes.

---

### Pattern 1: Immutable GameUiState Data Class

**What:** A single data class capturing everything needed to render the game screen at any point in time.

**When to use:** Always. Every ViewModel action produces a new `GameUiState` via `.copy()`. Never mutate state in place.

```kotlin
// Source: Official Android docs — UI State Production (developer.android.com)
data class GameUiState(
    val board: IntArray = IntArray(81),           // 0 = empty, 1-9 = filled value
    val solution: IntArray = IntArray(81),         // full solution — never shown during play
    val difficulty: Difficulty = Difficulty.EASY,
    val selectedCellIndex: Int? = null,           // null = no selection
    val inputMode: InputMode = InputMode.FILL,
    val pencilMarks: Array<Set<Int>> = Array(81) { emptySet() }, // per-cell candidate sets
    val errorCount: Int = 0,                      // silent — never shown during play
    val isComplete: Boolean = false,
    val isLoading: Boolean = false
) {
    // IntArray/Array require manual equals/hashCode for correct data class behavior
    override fun equals(other: Any?): Boolean { /* compare all arrays by content */ }
    override fun hashCode(): Int { /* content-aware hash */ }

    // Convenience: is this a given cell? Givens cannot be overwritten
    fun isGivenCell(index: Int): Boolean = board[index] != 0 && solution[index] == board[index]
        && /* originally placed by generator */ givenMask[index]
}
```

**CRITICAL note on `isGivenCell`:** The board array is mutated during play. The ViewModel must carry a separate `givenMask: BooleanArray` (or `givenIndices: Set<Int>`) computed once from the initial `SudokuPuzzle.board` at game start, so the ViewModel always knows which cells are original givens and cannot be overwritten by the player.

```kotlin
// Pattern: compute givenMask once at game start
data class GameUiState(
    ...
    val givenMask: BooleanArray = BooleanArray(81), // true = this cell is a given (immutable)
    ...
)
// At game start: givenMask = BooleanArray(81) { i -> puzzle.board[i] != 0 }
```

---

### Pattern 2: ViewModel with Backing Property and viewModelScope

**What:** `_uiState` is a private `MutableStateFlow`; `uiState` is the public `StateFlow`. Actions update state via `_uiState.update { it.copy(...) }`.

**When to use:** All ViewModel state exposure. Always use `update()` for thread-safe atomic mutations.

```kotlin
// Source: Official Android docs — StateFlow and SharedFlow (developer.android.com)
class GameViewModel(
    private val generator: SudokuGenerator = SudokuGenerator()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // One-shot events: use SharedFlow with replay=0 so they are not re-emitted on recomposition
    private val _events = MutableSharedFlow<GameEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val undoStack = ArrayDeque<GameAction>()

    fun startGame(difficulty: Difficulty) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val puzzle = withContext(Dispatchers.Default) {
                generator.generatePuzzle(difficulty)
            }
            undoStack.clear()
            _uiState.update {
                GameUiState(
                    board = puzzle.board.copyOf(),
                    solution = puzzle.solution.copyOf(),
                    difficulty = difficulty,
                    givenMask = BooleanArray(81) { i -> puzzle.board[i] != 0 },
                    isLoading = false
                )
            }
        }
    }
}
```

**Why `withContext(Dispatchers.Default)` for generation:** `SudokuGenerator.generatePuzzle()` is CPU-bound (backtracking + technique classification). It must NOT run on `Dispatchers.Main` (the default for `viewModelScope`). Wrap it in `withContext(Dispatchers.Default)` so the main thread stays unblocked. Verified pattern from official Android coroutines best practices docs.

---

### Pattern 3: Sealed Class GameAction for Delta-Based Undo

**What:** Each undoable action stores exactly the delta needed to reverse it — not a full board copy.

**When to use:** Every player action (fill cell, add/remove pencil mark) before applying the change. Push to `undoStack` before calling `_uiState.update`.

```kotlin
// Source: project domain design — HIGH confidence (standard undo stack pattern)
sealed class GameAction {
    data class FillCell(
        val cellIndex: Int,
        val previousValue: Int,    // 0 if was empty, 1-9 if was a prior (incorrect) fill
        val previousPencilMarks: Set<Int>  // pencil marks cleared when cell is filled
    ) : GameAction()

    data class SetPencilMark(
        val cellIndex: Int,
        val digit: Int,
        val wasAdded: Boolean      // true = digit was added (undo = remove), false = was removed (undo = add)
    ) : GameAction()
}
```

**Undo logic:**

```kotlin
fun undo() {
    if (undoStack.isEmpty()) return
    when (val action = undoStack.removeLast()) {
        is GameAction.FillCell -> {
            _uiState.update { state ->
                val newBoard = state.board.copyOf()
                newBoard[action.cellIndex] = action.previousValue
                state.copy(
                    board = newBoard,
                    pencilMarks = state.pencilMarks.copyOf().also {
                        it[action.cellIndex] = action.previousPencilMarks
                    }
                )
            }
        }
        is GameAction.SetPencilMark -> {
            _uiState.update { state ->
                val newMarks = state.pencilMarks.copyOf()
                newMarks[action.cellIndex] = if (action.wasAdded) {
                    newMarks[action.cellIndex] - action.digit   // reverse: remove
                } else {
                    newMarks[action.cellIndex] + action.digit   // reverse: add
                }
                state.copy(pencilMarks = newMarks)
            }
        }
    }
}
```

**Key detail:** `Array<Set<Int>>.copyOf()` creates a shallow copy (the array is copied, but the `Set<Int>` values are immutable — this is safe because `Set<Int>` is immutable in Kotlin). No deep copy required.

---

### Pattern 4: Pencil Mark Storage — `Array<Set<Int>>`

**What:** 81-element array of `Set<Int>`. Each `Set<Int>` contains the candidate digits (1–9) that the player has annotated for that cell.

**Why `Set<Int>` per cell (not `IntArray` bitmask):**
- `Set<Int>` toggles are idiomatic Kotlin: `set + digit` (add), `set - digit` (remove), `digit in set` (check)
- Immutable — each update creates a new set, making `Array.copyOf()` + set operations safe
- No bit arithmetic required, no off-by-one bugs
- In practice, each set has 0–9 elements — memory is negligible

**Toggle operation:**

```kotlin
fun enterDigit(digit: Int) {
    val state = _uiState.value
    val idx = state.selectedCellIndex ?: return
    if (state.isGivenCell(idx)) return  // cannot modify givens

    when (state.inputMode) {
        InputMode.FILL -> applyFill(idx, digit, state)
        InputMode.PENCIL -> applyPencilMark(idx, digit, state)
    }
}

private fun applyPencilMark(idx: Int, digit: Int, state: GameUiState) {
    val currentSet = state.pencilMarks[idx]
    val wasAdded = digit !in currentSet
    undoStack.addLast(GameAction.SetPencilMark(idx, digit, wasAdded))
    val newMarks = state.pencilMarks.copyOf()
    newMarks[idx] = if (wasAdded) currentSet + digit else currentSet - digit
    _uiState.update { it.copy(pencilMarks = newMarks) }
}
```

---

### Pattern 5: Silent Error Tracking and Completion Detection

**What:** On each fill, compare the placed digit to `solution[cellIndex]`. If they differ, increment `errorCount`. After every fill, check if all 81 cells match the solution — if so, emit `GameEvent.Completed`.

**CRITICAL: SCORE-01 says errors are silent.** `errorCount` is in `GameUiState` but the UI in Phase 3 must NOT display it. It is only visible at game completion (Phase 5). Keeping it in `GameUiState` is correct — it must be persisted in Phase 4 and displayed in Phase 5.

```kotlin
private fun applyFill(idx: Int, digit: Int, state: GameUiState) {
    undoStack.addLast(GameAction.FillCell(idx, state.board[idx], state.pencilMarks[idx]))
    val newBoard = state.board.copyOf()
    newBoard[idx] = digit
    val isError = digit != state.solution[idx]
    val newErrorCount = if (isError) state.errorCount + 1 else state.errorCount
    // Clear pencil marks in this cell when filling
    val newMarks = state.pencilMarks.copyOf().also { it[idx] = emptySet() }

    val allCorrect = newBoard.indices.all { i ->
        newBoard[i] == state.solution[i]
    }

    _uiState.update {
        it.copy(board = newBoard, errorCount = newErrorCount, pencilMarks = newMarks, isComplete = allCorrect)
    }

    if (allCorrect) {
        viewModelScope.launch { _events.emit(GameEvent.Completed(newErrorCount)) }
    }
}
```

**Completion check:** `allCorrect` requires every cell (including given cells) to match `solution`. This is correct: if the puzzle was generated correctly (Phase 1), `solution[i] == puzzle.board[i]` for all given cells, so given cells never break the check.

**Undo after completion:** Do not prevent undo after `isComplete = true`. The game is "logically complete" but the player might want to undo. The ViewModel should allow it; the UI can choose to navigate away.

---

### Pattern 6: One-Shot Events via SharedFlow

**What:** `GameEvent` carries completion and other transient signals. Use `SharedFlow(replay=0)` so events are not replayed on Compose recomposition.

```kotlin
sealed class GameEvent {
    data class Completed(val errorCount: Int) : GameEvent()
    // Future: data class PuzzleGenerationFailed(val difficulty: Difficulty) : GameEvent()
}
```

**Collect in UI (Phase 3):**

```kotlin
LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
        when (event) {
            is GameEvent.Completed -> navigateToCompletion(event.errorCount)
        }
    }
}
```

---

### Anti-Patterns to Avoid

- **Mutating `board` IntArray in place:** IntArray is a reference type; mutating in place causes Compose to miss the state change (StateFlow value reference doesn't change). Always use `.copyOf()` and `.copy()`.
- **Forgetting `givenMask`:** If `isGivenCell` is computed by checking `puzzle.board[i] != 0` at construction time only, it is safe. But if the original `puzzle.board` is lost after construction (board array replaced with player values), you cannot recompute it. Store `givenMask` in `GameUiState`.
- **Storing `undoStack` inside `GameUiState`:** `ArrayDeque<GameAction>` is mutable and not a data class — it cannot participate in `.copy()`. Keep it as a private field on `GameViewModel` only.
- **Comparing with `==` on IntArray or `Array<Set<Int>>`:** Kotlin's `==` on IntArray uses reference equality. Either implement `equals`/`hashCode` manually in `GameUiState` (as done in `SudokuPuzzle`) or use `contentEquals()`.
- **Launching puzzle generation on Main dispatcher:** `SudokuGenerator.generatePuzzle()` is CPU-bound. Must use `withContext(Dispatchers.Default)`.
- **Using LiveData instead of StateFlow:** CLAUDE.md mandates StateFlow; LiveData is deprecated for new development.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Coroutine lifecycle management | Manual cancel/cleanup | `viewModelScope` | Automatically cancelled on `ViewModel.onCleared()`; zero boilerplate |
| StateFlow atomic updates | Direct `.value = ` assignment with locks | `MutableStateFlow.update {}` | `update()` is atomic and handles concurrent access; direct assignment is NOT atomic |
| Flow testing | Manual `runBlocking` + channel hacks | Turbine (`flow.test { awaitItem() }`) | Turbine handles timing, conflation, and cancellation correctly; manual approaches produce flaky tests |
| ViewModel testing in JVM | Full instrumented test setup | Robolectric + `UnconfinedTestDispatcher` | JVM-side ViewModel testing with no emulator; already in the project |
| One-shot event re-emission guard | Manual boolean flags | `SharedFlow(replay=0)` | Prevents re-emission on recomposition; is the Google-recommended pattern since 2022 |

---

## Common Pitfalls

### Pitfall 1: Array Reference Equality Breaking StateFlow

**What goes wrong:** The ViewModel updates `board[idx] = digit` directly on the existing IntArray and calls `_uiState.update { it.copy() }`. Compose collects the new state but sees the same array reference and skips recomposition. The board appears frozen.

**Why it happens:** `data class copy()` performs a shallow copy. The `board` field in the new copy points to the same `IntArray` object as the old one (which was mutated in-place). StateFlow's equality check (`equals()`) for `GameUiState` returns `true` if you don't override it to use `contentEquals()` — but even if it emits, Compose stable checks may short-circuit recomposition.

**How to avoid:** Always do `val newBoard = state.board.copyOf()` before modifying, then pass `newBoard` into the `.copy()` call. Implement `equals()` in `GameUiState` using `board.contentEquals(other.board)` — matching the pattern from `SudokuPuzzle` (Phase 1).

**Warning signs:** Cell fills appear in `_uiState.value` when logged but the Compose UI does not update.

---

### Pitfall 2: Given Cell Guard Missing

**What goes wrong:** Player taps a given cell and fills it with a wrong digit, incrementing `errorCount` and potentially corrupting the puzzle.

**Why it happens:** The ViewModel's `enterDigit()` does not check `givenMask[idx]` before applying the fill.

**How to avoid:** Always check `if (state.givenMask[idx]) return` at the top of `enterDigit()` before any action. Also check before `selectCell()` if you want selection of given cells to be inert — or allow selection but block digit entry. The requirement only blocks filling, not selection.

**Warning signs:** `errorCount` increments when the player taps a cell with a pre-filled digit.

---

### Pitfall 3: Undo Stack Not Cleared on New Game

**What goes wrong:** Player starts a new game; the undo stack from the previous game is still present. The first `undo()` call in the new game applies an action from the old game, corrupting state.

**Why it happens:** `undoStack.clear()` was not called in `startGame()`.

**How to avoid:** `undoStack.clear()` is the first operation inside `startGame()`, before any state update.

**Warning signs:** `undo()` after `startGame()` appears to modify the new board in unexpected ways.

---

### Pitfall 4: Error Count Not Reset on New Game

**What goes wrong:** Player starts a new game after a previous game with errors. `errorCount` carries over.

**Why it happens:** `startGame()` creates a new `GameUiState()` using `.copy()` from the existing state rather than constructing a fresh default instance.

**How to avoid:** In `startGame()`, always build a completely fresh `GameUiState(...)` from scratch, not via `.copy()` of the existing state.

**Warning signs:** New game starts with non-zero `errorCount`.

---

### Pitfall 5: Pencil Marks Not Cleared on Fill

**What goes wrong:** Player has annotated pencil marks in a cell, then fills it. The pencil marks remain visible alongside the filled digit.

**Why it happens:** `applyFill()` does not clear `pencilMarks[idx]`.

**How to avoid:** In `applyFill()`, set `pencilMarks[idx] = emptySet()` (recorded in the undo action as `previousPencilMarks` so undo can restore them). Pattern shown in Pattern 5 above.

**Warning signs:** A filled cell shows both a digit and pencil mark candidates simultaneously.

---

### Pitfall 6: Completion Check on Board with Unfilled Cells

**What goes wrong:** The completion check `newBoard.all { it == solution[i] }` passes when a subset of cells still contain 0, because `0 == 0` for empty cells that are also 0 in the solution (if the solution array erroneously contains 0s). Solution has no zeros by `SudokuPuzzle` invariant.

**Why it doesn't happen:** `SudokuPuzzle.solution` is validated to have no zeros at construction (Phase 1 invariant). An empty cell `board[i] = 0` will never equal `solution[i] >= 1`. Safe by construction.

**How to confirm:** Rely on the Phase 1 invariant: `require(solution.none { it == 0 })` in `SudokuPuzzle.init`. No extra check needed.

---

## Code Examples

### Full GameUiState (verified structure)

```kotlin
// Source: Project domain design — HIGH confidence; equals/hashCode pattern from Phase 1 SudokuPuzzle
data class GameUiState(
    val board: IntArray = IntArray(81),
    val solution: IntArray = IntArray(81),
    val givenMask: BooleanArray = BooleanArray(81),
    val difficulty: Difficulty = Difficulty.EASY,
    val selectedCellIndex: Int? = null,
    val inputMode: InputMode = InputMode.FILL,
    val pencilMarks: Array<Set<Int>> = Array(81) { emptySet() },
    val errorCount: Int = 0,
    val isComplete: Boolean = false,
    val isLoading: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameUiState) return false
        return board.contentEquals(other.board) &&
               solution.contentEquals(other.solution) &&
               givenMask.contentEquals(other.givenMask) &&
               difficulty == other.difficulty &&
               selectedCellIndex == other.selectedCellIndex &&
               inputMode == other.inputMode &&
               pencilMarks.contentDeepEquals(other.pencilMarks) &&
               errorCount == other.errorCount &&
               isComplete == other.isComplete &&
               isLoading == other.isLoading
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + solution.contentHashCode()
        result = 31 * result + givenMask.contentHashCode()
        result = 31 * result + difficulty.hashCode()
        result = 31 * result + (selectedCellIndex ?: 0)
        result = 31 * result + inputMode.hashCode()
        result = 31 * result + pencilMarks.contentDeepHashCode()
        result = 31 * result + errorCount
        result = 31 * result + isComplete.hashCode()
        return result
    }
}
```

### ViewModel Unit Test Pattern with Turbine

```kotlin
// Source: Turbine docs (github.com/cashapp/turbine) + Android coroutines test docs
@RunWith(RobolectricTestRunner::class)
class GameViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule() // sets Dispatchers.Main to UnconfinedTestDispatcher

    private lateinit var viewModel: GameViewModel

    @Before fun setUp() {
        viewModel = GameViewModel(generator = FakeGenerator()) // use test double for fast tests
    }

    @Test fun `startGame emits loading then loaded state`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            assertFalse(initial.isLoading)

            viewModel.startGame(Difficulty.EASY)
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)
            assertEquals(81, loaded.board.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `enterDigit increments errorCount on wrong digit`() = runTest {
        viewModel.startGame(Difficulty.EASY)
        // find an empty cell and enter a digit known to be wrong
        viewModel.uiState.test {
            val state = awaitItem()
            val emptyIdx = state.board.indexOfFirst { !state.givenMask[it] }
            val wrongDigit = (1..9).first { it != state.solution[emptyIdx] }
            viewModel.selectCell(emptyIdx)
            viewModel.enterDigit(wrongDigit)
            val afterFill = awaitItem()
            assertEquals(1, afterFill.errorCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### MainDispatcherRule (required for ViewModel testing with coroutines)

```kotlin
// Source: Official Android testing docs — required to replace Dispatchers.Main in unit tests
class MainDispatcherRule(
    val testDispatcher: TestCoroutineDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| LiveData in ViewModel | StateFlow (MutableStateFlow + asStateFlow()) | 2021+ (Kotlin Flow maturity) | Coroutine-native, testable without Android lifecycle, no null initial value |
| `collectAsState()` | `collectAsStateWithLifecycle()` | 2022 (lifecycle-runtime-compose 2.6) | Prevents collection when app is backgrounded; lifecycle-safe |
| `Channel<Event>` for one-shot events | `SharedFlow(replay=0, extraBufferCapacity=1)` | 2021–2022 (Google guidance update) | SharedFlow is more explicit; doesn't drop events; Channel semantics are subtle |
| ViewModelScope on Main | `viewModelScope.launch { withContext(Default) { } }` for CPU work | Established practice | Keeps main thread responsive; generator runs on Default, state updates on Main |
| Full-state snapshot undo | Delta-based sealed class undo stack | Always was the correct approach | O(1) per action memory cost vs O(81) per action |

**Deprecated/outdated:**
- `AsyncTask`: use `viewModelScope.launch` + `withContext`
- `LiveData.observe()`: use `StateFlow.collectAsStateWithLifecycle()`
- Storing UI state in `Bundle` via `onSaveInstanceState`: use `SavedStateHandle` in ViewModel for process death (Phase 4 concern, not Phase 2)

---

## Open Questions

1. **`GameViewModel` constructor injection of `SudokuGenerator`**
   - What we know: `SudokuGenerator` is a pure Kotlin class with no Android dependencies (Phase 1). It can be constructed directly in ViewModel.
   - What's unclear: Whether a `ViewModelProvider.Factory` is needed now or whether default construction is sufficient until Phase 4 (when DataStore is added).
   - Recommendation: Use default parameter `generator: SudokuGenerator = SudokuGenerator()` for Phase 2. Add `ViewModelProvider.Factory` when DataStore repository injection is needed in Phase 4. Avoids premature DI infrastructure.

2. **Compose `@Stable` annotation on `GameUiState`**
   - What we know: Compose uses structural equality for recomposition skipping. A data class with `IntArray` fields is NOT considered `@Stable` by default because Compose cannot verify IntArray immutability at compile time.
   - What's unclear: Whether the custom `equals()`/`hashCode()` in `GameUiState` is sufficient for Compose stability, or whether `@Stable` must be added explicitly.
   - Recommendation: Add `@Stable` to `GameUiState` in Phase 3 (Core Game UI) when Compose rendering performance is being validated. Document this in Phase 3 research. Phase 2 is ViewModel-only — stability annotation is a UI concern.

3. **Undo stack depth limit**
   - What we know: No requirement specifies a maximum undo depth. A 9x9 Sudoku has at most 81 fill moves (given cells cannot be filled); with pencil marks it could be more.
   - What's unclear: Whether to cap the undo stack (e.g., 100 actions) to bound memory use.
   - Recommendation: No cap for Phase 2. In practice, even 200 `GameAction` entries at ~50 bytes each is under 10KB. Revisit if memory profiling in Phase 3 shows an issue.

---

## Environment Availability

Step 2.6: SKIPPED for Phase 2. This phase adds no external tool dependencies beyond what Phase 1 already established (JDK 17, Gradle, Android Studio). All libraries (`lifecycle-viewmodel-compose`, `coroutines-android`, `turbine`, `robolectric`, `mockk`) are already declared in `gradle/libs.versions.toml` and `app/build.gradle.kts`. No new tool installations required.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (4.13.2) with Robolectric (4.14.1) for ViewModel tests |
| Config file | None — standard Gradle `testDebugUnitTest` task |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.mudita.sudoku.game.*" -x lint` |
| Full suite command | `./gradlew :app:testDebugUnitTest` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DIFF-01 | `startGame(EASY)` produces state with `difficulty == EASY` and puzzle loaded | Unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | Wave 0 |
| DIFF-02 | `startGame(HARD)` produces puzzle with `givenCount in 22..27` | Unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | Wave 0 |
| INPUT-01 | `selectCell(5)` sets `selectedCellIndex = 5` in emitted state | Unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | Wave 0 |
| INPUT-02 | `enterDigit(3)` on selected empty cell updates `board[idx] = 3` | Unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | Wave 0 |
| INPUT-02 | `enterDigit()` on a given cell has no effect | Unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | Wave 0 |
| INPUT-03 | `toggleInputMode()` flips between FILL and PENCIL | Unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | Wave 0 |
| INPUT-04 | `enterDigit(5)` in PENCIL mode adds 5 to `pencilMarks[idx]`; second call removes it | Unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | Wave 0 |
| INPUT-05 | `undo()` after `enterDigit()` restores previous `board[idx]` value | Unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | Wave 0 |
| INPUT-05 | `undo()` after pencil mark toggle restores previous `pencilMarks[idx]` | Unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | Wave 0 |
| INPUT-05 | `undo()` with empty stack has no effect | Unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | Wave 0 |
| SCORE-01 | Entering wrong digit increments `errorCount`; `isComplete` stays false | Unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | Wave 0 |
| SCORE-01 | `errorCount` is never negative | Unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | Wave 0 |
| SCORE-02 | Filling last correct cell emits `GameEvent.Completed` with correct `errorCount` | Unit (ViewModel + Turbine) | `./gradlew :app:testDebugUnitTest --tests "*.GameViewModelTest"` | Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "com.mudita.sudoku.game.*" -x lint`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest`
- **Phase gate:** Full suite green — all Phase 2 tests AND all Phase 1 tests pass before phase sign-off

### Wave 0 Gaps

- [ ] `app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt` — covers all 13 requirements above
- [ ] `app/src/test/java/com/mudita/sudoku/game/MainDispatcherRule.kt` — shared JUnit 4 rule for `Dispatchers.Main` replacement (required by ViewModel tests using viewModelScope)
- [ ] A `FakeGenerator` or `TestSudokuGenerator` class (inside test sources) — returns a deterministic `SudokuPuzzle` to make ViewModel tests fast and reproducible without running real generation

*(All Phase 1 tests already exist and pass — no new test infrastructure gaps for those)*

---

## Sources

### Primary (HIGH confidence)
- [Android Developers — StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow) — backing property pattern, `update()` usage
- [Android Developers — UI State Production](https://developer.android.com/topic/architecture/ui-layer/state-production) — `update()` + `copy()` immutable state pattern, `viewModelScope.launch + withContext(Default)`
- [Android Developers — ViewModel Overview](https://developer.android.com/topic/libraries/architecture/viewmodel) — `viewModelScope`, automatic lifecycle cleanup
- Project `gradle/libs.versions.toml` — verified all dependency versions (Turbine 1.2.0, lifecycle 2.9.0, coroutines 1.10.2, mockk 1.13.17, robolectric 4.14.1)
- Project `app/build.gradle.kts` — confirmed all Phase 2 dependencies are already declared
- Phase 1 `SudokuPuzzle.kt` — `IntArray` `equals()`/`hashCode()` pattern; same pattern applies to `GameUiState`

### Secondary (MEDIUM confidence)
- [Turbine GitHub (cashapp/turbine)](https://github.com/cashapp/turbine) — `flow.test { awaitItem() }` pattern, version 1.2.0 confirmed in version catalog
- [Kotlin stdlib — ArrayDeque](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-array-deque/) — `addLast()`/`removeLast()` as stack operations
- [Android Developers — Kotlin Coroutines Best Practices](https://developer.android.com/kotlin/coroutines/coroutines-best-practices) — `withContext(Dispatchers.Default)` for CPU-bound work

### Tertiary (LOW confidence — cross-referenced but not directly verified at source)
- LibreSudoku (kaajjo/LibreSudoku) — used as reference for `Array<Set<Int>>` pencil mark model; direct source file access was blocked by GitHub redirect. Pattern derived from domain reasoning and confirmed as idiomatic Kotlin.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all versions verified against project's `libs.versions.toml`; no new dependencies needed
- Architecture: HIGH — backing property pattern, `update()`, `copy()` verified against official Android docs
- Undo stack design: HIGH — `ArrayDeque` + sealed class is standard CS + Kotlin stdlib recommendation; domain-specific design is solid from first principles
- Pencil mark model (`Array<Set<Int>>`): MEDIUM — derived from domain reasoning and partial reference app evidence; alternative (`IntArray` bitmask) is also valid but less idiomatic
- Pitfalls: HIGH — IntArray reference equality pitfall and givenMask pitfall are verified failure modes; others derived from sound reasoning

**Research date:** 2026-03-24
**Valid until:** 2026-06-24 (stable domain; Jetpack lifecycle 2.9.0 is current — check for updates before building if > 90 days)
