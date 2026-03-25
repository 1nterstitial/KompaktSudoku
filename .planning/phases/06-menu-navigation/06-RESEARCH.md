# Phase 6: Menu & Navigation - Research

**Researched:** 2026-03-25
**Domain:** Android Compose screen routing, back-press handling, UI composable modification
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Main Menu Screen**
- D-01: App title on the menu screen: "Sudoku" — minimal, no "Mudita" prefix needed in the UI.
- D-02: Menu buttons (top to bottom): "New Game" → "Resume" (conditional, only when `hasSavedGame()` is true) → "Best Scores". Resume is absent (not greyed out) when no save exists.
- D-03: Full-screen vertical layout — title at top, buttons stacked below with 56dp height constraint.

**Difficulty Selection Screen**
- D-04: Separate full-screen composable — not a dialog or bottom sheet.
- D-05: Three buttons: "Easy", "Medium", "Hard" — each 56dp tall, full width.
- D-06: A "Back" button at the bottom (or system back) navigates back to the main menu.
- D-07: Screen heading: "Select Difficulty".

**Navigation Graph**
- D-08: Routes: MENU (entry point), DIFFICULTY, GAME, SUMMARY, LEADERBOARD. All routes back to MENU.
- D-09: Extend the existing `Screen` enum with MENU and DIFFICULTY states rather than introducing Jetpack Navigation NavHost. Use `when(currentScreen)` switch in `MainActivity`. Claude's discretion on exact `remember`/`mutableStateOf` structure.

**Back-Press Behavior**
- D-10: Back mid-game: auto-save via `viewModel.saveNow()` and navigate to MENU, no confirmation dialog.
- D-11: Back at MENU: exits the app (ComponentActivity default, no override needed).
- D-12: Back at DIFFICULTY, SUMMARY, or LEADERBOARD: returns to MENU.

**GameScreen Changes**
- D-13: Remove `ResumeDialog` composable from `GameScreen` entirely.
- D-14: Remove the `LaunchedEffect` auto-start (`viewModel.startGame(Difficulty.EASY)` guard).
- D-15: Claude's discretion on whether `GameScreen` receives Difficulty as a param or `startGame` was already called before `GameScreen` composes.

**SummaryScreen Changes**
- D-16: Replace `onNewGame` callback with `onBackToMenu`. "New Game" button becomes "Back to Menu".

**LeaderboardScreen Changes**
- D-18: Replace `onNewGame` callback with `onBackToMenu`. "New Game" button becomes "Back to Menu".

### Claude's Discretion

- Exact `MainActivity` state structure (how `currentScreen`, `completionResult`, and `selectedDifficulty` are held and updated) — follow the same `remember { mutableStateOf(...) }` pattern already in place.
- Whether `DifficultyScreen` is a new file in `ui/game/` or `ui/menu/` — follow existing `ui/game/` convention for consistency.
- `MenuScreen` file location — `ui/game/MenuScreen.kt` following existing conventions.
- Whether `GameScreen` receives Difficulty as a parameter or `startGame` was already called before composing.

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| NAV-01 | App has a main menu with options to start a new game (with difficulty selection) and view the leaderboard; conditional resume when saved game exists; completion navigates to summary; coherent back stack throughout | Screen enum extension pattern, BackHandler, hasSavedGame() StateFlow collection, saveNow() coroutine call from BackHandler |
</phase_requirements>

---

## Summary

Phase 6 is a pure navigation and screen assembly phase with no new domain logic. All ViewModel methods needed by the new screens (`hasSavedGame()`, `startGame(difficulty)`, `resumeGame()`, `startNewGame()`, `saveNow()`) already exist. The work is: extend the `Screen` enum with two new values, create two new composable files (`MenuScreen.kt`, `DifficultyScreen.kt`), wire back-press behavior with `BackHandler`, and rename callbacks in `SummaryScreen` and `LeaderboardScreen`.

