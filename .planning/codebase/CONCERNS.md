# Codebase Concerns

> Last updated: 2026-03-24

---

## Technical Debt

### PUZZ-02 Medium Technique Tier: Requirement Fidelity Gap

- **Issue:** `REQUIREMENTS.md` marks PUZZ-02 complete (`[x]`) with the text "Medium requires hidden pairs/pencil marks." The implementation uses `NAKED_SINGLES_ONLY` for Medium because Sudoklify's preset schemas produce zero `HIDDEN_PAIRS`-tier puzzles empirically. Medium is differentiated from Easy by given-cell count only (27–35 vs 36–45), not by solving technique.
- **Files:** `app/src/main/java/com/mudita/sudoku/puzzle/model/DifficultyConfig.kt` line 21 (comment documents workaround), `.planning/REQUIREMENTS.md` (PUZZ-02 still states hidden pairs for Medium)
- **Impact:** A player selecting Medium will not encounter puzzles requiring hidden-singles reasoning. The difficulty curve between Easy and Medium is cosmetic (fewer givens), not qualitative. Undermines the core difficulty differentiation design.
- **Fix approach:** Either (a) update `REQUIREMENTS.md` PUZZ-02 to formally accept count-only differentiation for Medium, or (b) replace the Sudoklify MEDIUM preset with an alternative puzzle source (e.g. QQWing port) that demonstrably produces HIDDEN_PAIRS-tier boards. This is a product decision. Referenced by `.planning/phases/01-puzzle-engine/01-VERIFICATION.md` gap entry.

### `SudokuGenerator.tryGenerateCandidate` Swallows All Exceptions

- **Issue:** `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt` lines 115–117: `catch (e: Exception) { null }` catches every exception from Sudoklify including legitimate programming errors and resource exhaustion. The rejection reason surfaced to `PuzzleGenerationException` after 50 failed attempts reads only "Sudoklify generation returned null," giving no diagnostic information.
- **Files:** `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt` lines 115–117
- **Impact:** Generation bugs silently retry 50 times before failing. Real errors are invisible during development. If Sudoklify throws a programming exception due to a bad API call, it is masked.
- **Fix approach:** Narrow the catch to known Sudoklify exception types, or at minimum log the exception type (and class name) before discarding it so the rejection reason is diagnostic.

### `SudoklifyArchitect` Instantiated Inside the Retry Loop

- **Issue:** `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt` line 92 constructs a new `SudoklifyArchitect { loadPresetSchemas() }` on every retry attempt (up to 50 times). `loadPresetSchemas()` likely parses and loads preset data on each call.
- **Files:** `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt` lines 92–100
- **Impact:** Measurable overhead on the Helio A22 CPU during puzzle generation, especially for Hard puzzles with potentially many retry attempts. If schema loading is not trivially cached by the library, this compounds latency and risks the 2-second on-device generation budget.
- **Fix approach:** Move `SudoklifyArchitect` construction outside the `repeat` loop (or hoist to a lazy property), then construct a fresh spec inside the loop using the same architect instance.

### `DifficultyClassifier.allUnits()` Rebuilt on Every Call

- **Issue:** `DifficultyClassifier.allUnits()` at `app/src/main/java/com/mudita/sudoku/puzzle/engine/DifficultyClassifier.kt` line 116 builds and returns a new list of 27 unit index lists every time it is called. It is called inside `applyHiddenSingles` which runs in a loop per classification pass. The 27-unit structure is a constant.
- **Files:** `app/src/main/java/com/mudita/sudoku/puzzle/engine/DifficultyClassifier.kt` lines 116–127
- **Impact:** Minor CPU waste per generation call. Compounds across the retry loop. Trivially avoidable.
- **Fix approach:** Hoist to a companion object `val` computed once: `companion object { private val ALL_UNITS: List<List<Int>> = buildUnits() }`.

### `SudokuValidator` Class Is a Redundant Wrapper

- **Issue:** `SudokuValidator` at `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuValidator.kt` lines 26–29 is a class whose only method delegates to the top-level `isValidPlacement` function in the same file. The class adds no behaviour and no state.
- **Files:** `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuValidator.kt` lines 26–29
- **Impact:** Low. Minor contributor confusion. No correctness risk.
- **Fix approach:** Remove the class and update `SudokuValidatorTest` to call the top-level function directly.

