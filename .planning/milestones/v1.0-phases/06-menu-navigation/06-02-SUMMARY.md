---
phase: 06-menu-navigation
plan: 02
subsystem: testing
tags: [kotlin, compose, robolectric, mmd, navigation, menu, tests]

# Dependency graph
requires:
  - phase: 06-menu-navigation
    provides: MenuScreen, DifficultyScreen, SummaryScreen (onBackToMenu), LeaderboardScreen (onBackToMenu)

provides:
  - MenuScreenTest: 8 Robolectric UI tests covering title, buttons, conditional Resume, and all callbacks
  - DifficultyScreenTest: 9 Robolectric UI tests covering heading, all difficulty/back buttons, and callbacks
  - SummaryScreenTest updated: onBackToMenu callbacks and "Back to Menu" button text assertions
  - LeaderboardScreenTest updated: onBackToMenu callbacks and "Back to Menu" button text assertions

affects:
  - Future phases that modify MenuScreen, DifficultyScreen, SummaryScreen, or LeaderboardScreen

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "All 4 UI test files follow identical pattern: @RunWith(RobolectricTestRunner::class) @Config(sdk=[31]) + createComposeRule() + ThemeMMD wrapping"
    - "Callback test pattern: var callCount = 0; set in setContent lambda; assertEquals(1, callCount) after performClick()"
    - "Conditional visibility test: hasSavedGame=true -> assertIsDisplayed(); hasSavedGame=false -> assertDoesNotExist()"

key-files:
  created:
    - app/src/test/java/com/mudita/sudoku/ui/game/MenuScreenTest.kt
    - app/src/test/java/com/mudita/sudoku/ui/game/DifficultyScreenTest.kt
  modified:
    - app/src/test/java/com/mudita/sudoku/ui/game/SummaryScreenTest.kt
    - app/src/test/java/com/mudita/sudoku/ui/game/LeaderboardScreenTest.kt

key-decisions:
  - "assertDoesNotExist() used for absent Resume button (not assertIsNotDisplayed) — correct semantics: node should not be in composition at all when hasSavedGame=false"

patterns-established:
  - "Menu/difficulty screen tests use assertDoesNotExist() for conditionally absent nodes (not assertIsNotDisplayed)"

requirements-completed: [NAV-01]

# Metrics
duration: 5min
completed: 2026-03-25
---

# Phase 6 Plan 02: Navigation UI Tests Summary

**Robolectric Compose tests for all 4 navigation screens — 22 new/updated tests covering conditional Resume button, difficulty selection callbacks, and renamed onBackToMenu parameter throughout**

## Performance

- **Duration:** 5 min
- **Started:** 2026-03-25T04:43:20Z
- **Completed:** 2026-03-25T04:48:50Z
- **Tasks:** 2
- **Files modified:** 4 (2 created, 2 modified)

## Accomplishments

- Created MenuScreenTest with 8 tests: title display, always-visible buttons (New Game, Best Scores), conditional Resume button (shown when hasSavedGame=true, absent when false via assertDoesNotExist()), and all 3 button callbacks
- Created DifficultyScreenTest with 9 tests: heading display, all 4 buttons (Easy/Medium/Hard/Back), difficulty callbacks with correct Difficulty.EASY/MEDIUM/HARD enum values, and Back callback
- Updated SummaryScreenTest: all 8 tests now use `onBackToMenu = {}` instead of `onNewGame = {}`; last test renamed and asserts `onNodeWithText("Back to Menu").performClick()`
- Updated LeaderboardScreenTest: all 5 tests now use `onBackToMenu = {}` instead of `onNewGame = {}`; last test renamed and asserts `onNodeWithText("Back to Menu").performClick()`
- Full test suite passes with `./gradlew :app:testDebugUnitTest`

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MenuScreenTest and DifficultyScreenTest** - `58f89b9` (test)
2. **Task 2: Update SummaryScreenTest and LeaderboardScreenTest for onBackToMenu** - `ee93238` (test)

## Files Created/Modified

- `app/src/test/java/com/mudita/sudoku/ui/game/MenuScreenTest.kt` — 8 tests for MenuScreen: title, New Game/Best Scores/Resume buttons, conditional Resume logic, all 3 callbacks
- `app/src/test/java/com/mudita/sudoku/ui/game/DifficultyScreenTest.kt` — 9 tests for DifficultyScreen: Select Difficulty heading, Easy/Medium/Hard/Back buttons, correct Difficulty enum in callbacks, Back callback
- `app/src/test/java/com/mudita/sudoku/ui/game/SummaryScreenTest.kt` — 8 tests; all `onNewGame` renamed to `onBackToMenu`; last test renamed to "Back to Menu button calls onBackToMenu callback"; performClick assertion updated to "Back to Menu"
- `app/src/test/java/com/mudita/sudoku/ui/game/LeaderboardScreenTest.kt` — 5 tests; all `onNewGame` renamed to `onBackToMenu`; last test renamed; performClick assertion updated to "Back to Menu"

## Decisions Made

- `assertDoesNotExist()` used for the absent Resume button (when hasSavedGame=false) rather than `assertIsNotDisplayed()` — the node is not in the composition at all (conditional `if` removes it), so `assertDoesNotExist()` is the correct semantic

## Deviations from Plan

None — plan executed exactly as written.

Note: The worktree branch was behind master and required a `git merge master` before execution to obtain the Plan 01 production files (MenuScreen.kt, DifficultyScreen.kt, SummaryScreen.kt, LeaderboardScreen.kt). This is an environment setup issue, not a plan deviation.

## Issues Encountered

- Worktree was missing `local.properties` (sdk.dir) and `gradle/wrapper/gradle-wrapper.jar` — both copied from main repo to enable Gradle test execution. This is a standard worktree setup requirement.
- SummaryScreenTest and LeaderboardScreenTest compilation failed before Task 2 edits because the production files (from Plan 01) already use `onBackToMenu` while the tests still referenced `onNewGame` — Task 2 was the correct fix.

## Next Phase Readiness

- All 4 NAV-01 test files are complete and green
- Phase 6 is fully complete — all production code and tests for the menu/navigation flow are in place
- No stubs: all screens are tested against real composables with correct parameters

---
*Phase: 06-menu-navigation*
*Completed: 2026-03-25*

## Self-Check: PASSED

- FOUND: app/src/test/java/com/mudita/sudoku/ui/game/MenuScreenTest.kt
- FOUND: app/src/test/java/com/mudita/sudoku/ui/game/DifficultyScreenTest.kt
- FOUND: app/src/test/java/com/mudita/sudoku/ui/game/SummaryScreenTest.kt
- FOUND: app/src/test/java/com/mudita/sudoku/ui/game/LeaderboardScreenTest.kt
- FOUND: 58f89b9 (Task 1 commit — MenuScreenTest + DifficultyScreenTest)
- FOUND: ee93238 (Task 2 commit — SummaryScreenTest + LeaderboardScreenTest updated)
- MenuScreenTest: 8 @Test methods confirmed
- DifficultyScreenTest: 9 @Test methods confirmed
- SummaryScreenTest: no remaining `onNewGame` references
- LeaderboardScreenTest: no remaining `onNewGame` references
- Full test suite: BUILD SUCCESSFUL (./gradlew :app:testDebugUnitTest)
