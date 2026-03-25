# Phase 6: Menu & Navigation - Context

**Gathered:** 2026-03-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Add the main menu as the app's entry point, a dedicated difficulty selection screen, and a coherent navigation graph connecting all five screens (Menu → Difficulty → Game → Summary → Leaderboard → Menu). Replace the Phase 4 `ResumeDialog` (currently inside `GameScreen`) with a menu-level "Resume" option. Wire back-press behavior so back mid-game auto-saves and returns to the menu.

This phase does NOT add new gameplay features, new scoring logic, or new screens beyond those listed above.

</domain>

<decisions>
## Implementation Decisions

### Main Menu Screen

- **D-01:** App title on the menu screen: **"Sudoku"** — minimal, no "Mudita" prefix needed in the UI.
- **D-02:** Menu buttons (top to bottom):
  1. **"New Game"** — navigates to the Difficulty screen.
  2. **"Resume"** — appears ONLY when a saved game exists (`viewModel.hasSavedGame()` returns true); absent when no save. Tapping it restores the saved game and navigates to the Game screen.
  3. **"Best Scores"** — navigates to the Leaderboard/Best Scores screen.
- **D-03:** Full-screen vertical layout — title at top, buttons stacked below with the same 56dp height constraint as all other screens (UI-03).

### Difficulty Selection Screen

- **D-04:** Separate full-screen composable — not a dialog or bottom sheet. Tapping "New Game" from the menu navigates to this screen.
- **D-05:** Three buttons: **"Easy"**, **"Medium"**, **"Hard"** — each 56dp tall, full width. Tapping one calls `viewModel.startGame(difficulty)` and navigates to the Game screen.
- **D-06:** A **"Back"** button at the bottom (or system back) navigates back to the main menu.
- **D-07:** Screen heading: **"Select Difficulty"**.

### Navigation Graph

- **D-08:** Screens and routes:
  - `MENU` — entry point on launch
  - `DIFFICULTY` — reached from MENU "New Game"
  - `GAME` — reached from DIFFICULTY (new game) OR MENU "Resume"
  - `SUMMARY` — reached from GAME on completion
  - `LEADERBOARD` — reached from SUMMARY "View Best Scores" OR MENU "Best Scores"

  All routes back to MENU — no routes that skip MENU to start a new game directly.

- **D-09:** Navigation implementation: extend the existing `Screen` enum with `MENU` and `DIFFICULTY` states rather than introducing Jetpack Navigation NavHost. The app has 5 screens, no deep-link requirements, and no fragment back-stack complexity — a `when(currentScreen)` switch in `MainActivity` keeps the code minimal and consistent with what Phases 3–5 established. **Claude's discretion** on the exact `remember`/`mutableStateOf` structure in `MainActivity`.

### Back-Press Behavior

- **D-10:** Back press mid-game: **auto-save and navigate to MENU, no confirmation dialog**. Consistent with the existing `onStop` auto-save (Phase 4 D-01/D-02). The game appears in the "Resume" slot on the menu immediately after.
- **D-11:** Back press at the MENU screen: **exits the app** (standard Android back behavior — `ComponentActivity` default, no override needed).
- **D-12:** Back press at DIFFICULTY, SUMMARY, or LEADERBOARD: **returns to MENU**.

### GameScreen Changes

- **D-13:** Remove `ResumeDialog` from `GameScreen` — the menu now handles the resume decision. `GameScreen` should no longer display any dialog on launch.
- **D-14:** Remove the `LaunchedEffect` auto-start in `GameScreen` (`viewModel.startGame(Difficulty.EASY)` guard) — difficulty selection now happens at the Difficulty screen, not inside `GameScreen`.
- **D-15:** `GameScreen` receives the `Difficulty` to start as a parameter (passed from `MainActivity` when navigating from DIFFICULTY). Alternatively, `GameViewModel` already knows the difficulty from `startGame()` — **Claude's discretion** on whether `GameScreen` needs it as a param or just calls `startGame` was already called by the time `GameScreen` composes.

### SummaryScreen Changes

- **D-16:** Replace the `onNewGame` callback with `onBackToMenu`. The "New Game" button becomes "Back to Menu" — difficulty selection happens at the menu.
- **D-17:** "View Best Scores" (currently `onViewLeaderboard`) callback name and behavior unchanged — navigates to LEADERBOARD.

### LeaderboardScreen Changes

- **D-18:** Replace the `onNewGame` callback with `onBackToMenu`. The "New Game" button becomes "Back to Menu".

### Claude's Discretion