The codebase already has `activity-compose` (which provides `BackHandler`) in `implementation` scope. The existing `when(currentScreen)` pattern in `MainActivity` is the correct extension point — no NavHost migration is needed or wanted. The `showResumeDialog` StateFlow in `GameViewModel` becomes unused after this phase; `hasSavedGame()` (which reads `pendingSavedState != null || _showResumeDialog.value`) is the check the menu uses.

The main pitfall is the `hasSavedGame()` timing issue: the ViewModel `init` block loads saved state asynchronously. If `MenuScreen` renders before the `init` coroutine completes, `hasSavedGame()` returns false and the Resume button is absent. The fix is to collect `viewModel.showResumeDialog` as a StateFlow (already exists) to drive the conditional Resume button reactively, rather than calling `hasSavedGame()` once at composition time.

**Primary recommendation:** Collect `viewModel.showResumeDialog` as a StateFlow in `MainActivity` (same as `leaderboardScores` is already collected) and pass `hasSavedGame: Boolean` as a parameter to `MenuScreen` — so the Resume button reacts when the async load completes.

---

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Jetpack Compose (via BOM) | 2026.03.00 | Screen composables | Already in project |
| activity-compose | 1.8.2 | `BackHandler` composable | Already in `implementation` scope — confirmed in `build.gradle.kts` |
| Lifecycle runtime-compose | 2.9.0 | `collectAsStateWithLifecycle` | Already used throughout — collect `showResumeDialog` StateFlow |

No new dependencies are required for this phase.

**Installation:** No new packages needed.

---

## Architecture Patterns

### Navigation Model: Screen Enum Extension

The existing pattern (confirmed from `MainActivity.kt`):

```kotlin
enum class Screen { GAME, SUMMARY, LEADERBOARD }

var currentScreen by remember { mutableStateOf(Screen.GAME) }
when (currentScreen) {
    Screen.GAME -> GameScreen(...)
    Screen.SUMMARY -> SummaryScreen(...)
    Screen.LEADERBOARD -> LeaderboardScreen(...)
}
```

Phase 6 extends to:

```kotlin
enum class Screen { MENU, DIFFICULTY, GAME, SUMMARY, LEADERBOARD }

var currentScreen by remember { mutableStateOf(Screen.MENU) }
var completionResult by remember { mutableStateOf<CompletionResult?>(null) }
var selectedDifficulty by remember { mutableStateOf(Difficulty.EASY) }
```

Initial value changes from `Screen.GAME` to `Screen.MENU` — the app now lands on the menu.

### Conditional Resume Button Pattern

The `hasSavedGame()` method on `GameViewModel` is NOT suitable for direct one-time call at composition because the `init` block loads saved state asynchronously. The existing `showResumeDialog` StateFlow already tracks this correctly.

Correct pattern in `MainActivity`:
```kotlin
val showResume by viewModel.showResumeDialog.collectAsStateWithLifecycle()
// Pass showResume: Boolean to MenuScreen
```

In `MenuScreen`:
```kotlin
@Composable
fun MenuScreen(
    hasSavedGame: Boolean,
    onNewGame: () -> Unit,
    onResume: () -> Unit,
    onBestScores: () -> Unit
) {
    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp)
    ) {
        TextMMD(
            text = "Sudoku",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 24.dp)
        )

        ButtonMMD(onClick = onNewGame, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            TextMMD("New Game")
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (hasSavedGame) {
            ButtonMMD(onClick = onResume, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                TextMMD("Resume")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        ButtonMMD(onClick = onBestScores, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            TextMMD("Best Scores")
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
```

### BackHandler Pattern

`BackHandler` is available from `androidx.activity.compose` (already in `implementation`). It intercepts the system back gesture/button when `enabled = true`.

