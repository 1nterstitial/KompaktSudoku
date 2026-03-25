---
phase: 04-persistence
plan: 03
subsystem: ui
tags: [compose, dialog, viewmodel, lifecycle, datastore, resume, save-on-stop]

# Dependency graph
requires:
  - phase: 04-persistence-plan-02
    provides: showResumeDialog StateFlow, hasSavedGame(), resumeGame(), startNewGame(), saveNow() suspend fun
  - phase: 04-persistence-plan-01
    provides: DataStoreGameRepository, gameDataStore extension, GameRepository interface

provides:
  - ResumeDialog composable in GameScreen.kt — modal with "Resume last game?" heading and Resume/New Game buttons
  - hasSavedGame() guard on LaunchedEffect auto-start (Pitfall 6 prevention)
  - ViewModelProvider.Factory in MainActivity injecting DataStoreGameRepository
  - onStop() lifecycle hook triggering viewModel.saveNow() on Dispatchers.IO

affects:
  - Phase 5: scoring/completion screen — no conflicts; GameEvent.Completed handler stub is already wired
  - Phase 6: menu/navigation — GameScreen will receive difficulty param from nav layer; no conflict

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "ViewModelProvider.Factory anonymous object pattern for constructor injection in Activity"
    - "by viewModels { factory } delegate — Activity-scoped ViewModel survives config changes"
    - "by lazy { } for repository init — ensures applicationContext is non-null when accessed"
    - "lifecycleScope.launch(Dispatchers.IO) in onStop — fire-and-forget save with correct dispatcher"
    - "BasicAlertDialog + Surface pattern for E-ink dialog — avoids ripple from AlertDialog built-ins"

key-files:
  created: []
  modified:
    - app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt
    - app/src/main/java/com/mudita/sudoku/MainActivity.kt

key-decisions:
  - "ButtonMMD import is com.mudita.mmd.components.buttons.ButtonMMD (plural) — confirmed from actual AAR class inspection; plan comment used singular which was wrong"
  - "ResumeDialog placed before isLoading check in GameScreen — dialog must appear immediately on launch, before board state is loaded or loading begins"

patterns-established:
  - "Activity ViewModel injection: by viewModels { ViewModelProvider.Factory } with by lazy {} repository — standard pattern for DataStore + Activity lifecycle"
  - "onStop save pattern: lifecycleScope.launch(Dispatchers.IO) { viewModel.saveNow() } — guards are inside saveNow() to keep Activity thin"

requirements-completed: [STATE-01, STATE-02, STATE-03]

# Metrics
duration: 3min
completed: 2026-03-25
---

# Phase 4 Plan 03: UI Persistence Wiring Summary

**ResumeDialog with "Resume last game?" heading, ButtonMMD buttons at 56dp height, hasSavedGame() guard on auto-start, and DataStoreGameRepository injection via ViewModelProvider.Factory with onStop save trigger**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-03-25T02:12:13Z
- **Completed:** 2026-03-25T02:15:30Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- `GameScreen.kt` now collects `showResumeDialog` via `collectAsStateWithLifecycle()` and renders `ResumeDialog` when true
- `LaunchedEffect(Unit)` auto-start is guarded by `hasSavedGame()` — prevents the saved game from being clobbered before the player sees the resume prompt (Pitfall 6)
- `ResumeDialog` is a private composable using `BasicAlertDialog` (not `AlertDialog`) — avoids ripple from the built-in `confirmButton`/`dismissButton` slots
- Dialog uses `Surface(color = Color.White, border = BorderStroke(1.dp, Color.Black))` — monochromatic E-ink display requirement
- Both buttons are `ButtonMMD` at `fillMaxWidth()` and `height(56.dp)` — meets UI-03 56dp minimum touch target
- `onDismissRequest = onNewGame` — back press or outside tap treated as "New Game" (D-03 dismissal contract)
- `MainActivity` now creates `DataStoreGameRepository` via `by lazy` using `applicationContext.gameDataStore`
- `GameViewModel` injected via `ViewModelProvider.Factory` — the real repository is wired in production while test ViewModel creation remains unaffected
- `onStop()` fires `viewModel.saveNow()` on `Dispatchers.IO` via `lifecycleScope.launch` — silent, automatic save when app is backgrounded
- All 38+ tests (persistence and game logic) still pass unchanged