- Exact `MainActivity` state structure (how `currentScreen`, `completionResult`, and `selectedDifficulty` are held and updated) — follow the same `remember { mutableStateOf(...) }` pattern already in place.
- Whether `DifficultyScreen` is a new file in `ui/game/` or `ui/menu/` — follow existing `ui/game/` convention for consistency unless the codebase grows.
- `MenuScreen` file location — `ui/game/MenuScreen.kt` following existing conventions.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Existing screens to modify
- `app/src/main/java/com/mudita/sudoku/MainActivity.kt` — Current Screen enum (GAME/SUMMARY/LEADERBOARD) + routing; add MENU + DIFFICULTY; wire back-press
- `app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt` — Remove ResumeDialog + auto-start LaunchedEffect; add back-press handler
- `app/src/main/java/com/mudita/sudoku/ui/game/SummaryScreen.kt` — Replace `onNewGame` with `onBackToMenu`; update button label to "Back to Menu"
- `app/src/main/java/com/mudita/sudoku/ui/game/LeaderboardScreen.kt` — Replace `onNewGame` with `onBackToMenu`; update button label to "Back to Menu"

### New screens to create
- `app/src/main/java/com/mudita/sudoku/ui/game/MenuScreen.kt` — Title "Sudoku", buttons: New Game / Resume (conditional) / Best Scores
- `app/src/main/java/com/mudita/sudoku/ui/game/DifficultyScreen.kt` — Heading "Select Difficulty", buttons: Easy / Medium / Hard / Back

### Domain models
- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` — `hasSavedGame()`, `startGame(difficulty)`, `resumeGame()`, `startNewGame()` — all needed by menu + difficulty routing
- `app/src/main/java/com/mudita/sudoku/puzzle/model/Difficulty.kt` — Difficulty enum used by DifficultyScreen

### Phase context from prior phases
- `.planning/phases/04-persistence/04-CONTEXT.md` D-03 — "Phase 6 replaces [ResumeDialog] with full navigation menu"
- `.planning/phases/05-scoring-completion/05-CONTEXT.md` D-05 — "Phase 6 replaces Screen enum with NavHost; leaf composables remain nav-unaware"

### Phase requirements
- `.planning/REQUIREMENTS.md` §Navigation & UI — NAV-01 (main menu, difficulty selection, resume option, leaderboard access)
- `.planning/ROADMAP.md` §Phase 6 — 4 success criteria: menu options, conditional resume, summary navigation, coherent back stack

### Project constraints
- `CLAUDE.md` §MMD Library — ThemeMMD, ButtonMMD, TextMMD only; no ripple; no animations; eInkColorScheme
- `CLAUDE.md` §SDK Levels — minSdk 31, targetSdk 31

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `Screen { GAME, SUMMARY, LEADERBOARD }` enum in `MainActivity.kt` — extend with MENU and DIFFICULTY
- `ResumeDialog` composable in `GameScreen.kt` — delete entirely (Phase 6 replaces with menu entry point)
- `SummaryScreen`, `LeaderboardScreen` — already nav-unaware leaf composables; only callback signatures change
- `GameViewModel.hasSavedGame()` — already exists; drives conditional Resume button visibility on menu
- `viewModel.startGame(difficulty)` / `resumeGame()` / `startNewGame()` — all exist, no ViewModel changes needed

### Established Patterns
- `BasicAlertDialog` + `ButtonMMD` + `TextMMD` pattern (from `ResumeDialog`) — reference for modal UI if ever needed, but no dialogs are introduced in this phase
- Full-screen Column with `windowInsetsPadding(WindowInsets.systemBars)` + `padding(horizontal = 16.dp)` — established in SummaryScreen and LeaderboardScreen; follow for MenuScreen and DifficultyScreen
- ButtonMMD full-width 56dp height — all interactive buttons follow this pattern

### Integration Points
- `MainActivity.kt` is the sole orchestrator of screen routing — all navigation state lives here
- `onStop()` already saves game state to DataStore — back-press mid-game triggers `saveNow()` via the same path if routed through `onStop`, or needs an explicit `viewModel.saveNow()` call in the back-press handler

</code_context>

<specifics>
## Specific Ideas

- "Sudoku" title chosen explicitly — no "Mudita" prefix in UI.
- Menu button order confirmed: New Game → Resume (conditional) → Best Scores.
- Resume is absent (not greyed out) when no save exists.
- Separate DifficultyScreen chosen over dialog for difficulty selection — consistent E-ink fullscreen approach.
- Auto-save-and-go on back mid-game (no confirmation) — consistent with existing auto-save-on-background.
- All routes go back to MENU; no screen provides a "New Game" shortcut that bypasses difficulty selection.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 06-menu-navigation*
*Context gathered: 2026-03-25*
