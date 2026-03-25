# Concerns & Risks

**Analysis Date:** 2026-03-24
**Project State:** Phase 1 complete (puzzle engine). Phases 2–6 not started.

---

## Technical Debt

**PUZZ-02 Medium technique tier — requirement vs. implementation mismatch:**
- File: `app/src/main/java/com/mudita/sudoku/puzzle/model/DifficultyConfig.kt` line 21
- `MEDIUM_CONFIG.requiredTechniqueTier` is `NAKED_SINGLES_ONLY`, but `REQUIREMENTS.md` originally specified Medium requires hidden pairs/pencil marks. This is now partially documented.
- Root cause: Sudoklify's preset schemas produce a bimodal distribution — puzzles are either trivially easy (`NAKED_SINGLES_ONLY`) or advanced (`ADVANCED`), with nothing in the hidden-pairs middle tier.
- Current state: Medium difficulty is differentiated from Easy by given-cell count only (27–35 vs 36–45). The `DifficultyClassifier` correctly implements `HIDDEN_PAIRS` detection and it is tested — but no generated Medium puzzle requires it.
- Fix approach: Either formally accept this as the definition of Medium (update `REQUIREMENTS.md` PUZZ-02 accordingly via plan `01-05-PLAN.md`), or replace Sudoklify's Medium preset with an alternative puzzle source that produces hidden-pairs-tier puzzles. Plan `01-05-PLAN.md` already exists to close the documentation gap.

**`SudokuGenerator.tryGenerateCandidate` silently swallows all exceptions:**
- File: `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt` lines 115–117
- `catch (e: Exception) { null }` catches every possible exception from Sudoklify including `OutOfMemoryError`-wrapping exceptions, `IllegalStateException` for bad seeds, and programming errors that should be surfaced.
- Impact: Generation failures are invisible during development; legitimate bugs in the Sudoklify integration are silently retried 50 times before `PuzzleGenerationException` is thrown with the unhelpful message "Sudoklify generation returned null".
- Fix approach: Narrow the catch to known Sudoklify exception types once they are identified, or at minimum log the exception type before discarding it.

**`SudoklifyArchitect` instantiated inside the retry loop:**
- File: `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt` line 92
- A new `SudoklifyArchitect { loadPresetSchemas() }` is constructed on every attempt (up to 50 times). `loadPresetSchemas()` likely parses and loads preset data each call.
- Impact: Measurable overhead on the Helio A22 CPU if schema loading is not cheaply cached by the library; increases Hard puzzle generation time, which already risks the 2-second on-device budget.
- Fix approach: Move `SudoklifyArchitect` construction outside the `repeat` loop, or confirm via profiling that `loadPresetSchemas()` is effectively free (e.g. returns a static object).

**`MainActivity` is a stub with no Compose entry point:**
- File: `app/src/main/java/com/mudita/sudoku/MainActivity.kt`
- `onCreate` has no `setContent {}` call, no ViewModel wiring, and no composable tree. The app currently renders a blank screen on device.
- This is expected at Phase 1, but must be addressed in Phase 3.

---

## Architecture Risks

**Whole-grid recomposition on cell interaction (high severity):**
- Not yet implemented, but the risk applies from the first grid composable written in Phase 3.
- If board state is modelled as a single `IntArray` or `List<Int>` collected at the grid root, any cell update triggers recomposition of all 81 cells.
- On an E-ink display, 81 simultaneous partial redraws per tap will cause a visible full-panel flicker on every keystroke, making the game unplayable.
- Mitigation: Model each cell as its own `@Stable` data class. Use `key(cellIndex)` in any grid loop. Use `derivedStateOf` for aggregate reads like completion detection. Verify with Android Studio Layout Inspector recomposition counts before layering any other feature.
- Reference: `.planning/research/PITFALLS.md` Pitfall 1.

**`PuzzleGenerationException` has no ViewModel-level error handling yet:**
- File: `app/src/main/java/com/mudita/sudoku/puzzle/PuzzleGenerationException.kt`
- The exception is defined and thrown correctly, but no ViewModel exists yet (Phase 2). If Phase 2 neglects to catch this exception inside a coroutine, an uncaught exception will crash the app.
- Mitigation: Phase 2 ViewModel must wrap `generatePuzzle()` in a try/catch or use a `Result`-returning wrapper, and emit an error state to the UI.

**`SudokuGenerator` runs synchronously — no coroutine boundary:**
- File: `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt`
- `generatePuzzle()` is a blocking call. Hard puzzle generation may require many retry attempts due to low acceptance rates from Gate 3. On the Helio A22, worst-case runs could exceed the 2-second on-device budget if called on the main thread.
- Mitigation: Phase 2 ViewModel must call `generatePuzzle()` inside `withContext(Dispatchers.Default)` to avoid ANR. This is not yet enforced by the API surface.