Pattern for GameScreen back-press (D-10):
```kotlin
// In GameScreen — new import: androidx.activity.compose.BackHandler
BackHandler {
    // saveNow() is a suspend fun — launch from a coroutine scope
    coroutineScope.launch {
        viewModel.saveNow()
        onBackToMenu()  // new callback on GameScreen
    }
}
```

`rememberCoroutineScope()` provides the scope inside a `@Composable`. This is the standard pattern for calling suspend functions from UI events.

Pattern for DIFFICULTY, SUMMARY, LEADERBOARD back-press (D-12):
```kotlin
BackHandler { onBackToMenu() }
```

### GameScreen Modification Pattern

Two items to remove from `GameScreen`:

1. The `showResumeDialog` state collection and `ResumeDialog` conditional block (lines 49, 88–93 of current `GameScreen.kt`).
2. The auto-start `LaunchedEffect` (lines 80–84 of current `GameScreen.kt`).

One item to add:

```kotlin
val coroutineScope = rememberCoroutineScope()
BackHandler {
    coroutineScope.launch {
        viewModel.saveNow()
        onBackToMenu()
    }
}
```

New `GameScreen` signature:
```kotlin
@Composable
fun GameScreen(
    viewModel: GameViewModel = viewModel(),
    onCompleted: (CompletionResult) -> Unit = {},
    onBackToMenu: () -> Unit = {}          // NEW
)
```

### DifficultyScreen Pattern

New full-screen composable following existing SummaryScreen/LeaderboardScreen layout contract:

```kotlin
@Composable
fun DifficultyScreen(
    onDifficultySelected: (Difficulty) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp)
    ) {
        TextMMD(
            text = "Select Difficulty",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 24.dp)
        )

        ButtonMMD(onClick = { onDifficultySelected(Difficulty.EASY) }, ...) { TextMMD("Easy") }
        Spacer(modifier = Modifier.height(8.dp))
        ButtonMMD(onClick = { onDifficultySelected(Difficulty.MEDIUM) }, ...) { TextMMD("Medium") }
        Spacer(modifier = Modifier.height(8.dp))
        ButtonMMD(onClick = { onDifficultySelected(Difficulty.HARD) }, ...) { TextMMD("Hard") }

        Spacer(modifier = Modifier.weight(1f))

        ButtonMMD(onClick = onBack, ...) { TextMMD("Back") }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
```

### SummaryScreen and LeaderboardScreen Rename

Both screens require only parameter renaming — no layout changes:

- `onNewGame: () -> Unit` → `onBackToMenu: () -> Unit`
- Button label `"New Game"` → `"Back to Menu"`

This is a two-line change per file in the composable signature plus the button label string.

### Recommended Project Structure (no change to existing)

```
ui/game/
├── MenuScreen.kt           # NEW — main menu
├── DifficultyScreen.kt     # NEW — difficulty selection
├── GameScreen.kt           # MODIFIED — remove ResumeDialog + LaunchedEffect, add BackHandler
├── SummaryScreen.kt        # MODIFIED — onNewGame → onBackToMenu
├── LeaderboardScreen.kt    # MODIFIED — onNewGame → onBackToMenu
├── GameGrid.kt             # unchanged
├── NumberPad.kt            # unchanged
└── ControlsRow.kt          # unchanged
```

### Anti-Patterns to Avoid

