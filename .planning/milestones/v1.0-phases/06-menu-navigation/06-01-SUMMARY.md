---
phase: 06-menu-navigation
plan: 01
subsystem: ui
tags: [kotlin, compose, mmd, navigation, menu, screen-routing]

# Dependency graph
requires:
  - phase: 05-scoring-completion
    provides: SummaryScreen, LeaderboardScreen, CompletionResult, GameViewModel.showResumeDialog
  - phase: 04-persistence
    provides: GameViewModel.saveNow(), resumeGame(), startGame(difficulty)
  - phase: 03-core-game-ui
    provides: GameScreen, GameViewModel

provides:
  - MenuScreen composable with conditional Resume button driven by showResumeDialog StateFlow
  - DifficultyScreen composable with Easy/Medium/Hard selection and BackHandler
  - Full 5-screen navigation flow via Screen enum (MENU, DIFFICULTY, GAME, SUMMARY, LEADERBOARD)
  - BackHandler wiring for GameScreen (save + navigate), SummaryScreen, LeaderboardScreen, DifficultyScreen
  - MainActivity routing rewritten to start at MENU, reactive hasSavedGame from showResumeDialog

affects:
  - 06-02 (test plan for this navigation flow)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Screen enum-based navigation: var currentScreen by remember { mutableStateOf(Screen.MENU) } drives when() routing"
    - "BackHandler with coroutine scope: rememberCoroutineScope().launch { saveNow(); onBackToMenu() } for async pre-nav actions"
    - "Reactive resume detection: collect showResumeDialog StateFlow in MainActivity for conditional menu button"
    - "startGame(difficulty) called BEFORE currentScreen = Screen.GAME to pre-start puzzle generation"

key-files:
  created:
    - app/src/main/java/com/mudita/sudoku/ui/game/MenuScreen.kt
    - app/src/main/java/com/mudita/sudoku/ui/game/DifficultyScreen.kt
  modified:
    - app/src/main/java/com/mudita/sudoku/MainActivity.kt
    - app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt
    - app/src/main/java/com/mudita/sudoku/ui/game/SummaryScreen.kt
    - app/src/main/java/com/mudita/sudoku/ui/game/LeaderboardScreen.kt

key-decisions:
  - "Screen enum retained (not NavHost) per D-09 — enum-based routing is sufficient for 5 screens with no deep linking"
  - "showResumeDialog StateFlow collected in MainActivity (not GameScreen) — Resume button visibility now lives in menu context"
  - "startGame(difficulty) called before navigating to GAME screen — ensures puzzle generation begins before GameScreen renders"
  - "MenuScreen has no BackHandler — default ComponentActivity back exits app (D-11, intentional)"

patterns-established:
  - "Screen composables are nav-unaware leaf components: receive callbacks only, emit decisions via lambdas"
  - "BackHandler pattern: DifficultyScreen/SummaryScreen/LeaderboardScreen use BackHandler { onBack() } for system back"
  - "GameScreen BackHandler launches coroutine: coroutineScope.launch { viewModel.saveNow(); onBackToMenu() }"

requirements-completed: [NAV-01]

# Metrics
duration: 15min
completed: 2026-03-25
---

# Phase 6 Plan 01: Menu & Navigation Summary

**5-screen navigation flow via Screen enum with MenuScreen, DifficultyScreen, and BackHandler wiring across all screens — app now launches to main menu with conditional Resume button**

## Performance

- **Duration:** 15 min
- **Started:** 2026-03-25T08:23:06Z
- **Completed:** 2026-03-25T08:37:54Z
- **Tasks:** 3
- **Files modified:** 6 (2 created, 4 modified)

## Accomplishments

