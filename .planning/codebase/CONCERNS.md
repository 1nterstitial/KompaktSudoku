# Codebase Concerns

**Analysis Date:** 2026-03-25

## Tech Debt

**`Json.decodeFromString` DEPRECATION suppressed:**
- `DataStoreGameRepository.kt` uses `@Suppress("DEPRECATION")` on `Json.decodeFromString<PersistedGameState>(json)`
- Impact: Low — works correctly at runtime. Risk grows if the deprecated overload is removed in a future kotlinx.serialization release
- Fix: Replace with the inline reified overload and remove the `@Suppress`

**`@Suppress("UNCHECKED_CAST")` on ViewModel factory:**
- `MainActivity.kt` uses a manual `ViewModelProvider.Factory` with `return GameViewModel(...) as T`
- Impact: Low — safe given only one ViewModel type. Would be a footgun if a second ViewModel is added without a `modelClass.isAssignableFrom` guard
- Fix: Introduce Hilt or a proper factory with a type check when a second ViewModel is needed

**Stale KDoc "not yet" strings in GameViewModel:**
- `GameViewModel.kt` lines 124 and 258 contain "not yet" in KDoc from an earlier draft
- Impact: Cosmetic only — no functional impact, but may confuse future maintainers
- Fix: Search-replace in a documentation cleanup pass

**MEDIUM difficulty uses `NAKED_SINGLES_ONLY` technique tier (not `HIDDEN_PAIRS`):**
- `DifficultyConfig.kt` notes that Sudoklify's preset schemas do not empirically produce HIDDEN_PAIRS-tier puzzles
- `MEDIUM_CONFIG` is differentiated from `EASY_CONFIG` only by given-cell count (27–35 vs 36–45), not by solving technique
- Impact: Medium — the difficulty curve between Easy and Medium is flatter than intended. HARD correctly requires ADVANCED techniques
- Fix: Investigate if Sudoklify stable 1.0.0 improves this, or accept as a v1.0 limitation (documented in source)

**`NoOpGameRepository` and `NoOpScoreRepository` are default ViewModel parameters:**
- A ViewModel created by Compose's `viewModel()` without a factory would silently use no-ops with no error
- Impact: Low at v1.0 — `MainActivity` always provides a factory. Would become a hidden footgun if a second entry point creates the ViewModel without a factory
- Fix: In v2.0, make `repository` and `scoreRepository` required constructor parameters (no defaults)

## Known Bugs

**None confirmed.** All 26 requirements verified SATISFIED at v1.0 completion. No user-reported bugs.

## Security Considerations

**No network, no credentials, no backend — attack surface is minimal.**
- DataStore files (`game_state`, `score_state`) are in the app's private data directory; readable on a rooted device
- Current mitigation: None — local game with no server-side score validation; tampering has no impact beyond the local device
- Recommendation: Not applicable for v1.0. If cross-device leaderboards are added, server-side validation would be required

## Performance Bottlenecks

**Puzzle generation latency on Helio A22 (unverified):**
- `SudokuGenerator.generatePuzzle()` runs a 3-gate acceptance loop (uniqueness backtracking + given-count + technique classification) with up to 50 attempts
- On JVM it completes under 2000ms per `SudokuEngineIntegrationTest`. Actual timing on Helio A22 has not been measured
- Improvement path: If latency is unacceptable on device, pre-generate one puzzle per difficulty in a background coroutine during `MenuScreen` display and store in a `PuzzleCache` singleton

**`generatePuzzle` is blocking, wrapped in a suspend lambda:**
- `SudokuGenerator.generatePuzzle()` is a plain (non-suspend) function; `GameViewModel` wraps it in `withContext(Dispatchers.Default)`
- This works correctly but the `suspend` wrapper is semantically misleading
- Fix: Either declare `generatePuzzle` as `suspend` inside `SudokuGenerator`, or add a comment. Low priority

**GameGrid recomputes all 81 `CellData` objects on every board change:**
- `remember(board, givenMask, selectedCellIndex, pencilMarks)` invalidates the entire 81-element list on any single cell change
- Cause: `IntArray`/`BooleanArray` use referential equality for `remember` keys
- Impact: Negligible for 81 cells on any hardware — known trade-off documented in Phase 3
- Fix if needed: Convert to `SnapshotStateList<Int>` for fine-grained change detection

**`refreshLeaderboard()` issues 3 sequential DataStore reads per call:**
- `GameViewModel.refreshLeaderboard()` calls `scoreRepository.getBestScore()` three times sequentially (EASY, MEDIUM, HARD)
- Impact: Negligible for v1.0 (3 in-memory preference reads)
- Fix if needed: Add `getAllScores(): Map<Difficulty, Int?>` backed by a single `dataStore.data.first()`

## Fragile Areas

