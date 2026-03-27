---
status: awaiting_human_verify
trigger: "After Save and Exit, main menu shows no Resume button. After Forfeit, also no Resume button."
created: 2026-03-26T00:00:00Z
updated: 2026-03-26T00:00:01Z
---

## Current Focus

hypothesis: CONFIRMED — saveNow() never sets _showResumeDialog = true, so hasSavedGame is always false after a fresh game is saved.
test: All unit tests pass after fix (26/26). Fix is a one-line addition to saveNow().
expecting: After fix, Resume button appears on menu after Save and Exit.
next_action: User to verify in app on device/emulator.

## Symptoms

expected: After "Save and Exit", main menu shows a Resume Game button because the game was saved to DataStore.
actual: Main menu shows no Resume button after either "Save and Exit" or "Forfeit". The game appears to be saving (no crash), but the menu never reflects saved state.
errors: None reported — silent failure.
reproduction: Start any game → press back → tap "Save and Exit" → observe main menu has no Resume button.
started: Introduced in Phase 09 (exit confirmation dialog). Prior to phase 09, back press auto-saved and Resume appeared correctly.

## Eliminated

(none yet)

## Evidence

- timestamp: 2026-03-26T00:00:00Z
  checked: GameViewModel._showResumeDialog state machine
  found: _showResumeDialog is set to TRUE only in init{} when repository.loadGame() returns non-null. It is set to FALSE in resumeGame(), startNewGame(), and quitGame(). It is NEVER set to true anywhere else.
  implication: After "Save and Exit", saveNow() writes to DataStore but _showResumeDialog stays at whatever value it currently holds. Since the game was started fresh (not loaded from a save), _showResumeDialog was false. It is NEVER updated to true after saveNow(). The menu reads hasSavedGame from _showResumeDialog, so the Resume button never appears.

- timestamp: 2026-03-26T00:00:00Z
  checked: MainActivity.hasSavedGame binding
  found: `val hasSavedGame by viewModel.showResumeDialog.collectAsStateWithLifecycle()` — this is bound to _showResumeDialog StateFlow.
  implication: The menu will only show Resume if _showResumeDialog emits true. Since saveNow() never updates _showResumeDialog, the button is absent.

- timestamp: 2026-03-26T00:00:00Z
  checked: GameViewModel.saveNow()
  found: saveNow() calls repository.saveGame(state) but does NOT update _showResumeDialog to true.
  implication: This is the root cause. saveNow() does the persistence correctly but does not update the in-memory flag that drives the Resume button.

- timestamp: 2026-03-26T00:00:00Z
  checked: Pre-phase-09 back press behavior (described in plan context)
  found: Phase 09 PLAN mentions "Prior to phase 09, back press auto-saved and Resume appeared correctly." This implies the old BackHandler called saveNow() AND something that made Resume appear. The old code likely set _showResumeDialog = true after saving, or the init{} re-ran (impossible — same ViewModel instance persists).
  implication: Most likely the old BackHandler set _showResumeDialog = true explicitly, or the fix is simply that saveNow() should set _showResumeDialog = true when it successfully saves.

## Resolution

root_cause: saveNow() persists the game state to DataStore but never sets _showResumeDialog to true. The Resume button visibility is driven exclusively by _showResumeDialog, which is only set to true in init{} when a previously saved game is loaded at startup. After a fresh game is saved via "Save and Exit" and the player returns to the menu, _showResumeDialog remains false — so hasSavedGame = false and the Resume button is absent.

fix: In GameViewModel.saveNow(), after repository.saveGame(state) succeeds, add _showResumeDialog.value = true. This makes the Resume button appear immediately upon returning to the menu after Save and Exit.

verification: All 26 unit tests pass (BUILD SUCCESSFUL). Fix is minimal — one line added to saveNow(). No existing test covers saveNow + showResumeDialog interaction, so no regression in test suite.
files_changed: [app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt]