### `PuzzleGenerationException` Not Caught in `GameViewModel.startGame`

- **Issue:** `GameViewModel.startGame` at `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` lines 55–73 launches a coroutine that calls `generatePuzzle(difficulty)`. If `SudokuGenerator.generatePuzzle` exhausts `maxAttempts`, it throws `PuzzleGenerationException`. This exception is not caught. The `viewModelScope` coroutine crashes silently; `uiState` remains at `isLoading = true` indefinitely. The player sees a frozen loading screen with no recovery path.
- **Files:** `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt` lines 57–73
- **Impact:** Frozen UI on generation failure. Rare in normal use (50 attempts is generous) but not impossible under unusual conditions (e.g. Sudoklify regression, API changes).
- **Fix approach:** Wrap `generatePuzzle(difficulty)` in a `try/catch (e: PuzzleGenerationException)` block inside `startGame`. On failure, emit a new `GameEvent.GenerationFailed` and reset `isLoading = false` so the UI can surface an error state.

### No Erase Action in `GameAction`

- **Issue:** `GameAction` at `app/src/main/java/com/mudita/sudoku/game/model/GameAction.kt` has `FillCell` and `SetPencilMark` variants. Phase 3 requires an Erase button (decision D-06 in `.planning/phases/03-core-game-ui/03-CONTEXT.md`) that clears a cell's digit or pencil marks. No `EraseCell` action variant exists, meaning undo of an erase has no representation.
- **Files:** `app/src/main/java/com/mudita/sudoku/game/model/GameAction.kt`, `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt`
- **Impact:** If Phase 3 implements erase without extending `GameAction`, undo support will be incomplete or inconsistent with the existing undo contract.
- **Fix approach:** Add `EraseCell(cellIndex: Int, previousValue: Int, previousPencilMarks: Set<Int>)` to `GameAction` in Phase 3 before implementing the erase flow in the ViewModel.

### `MainActivity` Has Empty `onCreate` and Uses `android.app.Activity`

- **Issue:** `app/src/main/java/com/mudita/sudoku/MainActivity.kt` extends `android.app.Activity` (not `ComponentActivity`) and has an empty `onCreate` body. No `setContent {}` call, no ViewModel wiring, no composable tree.
- **Files:** `app/src/main/java/com/mudita/sudoku/MainActivity.kt`
- **Impact:** App is not runnable end-to-end. This is a known planned gap (decision D-10 in `03-CONTEXT.md`) for Phase 3.
- **Fix approach:** Phase 3 step 0: upgrade `MainActivity` to `ComponentActivity` with `setContent { ThemeMMD { GameScreen() } }`.

### MMD Library Not Added to `app/build.gradle.kts`

- **Issue:** `app/build.gradle.kts` has no `implementation(libs.mmd)` dependency. No MMD `ThemeMMD`, `ButtonMMD`, or `TextMMD` can be used until this is declared.
- **Files:** `app/build.gradle.kts`, `gradle/libs.versions.toml`
- **Impact:** Blocking for all Phase 3 UI work. Must be the first step of Phase 3.
- **Fix approach:** Add MMD 1.0.1 to `libs.versions.toml` if not already present, then `implementation(libs.mmd)` in `app/build.gradle.kts`. Confirm `minSdk` compatibility.

### `isMinifyEnabled = false` in Release Build

- **Issue:** `app/build.gradle.kts` line 25 disables ProGuard/R8 shrinking in the release build type. The `proguardFiles` line is present but has no effect.
- **Files:** `app/build.gradle.kts` lines 23–28
- **Impact:** Release APK will be larger than necessary, including all dependency bytecode. Low priority, but storage on MuditaOS K flash is a potential constraint.
- **Fix approach:** Enable `isMinifyEnabled = true` and define keep rules before shipping. Defer until near release.

---

## Risks

### E-ink Ghosting on Physical Hardware (Critical, Unvalidated)