**`completionResult!!` non-null assertion in MainActivity:**
- `MainActivity.kt` line 130: `completionResult` is null-asserted when rendering `Screen.SUMMARY`
- Safe because `completionResult` is always set before `currentScreen = Screen.SUMMARY` in the same recomposition batch
- Risk: A future refactor that introduces an async gap between the two assignments would cause a `NullPointerException` at runtime
- Mitigation: Always set `completionResult = result` before `currentScreen = Screen.SUMMARY` in any code path. The ordering is documented with a `CRITICAL` comment in `MainActivity`

**Screen enum routing does not support back stack:**
- Navigation is a `var currentScreen by remember { mutableStateOf(Screen.MENU) }` — no back stack
- Back-press on `Screen.LEADERBOARD` reached via `Screen.SUMMARY` exits the app, not returns to Summary
- `BackHandler` is only installed on `GameScreen` and `SummaryScreen`
- Mitigation: Any new screen added to the `Screen` enum must add its own `BackHandler` if non-exit back-press behavior is needed

**`pendingSavedState` is a nullable ViewModel field with no concurrency guard:**
- Set in a `viewModelScope.launch` coroutine during `init`; read in `resumeGame()` / `startNewGame()` on the main thread
- Safe in practice because the init coroutine writes before any UI exists, and the UI cannot read until `showResumeDialog` emits `true` (in the same init coroutine)
- Risk: No explicit synchronization (`@GuardedBy` annotation or `Mutex`); future refactoring could break the ordering assumption
- Mitigation: Do not read `pendingSavedState` outside main-thread UI callbacks. Introduce a `Mutex` if a background task ever needs access

## Scaling Limits

**High score storage bounded to 3 difficulties:**
- Adding a 4th difficulty requires a new `intPreferencesKey`, a new `Difficulty` enum value, updates to `DifficultyConfig.kt`, all exhaustive `when` expressions, and `LeaderboardScreen.kt`
- For more than 5 difficulties, consider storing scores as a JSON-encoded `Map<String, Int>` in a single DataStore string key

**Undo stack is unbounded in memory:**
- `ArrayDeque<GameAction>` in `GameViewModel` with no size cap
- Negligible for standard 9×9 Sudoku (max ~81 entries, each a few primitives)
- If larger grid sizes (16×16) are ever added, cap the undo stack (e.g., 100 entries)

## Dependencies at Risk

**Sudoklify `1.0.0-beta04` — pre-stable beta:**
- API has already changed between versions; package paths and DSL were discovered via JAR bytecode inspection rather than docs
- A stable 1.0.0 release could introduce breaking changes in `SudoklifyArchitect`, `loadPresetSchemas()`, `constructSudoku {}`, `generateGridWithGivens()`, and `rawPuzzle.solution`
- Mitigation: Monitor GitHub releases. Run `SudokuEngineIntegrationTest` (120 puzzle generations) immediately after any version bump

**MMD `1.0.1` — closed-source, GitHub Packages hosted:**
- If Mudita changes the hosting location, removes the package, or publishes a breaking release, the build fails at dependency resolution
- Every UI composable uses `ButtonMMD`, `TextMMD`, and `ThemeMMD` — entire build breaks without MMD
- Mitigation: Pin to `mmd = "1.0.1"` (no version ranges). Keep a local AAR snapshot of 1.0.1 as a fallback. Update `settings.gradle.kts` maven URL if hosting moves

**`activity = "1.8.2"` — below current stable:**
- `androidx.activity:activity-compose` pinned to 1.8.2 while current stable is 1.9.x
- Impact: Low — fully compatible with Compose BOM 2026.03.00; no deprecation issues detected
- Migration: Bump when upgrading the Compose BOM in a future milestone

## Missing Features (v1.0 Scope Gaps)

**No timer / elapsed-time tracking:**
- Score formula does not factor in time; no timed challenge mode is possible without this

**No "New Game from Summary" shortcut:**
- Player must navigate Menu → Difficulty → Game to replay; no one-tap repeat

**Undo stack cleared on pause/resume (intentional):**
- Per decision D-05, `undoStack` is not persisted to DataStore
- Player cannot undo moves made in a previous session after a background kill

**No accessibility / screen reader support:**
- `GameGrid` is a single `Canvas` — screen readers (TalkBack) cannot describe the grid, announce selected cells, or read digit values
- Fix: Add `semantics { contentDescription = ... }` or restructure to per-cell semantic composables

## Test Coverage Gaps

See `TESTING.md` — Coverage Gaps section for full details.

Summary:
- `DataStoreGameRepository` has no integration tests (only ViewModel-level coverage)
- `completionResult!!` null-safety contract in `MainActivity` has no test
- `GameGrid` error state rendering (inset border) has no visual assertion
- All 6 phases have pending device-side verifications that cannot be automated with Robolectric

---

*Concerns audit: 2026-03-25*
