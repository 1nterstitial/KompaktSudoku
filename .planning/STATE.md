---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: Bug Fixes and Improvements
status: executing
stopped_at: Completed 08-01-PLAN.md — Controls and Number Pad Visual Fixes
last_updated: "2026-03-27T01:36:00Z"
last_activity: 2026-03-27 -- Phase 08 plan 01 complete
progress:
  total_phases: 3
  completed_phases: 1
  total_plans: 2
  completed_plans: 1
---

# Project State: Mudita Kompakt Sudoku

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-25)

**Core Value:** A fully playable Sudoku experience that feels native on the Mudita Kompakt's E-ink display — responsive touch input, high-contrast grid, and smooth puzzle flow without display artifacts.
**Current Focus:** Phase 08 — controls-number-pad-fixes

---

## Current Position

Phase: 08 (controls-number-pad-fixes) — COMPLETE
Plan: 1 of 1
Status: Phase 08 complete — all CTRL requirements implemented
Last activity: 2026-03-27 -- Phase 08 plan 01 executed and committed

## Phase Sequence

| Phase | Name | Milestone | Status |
|-------|------|-----------|--------|
| 1 | Puzzle Engine | v1.0 | Complete |
| 2 | Game State & Domain | v1.0 | Complete |
| 3 | Core Game UI | v1.0 | Complete |
| 4 | Persistence | v1.0 | Complete |
| 5 | Scoring & Completion | v1.0 | Complete |
| 6 | Menu & Navigation | v1.0 | Complete |
| 7 | Grid Rendering Fixes | v1.1 | Not started |
| 8 | Controls & Number Pad Fixes | v1.1 | Complete |
| 9 | Game Navigation | v1.1 | Not started |

---

## Performance Metrics

**Plans completed:** 0 (v1.1)
**Plans total:** TBD (phases not yet planned)
**Phases completed:** 0/3 (v1.1)
**Requirements mapped:** 9/9

| Phase | Plan | Duration | Tasks | Files | Completed |
|-------|------|----------|-------|-------|-----------|
| — | — | — | — | — | — |
| Phase 07-grid-rendering-fixes P01 | 7 | 2 tasks | 3 files |
| 08 | 01 | 11min | 2 | 2 | 2026-03-27 |

## Accumulated Context

### Key Decisions