- **Calling `hasSavedGame()` once at composition time:** It returns false until the async `init` block completes. Use `showResumeDialog` StateFlow collected with `collectAsStateWithLifecycle()`.
- **Using `AnimatedVisibility` for the Resume button:** Violates UI-02 (no animations). Use a plain `if (hasSavedGame)` block.
- **Calling `saveNow()` without a coroutine scope:** `saveNow()` is a `suspend fun`. It cannot be called directly from a click handler. Use `rememberCoroutineScope()` inside the composable.
- **Nesting the coroutine launch inside BackHandler without a scope:** `BackHandler { viewModel.saveNow() }` does not compile — saveNow is suspend. Pattern: `coroutineScope.launch { viewModel.saveNow(); onBackToMenu() }`.
- **Setting `currentScreen = Screen.MENU` before `completionResult`:** The existing comment in `MainActivity` (Pitfall 2) documents that `completionResult` must be set BEFORE `currentScreen`. The same ordering discipline applies: when navigating to SUMMARY, set `completionResult` first.
- **Displaying a greyed-out Resume button:** D-02 and the UI-SPEC both specify absence (not disabled) when no save exists. Use `if` not `ButtonMMD(enabled = hasSavedGame)`.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Back-press interception in Compose | Custom `OnBackPressedCallback` registration in the Activity | `BackHandler` composable from `activity-compose` | `BackHandler` is lifecycle-aware and automatically deregisters when the composable leaves the composition; manual callback registration requires manual cleanup |
| Navigation back stack | Custom stack data structure | Extend existing `Screen` enum + `mutableStateOf` in `MainActivity` | D-09 decision locks this; the 5-screen app has no branching back stack complexity requiring a real stack |
| Coroutine scope in composable | Launching on `GlobalScope` | `rememberCoroutineScope()` | Scoped to composable lifecycle; auto-cancelled on recomposition exit |

**Key insight:** This phase is wiring, not building. Every primitive needed (BackHandler, StateFlow collection, `when` routing) is already established in the codebase or in existing dependencies.

---

## Common Pitfalls

### Pitfall 1: hasSavedGame() is not reactive
**What goes wrong:** Calling `viewModel.hasSavedGame()` directly in a composable returns the value at composition time. If the `init` coroutine hasn't completed yet (DataStore read is async), it returns false even when a save exists — the Resume button never appears.
**Why it happens:** `hasSavedGame()` reads `pendingSavedState != null || _showResumeDialog.value` synchronously. The DataStore load in `init` dispatches to `ioDispatcher` — by the time the first composition runs, the load may not have completed.
**How to avoid:** Collect `viewModel.showResumeDialog` as a StateFlow in `MainActivity` using `collectAsStateWithLifecycle()`. Pass the resulting `Boolean` as a parameter to `MenuScreen`. When the DataStore load completes and `_showResumeDialog.value = true` fires, Compose recomposes `MenuScreen` and the Resume button appears.
**Warning signs:** Resume button absent on first launch even though a saved game exists.

### Pitfall 2: saveNow() must be called as suspend inside a coroutine
**What goes wrong:** `BackHandler { viewModel.saveNow() }` fails to compile — `saveNow()` is a `suspend fun`.
**Why it happens:** `BackHandler` lambda is not a coroutine builder; it takes a plain `() -> Unit`.
**How to avoid:** Declare `val coroutineScope = rememberCoroutineScope()` at the top of `GameScreen`, then `BackHandler { coroutineScope.launch { viewModel.saveNow(); onBackToMenu() } }`.
**Warning signs:** Compile error "Suspension functions can be called only within coroutine body."

### Pitfall 3: completionResult ordering in onCompleted callback
**What goes wrong:** Setting `currentScreen = Screen.SUMMARY` before `completionResult = result` causes `SummaryScreen` to receive a null result on the first recomposition, crashing with the `!!` force-unwrap.
**Why it happens:** Already documented in `MainActivity.kt` with an explicit comment. Extending the routing must preserve this ordering.
**How to avoid:** Always set `completionResult = result` before `currentScreen = Screen.SUMMARY`. The existing `MainActivity` code already does this — do not reorder when refactoring.
**Warning signs:** `NullPointerException` at `completionResult!!` in the SUMMARY branch.