- Created MenuScreen with conditional Resume button (absent when no saved game, not greyed-out), following established 56dp ButtonMMD layout pattern
- Created DifficultyScreen with Easy/Medium/Hard options and BackHandler for system back support
- Rewired MainActivity: Screen enum extended with MENU and DIFFICULTY, initial screen changed from GAME to MENU, showResumeDialog collected reactively as hasSavedGame
- Removed ResumeDialog from GameScreen — dialog replaced by MenuScreen Resume button driven by reactive StateFlow
- Added BackHandler to GameScreen (async save then navigate), SummaryScreen, LeaderboardScreen
- Renamed SummaryScreen/LeaderboardScreen button from "New Game" to "Back to Menu" with `onBackToMenu` parameter

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MenuScreen and DifficultyScreen composables** - `d984d14` (feat)
2. **Task 2: Modify GameScreen, SummaryScreen, LeaderboardScreen** - `658ee85` (feat)
3. **Task 3: Rewire MainActivity routing with MENU and DIFFICULTY** - `ab0a9b2` (feat)

## Files Created/Modified

- `app/src/main/java/com/mudita/sudoku/ui/game/MenuScreen.kt` — Main menu composable; conditional Resume button; no BackHandler (ComponentActivity back exits app)
- `app/src/main/java/com/mudita/sudoku/ui/game/DifficultyScreen.kt` — Difficulty selection composable; BackHandler for system back to MENU
- `app/src/main/java/com/mudita/sudoku/MainActivity.kt` — Extended Screen enum, MENU initial screen, reactive hasSavedGame, 5-branch when() routing
- `app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt` — Removed ResumeDialog, removed auto-start LaunchedEffect, added BackHandler with saveNow()
- `app/src/main/java/com/mudita/sudoku/ui/game/SummaryScreen.kt` — Renamed onNewGame to onBackToMenu, "Back to Menu" label, added BackHandler
- `app/src/main/java/com/mudita/sudoku/ui/game/LeaderboardScreen.kt` — Renamed onNewGame to onBackToMenu, "Back to Menu" label, added BackHandler

## Decisions Made

- Screen enum retained rather than NavHost — 5 screens with no deep linking do not justify NavHost complexity (D-09)
- showResumeDialog StateFlow now collected in MainActivity to drive MenuScreen's hasSavedGame parameter — previously consumed inside GameScreen for dialog display
- startGame(difficulty) called BEFORE setting currentScreen to GAME to pre-start puzzle generation (Pitfall 5 avoidance from research)
- resumeGame() called in MENU's onResume callback (not in GameScreen) — clears showResumeDialog and restores board state before navigation

## Deviations from Plan

None — plan executed exactly as written.

The worktree required merging from local master to obtain phase 06 plan files (the worktree was initialized before phase 06 planning was complete). This is an environment setup issue, not a plan deviation.

## Issues Encountered

- Worktree branch was behind local master (missing phase 04, 05, 06 files) — resolved by `git merge master` before execution
- `local.properties` (sdk.dir) was absent in the worktree — resolved by copying from main repo for compilation verification
- Gradle wrapper scripts fail when invoked as `./gradlew` in bash (ClassNotFoundException) — workaround: invoke Gradle jar directly via the cached wrapper distribution

## Next Phase Readiness

- All 6 production files compile cleanly (`./gradlew :app:compileDebugKotlin` passes)
- Navigation flow is complete — Phase 06 Plan 02 can now add tests for the navigation flow
- No stubs: all screens are fully wired with real data from GameViewModel StateFlows

---
*Phase: 06-menu-navigation*
*Completed: 2026-03-25*

## Self-Check: PASSED

- FOUND: app/src/main/java/com/mudita/sudoku/ui/game/MenuScreen.kt
- FOUND: app/src/main/java/com/mudita/sudoku/ui/game/DifficultyScreen.kt
- FOUND: .planning/phases/06-menu-navigation/06-01-SUMMARY.md
- FOUND: d984d14 (Task 1 commit)
- FOUND: 658ee85 (Task 2 commit)
- FOUND: ab0a9b2 (Task 3 commit)
- FOUND: 4beeef0 (metadata commit)
