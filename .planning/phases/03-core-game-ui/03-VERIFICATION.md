---
phase: 03-core-game-ui
verified: 2026-03-24T00:00:00Z
status: gaps_found
score: 17/20 must-haves verified
re_verification: false
gaps:
  - truth: "Project compiles cleanly after adding MMD dependency"
    status: failed
    reason: "MMD library dependency is commented out in build.gradle.kts. The real MMD AAR is not resolvable (GitHub Packages returns 401, JFrog instance deactivated). Local stub file MmdComponents.kt substitutes ThemeMMD/ButtonMMD/TextMMD but stubs use Material3, not the real E-ink-optimized library. The app cannot be deployed or APK-linked without credentials."
    artifacts:
      - path: "app/build.gradle.kts"
        issue: "libs.mmd is commented out — the real MMD library is not on the classpath; stubs are compiled into main source set instead"
      - path: "app/src/main/java/com/mudita/mmd/MmdComponents.kt"
        issue: "Non-E-ink-optimized stub: ThemeMMD wraps MaterialTheme (not eInkColorScheme), ButtonMMD uses Box+clickable (not ripple-free ButtonMMD), TextMMD uses Material3 Text (not E-ink typography)"
    missing:
      - "Resolve MMD AAR access: configure GitHub Packages credentials in ~/.gradle/gradle.properties (githubToken) and set credentials in settings.gradle.kts, then switch build.gradle.kts back to implementation(libs.mmd) and delete MmdComponents.kt"
  - truth: "No ripple, no animation, no Animated* composable is used anywhere"
    status: partial
    reason: "The production UI source files contain no Animated* or rememberRipple calls. However, the stub ButtonMMD in MmdComponents.kt uses Box+clickable WITHOUT indication=null — the clickable modifier's default indication will apply ripple in Material3 at runtime on devices where the real MMD is not providing the ripple override. This only affects NumberPad buttons and ControlsRow Undo button; the Fill/Pencil toggles correctly use indication=null."
    artifacts:
      - path: "app/src/main/java/com/mudita/mmd/MmdComponents.kt"
        issue: "Stub ButtonMMD implementation: Box(modifier = modifier.clickable(onClick = onClick)) — clickable without indication=null will render ripple on Material3 (E-ink ghosting risk when stubs are active)"
    missing:
      - "Add indication=null and MutableInteractionSource to stub ButtonMMD.clickable, or accept the risk as a dev-only stub concern and document that the real MMD ButtonMMD is ripple-free by design"
human_verification:
  - test: "Install the APK on a Mudita Kompakt device (requires resolving MMD credentials first)"
    expected: "ThemeMMD provides eInkColorScheme (monochromatic), E-ink typography, and ripple-free button behavior matching CLAUDE.md E-ink requirements"
    why_human: "Cannot verify real MMD behavior without device access and resolved AAR"
  - test: "Visually inspect the running game screen: tap a cell, tap a digit, toggle modes, use undo and erase"
    expected: "Grid renders with thick box borders (2.5dp at 3x3 boundaries) and thin cell borders (1dp); selected cell shows solid black fill with white digit; given cells use bold text; player entries use regular weight; error cells show 1dp inset border; loading state shows centered static text; no ripple visible on any button tap"
    why_human: "Canvas rendering, cell selection visuals, and E-ink display behavior cannot be confirmed without a running device or emulator"
---

# Phase 3: Core Game UI Verification Report

**Phase Goal:** Build the complete game UI screen using MMD components — Canvas grid, number pad, mode controls, and ViewModel wiring — so the app is playable end-to-end on device.
**Verified:** 2026-03-24
**Status:** gaps_found
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

All truths from the three plan must_haves sections are consolidated and verified below.

**From Plan 01 (03-01):**

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 1 | Project compiles cleanly after adding MMD dependency | FAILED | MMD dependency commented out; stub MmdComponents.kt substitutes the library — app cannot be deployed |
| 2 | MainActivity is a ComponentActivity with ThemeMMD root wrapper | VERIFIED | MainActivity.kt extends ComponentActivity, calls setContent { ThemeMMD { GameScreen() } } |
| 3 | GameViewModel exposes eraseCell() that clears selected cell digit and pencil marks | VERIFIED | eraseCell() present in GameViewModel.kt with full guard logic and undo support; 5/5 TDD tests pass |