### Pitfall 4: showResumeDialog is not cleared after navigating to GAME via Resume
**What goes wrong:** Player taps Resume on the menu → navigates to `GAME` screen. If `showResumeDialog` StateFlow is still `true`, the collected value in `MainActivity` continues to show `hasSavedGame = true`. After the game completes, returning to the menu would still show a Resume button even though `resumeGame()` cleared `pendingSavedState`.
**Why it happens:** `viewModel.resumeGame()` already sets `_showResumeDialog.value = false` — this is already handled correctly in `GameViewModel`. The risk is only if `resumeGame()` is not called when navigating from MENU to GAME via Resume.
**How to avoid:** In `MainActivity`, the Resume button's `onClick` must call `viewModel.resumeGame()` (to restore state AND clear `showResumeDialog`) then navigate to `Screen.GAME`. Do not navigate to GAME and let `GameScreen` call `resumeGame()`.
**Warning signs:** Resume button persists on menu after a completed game.

### Pitfall 5: Auto-start LaunchedEffect removal breaks new game flow
**What goes wrong:** Removing the `LaunchedEffect { if (!viewModel.hasSavedGame()) viewModel.startGame(Difficulty.EASY) }` from `GameScreen` without ensuring `startGame(difficulty)` is called before composing `GameScreen` leaves the screen showing `LoadingScreen` indefinitely (since `isLoading` starts false but no game is generated).
**Why it happens:** `GameUiState()` initializes with `isLoading = false` and an all-zeros board. Without the auto-start, `GameScreen` renders the grid immediately but with an empty board.
**How to avoid:** `MainActivity` must call `viewModel.startGame(selectedDifficulty)` before (or at the same time as) setting `currentScreen = Screen.GAME`. The difficulty is captured as `selectedDifficulty` from the `DifficultyScreen` callback. In the resume flow, `viewModel.resumeGame()` is called instead.
**Warning signs:** `GameScreen` shows a blank grid with no digits and no loading indicator.

---

## Code Examples

Verified patterns from the existing codebase (confirmed by reading source files):

### BackHandler (from activity-compose 1.8.2)
```kotlin
// Source: androidx.activity.compose.BackHandler
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

// Inside a @Composable:
val coroutineScope = rememberCoroutineScope()
BackHandler {
    coroutineScope.launch {
        viewModel.saveNow()
        onBackToMenu()
    }
}
```

### StateFlow collection in MainActivity (established pattern)
```kotlin
// Source: MainActivity.kt line 78 — existing pattern
val leaderboardScores by viewModel.leaderboardScores.collectAsStateWithLifecycle()

// Apply same pattern for showResumeDialog:
val hasSavedGame by viewModel.showResumeDialog.collectAsStateWithLifecycle()
```

### Full-screen Column layout (established pattern)
```kotlin
// Source: SummaryScreen.kt lines 46–49 — established pattern
Column(
    modifier = Modifier
        .windowInsetsPadding(WindowInsets.systemBars)
        .padding(horizontal = 16.dp)
) { ... }
```

### Conditional composable presence (not visibility)
```kotlin
// Correct: absent from layout, not disabled
if (hasSavedGame) {
    ButtonMMD(onClick = onResume, ...) { TextMMD("Resume") }
    Spacer(modifier = Modifier.height(8.dp))
}
```