**Game state persistence must use `applicationScope`, not `viewModelScope`:**
- Not yet implemented (Phase 4).
- If DataStore writes are launched in `viewModelScope`, they are cancelled when the ViewModel is destroyed (e.g., app backgrounded by OS). The player loses the last N moves before the write completes.
- Mitigation: Phase 4 must use an `ApplicationScope` tied to the `Application` object for all DataStore write operations. Reference: `.planning/research/PITFALLS.md` Pitfall 6.

**Cell model requires a `GIVEN`/`PLAYER`/`HINT` type enum from day one:**
- Not yet implemented (Phase 2).
- Without explicit cell type tracking, error counting logic risks treating pre-filled clue cells as player entries, inflating the error count and breaking score integrity.
- Mitigation: Phase 2 must define a `CellType` enum (`GIVEN`, `PLAYER`, `HINT`) on the cell domain model before any input handling is written. Reference: `.planning/research/PITFALLS.md` Pitfall 8.

**Score formula must enforce a zero floor:**
- Not yet implemented (Phase 5).
- Subtracting error and hint penalties without a floor can produce a negative final score, which is confusing and inconsistent with the mindful design ethos.
- Mitigation: Phase 5 must apply `finalScore = max(0, baseScore - errorPenalties - hintPenalties)`. Reference: `.planning/research/PITFALLS.md` Pitfall 5.

---

## Coverage Gaps

**Device performance for puzzle generation is unverified:**
- The JVM timing tests in `app/src/test/java/com/mudita/sudoku/puzzle/SudokuEngineIntegrationTest.kt` are proxy tests on a development machine. They assert `< 2000ms` but JVM timing has no direct correlation to Helio A22 performance.
- Phase 1 success criterion 5 ("generation completes in under 2 seconds on a Mudita Kompakt device") is explicitly unverified. This requires running the test on physical hardware.
- Documented in `.planning/phases/01-puzzle-engine/01-VERIFICATION.md` as a required human verification step.

**Full test suite requires human execution:**
- 28 passing tests are reported in plan summaries but the build environment (JDK, Android SDK, Gradle daemon) is not available on the planning machine. The test suite cannot be re-run to confirm no regressions without developer tooling.
- Documented in `.planning/phases/01-puzzle-engine/01-VERIFICATION.md`.

**No tests for `SudokuGenerator.pickSudoklifyDifficulty` mapping:**
- File: `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt` lines 136–141
- The `pickSudoklifyDifficulty` private function is exercised only implicitly through integration tests. The specific Sudoklify difficulty pool selected for each `Difficulty` value is not directly asserted.
- Risk: If the empirical data backing the mapping (documented in the function KDoc) changes with a Sudoklify library update, there is no unit test to catch the regression.

**No error handling tests for `tryGenerateCandidate`:**
- The `SudokuGeneratorTest` tests the `PuzzleGenerationException` path using a test double that makes `hasUniqueSolution` always return false. The path where Sudoklify itself throws an exception (and `tryGenerateCandidate` returns null) is not directly tested.

**Phases 2–6 have zero test coverage:**
- ViewModel, UI state, input handling, scoring, persistence, navigation — none of these exist yet.
- The absence is expected at this stage, but there are no placeholder test files or test infrastructure for future phases.

---

## Known Issues

**PUZZ-02 marked `[x]` complete in `REQUIREMENTS.md` while technique-tier clause is unmet:**
- File: `.planning/REQUIREMENTS.md`
- The `[x]` status on PUZZ-02 was set before the empirical Sudoklify HIDDEN_PAIRS limitation was discovered. The checkbox currently misrepresents the implementation.
- Plan `01-05-PLAN.md` exists to update the requirement text and make the `[x]` honest by redefining the scope of PUZZ-02. This plan has not been executed yet as of 2026-03-24.

**No logging anywhere in the codebase:**
- Neither `android.util.Log` nor any structured logging framework is used in any production file.
- During Phase 3+ debugging on the physical Kompakt device, absence of logs will make diagnosing E-ink rendering issues and generation failures difficult.
- Impact: Low now; grows as the app gains interactive behaviour.

---

## Phase Gaps

The following v1 requirements are fully unimplemented. Each maps to a pending phase.

**Phase 2 — Game State & Domain (not started):**
- `DIFF-01`, `DIFF-02`: Difficulty selection UI and puzzle matching
- `INPUT-01` through `INPUT-05`: Cell selection, digit fill, pencil marks, undo
- `SCORE-01`, `SCORE-02`: Silent error tracking, completion detection
- No `GameViewModel`, no `GameUiState`, no cell domain model, no input handler