- Error-based scoring (not time-based) — aligns with mindful, low-pressure design ethos
- Silent error tracking (not real-time highlights) — errors revealed at completion only
- Hint penalty instead of disqualification — keeps game flowing
- Pause/resume via DataStore — E-ink users may put down device for hours; state must survive
- [01-01] Project-local Difficulty enum separate from Sudoklify enum — decouples domain contracts from library internals
- [01-01] SudokuPuzzle carries solution alongside board — prevents solution loss during domain handoffs
- [01-01] givenCount computed from board at construction — avoids caller-side counting errors; validated against 17-cell minimum
- [01-02] isValidPlacement as top-level function — eliminates class instantiation inside backtracking loop; importable by UniquenessVerifier without wrapping
- [01-02] UniquenessVerifier and hasUniqueSolution declared open — required for Plan 04 test double subclassing (PuzzleGenerationException test)
- [01-02] kotlin { compilerOptions } DSL — required by Kotlin 2.3.20 K2; kotlinOptions jvmTarget String is an error in Kotlin 2.x
- [01-03] Exact-tier matching for meetsRequirements — easy puzzle fails Hard requirements (too easy for challenge)
- [01-03] No naked-pairs pass in DifficultyClassifier — pairs pass without mutable candidate-set would loop infinitely; two-tier design is termination-safe and sufficient for PUZZ-02
- [01-04] Sudoklify MEDIUM produces 0% HIDDEN_PAIRS tier empirically — MEDIUM_CONFIG updated to NAKED_SINGLES_ONLY; MEDIUM differs from EASY by given-count range only (27-35 vs 36-45)
- [01-04] toSeed() requires strictly positive Long — use Random.nextLong(1L, Long.MAX_VALUE); zero/negative seeds throw InvalidSeedException
- [01-04] Sudoklify 1.0.0-beta04 actual API: components.toSeed, presets.loadPresetSchemas(), SudoklifyArchitect factory lambda, constructSudoku DSL builder
- [02-01] GameUiState stores no undoStack — mutable undo history belongs in GameViewModel; keeps state immutable and recomposition-safe
- [02-01] FakeGenerator is a standalone class (not SudokuGenerator subclass) — avoids modifying Phase 1; GameViewModel will accept a generator lambda in Plan 02
- [02-01] Array<Set<Int>> pencilMarks requires contentDeepEquals/contentDeepHashCode — standard data class equals is reference-based for arrays
- [02-02] GameViewModel constructor accepts suspend lambda for puzzle generation — allows FakeGenerator injection without subclassing SudokuGenerator
- [02-02] applyPencilMark stubbed as no-op — pencil mark logic deferred to Plan 03 per plan spec; stub keeps Plan 02 scope clean
- [02-03] applyPencilMark wasAdded flag encodes toggle direction at write time — undo reverses without re-reading set state
- [02-03] errorCount never decremented on undo — permanent silent tracking counts mistakes made, not current board state (SCORE-01)
- [03-01] MMD declared as compileOnly due to inaccessible repos — GitHub Packages needs auth token, JFrog instance deactivated; switch to implementation once credentials configured
- [03-01] eraseCell() reuses FillCell undo action — pushes previous value/marks before clearing, enabling undo via existing infrastructure
- [03-01] GameScreen stub created in Plan 01 — allows MainActivity compilation before Plan 02 wires the real UI
- [03-02] ControlsRow mode toggles use Box+clickable (indication=null) instead of ButtonMMD with colors param — ButtonMMD colors API unverifiable without MMD AAR; Box approach is guaranteed safe
- [03-02] Grid lines drawn last in Canvas to prevent thick box borders being partially overwritten by adjacent cell fills
- [03-03] MMD stubs placed in main source set (com.mudita.mmd) — only way to compile production files without AAR; replaces compileOnly(libs.mmd) for offline dev
- [03-03] Canvas needs Modifier.fillMaxSize() for pointer-input hit testing — Spacer-based Canvas is 0x0 without explicit size modifier; this was a production bug
- [05-02] Injectable Random via constructor (Random.Default in production, Random(seed) in tests) — deterministic hint cell selection for tests without seam
- [05-02] Hints permanently non-undoable — not pushed to undoStack; reverting hint while hintCount stays elevated creates inconsistent state (locked product decision)
- [05-02] handleCompletion() ordering: saveBestScore -> refreshLeaderboard -> clearGame -> emit event — leaderboard must reflect new score before game is cleared
- [03-03] Square Canvas tap coords use min(width, height) matching BoxWithConstraints minOf(maxWidth, maxHeight) logic
- [03-03] activity-compose declared explicitly — was previously provided only transitively by MMD; must be explicit dependency
- [04-02] ioDispatcher injected into GameViewModel constructor (Dispatchers.IO default) — allows UnconfinedTestDispatcher in tests to make withContext(ioDispatcher) synchronous, avoiding advanceUntilIdle() races with real thread pools
- [04-02] Tests that need loaded game state use Turbine awaitItem() sequences rather than advanceUntilIdle() — startGame dispatches to Dispatchers.Default (real thread), which test schedulers cannot control
- [04-02] saveNow() is a suspend fun (not fire-and-forget) — caller controls timing; critical for lifecycle-aware save-on-pause in Plan 03
- [04-03] ButtonMMD import is com.mudita.mmd.components.buttons.ButtonMMD (plural) — confirmed from actual AAR class inspection; plan comment used wrong singular path
- [04-03] ResumeDialog placed before isLoading check in GameScreen — dialog must appear immediately at launch before startGame sets isLoading=true
- [05-01] calculateScore as top-level function in ScoreCalculation.kt — independently testable and importable by GameViewModel (mirrors isValidPlacement pattern)
- [05-01] scoreDataStore separate from gameDataStore — one DataStore file per instance; mixing causes data corruption
- [05-01] hintCount default = 0 in PersistedGameState — backward-compatible deserialization of Phase 4 JSON saved games
- [05-01] GameEvent.Completed extended to full payload (hintCount, score, difficulty, isPersonalBest) — SummaryScreen gets all data without secondary state query
- [05-01] GameViewModel emits isPersonalBest=false placeholder — Plan 02 wires ScoreRepository to compute correctly
- [05-03] Screen enum routing in MainActivity — top-level GAME/SUMMARY/LEADERBOARD enum; Phase 6 replaces with NavHost; leaf composables remain nav-unaware
- [05-03] completionResult set BEFORE currentScreen in onCompleted callback — prevents null CompletionResult on SummaryScreen first recomposition (Pitfall 2 ordering)
- [05-03] LeaderboardScreen heading is "Best Scores" not "Leaderboard" — reflects single-best-score-per-difficulty design (D-08); "Leaderboard" only in code identifiers
- [06-01] Screen enum retained (not NavHost) — enum-based routing sufficient for 5 screens with no deep linking (D-09)
- [06-01] showResumeDialog StateFlow now collected in MainActivity to drive MenuScreen hasSavedGame — Resume button moved from GameScreen dialog to main menu
- [06-01] startGame(difficulty) called before navigating to GAME screen to pre-start puzzle generation (Pitfall 5 avoidance)
- [06-01] MenuScreen has no BackHandler — ComponentActivity back exits app by default (D-11 intentional)
- [06-02] assertDoesNotExist() used for conditionally absent Resume button — node is not in composition at all when hasSavedGame=false (if block removes it); assertIsNotDisplayed() would fail
- [07-01] drawPencilMarks builds TextStyle internally from isSelected+density (D-08) — removes style param, encapsulates all pencil style logic in the function
- [07-01] Dynamic pencil font: (cellSize/2 * 0.60f) / density sp — fills ~60% of each 2x2 slot, device-density-aware; reduce to 0.55f if marks clip on device
- [07-01] 4-mark cap guard placed before undoStack.addLast — blocked adds leave undo history clean (D-05)
- [07-01] marks.sorted().forEachIndexed for 2x2 layout — slot index (0-3) maps to positions via i/2 (row) and i%2 (col); sorted ascending (D-06)
- [08-01] sizeIn(minHeight=56.dp) on inner Boxes not fillMaxHeight() — Row without bounded max height causes infinite constraint; fillMaxHeight() in unbounded Row pushes subsequent composables off screen (Robolectric test revealed)
- [08-01] RectangleShape import is androidx.compose.ui.graphics.RectangleShape not foundation.shape — confirmed via compile error
- [08-01] TextMMD fontFamily parameter works in MMD 1.0.1 — confirmed via javap decompilation of AAR classes.jar; BasicText fallback not needed for CTRL-01