### Screen enum extension
```kotlin
// Source: MainActivity.kt line 34 — extend, don't replace
enum class Screen { MENU, DIFFICULTY, GAME, SUMMARY, LEADERBOARD }
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `Screen.GAME` as initial enum value | `Screen.MENU` as initial value | Phase 6 | App now lands on menu, not game |
| `onNewGame` callback on SummaryScreen and LeaderboardScreen | `onBackToMenu` callback | Phase 6 | "New Game" button → "Back to Menu"; new game flow goes through DifficultyScreen |
| `ResumeDialog` inside `GameScreen` | Resume option on `MenuScreen` | Phase 6 | Phase 4 D-03 documented this intent |
| Auto-start `LaunchedEffect` in `GameScreen` | `startGame(difficulty)` called from `MainActivity` before navigating | Phase 6 | Difficulty is now user-selected, not hardcoded to EASY |

**Deprecated/outdated after this phase:**
- `showResumeDialog` StateFlow: Still used (to drive `hasSavedGame` on menu), but the `ResumeDialog` composable that consumed it is deleted. The StateFlow itself remains the correct reactive source.
- `viewModel.startNewGame()`: This method currently calls `startGame(Difficulty.EASY)` internally. After Phase 6, `startNewGame()` should probably not be called from the menu flow (difficulty is selected separately). However, it may still be called from `GameScreen` back-press if the game was freshly started — **no change to ViewModel needed**; the nav graph now handles difficulty selection separately.

---

## Open Questions

1. **Should `_showResumeDialog` be renamed or kept as-is?**
   - What we know: The field was named for its original purpose (driving the dialog). After Phase 6, it drives the menu Resume button instead.
   - What's unclear: Whether the field name causes confusion for future maintainers.
   - Recommendation: Keep the name as-is — renaming ViewModel fields is a separate refactor outside this phase scope.

2. **Does `GameScreen` need `onBackToMenu` as a required or default-empty callback?**
   - What we know: D-15 gives Claude discretion. `GameScreen` currently has `onCompleted: (CompletionResult) -> Unit = {}` as a defaulted callback.
   - What's unclear: Test files pass `GameScreen(viewModel = ...)` without `onBackToMenu`. Adding it as a required param would break existing tests.
   - Recommendation: Add `onBackToMenu: () -> Unit = {}` as a defaulted param — consistent with `onCompleted` pattern; existing tests continue to compile.

---

## Environment Availability

Step 2.6: SKIPPED (no external dependencies — phase is pure code changes to existing files; no new tools, services, or CLIs required).

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 (Robolectric + Compose UI Test) |
| Config file | `robolectric.properties` (per-test `@Config(sdk = [31])`) |
| Quick run command | `./gradlew testDebugUnitTest --tests "com.mudita.sudoku.ui.game.*" -x lint` |
| Full suite command | `./gradlew testDebugUnitTest -x lint` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| NAV-01a | MenuScreen renders "Sudoku" title, "New Game" and "Best Scores" buttons | unit (Robolectric) | `./gradlew testDebugUnitTest --tests "*.MenuScreenTest" -x lint` | ❌ Wave 0 |
| NAV-01b | MenuScreen shows "Resume" button only when `hasSavedGame = true` | unit (Robolectric) | `./gradlew testDebugUnitTest --tests "*.MenuScreenTest" -x lint` | ❌ Wave 0 |
| NAV-01c | DifficultyScreen renders heading and all three difficulty buttons | unit (Robolectric) | `./gradlew testDebugUnitTest --tests "*.DifficultyScreenTest" -x lint` | ❌ Wave 0 |
| NAV-01d | DifficultyScreen button callbacks invoked on tap | unit (Robolectric) | `./gradlew testDebugUnitTest --tests "*.DifficultyScreenTest" -x lint` | ❌ Wave 0 |
| NAV-01e | SummaryScreen shows "Back to Menu" button (not "New Game") | unit (Robolectric) | `./gradlew testDebugUnitTest --tests "*.SummaryScreenTest" -x lint` | ❌ update existing |
| NAV-01f | LeaderboardScreen shows "Back to Menu" button (not "New Game") | unit (Robolectric) | `./gradlew testDebugUnitTest --tests "*.LeaderboardScreenTest" -x lint` | ❌ update existing |
| NAV-01g | Back-press mid-game (BackHandler) — integration tested manually on device | manual | n/a | n/a — BackHandler is untestable with Robolectric |

**Note on NAV-01g:** `BackHandler` relies on `OnBackPressedDispatcher` which Robolectric does not simulate. Back-press behavior must be verified by manual testing on device or emulator. The `saveNow()` call itself is unit-tested via `GameViewModelPersistenceTest` (existing).

### Sampling Rate
- **Per task commit:** `./gradlew testDebugUnitTest --tests "com.mudita.sudoku.ui.game.*" -x lint`
- **Per wave merge:** `./gradlew testDebugUnitTest -x lint`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `app/src/test/java/com/mudita/sudoku/ui/game/MenuScreenTest.kt` — covers NAV-01a, NAV-01b
- [ ] `app/src/test/java/com/mudita/sudoku/ui/game/DifficultyScreenTest.kt` — covers NAV-01c, NAV-01d
- [ ] Update `SummaryScreenTest.kt` — change `"New Game"` button text assertions to `"Back to Menu"`, update `onNewGame` to `onBackToMenu` in test calls
- [ ] Update `LeaderboardScreenTest.kt` — change `"New Game"` button text assertions to `"Back to Menu"`, update `onNewGame` to `onBackToMenu` in test calls

Existing test infrastructure (JUnit 4, Robolectric, Compose UI Test, `createComposeRule`) fully covers all new composables — no new dependencies needed.

---

## Project Constraints (from CLAUDE.md)

| Constraint | Enforcement |
|------------|-------------|
| All UI uses ThemeMMD, ButtonMMD, TextMMD | New screens: MenuScreen and DifficultyScreen must wrap in ThemeMMD (via MainActivity) and use ButtonMMD/TextMMD exclusively |
| No ripple effects, no animations | No `AnimatedVisibility` for conditional Resume button; use plain `if`. No `animate*AsState`. No `Crossfade`. |
| No dynamicColorScheme | eInkColorScheme only via ThemeMMD — no MaterialTheme override |
| Touch targets minimum 56dp | All buttons in MenuScreen and DifficultyScreen: `Modifier.height(56.dp).fillMaxWidth()` |
| minSdk 31, targetSdk 31 | No API > 31 introduced; BackHandler is API 1+ (available since activity-ktx 1.0) |
| MVVM + StateFlow | Menu drives conditional state from `viewModel.showResumeDialog` StateFlow — no local mutable state for save detection |
| Local only, no network | Not applicable to this phase |
| No font size overrides below MMD defaults | TextMMD without explicit `fontSize` parameter throughout |

---

## Sources

### Primary (HIGH confidence)
- `MainActivity.kt` (read directly) — existing `Screen` enum, routing pattern, `mutableStateOf`, `collectAsStateWithLifecycle` usage
- `GameViewModel.kt` (read directly) — `hasSavedGame()`, `showResumeDialog`, `saveNow()`, `resumeGame()`, `startGame()`, `startNewGame()` signatures and behavior
- `GameScreen.kt` (read directly) — `ResumeDialog`, `LaunchedEffect` auto-start, exact lines to remove
- `SummaryScreen.kt`, `LeaderboardScreen.kt` (read directly) — exact callback signatures and button labels to change
- `build.gradle.kts` + `libs.versions.toml` (read directly) — `activity-compose 1.8.2` is in `implementation` scope; no new deps needed
- Existing test files (read directly) — `SummaryScreenTest.kt` and `LeaderboardScreenTest.kt` test `"New Game"` text; must be updated

### Secondary (MEDIUM confidence)
- `CONTEXT.md` (read directly) — all locked decisions D-01 through D-18
- `UI-SPEC.md` (read directly) — spacing, typography, and layout contracts for MenuScreen and DifficultyScreen

### Tertiary (LOW confidence)
- None — all findings backed by direct source inspection.

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all dependencies confirmed from `build.gradle.kts` and `libs.versions.toml`
- Architecture: HIGH — navigation pattern confirmed from `MainActivity.kt`; BackHandler confirmed from `activity-compose` in build graph
- Pitfalls: HIGH — three of five pitfalls directly observed from existing code (Pitfall 2 ordering already has a comment in MainActivity; Pitfall 1 diagnosed from `init` block async behavior in `GameViewModel`)
- Test architecture: HIGH — existing test files read directly; gaps are explicit and specific

**Research date:** 2026-03-25
**Valid until:** Stable — no external dependencies to track; all findings from local source files