**Phase 3 — Core Game UI (not started):**
- `UI-01`, `UI-02`, `UI-03`: MMD-compliant game screen, zero animations, 56dp touch targets
- `MainActivity` renders nothing — `setContent {}` is absent

**Phase 4 — Persistence (not started):**
- `STATE-01`, `STATE-02`, `STATE-03`: Pause/resume, DataStore serialisation, resume prompt
- `@Serializable` annotation not yet applied to any domain model
- No `GameRepository`, no DataStore instance, no `CorruptionHandler`

**Phase 5 — Scoring & Completion (not started):**
- `SCORE-03` through `SCORE-06`: Hints, score computation, completion summary
- `HS-01`, `HS-02`, `HS-03`: Per-difficulty high score storage, personal best detection, leaderboard

**Phase 6 — Menu & Navigation (not started):**
- `NAV-01`: Main menu, navigation graph, back stack, resume prompt integration
- No Compose Navigation dependency in build yet

---

## E-ink / Platform Risks

**E-ink ghosting from partial refreshes (critical):**
- Partial screen refreshes accumulate ghost images on E-ink panels. After 5–10 number placements the grid may show residue of prior digits, making the board unreadable.
- No mitigation is in place yet because the UI layer (Phase 3) does not exist.
- Required mitigations for Phase 3: use `ThemeMMD` exclusively, ensure every composable has an explicit solid background colour (`Modifier.background(Color.White)`), use zero animations or `AnimatedVisibility`, and investigate whether MMD exposes a waveform mode API for triggering full-panel refreshes.
- Reference: `.planning/research/PITFALLS.md` Pitfall 2.

**Touch target sizing on 800×480 E-ink display:**
- At 800px width, a standard 9-column Sudoku grid gives ~88px per cell (~10.3mm). On a normal LCD this is acceptable. On E-ink, where visual feedback is delayed 50–200ms, players cannot rely on live hover state to confirm a tap registered before the next tap.
- Android's 48dp minimum is insufficient for this display. All interactive cells and number pad buttons must be at least 56dp.
- The MMD navigation bar's extra padding (~20px) reduces available vertical space — the grid and number pad layout must account for this.
- This is a Phase 3 concern but the layout constraints must be established before the grid composable is written.
- Reference: `.planning/research/PITFALLS.md` Pitfall 4.

**MMD library version confidence is MEDIUM:**
- MMD 1.0.1 is confirmed on GitHub but internal build requirements (exact `minSdk`, `compileSdk`, full component list) were not extractable from the repository's build files at research time.
- Risk: A hidden `minSdk > 31` in MMD's manifest would block installation on the Kompakt. Confirmed only at first build attempt on a device.

**Sudoklify is beta (`1.0.0-beta04`):**
- The only Kotlin-native puzzle generation library with built-in difficulty calibration is in beta. API changes between beta versions broke the original `SudokuSpec { }` DSL (research-phase API) versus the actual `constructSudoku { }` DSL (implementation-phase API).
- A future beta or `1.0.0` release may change the API again. The Sudoklify integration is isolated to `SudokuGenerator.kt` — any breaking change requires updating only that file.
- Fallback: QQWing (used by LibreSudoku, a production Compose Sudoku app) is the documented alternative. It requires manual porting from Java but is otherwise proven at scale.

**MuditaOS K has no Google Play Services:**
- AOSP-based, de-Googled. Any library with a transitive dependency on `com.google.android.gms` will fail at runtime with `ServiceNotFoundException`.
- Current dependencies (DataStore, Compose, Kotlin coroutines, Sudoklify, MMD) have no GMS dependency.
- Risk grows as new libraries are added in Phases 2–6. Each new dependency must be audited with `./gradlew dependencies | grep gms` before committing.
- Reference: `.planning/research/PITFALLS.md` Pitfall 12.

**Ripple effects must never be re-enabled:**
- `ThemeMMD` disables ripple by default. Any `indication` override that restores ripple will cause a visible animation artifact on E-ink (rapid partial refreshes).
- No linting rule enforces this. Phase 3 code review must check every interactive composable for `indication = rememberRipple()` or similar.

**Physical device validation is required before Phase 3 can close:**
- Phase 3 success criteria include "no visible ghosting artifacts after 30+ successive cell interactions" and "all touch targets reliably activatable on physical hardware." These cannot be verified without a Mudita Kompakt device.
- Phase 3 cannot be marked complete via code inspection alone.

---

*Concerns audit: 2026-03-24*
