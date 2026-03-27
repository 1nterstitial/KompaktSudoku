---
phase: 09-game-navigation
plan: "01"
subsystem: navigation
tags: [game-screen, back-handler, dialog, quit, forfeit, save-exit, nav]
dependency_graph:
  requires:
    - GameViewModel.saveNow() (phase 04)
    - GameRepository.clearGame() (phase 04)
    - GameScreen BackHandler (phase 06)
  provides:
    - GameViewModel.quitGame() suspend function
    - ExitConfirmationDialog composable
    - Dialog-gated BackHandler in GameScreen
  affects:
    - app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt
    - app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt
tech_stack:
  added: []
  patterns:
    - BackHandler with enabled parameter for loading guard
    - Box overlay pattern for dialog over game content
    - RectangleShape + Color.Black.copy(alpha) for E-ink safe scrim
key_files:
  created:
    - app/src/test/java/com/mudita/sudoku/game/GameViewModelQuitTest.kt
  modified:
    - app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt
    - app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt
decisions:
  - Dialog state as local composable state (not ViewModel) — pure UI concern
  - quitGame() clears repository, resets uiState, clears undoStack, sets showResumeDialog false
  - BackHandler disabled during isLoading to prevent saving empty/partial board
  - Second back press dismisses dialog (standard Android UX pattern)
  - RectangleShape exclusively — no RoundedCornerShape (E-ink anti-aliasing artifacts)
  - No animations — E-ink ghosting prevention (UI-02)
metrics:
  duration: "11 minutes"
  completed: "2026-03-27"
  tasks: 2
  files_modified: 3
---

# Phase 9 Plan 1: Game Navigation — Exit Confirmation Dialog Summary

**One-liner:** Exit confirmation dialog with Save/Forfeit options gated by BackHandler using local dialog state and quitGame() in GameViewModel.

## What Was Built

Added a back-press confirmation dialog to the game screen so players can choose between saving their progress and forfeiting. Previously, back always saved and returned to menu. Now:

1. **Back press during active game** shows "Leave game?" dialog with two options
2. **"Save and Exit"** calls `saveNow()` then navigates to menu (Resume button appears)
3. **"Forfeit"** calls `quitGame()` then navigates to menu (no Resume button)
4. **Second back press** while dialog is visible dismisses it

Force-close recovery (NAV-03) was already handled by `onStop()` in `MainActivity` — no additional changes needed.

## Tasks Completed

| Task | Description | Commit |
|------|-------------|--------|
| 1 | Add quitGame() to GameViewModel with TDD tests | d770421 |
| 2 | Replace BackHandler with dialog-gated exit confirmation in GameScreen | 6e1f461 |

## Verification Results

- All unit tests pass (BUILD SUCCESSFUL)
- Project compiles successfully
- GameViewModel.kt contains `suspend fun quitGame()` with correct implementation
- GameScreen.kt contains `ExitConfirmationDialog` with "Leave game?", "Save and Exit", "Forfeit"
- BackHandler gated with `enabled = !uiState.isLoading`
- No RoundedCornerShape, no AnimatedVisibility, no Crossfade in GameScreen.kt

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None — all functionality is fully wired.

## Self-Check: PASSED

Files created/modified:
- FOUND: app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt (quitGame added)
- FOUND: app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt (dialog added)
- FOUND: app/src/test/java/com/mudita/sudoku/game/GameViewModelQuitTest.kt (4 tests)

Commits:
- d770421 — feat(09-01): add quitGame() to GameViewModel with unit tests
- 6e1f461 — feat(09-01): add exit confirmation dialog to GameScreen with BackHandler gate
