# Phase 4: Persistence - Context

**Gathered:** 2026-03-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Make a paused game survive app closure and device sleep. On app launch, if a saved game exists, present a dialog prompting the player to resume or start a new game. All grid state, pencil marks, error count, hint count (always 0 in this phase), solution, difficulty, and given mask are persisted to and restored from DataStore.

This phase does NOT include: hint logic, score computation, high score storage, a full navigation graph, difficulty selection UI, or completion summary. Those belong in Phases 5 and 6.

</domain>

<decisions>
## Implementation Decisions

### Pause Trigger

- **D-01:** Game state is saved **automatically when the app goes to background** (via `Activity.onStop()` or `ProcessLifecycleOwner`). No explicit pause button needed in Phase 4. Player never has to think about saving — it just works.
- **D-02:** Save on **backgrounding only** — no per-move DataStore writes. Avoids performance overhead on the Helio A22 CPU.

### Resume Entry Point

- **D-03:** On app launch, if a saved game exists, a **modal dialog appears on GameScreen**: "Resume last game?" with two options — **Resume** and **New Game**. Phase 6 replaces this flow with the full navigation menu.
- **D-04:** If the player chooses "New Game" from the resume dialog, a new **Easy** game is started automatically. No difficulty picker in Phase 4 — that belongs in Phase 6's menu screen.

### Undo Stack on Resume

- **D-05:** Undo history is **NOT persisted** across pause/resume. On resume, the undo stack starts empty. Player can undo moves made after resuming, but not moves from before the pause. This avoids serializing `GameAction` types and keeps the serialized state minimal.

### Claude's Discretion

- Repository layer design (how `GameRepository` is structured, how it's injected into `GameViewModel`) — Claude decides the cleanest approach consistent with MVVM + StateFlow.
- Serialization data class shape (whether a separate `PersistedGameState` is introduced or `GameUiState` is annotated directly) — Claude decides based on what keeps the domain model clean.
- Lifecycle observer placement (Activity `onStop` vs `ProcessLifecycleOwner`) — Claude decides based on what works reliably on AOSP 12.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Domain Models (what gets serialized)
- `app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt` — Source of truth for game state fields: board, solution, givenMask, difficulty, selectedCellIndex, inputMode, pencilMarks, errorCount, isComplete, isLoading
- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` — Where save/load logic will integrate; `undoStack` lives here (ArrayDeque, not persisted)
- `app/src/main/java/com/mudita/sudoku/game/model/GameAction.kt` — Undo action types (NOT persisted per D-05)
- `app/src/main/java/com/mudita/sudoku/puzzle/model/Difficulty.kt` — Difficulty enum (must be serializable)

### Persistence Stack
- `CLAUDE.md` §Local Persistence — DataStore Preferences + kotlinx.serialization JSON; `in_progress_game` key; `@Serializable` on domain models; `Json.encodeToString` / `Json.decodeFromString`
- `CLAUDE.md` §Technology Stack — DataStore 1.2.1, kotlinx.serialization 1.8.0, Coroutines 1.10.2

### Phase Requirements
- `.planning/REQUIREMENTS.md` §Game State — STATE-01, STATE-02, STATE-03 are the three requirements this phase must satisfy
- `.planning/ROADMAP.md` §Phase 4 — Success criteria: (1) pausing + force-close + reopen presents resume prompt with exact state, (2) resume + more moves + re-pause does not corrupt state, (3) new game correctly discards prior saved state

### Project Constraints
- `CLAUDE.md` §Architecture — MVVM + StateFlow; no ANR risk (DataStore async, coroutine-native)
- `CLAUDE.md` §SDK Levels — minSdk 31 (AOSP 12)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `GameViewModel`: Already has `viewModelScope` and `Dispatchers.Default` pattern. Will need a `GameRepository` parameter injected for save/load, following the same injectable-lambda pattern used for `generatePuzzle`.
- `GameUiState`: All fields needed for persistence are present. Arrays (board, solution, givenMask, pencilMarks) require custom serializers or conversion to List for `@Serializable` compatibility.
- `kotlinx-serialization-json` and `androidx-datastore-preferences` are already declared as `implementation` dependencies in `app/build.gradle.kts` — no new dependencies needed.

### Established Patterns
- Injectable constructor parameters for testability (see `generatePuzzle` lambda in `GameViewModel`).
- `StateFlow` + `collectAsStateWithLifecycle` for UI state — lifecycle-aware collection prevents background DataStore reads leaking.
- `@Stable` data classes — any new serialization model should be `@Stable` if exposed to Compose.

### Integration Points
- `MainActivity.kt` — will observe lifecycle events (onStop) to trigger save. Also the entrypoint where the resume dialog must appear on launch.
- `GameViewModel.startGame(difficulty)` — called when "New Game" is chosen; must also clear persisted state.

</code_context>

<specifics>
## Specific Ideas

- Auto-save on backgrounding (not per-move) was explicitly chosen for simplicity and performance.
- Resume dialog is a modal over GameScreen — not a separate screen, not an inline banner.
- "New Game" from resume dialog defaults to Easy difficulty in Phase 4; difficulty picker comes in Phase 6.
- Undo stack intentionally not persisted — minimal serialization, no `GameAction` types in DataStore.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 04-persistence*
*Context gathered: 2026-03-24*
