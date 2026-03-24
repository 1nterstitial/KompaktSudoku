# Domain Pitfalls

**Domain:** Sudoku game on Mudita Kompakt E-ink Android (MMD/Compose)
**Researched:** 2026-03-23

---

## Critical Pitfalls

Mistakes that cause rewrites, broken gameplay, or unplayable E-ink rendering.

---

### Pitfall 1: Whole-Grid Recomposition on Every Cell Interaction

**What goes wrong:** When the player taps a cell or fills in a number, the entire 9x9 grid (81 composables) recomposes rather than only the affected cells. On a normal display this causes dropped frames; on an E-ink display it causes a visible full-panel flicker on every keystroke, making the game feel broken.

**Why it happens:** The root cause is collecting the entire board state as a single `List<Int>` or `Array<Int>` at the top-level composable, then passing it down through all 81 cells. Any mutation to the list emits a new instance — even if values are structurally identical — and Compose treats the new instance as changed state, triggering recomposition for every consumer. Using mutable `var` instead of `val` in data classes further destabilises the state tree.

**Consequences:** 81x unnecessary redraws per interaction. On E-ink each draw is visible as a flash. User cannot play comfortably.

**Prevention:**
- Model each cell as its own `@Stable` data class. Pass cell-level `StateFlow<CellState>` to each cell composable, not the full board.
- Use `derivedStateOf` for any aggregate read (e.g., "is the board complete?") so it only recomputes when its inputs actually change.
- Use `key(cellIndex)` in any `LazyGrid` or manual `for` loop rendering cells.
- Verify with Android Studio Layout Inspector > Recomposition counts before considering this closed.

**Detection:** Tap any cell; if the Layout Inspector shows all 81 cells recomposing, this pitfall is active.

**Phase:** Foundation / Grid UI — address before any other feature is layered on.

---

### Pitfall 2: E-ink Ghosting from Partial Refreshes Accumulating

**What goes wrong:** E-ink panels do not erase pixel state — they physically move charged microcapsules. Partial refreshes (fast waveform mode) leave faint traces of prior content visible. After a handful of number placements the grid fills with ghost digits, making the board unreadable.

