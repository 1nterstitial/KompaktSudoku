# Phase 9: Game Navigation - Context

**Gathered:** 2026-03-26
**Status:** Ready for planning

<domain>
## Phase Boundary

Add a back-press confirmation overlay to `GameScreen.kt` so players can choose between saving their progress ("Save and Exit") and discarding it ("Forfeit") instead of the current behavior of immediately saving and returning to menu.

Changes are self-contained to:
- `GameScreen.kt` — replace the direct BackHandler with a dialog-gated one; add the overlay composable
- `GameViewModel.kt` — add `quitGame()` function that clears DataStore save and resets in-memory state
- `GameViewModelTest.kt` — tests for `quitGame()`

No new screens. No changes to MainActivity routing. No changes to any other file.

</domain>

<decisions>
## Implementation Decisions

### Dialog State Ownership
- **D-01:** The exit confirmation dialog visibility is tracked as **local composable state** in `GameScreen.kt` — `var showExitDialog by remember { mutableStateOf(false) }`. This is a pure UI concern; no ViewModel field added. `GameUiState` is NOT modified.

### Dialog Visual Design
- **D-02:** The confirmation overlay is a **centered Box with 1dp black border and RectangleShape** layered over the game content using a `Box(Modifier.fillMaxSize())` wrapper. A semi-opaque scrim (black at ~40% alpha) sits behind the dialog box.
- **D-03:** The dialog contains a `TextMMD` message and two stacked `ButtonMMD` buttons. No ripple, no RoundedCornerShape, no animations (UI-02).
- **D-04:** Dialog copy:
  - Message: **"Leave game?"**
  - First button: **"Save and Exit"** — saves game state and navigates to menu (Resume button will appear)
  - Second button: **"Forfeit"** — clears saved state and navigates to menu (no Resume button)

### BackHandler Guard
- **D-05:** `BackHandler` is **only enabled when `!uiState.isLoading`**. During puzzle generation, the system default back behavior applies (exits the app from the game screen, same as on MenuScreen). This avoids saving an empty or partial board state.
- **D-06:** When `showExitDialog` is true and Back is pressed again, the dialog is dismissed (sets `showExitDialog = false`). The `BackHandler` handles both: first press = show dialog, second press (while dialog visible) = dismiss dialog.

### Save and Exit Path
- **D-07:** "Save and Exit" calls `viewModel.saveNow()` (suspend) then `onBackToMenu()`. The save happens before navigation — `showResumeDialog` will be `true` when the menu renders, so the Resume button appears immediately. This reuses the existing `saveNow()` mechanism.

### Forfeit / Quit Game Path
- **D-08:** Add a new `quitGame()` suspend function to `GameViewModel`. It:
  1. Calls `withContext(ioDispatcher) { repository.clearGame() }` — removes DataStore save
  2. Resets `_uiState.value = GameUiState()` — clears in-memory board state
  3. Sets `_showResumeDialog.value = false` — ensures no Resume button appears on menu
  4. Clears `undoStack`
- **D-09:** "Forfeit" calls `viewModel.quitGame()` (suspend) then `onBackToMenu()`.

### Claude's Discretion
- Exact scrim alpha value (40% is the target; adjust if E-ink waveform makes it ghosty)
- Padding and sizing of the centered dialog box
- Whether to add a Compose UI test for the dialog dismissal flow in `GameScreenTest.kt`

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements
- `.planning/REQUIREMENTS.md` — NAV-01, NAV-02, NAV-03 definitions

### Roadmap
- `.planning/ROADMAP.md` — Phase 9 goal and success criteria

### Files being modified
- `app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt` — replace direct BackHandler with dialog-gated version; add exit dialog composable
- `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` — add `quitGame()` suspend function

### Files for context (read before modifying)
- `app/src/main/java/com/mudita/sudoku/MainActivity.kt` — routing; `onBackToMenu` wires to `currentScreen = Screen.MENU`; `onStop()` already saves for force-close recovery
- `app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt` — NOT modified; dialog state stays in composable
- `app/src/main/java/com/mudita/sudoku/game/model/GameViewModel.kt` — existing `saveNow()`, `clearGame()` patterns to mirror in `quitGame()`

### Test files
- `app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt` — unit tests for `quitGame()`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `BackHandler` — already imported (`androidx.activity.compose.BackHandler`) and wired in `GameScreen.kt`; just needs a state gate added
- `saveNow()` — suspend fun with existing guards (`isLoading`, `isComplete`, empty board); "Save and Exit" reuses it directly
- `repository.clearGame()` — already used in `startNewGame()`; `quitGame()` will use the same call

### Established Patterns
- `Box + clickable(indication=null)` for interactive elements without ripple (UI-02)
- `RectangleShape` only — no `RoundedCornerShape` (would produce anti-aliased gray pixels on E-ink)
- `coroutineScope.launch { viewModel.saveNow(); onBackToMenu() }` — the existing `BackHandler` body is the template for both dialog actions
- `ButtonMMD` for all buttons; `TextMMD` for all text
- Nav-unaware composables: `GameScreen` receives `onBackToMenu` callback, does not know about `Screen` enum

### Integration Points
- `BackHandler { ... }` at line 51 in `GameScreen.kt` — replace body with `showExitDialog = true`
- `onBackToMenu` callback in `GameScreen` — called after both "Save and Exit" and "Forfeit"
- `_showResumeDialog` StateFlow in `GameViewModel` — `quitGame()` must set this to `false`
- `onStop()` in `MainActivity` already calls `viewModel.saveNow()` — this covers NAV-03 (force-close recovery) with no additional changes needed

</code_context>

<specifics>
## Specific Ideas

- Dialog copy confirmed: "Leave game?" / "Save and Exit" / "Forfeit"
- The existing `onStop()` save in MainActivity handles NAV-03 (unexpected app kill) — no additional persistence logic needed for that requirement
- "Save and Exit" and "Forfeit" both call `onBackToMenu()` after their respective async operations complete
- The BackHandler `enabled` parameter should be `!uiState.isLoading` (not active during puzzle generation)
- Second Back press while dialog is open should dismiss the dialog (standard Android pattern)

</specifics>

<deferred>
## Deferred Ideas

- None — discussion stayed within phase scope

</deferred>

---

*Phase: 09-game-navigation*
*Context gathered: 2026-03-26*