**From Plan 02 (03-02):**

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 4 | The game screen renders a 9x9 grid with thick box borders and thin cell borders | VERIFIED | drawGridLines() in GameGrid.kt: 2.5dp at i%3==0, 1dp otherwise |
| 5 | Selected cell shows solid black fill with white digit | VERIFIED | drawSelectedCell() draws solid black rect; digit text uses digitStyleSelectedGiven/Player (Color.White) |
| 6 | Given cells display digits in bold; player entries display in regular weight | VERIFIED | digitStyleGiven: FontWeight.Bold; digitStylePlayer: FontWeight.Normal |
| 7 | Error cells (wrong player digit) show a 1dp inset border indicator | VERIFIED | drawErrorIndicator() draws Stroke(1dp) rect with 2dp inset; skipped on selected cell |
| 8 | Pencil marks render as a 3x3 mini-grid inside the cell | VERIFIED | drawPencilMarks() places digit n at subCell ((n-1)/3, (n-1)%3) with 9sp font |
| 9 | Number pad shows digits 1-9 plus an Erase button in a single horizontal row | VERIFIED | NumberPad.kt: Row of 10 ButtonMMD items (1-9 + U+00D7), Arrangement.spacedBy(2dp) |
| 10 | Fill and Pencil mode buttons are visually distinct (active = black fill, inactive = white fill) | VERIFIED | ControlsRow.kt: Box.background(Color.Black/White) per inputMode; TextMMD.color inverted |
| 11 | Undo button is present alongside mode toggles | VERIFIED | ControlsRow.kt: ButtonMMD("Undo") in same Row as Fill/Pencil Box elements |
| 12 | Loading state shows centered static text with no animation | VERIFIED | LoadingScreen(): Box(fillMaxSize, Center) { TextMMD("Generating puzzle...") } — no Animated* composable |
| 13 | No ripple, no animation, no Animated* composable is used anywhere | PARTIAL | Production UI files have no Animated*/rememberRipple; stub ButtonMMD uses clickable without indication=null — potential ripple in stub-active builds |
| 14 | All interactive elements meet >=56dp minimum height | VERIFIED | All NumberPad buttons and all ControlsRow elements: Modifier.sizeIn(minHeight = 56.dp); confirmed by assertHeightIsAtLeast(56.dp) tests |

**From Plan 03 (03-03):**

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 15 | Cell tap dispatches onCellClick with the correct index (0-80) | VERIFIED | GameGridTest: 3 tap-to-index tests pass (cells 0, 11, 80) |
| 16 | Digit button taps dispatch the correct digit to onDigitClick | VERIFIED | NumberPadTest: digits 1, 5, 9 dispatch tests pass |
| 17 | Erase button tap dispatches onErase | VERIFIED | NumberPadTest: erase dispatch test passes |
| 18 | Fill/Pencil toggle buttons call onToggleMode only when switching modes | VERIFIED | ControlsRowTest: 4 mode-toggle tests pass (no-op on active, fires on inactive) |
| 19 | Undo button tap dispatches onUndo | VERIFIED | ControlsRowTest: undo dispatch test passes |
| 20 | All interactive composables have a minimum touch target of 56dp | VERIFIED | ControlsRowTest: assertHeightIsAtLeast(56dp) for Fill, Pencil, Undo all pass; NumberPadTest: 56dp for digit and erase |

**Score: 17/20 truths verified** (2 failed/partial, 1 additional human verification needed)

## Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/build.gradle.kts` | MMD library on compile classpath | PARTIAL | `libs.mmd` commented out; `implementation(libs.mmd)` blocked by inaccessible repos; local stubs are compiled instead |
| `app/src/main/java/com/mudita/sudoku/MainActivity.kt` | ComponentActivity with ThemeMMD + GameScreen | VERIFIED | Extends ComponentActivity, setContent { ThemeMMD { GameScreen() } }, 19 lines, substantive |
| `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` | eraseCell() public method | VERIFIED | eraseCell() present at line 123 with guards, undo-push, board/pencilMark clearing |
| `app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt` | Root screen composable; ViewModel wiring; layout Column | VERIFIED | 140 lines; collectAsStateWithLifecycle; full Column layout; wired to GameGrid/ControlsRow/NumberPad |
| `app/src/main/java/com/mudita/sudoku/ui/game/GameGrid.kt` | Canvas-based 9x9 grid; CellData model; drawGrid DrawScope extension | VERIFIED | 239 lines; single Canvas composable; CellData@Stable; 4 DrawScope extensions |
| `app/src/main/java/com/mudita/sudoku/ui/game/NumberPad.kt` | Row of 10 ButtonMMD items (1-9 + Erase) | VERIFIED | 50 lines; Row of 10 ButtonMMD+TextMMD; sizeIn(minHeight=56dp) on all |
| `app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt` | Fill/Pencil mode toggle + Undo button row | VERIFIED | 89 lines; Box+clickable(indication=null) for toggles; ButtonMMD for Undo; all sizeIn(minHeight=56dp) |
| `app/src/main/java/com/mudita/mmd/MmdComponents.kt` | Not in plan (gap remediation artifact) | WARNING | Local stub substituting real MMD library; not E-ink optimized; present in MAIN source set — must be deleted when real MMD AAR becomes accessible |
| `app/src/test/java/com/mudita/sudoku/ui/game/GameGridTest.kt` | Robolectric Compose tests; min_lines: 40 | VERIFIED | 181 lines; 6 tests; @RunWith(RobolectricTestRunner) @Config(sdk=[31]) |
| `app/src/test/java/com/mudita/sudoku/ui/game/NumberPadTest.kt` | Robolectric Compose tests; min_lines: 30 | VERIFIED | 143 lines; 8 tests |
| `app/src/test/java/com/mudita/sudoku/ui/game/ControlsRowTest.kt` | Robolectric Compose tests; min_lines: 30 | VERIFIED | 164 lines; 8 tests |
| `app/src/test/java/com/mudita/sudoku/ui/game/GameScreenTest.kt` | Integration smoke test; min_lines: 20 | VERIFIED | 79 lines; 2 tests; fake puzzle generator; assertIsDisplayed on number pad |

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| app/build.gradle.kts | gradle/libs.versions.toml | libs.mmd alias | PARTIAL | `libs.mmd` alias exists in toml but `compileOnly(libs.mmd)` is commented out in build.gradle.kts |
| MainActivity.kt | ThemeMMD | setContent { ThemeMMD { ... } } | VERIFIED | Import `com.mudita.mmd.ThemeMMD`; ThemeMMD { GameScreen() } present — resolves to stub |
| GameScreen.kt | GameViewModel | viewModel() + collectAsStateWithLifecycle() | VERIFIED | Line 35: `viewModel: GameViewModel = viewModel()`; line 37: `collectAsStateWithLifecycle()` |
| GameScreen.kt | GameGrid.kt | board, givenMask, selectedCellIndex, pencilMarks, solution props | VERIFIED | All 6 props passed at GameScreen lines 73-83; onCellClick = viewModel::selectCell |
| GameScreen.kt | NumberPad.kt | onDigitClick = viewModel::enterDigit, onErase = viewModel::eraseCell | VERIFIED | Lines 98-99: `onDigitClick = viewModel::enterDigit, onErase = viewModel::eraseCell` |
| GameGrid.kt | Canvas DrawScope | Canvas + drawGrid extension + rememberTextMeasurer | VERIFIED | rememberTextMeasurer() at line 73; Canvas composable at line 107; 4 DrawScope extensions |
| GameGridTest.kt | GameGrid composable | createComposeRule + onRoot().performTouchInput | VERIFIED | performTouchInput { click(Offset(...)) } present in all 3 tap-index tests |
| NumberPadTest.kt | NumberPad composable | onNodeWithText("5").performClick() | VERIFIED | performClick() used for digits 1, 5, 9 and erase |

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| GameScreen.kt | uiState | GameViewModel._uiState (StateFlow) | Yes — ViewModel.startGame() calls SudokuGenerator.generatePuzzle() which queries Sudoklify | FLOWING |
| GameGrid.kt | board, pencilMarks, selectedCellIndex | Passed as props from GameScreen via uiState | Yes — props flow from ViewModel state, not hardcoded | FLOWING |
| NumberPad.kt | onDigitClick, onErase | Passed as lambdas from GameScreen | Yes — wired to viewModel::enterDigit and viewModel::eraseCell | FLOWING |
| ControlsRow.kt | inputMode | uiState.inputMode from ViewModel | Yes — flows from ViewModel toggleInputMode() | FLOWING |

## Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| All 119 tests pass (including 24 new UI tests) | ./gradlew.bat :app:testDebugUnitTest | 119 tests, 0 failures, 0 errors, BUILD SUCCESSFUL | PASS |
| UI game tests specifically pass | Filter to com.mudita.sudoku.ui.game.* | BUILD SUCCESSFUL | PASS |
| Tap cell 0 dispatches index 0 | GameGridTest.tapping top-left cell | assertEquals(0, clicked) — PASS | PASS |
| Tap cell 80 dispatches index 80 | GameGridTest.tapping bottom-right cell | assertEquals(80, clicked) — PASS | PASS |
| Mode toggle no-op when active | ControlsRowTest.in FILL mode tapping Fill | assertEquals(0, toggleCount) — PASS | PASS |
| APK build / compilation | ./gradlew.bat :app:compileDebugKotlin | BUILD SUCCESSFUL (all MMD refs resolve to stubs) | PASS |

## Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|---------|
| UI-01 | 03-01, 03-02, 03-03 | All UI components built using the MMD library wrapped in ThemeMMD | PARTIAL | ThemeMMD wraps all content in MainActivity and tests; ButtonMMD/TextMMD used throughout; however real MMD library is inaccessible — stubs from MmdComponents.kt serve as the implementation, which lack E-ink optimizations |
| UI-02 | 03-01, 03-02, 03-03 | No animations, ripple effects, or transitions anywhere | PARTIAL | No Animated* composable or rememberRipple in production UI; ControlsRow Fill/Pencil use indication=null; however stub ButtonMMD uses clickable without indication=null — ripple risk in stub-active builds |
| UI-03 | 03-02, 03-03 | All interactive touch targets minimum 56dp | SATISFIED | sizeIn(minHeight=56dp) on all 10 NumberPad buttons and all 3 ControlsRow elements; confirmed by 5 assertHeightIsAtLeast(56dp) tests |

**Orphaned requirements check:** REQUIREMENTS.md traceability table maps UI-01, UI-02, UI-03 to Phase 3 — all three are claimed in plan frontmatter. No orphaned requirements.

## Anti-Patterns Found

| File | Location | Pattern | Severity | Impact |
|------|----------|---------|----------|--------|
| `app/src/main/java/com/mudita/mmd/MmdComponents.kt` | Line 46 | `Box(modifier = modifier.clickable(onClick = onClick))` — no indication=null | Warning | ButtonMMD stub used in NumberPad and ControlsRow Undo; ripple will appear in stub-active builds (not production with real MMD) |
| `app/src/main/java/com/mudita/sudoku/ui/game/GameScreen.kt` | Line 44 | `// TODO Phase 5: navigate to completion/score screen` | Info | Planned gap — completion navigation not yet implemented; correctly deferred to Phase 5 |
| `app/build.gradle.kts` | Lines 87-95 | `// compileOnly(libs.mmd)` commented out | Blocker (deploy-time) | App cannot be deployed to real device without real MMD AAR; stubs block production use |

**Stub classification note:** The `MmdComponents.kt` stubs are explicitly labeled as dev/test scaffolding and are not hollow in the sense of hiding data — they provide real interaction behavior (clickable, sized, labeled). However, they substitute a required third-party library for which access has not been established, which affects UI-01 and UI-02 compliance on the actual device.

## Human Verification Required

### 1. Real MMD Library Integration

**Test:** Obtain GitHub Packages credentials for `maven.pkg.github.com/mudita/MMD` (a GitHub token with `read:packages` scope), configure in `~/.gradle/gradle.properties` as `githubToken=...`, update `settings.gradle.kts` credentials block, switch `build.gradle.kts` back to `implementation(libs.mmd)`, delete `MmdComponents.kt`, and build.
**Expected:** APK compiles and links against the real MMD 1.0.1 AAR. ThemeMMD provides the eInkColorScheme (monochromatic). ButtonMMD is ripple-free by design. TextMMD uses E-ink tuned typography. UI-01 and UI-02 are fully satisfied.
**Why human:** Requires credential access to Mudita's private Maven repository that is not available in the automated build environment.

### 2. Visual Rendering on Device/Emulator

**Test:** Install the APK (once MMD resolves) on a Mudita Kompakt device or compatible emulator, start the game, and visually inspect.
**Expected:** 9x9 grid renders with thick box borders between 3x3 sub-grids and thin cell borders; selected cell highlights solid black with white digit; given cells appear bold; player-entered digits appear regular weight; loading state shows static "Generating puzzle..." text with no spinner; no ripple visible on any button tap.
**Why human:** Canvas drawing correctness (border widths, text alignment, pencil mark position), E-ink display artifact absence, and visual distinction between given/player cells require physical inspection.

## Gaps Summary

Two gaps block full goal achievement:

**Gap 1 — MMD library not resolvable (root cause of UI-01 partial):** The real Mudita MMD library is inaccessible from this environment. Both known distribution endpoints require authentication that was not available. As a pragmatic workaround, local Material3-backed stubs were placed in the main source set. The architecture is correct — all production code imports from `com.mudita.mmd.*` and the stubs provide the exact API surface. Swapping stubs for the real AAR requires only credential setup and a dependency declaration change. The game is end-to-end playable with the stubs (119 tests pass), but cannot be deployed to device.

**Gap 2 — Stub ButtonMMD uses clickable with ripple (UI-02 partial):** The stub `ButtonMMD` uses `Box + clickable` without `indication=null`. This means button taps show Material3 ripple in the stub-active build. This is a dev-stub concern only — the real MMD ButtonMMD is documented to be ripple-free by design. The gap is resolved once the real MMD AAR replaces the stubs. As a low-effort fix, `indication=null` can also be added to the stub to maintain UI-02 compliance during development.

Both gaps share a single root cause: the MMD AAR is inaccessible without credentials. Resolving that one blocker closes both gaps.

---

_Verified: 2026-03-24_
_Verifier: Claude (gsd-verifier)_