## Task Commits

Each task was committed atomically:

1. **Task 1: ResumeDialog composable + GameScreen guard** - `9f118af` (feat)
2. **Task 2: MainActivity ViewModel factory + onStop save trigger** - `b53bde6` (feat)

## Files Created/Modified

- `app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt` — Added `showResumeDialog` state collection, `hasSavedGame()` guard on `LaunchedEffect`, `ResumeDialog` conditional, `ResumeDialog` private composable with `BasicAlertDialog`/`Surface`/`ButtonMMD`/`TextMMD`
- `app/src/main/java/com/mudita/sudoku/MainActivity.kt` — Added `repository by lazy`, `viewModel by viewModels { factory }` with `DataStoreGameRepository` injection, `GameScreen(viewModel = viewModel)`, `onStop()` with `lifecycleScope.launch(Dispatchers.IO) { viewModel.saveNow() }`

## Decisions Made

- `ButtonMMD` import is `com.mudita.mmd.components.buttons.ButtonMMD` (plural `buttons`) — the plan comment specified the singular package `components.button` which does not exist in the actual AAR. Confirmed by inspecting `classes.jar` from the cached `mmd-core-release.aar`. This is consistent with `ControlsRow.kt` and `NumberPad.kt` which already use the plural path.
- `ResumeDialog` is placed before the `if (uiState.isLoading)` check — the dialog must appear immediately at launch when a saved game exists. At that moment the board is in its initial empty state (`isLoading = false` initially), so placing it before the loading check ensures it is visible before `startGame()` sets `isLoading = true`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Wrong ButtonMMD import package (singular vs plural)**
- **Found during:** Task 1 (compile verification)
- **Issue:** Plan specified `import com.mudita.mmd.components.button.ButtonMMD` (singular `button`). The actual MMD AAR uses `com.mudita.mmd.components.buttons.ButtonMMD` (plural `buttons`). Build failed with "Unresolved reference: ButtonMMD".
- **Fix:** Changed import to plural `buttons` package, consistent with `ControlsRow.kt` and `NumberPad.kt`. Confirmed by inspecting `classes.jar` from the cached MMD AAR.
- **Files modified:** `GameScreen.kt`
- **Commit:** `9f118af` (part of Task 1 commit after fix)

---

**Total deviations:** 1 auto-fixed (Rule 1 — bug: wrong package path in import from plan spec)
**Impact on plan:** Compile-time only; no behavior change.

## Issues Encountered

None beyond the import fix above.

## User Setup Required

None — no external service configuration required. The DataStore file (`game_state.preferences_pb`) is created automatically by the Android framework on first use.

## Next Phase Readiness

- Phase 4 (Persistence) is fully complete:
  - Plan 01: DataStore repository layer with serialization
  - Plan 02: GameViewModel persistence integration (14 TDD tests)
  - Plan 03: UI wiring (ResumeDialog + factory + onStop)
- Phase 5 (Scoring & Completion) can proceed. `GameEvent.Completed(errorCount)` is already emitted by `GameViewModel`; Phase 5 just needs to handle it in `GameScreen`.
- No blockers.

## Known Stubs

- `GameScreen.kt` line ~55: `// TODO Phase 5: navigate to completion/score screen` — `GameEvent.Completed` is collected but no navigation or score screen is shown. This is intentional: scoring is Phase 5 scope. The game loop functions correctly (board locks at completion via `isComplete = true`); only the post-completion UX is deferred.

---
*Phase: 04-persistence*
*Completed: 2026-03-25*