**Why it happens:** Android apps do not automatically trigger a full-panel refresh. Since Android 4.4.2, the responsibility was shifted from the OS to the app. Apps not written for E-ink — including Compose apps using default rendering — accumulate ghost images indefinitely because no full-refresh is requested. Community reports (AnkiDroid issue #16117, KOReader discussion) confirm this is the standard failure mode.

**Consequences:** The game board becomes visually corrupted within minutes. Players cannot distinguish real cell values from ghost residue.

**Prevention:**
- Use the MMD library's `ThemeMMD` and its E-ink-optimised components which disable ripple effects and prefer solid state changes over transitions.
- Do not use animations, animated transitions, or Compose `AnimatedVisibility`/`animateColorAsState` anywhere. Replace with instant state swaps.
- Ensure every composable has an explicit solid background color (`Modifier.background(Color.White)`) rather than relying on transparent stacking. Transparent composables over a non-white previous frame cause the ghost to show through.
- If the MMD library exposes a display waveform mode API (check the library source — not yet confirmed in docs), trigger a full-panel refresh after a configurable number of partial operations (the E-ink industry guidance is every ~5 consecutive partial refreshes).
- Test by filling in 10+ cells rapidly and inspecting the board under different lighting.

**Detection:** Visible ghost of a previously placed digit after another digit is placed in a nearby cell.

**Phase:** Foundation / Grid UI — must be resolved before any UX work.

---

### Pitfall 3: Puzzle Generator Producing Multiple-Solution Puzzles

**What goes wrong:** The puzzle generator removes clues from a solved grid until the target clue count is reached, but does not verify that the resulting puzzle has exactly one solution. Players can reach a valid-looking completion that differs from the intended solution, causing a correct board to be scored as wrong (or vice versa), breaking game integrity.

**Why it happens:** Naive clue-removal loops stop when the clue count hits a target number. They do not re-run the solver to count solutions. A solver that terminates on the first valid completion cannot detect multiple solutions — it must be configured to continue searching after finding solution #1 and abort only when it finds solution #2 (or exhausts the search tree).

**Consequences:** Players who solve the puzzle correctly with a logically valid (but unintended) solution are told they made errors. Discovered by users, this is the most-cited integrity complaint in Sudoku game reviews.

**Prevention:**
- After removing each clue candidate, run a modified solver that aborts on second solution found. Only accept the removal if the solver exhausts the search tree with exactly one solution.
- Never remove clues purely by count without this uniqueness check.
- Hard puzzles require more removal attempts and more solver runs — budget generation time accordingly (target < 2 seconds on-device for Hard).
- The minimum-clue lower bound is 17. Never generate a puzzle with fewer than 17 given clues.

**Detection:** Run the solver on the generated puzzle; if it finds 2+ solutions, the generator is broken.

**Phase:** Puzzle Engine — the uniqueness check is a non-negotiable invariant, not an optimisation.

---

### Pitfall 4: Touch Targets Too Small for E-ink Interaction Model

**What goes wrong:** On a standard LCD phone, small touch targets are annoying but survivable. On E-ink, the display lag (50–200ms visual feedback) means players do not know whether their tap registered until the update propagates. With small cells, a mis-tap next to the intended cell is not corrected by visual feedback before the next tap happens, leading to double-taps or adjacent-cell selections.

**Why it happens:** The Mudita Kompakt is 800×480 px on 4.3 inches (~216 ppi). A standard 9-column Sudoku grid at full width gives ~88 px per cell. At ~216 ppi that is ~10.3mm per cell — just above Android's 48dp recommended minimum, but E-ink interaction requires more margin because players cannot rely on live hover feedback.

**Consequences:** Players repeatedly select wrong cells, fill in wrong cells, lose trust in touch accuracy, and stop playing.

**Prevention:**
- Target minimum 56dp per cell (not 48dp) for E-ink; this is ~62px at the device's density.
- At 800px width, a 9-column grid with 56dp cells at the device's density may require the number input pad to be placed below the grid (not side-by-side) to preserve cell size.
- The MMD framework's `ButtonMMD` already enforces E-ink-appropriate sizing — use it for the number pad. Do not build raw `Box` buttons with less than 56dp touch target.
- One developer report on the Mudita Forum noted that the MMD navigation bar's extra padding (~20px over first-party apps) reduces available vertical space; account for this in layout calculations.
- Test touch accuracy by sitting at arm's length and attempting 20 rapid taps on specific cells. Failure rate > 5% means targets are too small.

**Detection:** Playtesting shows player frequently selects adjacent cell to intended cell.

**Phase:** Grid UI / Input — establish sizing constants before building the number pad.

---

## Moderate Pitfalls

---

### Pitfall 5: Score Can Go Negative or Produce Nonsensical Results

**What goes wrong:** The scoring formula subtracts a fixed penalty per error and per hint. With enough errors and hints, the score goes negative. Showing a negative final score is confusing and undermines the mindful, low-pressure tone of the Mudita design ethos.

**Why it happens:** The penalty deduction is applied without a floor. Edge cases: a player uses all hints on a Hard puzzle (high penalty multiplier) and also makes many errors. The combination can easily exceed the base score.

**Consequences:** Score summary screen shows "-40 points" or similar. Players do not understand what this means relative to zero. Leaderboard entries with negative scores sort unpredictably if not handled.

**Prevention:**
- Define a score floor of 0. `finalScore = max(0, baseScore - errorPenalties - hintPenalties)`.
- Store `errorCount` and `hintCount` separately in state — do not compute them from the score. The score is a display artifact derived from raw counters at game end.
- When displaying the score summary, show both the raw counters (errors: N, hints used: N) and the final score, so the player understands why they scored 0 rather than a negative number.
- Add unit tests with pathological inputs: 81 errors, 20 hints, Hard difficulty.

**Detection:** Write a unit test with maximal penalties; assert score >= 0.

**Phase:** Scoring / Completion Screen — required before the completion flow is considered done.

---

### Pitfall 6: Game State Lost on Process Death

**What goes wrong:** The player pauses mid-game, the Mudita Kompakt low-memory killer terminates the app process, and the game state is gone on resume. The app re-opens to the main menu with no trace of the in-progress game.

**Why it happens:** Game state stored only in `ViewModel` memory survives configuration changes but does not survive process death. `onSaveInstanceState` bundle has size limits and runs on the main thread (causing UI jank for large payloads like a 81-cell board + pencil marks). DataStore writes are async — if the app is killed before the coroutine completes, the last few moves are lost.

**Consequences:** Player loses progress, especially on Hard puzzles that may have taken 20+ minutes of effort.

**Prevention:**
- Persist game state to DataStore (or Room) on every significant state change (cell filled, hint used, not just on explicit pause). Use DataStore's transactional `edit {}` to ensure atomicity.
- Do not use `SavedStateHandle` alone for the full game state — it is appropriate for navigation arguments, not 81-cell boards.
- Configure a `CorruptionHandler` on the DataStore instance: `setCorruptionHandler { ReplaceFileCorruptionHandler { defaultGameState() } }` to recover from file corruption gracefully.
- On app launch, check DataStore before showing the main menu. If a saved game exists and is incomplete, offer to resume.
- Use `viewModelScope` for read operations but write operations should use a scope tied to the Application lifecycle (`applicationScope`) so saves are not cancelled when the ViewModel is destroyed.

**Detection:** Fill in 5 cells, force-stop the app via Settings, reopen it — the game should be recoverable.

**Phase:** Persistence / Pause-Resume — required before the pause feature is signed off.

---

### Pitfall 7: Difficulty Implemented as Clue Count Only

**What goes wrong:** Easy, Medium, and Hard puzzles are differentiated only by how many starting numbers are shown (e.g., Easy=40, Medium=32, Hard=26). However, all three difficulties end up feeling equally hard because the solving complexity — which solving techniques are required — is not controlled. A puzzle with 26 clues but only requiring basic elimination is not actually "Hard."

**Why it happens:** Clue count is the obvious proxy for difficulty. It is easier to implement than solving-technique analysis. Most open-source Sudoku generators stop here.

**Consequences:** Players notice all difficulties feel similar. Hard puzzles are not noticeably harder than Medium. The difficulty selection screen loses its value, reducing replayability.

**Prevention:**
- Integrate a constraint solver that can classify required techniques (naked singles, hidden pairs, X-Wing, etc.). Accept a puzzle for a given difficulty level only if the required technique tier matches.
- At minimum: Easy should be solvable with naked singles and box/row/col scanning only. Medium should require at least hidden singles or naked pairs. Hard should require advanced elimination.
- Pre-generate a pool of validated puzzles per difficulty at build time (or first-launch seed), rather than generating on demand, to avoid on-device solving complexity being a bottleneck.

**Detection:** Ask a Sudoku player to solve puzzles across all three difficulties and rate them.

**Phase:** Puzzle Engine — implement alongside the generator, not as an afterthought.

---

### Pitfall 8: Error Tracking Counts Pre-Filled Clue Cells as Errors

**What goes wrong:** The error detection logic counts an entry as wrong if it does not match the solution. If the implementation is not careful, it may also flag the player "correcting" a hint-revealed cell (or accidentally allow editing a pre-filled clue cell), producing a spurious error count.

**Why it happens:** The board state is a flat array of values. Without explicit flags distinguishing `CLUE`, `PLAYER_ENTRY`, `HINT_REVEALED` cell types, the error check logic treats all non-empty cells symmetrically.

**Consequences:** Player's final error count is inflated by interactions with pre-filled cells. Score is unfairly penalised. Especially problematic if hints reveal a cell — subsequent re-tap of that cell should not re-count as an error or be editable.

**Prevention:**
- Model each cell with a type enum: `GIVEN` (clue, uneditable), `PLAYER` (editable, error-trackable), `HINT` (revealed by hint, uneditable, not error-counted).
- Error detection runs only on `PLAYER` cells.
- Tapping a `GIVEN` or `HINT` cell selects it for context (e.g., highlighting the row/column) but the number pad does nothing when one of these is selected.

**Detection:** Tap a pre-filled clue cell and then tap a number — the error count must not increment.

**Phase:** Grid logic / Cell model — define the enum before any input handling is written.

---

## Minor Pitfalls

---

### Pitfall 9: Ripple / Elevation Effects Imported from Material 3

**What goes wrong:** Material 3 components — `Button`, `Card`, `Surface` — include ripple effects and shadow elevation by default. On E-ink, ripples cause a visible animation artifact (rapid partial refreshes) and shadows appear as dark halos around components.

**Prevention:** Only use MMD library components (`ButtonMMD`, `TextMMD`, etc.) and apply `ThemeMMD`. Do not mix Material 3 components into the UI. If a Material 3 component is needed temporarily, explicitly disable ripple via `indication = null` and set `elevation = 0.dp`.

**Phase:** Foundation — enforce as a lint rule or code review checklist item from project start.

---

### Pitfall 10: High Score Leaderboard Allows Duplicate Entries per Session

**What goes wrong:** If the player completes a puzzle and the "back to menu" flow is navigated through twice (e.g., a double-tap or navigation stack bug), the score is recorded twice in the per-difficulty leaderboard.

**Prevention:** The score save is guarded by a one-time flag set immediately on completion. The `ViewModel` transitions to a `COMPLETED` state that ignores further completion triggers. High score writes use a uniqueness check (timestamp + score) before inserting.

**Phase:** Scoring / Completion Screen — minor, address as part of completion flow implementation.

---

### Pitfall 11: Hint Reveals an Already-Correct Player Entry

**What goes wrong:** Player enters the correct digit in a cell, then requests a hint. If the hint system picks that cell, it "reveals" a value already there, wastes a hint slot, applies the score penalty, and shows no visible change. Player is confused.

**Prevention:** The hint selector must filter out cells already correctly filled by the player. Eligible hint candidates are cells where: the cell is empty OR the cell has a wrong player entry.

**Phase:** Hint system — small guard clause, trivial to implement if caught early.

---

### Pitfall 12: MuditaOS K Missing Google Services Breaks Dependencies

**What goes wrong:** MuditaOS K is AOSP-based with no Google Play Services. Any library that transitively depends on `com.google.android.gms` or Firebase will fail at runtime with a `ServiceNotFoundException` or silent crash.

**Prevention:** Audit all dependencies for GMS/Firebase transitives before adding them. DataStore, Room, Kotlin coroutines, and Jetpack Compose have no GMS dependency. Crashlytics, Firebase Analytics, and Google AdMob do and must not be used. Run `./gradlew dependencies | grep gms` to check.

**Phase:** Foundation / Dependency setup — check at project creation.

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|---|---|---|
| Grid UI composable structure | Whole-grid recomposition (Pitfall 1) | Cell-level StateFlow + key() before other features |
| Grid rendering on device | E-ink ghosting (Pitfall 2) | ThemeMMD, no animations, solid backgrounds — verify on hardware |
| Cell and number pad sizing | Touch targets too small (Pitfall 4) | 56dp minimum cell size; test on physical device |
| Puzzle generation | Multiple solutions (Pitfall 3) | Uniqueness solver required; not optional |
| Difficulty levels | Clue count only (Pitfall 7) | Solving-technique classification gate per difficulty |
| Cell model design | Error counting clue cells (Pitfall 8) | GIVEN/PLAYER/HINT enum from day one |
| Scoring formula | Negative score (Pitfall 5) | `max(0, score)` floor + separate counter state |
| Pause/resume persistence | State lost on process death (Pitfall 6) | DataStore write on every change, applicationScope |
| Dependency selection | GMS transitive dependency (Pitfall 12) | Audit at project creation |
| Material 3 components | Ripple artifacts (Pitfall 9) | MMD-only components, enforced from start |
| Hint selection logic | Hint on correct cell (Pitfall 11) | Filter already-correct cells from hint candidates |
| Completion flow | Duplicate leaderboard entries (Pitfall 10) | One-shot completion state flag |

---

## Sources

- AnkiDroid E-ink ghosting issue: [Reduce ghosting in E-ink devices #16117](https://github.com/ankidroid/Anki-Android/issues/16117) — MEDIUM confidence (open issue, community discussion)
- BOOX E-ink ghosting optimization guide: [How to Optimize Ghosting on Color E Ink Screen](https://onyxboox.medium.com/how-to-optimize-ghosting-on-color-e-ink-screen-fa0b9b77a171) — MEDIUM confidence
- Viwoods E-ink ghosting explainer: [E Ink Ghosting Decoded](https://viwoods.com/blogs/paper-tablet/e-ink-ghosting-explained) — MEDIUM confidence
- Compose Sudoku recomposition deep-dive: [Debugging Compose in Sudoku app](https://christopherward.medium.com/debugging-and-fixing-a-huge-jetpack-compose-performance-problem-in-my-sudoku-solver-app-8f67fa229dc2) — HIGH confidence (practitioner report, directly applicable)
- Compose performance best practices: [Android Developers — Follow best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices) — HIGH confidence (official)
- DataStore corruption handler: [Android Developers — DataStore](https://developer.android.com/topic/libraries/architecture/datastore) — HIGH confidence (official)
- Sudoku unique solution requirement: [Mathematics of Sudoku — Wikipedia](https://en.wikipedia.org/wiki/Mathematics_of_Sudoku) — HIGH confidence (mathematical fact, 17-clue minimum)
- Sudoku difficulty levels: [SudokuPuzzles.net — Difficulty Levels](https://www.sudokupuzzles.net/blog/sudoku-difficulty-levels-explained) — MEDIUM confidence
- Mudita Kompakt sideloading limitations: [Sideloading Apps on Mudita Kompakt](https://forum.mudita.com/t/sideloading-apps-on-mudita-kompakt-what-you-need-to-know/7178) — HIGH confidence (official Mudita forum)
- MMD library README: [github.com/mudita/MMD](https://github.com/mudita/MMD) — HIGH confidence (primary source)
- Mudita Kompakt developer experience: [One week with the Mudita Kompakt as a software developer](https://forum.mudita.com/t/one-week-with-the-mudita-kompakt-as-a-software-developer/8518) — MEDIUM confidence (single user report)
- E-ink display refresh mode discussion: [E-ink Display Slow vs Fast Modes — Mudita Forum](https://forum.mudita.com/t/e-ink-display-slow-vs-fast-modes/8716) — MEDIUM confidence
- Generating difficult Sudoku puzzles: [dlbeer.co.nz — Generating difficult Sudoku puzzles quickly](https://dlbeer.co.nz/articles/sudoku.html) — HIGH confidence (detailed technical article)
