# Phase 5: Scoring & Completion - Context

**Gathered:** 2026-03-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Close the game loop: add hint logic (reveal one correct cell value, count uses), compute a final score from errors and hints, show a completion summary screen, store and compare personal bests per difficulty, and display a leaderboard screen.

This phase does NOT include: a full navigation graph, the main menu, difficulty selection UI, or back-stack management. Those belong to Phase 6.

</domain>

<decisions>
## Implementation Decisions

### Score Formula

- **D-01:** Final score = `max(0, 100 - (errorCount × 10) - (hintCount × 5))`. Score floors at 0 (never negative). Max possible score is 100 (perfect game: 0 errors, 0 hints).
  - Each error costs 10 points.
  - Each hint costs 5 points.
  - Example: 3 errors + 2 hints → 100 - 30 - 10 = 60.

### Hint Logic

- **D-02:** Hint button placed in `ControlsRow` as a fourth button: `[ Fill ] [ Pencil ] [ Undo ] [ Hint ]`. No additional row needed — extends the existing controls row.
- **D-03:** Hint reveals exactly one unfilled cell — pick any unfilled, non-correct cell at random and fill it with its solution value. Constraint from SCORE-03: cannot hint a cell that already contains the correct value.
- **D-04:** `hintCount` is a new field on `GameUiState` (Int, default 0). It must also be added to `PersistedGameState` (for save/resume continuity) and included in `GameEvent.Completed`.

### Completion & Leaderboard Screens

- **D-05:** A top-level `Screen` enum (`GAME`, `SUMMARY`, `LEADERBOARD`) drives which composable is active in `MainActivity`. `GameScreen` stays unchanged in structure — on completion it calls back with the `CompletionResult` (errorCount, hintCount, finalScore, difficulty, isPersonalBest).
  - `GAME → SUMMARY`: triggered by `GameEvent.Completed`.
  - `SUMMARY → LEADERBOARD`: "View Leaderboard" button on summary screen.
  - `LEADERBOARD → GAME`: "Back" / "New Game" button on leaderboard screen.
  - Phase 6 replaces this `Screen` enum with a proper NavHost — these screens are designed to be composable leaf nodes with no nav-awareness.
- **D-06:** Completion summary screen shows: difficulty label, error count, hints used, final score, and (if new personal best) a "New personal best!" notice. Two action buttons: "View Leaderboard" and "New Game".
- **D-07:** Leaderboard screen shows three rows — one per difficulty (Easy / Medium / Hard) — each displaying the stored best score (or "—" if no score recorded yet). One action button: "New Game" (returns to GAME screen and starts a new Easy game, same as Phase 4 behavior for now).

### High Score Storage

- **D-08:** High scores stored in DataStore using the keys already specified in CLAUDE.md: `high_score_easy`, `high_score_medium`, `high_score_hard` (each an `Int`). Single best score per difficulty — the leaderboard has exactly 3 rows.
- **D-09:** A separate `ScoreRepository` interface handles high score read/write, distinct from `GameRepository` (which handles in-progress game state). `DataStoreScoreRepository` implements it. Both are injected into `GameViewModel` for testability.
- **D-10:** After completion: read the current best for the completed difficulty → compare → if new score is higher, write it. `isPersonalBest` flag is derived at this point and passed to `SummaryScreen` via `CompletionResult`.

### Claude's Discretion

- Exact random cell selection strategy for hints (random among all invalid unfilled cells is fine — no need to prefer any particular cell).
- `CompletionResult` data class shape — Claude designs whatever is cleanest to carry `(difficulty, errorCount, hintCount, finalScore, isPersonalBest)` from ViewModel to UI.
- Whether `ScoreRepository` is injected as a constructor param (like `GameRepository`) or passed differently — follow the existing injectable-lambda pattern for testability.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Domain Models (integration points)
- `app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt` — Add `hintCount: Int = 0` field; update equals/hashCode
- `app/src/main/java/com/mudita/sudoku/game/model/PersistedGameState.kt` — Add `hintCount: Int = 0` field; update toPersistedState() / toUiState() converters
- `app/src/main/java/com/mudita/sudoku/game/model/GameEvent.kt` — `GameEvent.Completed` must include `hintCount` (currently only `errorCount`)
- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` — Add `requestHint()` action; update completion emission to include hintCount; inject ScoreRepository
- `app/src/main/java/com/mudita/sudoku/game/GameRepository.kt` — Reference for the interface pattern to follow when designing ScoreRepository
- `app/src/main/java/com/mudita/sudoku/MainActivity.kt` — Add Screen enum + when-based routing; wire GameEvent.Completed → SUMMARY transition

### Existing UI (extension points)
- `app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt` — Extend with Hint button (4th button in row)
- `app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt` — Phase 5 TODO comment is here; wire completion event → callback

### Persistence Stack
- `CLAUDE.md` §Local Persistence — DataStore keys `high_score_easy`, `high_score_medium`, `high_score_hard` (Int per difficulty); use same kotlinx.serialization + DataStore pattern as GameRepository
- `CLAUDE.md` §Technology Stack — DataStore 1.2.1, kotlinx.serialization 1.8.0

### Phase Requirements
- `.planning/REQUIREMENTS.md` §Scoring & Completion — SCORE-03, SCORE-04, SCORE-05, SCORE-06
- `.planning/REQUIREMENTS.md` §High Scores — HS-01, HS-02, HS-03
- `.planning/ROADMAP.md` §Phase 5 — 5 success criteria to satisfy

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `GameViewModel`: Injectable constructor pattern (generatePuzzle lambda, repository) — follow same pattern for ScoreRepository injection.
- `GameRepository` / `DataStoreGameRepository`: Reference implementation for how a DataStore-backed repository is structured. ScoreRepository will be simpler (no JSON serialization — just raw Int preferences).
- `ControlsRow`: Currently `Fill | Pencil | Undo` as ButtonMMD buttons in a Row. Extend with a fourth `Hint` ButtonMMD.
- `GameEvent.Completed`: Already emitted in `applyFill()` when `allCorrect == true`. Extend payload — add `hintCount`.

### State Fields Needing Addition
- `GameUiState.hintCount: Int` — not present; add with default 0.
- `PersistedGameState.hintCount: Int` — not present; add with default 0 for backward-compatible deserialization.

### Navigation Hook
- `GameScreen.kt` has a `LaunchedEffect` collecting `viewModel.events` with a `// TODO Phase 5` comment. This is where `GameEvent.Completed` triggers the `GAME → SUMMARY` transition.

### Established Patterns
- MVVM + StateFlow: all new state (hintCount, completion result) via StateFlow/SharedFlow.
- Injectable constructor params for testability — all repositories.
- `@Stable` data classes with manual equals/hashCode for array fields.
- `BasicAlertDialog` + `ButtonMMD` + `TextMMD` pattern established in `ResumeDialog` — follow for any modal UI.

</code_context>

<specifics>
## Specific Ideas

- Score formula explicitly chosen: 100 − (errors×10) − (hints×5), floor 0. Non-negotiable — do not deviate.
- Hint button goes in ControlsRow as 4th button — not a separate row.
- Screen enum approach chosen over dialogs — summary and leaderboard are proper full screens, not overlays. Phase 6 replaces the enum with NavHost; screens themselves don't change.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 05-scoring-completion*
*Context gathered: 2026-03-24*