- **Risk:** The entire Phase 3 rendering approach has not been tested on a physical Mudita Kompakt. E-ink ghosting from partial refreshes (Pitfall 2 in `.planning/research/PITFALLS.md`) is the most severe rendering risk. Solid black selected-cell fill (D-02) and no animations are the planned mitigations, but neither has been validated.
- **Files:** All Phase 3 composables (not yet written)
- **Impact:** If ghosting is severe after several number placements, the game board becomes visually corrupted and unreadable. The app fails its core purpose.
- **Current mitigation:** `ThemeMMD` disables ripple by default. No `AnimatedVisibility` or `animateColorAsState` planned. Explicit solid backgrounds required per Pitfall 2 guidance.
- **Recommendation:** Validate on physical hardware after the first grid composable renders — before layering pencil marks, mode toggle, or any transitions.

### Whole-Grid Recomposition on Cell Interaction (High, Unvalidated)

- **Risk:** If `GameUiState.board` (an `IntArray`) is passed wholesale to the grid composable without cell-level stability, every cell tap or digit entry triggers recomposition of all 81 cell composables. On E-ink this manifests as a full-panel flicker per interaction, making the game unplayable. Documented as Pitfall 1 in `.planning/research/PITFALLS.md`.
- **Files:** Phase 3 game screen composable (not yet written)
- **Impact:** Game is unplayable on target device.
- **Current mitigation:** Planned but not implemented. `STATE.md` documents the required approach: `@Stable` cell data classes + `key(cellIndex)` in the grid loop + `derivedStateOf` for aggregate reads.
- **Recommendation:** Implement `@Stable` cell model from the first grid composable. Verify recomposition counts in Android Studio Layout Inspector before Phase 3 is closed.

### Sudoklify Beta Dependency with `@ExperimentalSudoklifyApi`

- **Risk:** `dev.teogor.sudoklify:sudoklify-core:1.0.0-beta04` is beta. The `@OptIn(ExperimentalSudoklifyApi::class)` annotation at `SudokuGenerator.kt` line 28 signals the API surface used is explicitly experimental. The DSL (`constructSudoku {}`, `loadPresetSchemas`, `generateGridWithGivens`, `toSeed`) changed once already between the research phase and implementation.
- **Files:** `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt` lines 7–14, 92–113
- **Current mitigation:** All Sudoklify calls are isolated to `SudokuGenerator.tryGenerateCandidate`. Any breaking API change requires updating only that private function.
- **Recommendation:** Pin at `1.0.0-beta04` in `gradle/libs.versions.toml`. Do not auto-upgrade. Monitor Sudoklify releases.

### Performance on Helio A22 Not Validated

- **Risk:** The JVM proxy timing tests in `SudokuEngineIntegrationTest.kt` assert generation completes under 2000ms on the development JVM. These are not reliable predictors of Helio A22 CPU performance. The comment at line 100 explicitly notes intermittent failure risk.
- **Files:** `app/src/test/java/com/mudita/sudoku/puzzle/SudokuEngineIntegrationTest.kt` lines 85–104
- **Impact:** If Hard puzzle generation exceeds 2 seconds on device, the player sees an extended loading spinner on an E-ink display that cannot show a progress animation.
- **Recommendation:** Validate on physical Mudita Kompakt before Phase 3 closes. If timing is marginal, implement background pre-generation (generate next puzzle while player solves current one).

### Game State Lost on Process Death Until Phase 4

- **Risk:** All game state lives in `GameViewModel` memory. The MuditaOS K low-memory killer can terminate the process mid-game. Player loses all progress. `DataStore` persistence (STATE-01/02/03) is Phase 4.
- **Files:** `app/src/main/java/com/mudita/sudoku/game/GameViewModel.kt`
- **Impact:** Progress loss on long Hard puzzles. Known planned gap per roadmap.
- **Current mitigation:** None until Phase 4. Phase 4 must use `applicationScope` (not `viewModelScope`) for DataStore writes per Pitfall 6 in `.planning/research/PITFALLS.md`.

### Scoring Can Go Negative (Phase 5 Risk)