### Architecture Decisions

- MVVM + StateFlow + Compose — official Google-recommended pattern
- Single-module layered architecture — appropriate for solo developer scope
- Puzzle engine as pure Kotlin (no Android deps) — independently testable with plain JUnit
- DataStore Preferences + kotlinx.serialization JSON — async, coroutine-native, no ANR risk

### Research Flags (carry into planning)

- **Phase 7 (GRID):** Dynamic pencil font multiplier `0.60f` implemented; 2x2 layout with 4-mark cap — tune to 0.55f on device if marks clip. Implemented in 07-01.
- **Phase 8 (CTRL):** RESOLVED — TextMMD `fontFamily` parameter confirmed working in MMD 1.0.1 (verified via javap decompilation of AAR classes.jar). BasicText fallback not needed.
- **Phase 8 (CTRL):** RESOLVED — 1dp border used for Fill/Pencil frame; verify on device if gray rendering occurs at fast waveform.
- **Phase 9 (NAV):** Back-press handling requires `BackHandler` composable in GameScreen — confirm interaction with existing dialog dismissal flow.

### Known Risks

- TextMMD closed-source AAR may silently ignore `fontSize`/`lineHeight` overrides — fallback path is `BasicText` inside `Box + clickable(indication=null)` (already established pattern in ControlsRow.kt)
- Diagonal hatching finer than 6px will produce E-ink ghosting artifacts — use solid borders only (no hatching)
- `RoundedCornerShape` on borders produces anti-aliased gray pixels on E-ink — use `RectangleShape` exclusively

### Blockers

(None)

### Todos

(None)

---

## Session Continuity

**Last session:** 2026-03-27T01:36:00Z
**Next action:** Phase 08 complete — proceed to Phase 09 (Game Navigation)
**Stopped at:** Completed 08-01-PLAN.md — Controls and Number Pad Visual Fixes

---

*State initialized: 2026-03-23*
*Last updated: 2026-03-25 — v1.1 roadmap defined (3 phases, 9 requirements)*