- **Risk:** Scoring formula (Phase 5) subtracts penalties per error and hint. Without a zero floor, large error counts or many hints yield a negative final score. Documented in `.planning/research/PITFALLS.md` Pitfall 5.
- **Impact:** Confusing UX. Leaderboard sort order breaks for negative entries.
- **Recommendation:** Phase 5 must apply `finalScore = max(0, baseScore - errorPenalties - hintPenalties)`. Include a unit test with maximal penalties (81 errors, 20 hints).

### Duplicate High Score Entries on Double-Navigation

- **Risk:** If the completion event triggers the summary screen and the user navigates back through it twice (back-stack bug or double-tap), the high score could be saved twice. Documented in `.planning/research/PITFALLS.md` Pitfall 10.
- **Recommendation:** Phase 5 must guard score saves with a one-shot `COMPLETED` state flag on the ViewModel. `GameEvent.Completed` already has `replay = 0` on the SharedFlow, but the ViewModel should transition to a terminal `isComplete = true` state that ignores further completion triggers.

### MMD Library `minSdk` Compatibility Unconfirmed

- **Risk:** MMD 1.0.1 is confirmed on GitHub but internal build requirements (exact `minSdk`, full component list) were not extractable from repository build files at research time. A hidden `minSdk > 31` in MMD's manifest would block installation on the Kompakt.
- **Recommendation:** Confirmed only at first successful device build. Make MMD integration the very first Phase 3 task so this risk is discovered early.

### `Array<Set<Int>>` in `GameUiState` Requires Custom Serializer for Phase 4

- **Risk:** `GameUiState.pencilMarks` is `Array<Set<Int>>` at `app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt` line 32. `kotlinx.serialization` has no default serializer for this type. Phase 4 DataStore persistence requires JSON serialization of the full `GameUiState`. A custom `KSerializer<Array<Set<Int>>>` must be written.
- **Files:** `app/src/main/java/com/mudita/sudoku/game/model/GameUiState.kt` line 32
- **Impact:** Blocking for Phase 4 if not planned for. A naively annotated `@Serializable` on `GameUiState` will fail to compile.
- **Recommendation:** Design and implement the `pencilMarks` serializer as the first task of Phase 4.

---

## Known Issues

### `GameViewModelTest` Completion Test Uses Fragile Polling Loop

- **Symptom:** `GameViewModelTest.kt` lines 876–880 use a manual `while (...) { delay(10) }` polling loop to wait for puzzle generation before filling cells for the completion event test. The loop cap is 100 iterations (1000ms total). Since `FakeGenerator` is synchronous, the loop exits on iteration 1 in practice.
- **Files:** `app/src/test/java/com/mudita/sudoku/game/GameViewModelTest.kt` lines 874–880
- **Trigger:** `completing all cells emits GameEvent Completed with correct errorCount` test
- **Risk:** If `FakeGenerator` is ever replaced or the test is adapted for a slow generator, the loop becomes a race condition. If generation takes > 1 second, the test proceeds with an all-zeros board and the fills are no-ops, producing a false-positive or unexpected assertion failure.
- **Fix approach:** Refactor to use `uiState.test { ... }` with `awaitItem()` in the Turbine block before filling cells, consistent with all other tests in the file.

### REQUIREMENTS.md PUZZ-02 `[x]` Status Misrepresents Implementation

- **Symptom:** PUZZ-02 is marked complete but the Medium technique clause (hidden pairs) is not satisfied. The Phase 1 verification report documents this explicitly as a partial gap.
- **Files:** `.planning/REQUIREMENTS.md`, `.planning/phases/01-puzzle-engine/01-VERIFICATION.md` (gap section)
- **Workaround:** A code comment in `DifficultyConfig.kt` line 17–20 documents the known limitation.

### ROADMAP Phase 2 Plan 03 Still Marked Incomplete

- **Symptom:** `.planning/ROADMAP.md` shows `02-03-PLAN.md` as `[ ]` incomplete, but the plan was executed and committed (commit `59d5f2a`). This is a documentation artifact — the code and git history are authoritative.
- **Files:** `.planning/ROADMAP.md`
- **Workaround:** `02-03-SUMMARY.md` and `02-VERIFICATION.md` both confirm completion. STATE.md records Phase 2 completion.

---

## Gaps

### No UI Layer Exists (Phase 3 Not Started)

The app has no renderable screen. `MainActivity` is empty, no game board composable, number pad, mode toggle, undo button, loading state, or difficulty selector exist. MMD is not yet a dependency.

- **Missing directories:** `app/src/main/java/com/mudita/sudoku/ui/` — does not exist
- **Blocks:** All user-facing feature validation and manual testing

### No Persistence Layer (Phase 4 Not Started)

`DataStore` is declared in `app/build.gradle.kts` (line 69) and `kotlinx.serialization` is declared (line 66) but neither is used. No `@Serializable` annotations on any domain model. No DataStore repository, no `GameState` JSON encoding, no pause/resume flow.

- **Blocks:** STATE-01 (pause), STATE-02 (resume prompt), STATE-03 (resume game)

### No Hint System (Phase 5 Not Started)

`GameViewModel` has no `hint()` method. `GameUiState` has no `hintCount` field. `GameAction` has no `HintRevealed` variant. Hint candidates must exclude already-correct cells (Pitfall 11 in `PITFALLS.md`).

- **Blocks:** SCORE-03, SCORE-04

### No Scoring Summary or High Score Storage (Phase 5 Not Started)

`GameEvent.Completed` carries `errorCount` but there is no score computation, summary screen, or DataStore write for high scores.

- **Blocks:** SCORE-05, SCORE-06, HS-01, HS-02, HS-03

### No Navigation Graph or Main Menu (Phase 6 Not Started)

Single empty Activity, no Compose Navigation dependency, no difficulty selection screen, no leaderboard screen.

- **Blocks:** NAV-01

### No Instrumented / Compose UI Tests Written

`app/build.gradle.kts` includes `androidTestImplementation(libs.compose.ui.test.junit4)` (line 83) but `app/src/androidTest/` does not exist. All tests are JVM unit tests. Phase 3 UI validation depends entirely on manual device testing and Layout Inspector inspection.

### No Logging Anywhere in Production Code

Neither `android.util.Log` nor any structured logging framework is used in any production file. On-device debugging of E-ink rendering issues and generation failures will have no log output to inspect.

---

## Recommendations

1. **Resolve PUZZ-02 Medium technique gap (product decision before Phase 3):** Either update `REQUIREMENTS.md` to formally accept count-only Medium differentiation, or add a pre-Phase 3 task to source hidden-pairs puzzles from a different generator. Leaving `[x]` with an unmet spec creates confusion.

2. **Add `try/catch` to `GameViewModel.startGame` (Phase 3 task 0):** Wrap `generatePuzzle(difficulty)` in a `try/catch (e: PuzzleGenerationException)` block and emit a `GameEvent.GenerationFailed` so the UI can recover from a frozen loading state.

3. **Add MMD to `app/build.gradle.kts` as the first Phase 3 task:** Phase 3 cannot begin without `implementation(libs.mmd)`. Making it task 0 discovers MMD `minSdk` compatibility issues immediately.

4. **Validate on physical Mudita Kompakt early in Phase 3:** Render the basic grid composable and verify: (a) no whole-grid recomposition, (b) no ghosting after 10 fills, (c) touch accuracy at arm's length. Do not build out pencil marks or the number pad before this passes.

5. **Hoist `DifficultyClassifier.allUnits()` to a companion object:** Two-line change. Eliminates repeated list construction across the retry loop. Zero risk.

6. **Add `GameAction.EraseCell` before Phase 3 implements the Erase button:** Ensures undo support for erase is complete from the start. Prevents an incomplete undo contract.

7. **Plan `@Serializable` for `GameUiState.pencilMarks` in Phase 4 task 0:** `Array<Set<Int>>` needs a custom `KSerializer`. Design this before starting any DataStore integration work to avoid a blocking mid-phase discovery.

8. **Apply `max(0, score)` floor in Phase 5 scoring:** Add a unit test with pathological inputs (81 errors, 20 hints, Hard difficulty) before the completion flow is considered done.

9. **Refactor the polling loop in `GameViewModelTest` completion test:** Replace `while (...) { delay(10) }` at lines 876–880 with Turbine `uiState.test { awaitItem() }` to eliminate the fragile timing dependency.

---

*Concerns audit: 2026-03-24*
