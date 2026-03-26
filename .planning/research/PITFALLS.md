# Domain Pitfalls

**Domain:** Sudoku game on Mudita Kompakt E-ink Android (MMD/Compose)
**Researched:** 2026-03-25
**Scope note:** Sections 1–12 carry over from v1.0 research. Section 13 onwards covers v1.1 cosmetic UI change pitfalls specifically.

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

## v1.1 Cosmetic UI Change Pitfalls

The five changes targeted in v1.1 each touch a narrow layer of the codebase — Canvas drawing, TextStyle construction, ButtonMMD content slots, and Compose layout modifiers. The pitfalls below are specific to those changes in the context of this app's E-ink target and existing architecture.

---

### Pitfall 13: Canvas drawText Color Not Changing for Pencil Marks on Selected Cell

**Change:** Make pencil mark digits appear white when they are inside the selected (black-filled) cell.

**What goes wrong:** The developer adds an `isSelected` branch to `drawPencilMarks` but passes the style with the updated color as a new `TextStyle` argument, then calls `drawText(textLayoutResult = measured, topLeft = ...)` with no explicit color override. The pencil marks remain black and are invisible against the black cell background.

**Why it happens:** The `drawText(textLayoutResult, topLeft)` overload used in the current `drawPencilMarks` function does not accept a color parameter — all color information must be baked into the `TextStyle` passed to `textMeasurer.measure()` at the time of measurement. The `drawText` overload that *does* accept a runtime `color` parameter (`drawText(textLayoutResult, color, topLeft, ...)`) is a separate overload and requires an explicit call-site change. Since both overloads exist and the distinction is invisible at a glance, it is easy to update the style object but call the wrong overload, resulting in the style's color being used (which, if `Color.Unspecified`, falls back to the measured style's color — often still black).

**Consequences:** Pencil marks are invisible (black on black) on the selected cell. If the player relies on pencil marks heavily, they cannot see them when the cell is active.

**E-ink risk:** Black-on-black is simply invisible — there is no intermediate gray on a monochromatic E-ink panel. No ghosting risk, but the failure mode is total invisibility rather than low contrast.

**Prevention:**
- Pass `isSelected` (or a pre-computed `Boolean`) into `drawPencilMarks` and create two style variants at the `GameGrid` composable level: `pencilStyle` (black) and `pencilStyleSelected` (white). Do not create new `TextStyle` objects inside the DrawScope lambda — create them at composable scope with `remember` so they are not reallocated on every draw call.
- Use the style argument to `textMeasurer.measure(digit.toString(), style)` to bake the color in at measure time. The existing code architecture already does this correctly for digit cells (`digitStyleSelected*` variants). Follow the same pattern.
- Do not attempt to use the `drawText(textLayoutResult, color = Color.White, ...)` overload as a color override without also being certain the `textLayoutResult` was measured with `Color.Unspecified` in the style — if the style has a hard-coded `Color.Black`, the overload's `color` parameter only applies when the style color is `Color.Unspecified`. Verify which overload semantics apply for the version of Compose in use.
- The simplest safe approach: always measure with the final intended color in the `TextStyle`. Avoid relying on the runtime color override path entirely.

**Confidence:** HIGH — confirmed by inspecting the current `drawPencilMarks` implementation and the `drawText` API overload contract.

---

### Pitfall 14: Dynamic Pencil Mark Font Size Computed in Wrong Units or Wrong Scope

**Change:** Size pencil mark digits dynamically so 4 marks fit in a 2×2 grid inside a cell, rather than using a fixed `9.sp`.

**What goes wrong (unit mismatch):** The cell size is available as `cellSizePx` (a Float in pixels). The developer computes the target font size as `(cellSizePx / 3f)` (one sub-cell width) and passes this directly as `sp` — e.g. `(cellSizePx / 3f).sp` or `(cellSizePx / 3f).toInt().sp`. This produces a wildly oversized font because on the Kompakt (~216 ppi, density ~2.25), a pixel value is much smaller than an sp value. A cell at ~53px wide divided by 3 gives ~17.7px; interpreting that as `17.7.sp` at 2.25× density would render at ~40px — larger than the full digit text.

**What goes wrong (scope problem):** The developer wraps the font-size calculation in the Canvas DrawScope to use `DrawScope.toPx()` for conversion, but then tries to call `with(density) { targetPx.toSp() }` inside the lambda. `DrawScope` does not directly expose `Density` as a named property at the call site; the conversion requires accessing it through `this` (DrawScope implements `Density`). A misplaced conversion (`targetPx / density.density` instead of the correct `targetPx / density.fontScale / density.density`) will produce wrong results when the user has system font scaling enabled.

**What goes wrong (recomputation):** The font size is derived from `cellSizePx`, which changes when the composable is first laid out. If the `TextStyle` is constructed inside the Canvas lambda (DrawScope) rather than outside at composable scope, it is recreated on every draw frame. This causes `textMeasurer.measure()` to be called on every frame rather than only when size changes, adding measurable latency on the Helio A22 CPU.

**Consequences:** Pencil marks too large (overflowing cell bounds), too small (unreadable), or jittery (font size flickers as density floats). On E-ink the oversized case causes partial-refresh artifacts at cell borders.

**E-ink risk:** Overflowing text that extends into adjacent cells triggers a broader redraw region, causing neighboring cells to partially refresh even when unchanged — visible as flicker on the cells adjacent to any selected pencil-mark cell.

**Prevention:**
- Convert pixel cell size to sp using the full density chain. Inside a `BoxWithConstraints` composable (which this code already uses), `LocalDensity.current` is available. Compute: `val pencilFontSizeSp = with(LocalDensity.current) { (cellSizePx / 3f).toSp() }` at composable scope (outside the Canvas lambda).
- Wrap the computed `TextStyle` in `remember(cellSizePx)` so it is only recomputed when the cell size actually changes. Cell size only changes on orientation change or window resize, not on every recomposition.
- Use the 3×3 sub-grid layout (current code) not a 2×2 layout, unless the requirement specifically says 2×2. The current code divides by 3 for each sub-cell — if changing to 2×2, divide by 2 instead. Do not conflate the sub-grid dimension with the font size divisor.
- After computing the target sp size, consider applying a 0.7× scale factor as a safety margin so the text visually fits within the sub-cell without measuring against its exact boundary. Verify on device: at ~216 ppi and typical cell size, the resulting sp value should be roughly 7–10sp for a 3×3 sub-grid or 11–14sp for a 2×2 sub-grid.
- Resist the urge to use `TextAutoSize` or `BasicText` auto-sizing inside Canvas — those APIs apply to Compose layout nodes, not DrawScope. They are not available inside a Canvas lambda.

**Confidence:** HIGH — unit conversion rules are well-established; the scope/performance concern is verified by inspecting the current code pattern.

---

### Pitfall 15: Custom Font/Typeface Applied to ButtonMMD Does Not Take Effect

**Change:** Apply a taller/thinner font to the number pad digit buttons so the digit characters appear more vertically centered and proportional.

**What goes wrong:** The developer wraps the `TextMMD` inside `ButtonMMD` with a `LocalTextStyle.provides(...)` composition local override, or passes a `style` parameter to `TextMMD` with a custom `fontFamily`. The button text appears unchanged on device.

**Why it happens — MMD typography override:** `ThemeMMD` provides its own `Typography` to `MaterialTheme`. `ButtonMMD` is a Material 3-derived component that applies the button label text style from `MaterialTheme.typography.labelLarge` (or equivalent) to its content slot via a `ProvideTextStyle`. This inner `ProvideTextStyle` call has higher specificity in the composition local chain than any outer override the developer applies. A `LocalTextStyle.provides(...)` placed above `ButtonMMD` in the tree is silently overridden by the one inside ButtonMMD's implementation.

**Why it happens — TextMMD ignores external style:** `TextMMD` in MMD 1.0.1 is a closed-source component. Its internal implementation likely reads from `LocalTextStyle.current` and applies additional E-ink constraints (specific font, minimum size). A `style` parameter passed to `TextMMD` may be merged rather than fully replaced, depending on how the MMD team implemented it. The exact merge behavior cannot be confirmed without access to source, which makes this a MEDIUM confidence finding. The safe assumption is that font-family overrides on `TextMMD` may be silently discarded.

**Why it happens — fontFamily asset not bundled:** If using a custom `.ttf`/`.otf` file, it must be placed in `app/src/main/res/font/` with a lowercase underscore filename, and referenced via `FontFamily(Font(R.font.my_font))`. A missing asset causes a silent fallback to the default system font with no error or exception.

**Consequences:** The number pad continues showing the default MMD font. The visual change the player observed (poorly centered digits) is not fixed, because the root cause may be vertical alignment within the cell rather than font choice. Chasing font change as the fix leads to wasted effort.

**E-ink risk:** Custom fonts that include hinting artifacts or non-standard baseline metrics can produce sub-pixel rendering mismatches on E-ink that appear as slightly blurry digit edges. E-ink panels do not perform sub-pixel anti-aliasing, so font hinting designed for LCD screens can produce unexpected visual results.

**Prevention:**
- Before changing font: confirm the actual problem is font shape vs. vertical alignment. The `ButtonMMD` content slot uses `contentAlignment = Alignment.Center` (inherited from Material 3 Button). If digits appear vertically off-center, the root cause is more likely the button's internal padding or the TextStyle's `lineHeight` vs `fontSize` ratio.
- If font change is genuinely needed: do NOT attempt to override via `LocalTextStyle`. Instead, check whether `TextMMD` accepts a `style: TextStyle` parameter. If it does, pass a style with only the `fontFamily` field set and all other fields as `TextStyle.Default` so MMD's constraints are preserved via merge.
- A safer alternative for centering text vertically: adjust the `lineHeight` in the `TextStyle` to match `fontSize`. The default Material 3 button style sets `lineHeight` larger than `fontSize` to accommodate descenders; setting `lineHeight = fontSize` removes the extra vertical breathing room and may achieve the centering effect without font substitution.
- If a fully custom font is required and `TextMMD` does not cooperate, replace the `ButtonMMD` + `TextMMD` combination with the existing `Box + clickable(indication = null)` pattern already used in `ControlsRow.kt` for the Fill/Pencil toggles. That approach bypasses MMD's typography injection entirely and gives full control over text style.

**Confidence:** MEDIUM — ButtonMMD API surface is closed-source; the typography override behavior is inferred from Material 3 internals and the MMD design philosophy, not from direct source inspection.

---

### Pitfall 16: Drawing Diagonal Hatching Over a Compose Button Breaks Touch or Clips Incorrectly

**Change:** Draw diagonal hatching lines over the inactive Fill/Pencil button to visually distinguish active vs. inactive state.

**What goes wrong (overlay approach):** The developer places a `Canvas` composable (or `Modifier.drawWithContent`) directly inside the `Box` that wraps the Fill/Pencil toggle. The hatching Canvas is rendered at a size of 0×0 or clips to its own layout bounds rather than covering the full button area.

**Why it happens:** A standalone `Canvas` composable occupies zero layout space by default unless given an explicit size or `Modifier.fillMaxSize()`. If placed as a sibling inside the `Box`, it is drawn at `(0,0)` with no size. If placed using `Modifier.drawWithContent`, the developer must call `drawContent()` explicitly or the button's text disappears.

**What goes wrong (modifier approach):** The developer applies `Modifier.drawBehind { ... drawLine(...) }` to the `Box`. This is the correct mechanism, but the line coordinates use pixel values derived from an incorrect source — for example, `size.width` is accessed assuming it reflects the final layout size, but if the modifier is applied before a `.sizeIn(minHeight = 56.dp)` modifier, the size at draw time may be the intrinsic content size, not the constrained size. Modifier ordering determines whether size constraints are applied before or after drawing.

**What goes wrong (E-ink specific — partial refresh):** Diagonal lines are the most challenging pattern for E-ink partial refresh. Each line touches many column positions across the cell area. A partial refresh for a single cell — triggered by a mode toggle — must redraw all pixels touched by those diagonal lines. On monochromatic E-ink, diagonal lines at angles that produce many 1-pixel-on / 1-pixel-off runs can cause visible banding artifacts or inconsistent line darkness. This is the same phenomenon that makes grayscale E-ink refresh difficult; diagonal patterns at certain angles stress the particle alignment mechanism.

**Consequences:** Hatching either does not appear, appears at wrong size, clips beyond button bounds (overlapping adjacent buttons in the Row), or causes visible E-ink refresh artifacts at the button boundaries.

**E-ink risk:** This is the highest E-ink risk of the five changes. Dense diagonal hatching is inherently a partial-refresh stress test. The Mudita Kompakt uses a fast waveform for UI interaction responses; fast waveform + dense diagonal = most likely path to visible ghosting. The existing v1.0 design (solid black / solid white) was chosen precisely because solid blocks refresh cleanly.

**Prevention:**
- Use `Modifier.drawBehind { ... }` applied after all size-constraining modifiers. Place it as the last modifier before `.clickable(...)` in the chain to ensure `size` in the DrawScope reflects the fully constrained layout size.
- Keep diagonal line spacing coarse: minimum 6–8px gap between lines. Finer spacing approaches a halftone pattern, which is exactly what degrades on E-ink partial refresh.
- Consider whether hatching is the right visual affordance. A thick border or a lighter `Color(0xFF808080)` background — a mid-gray rather than a diagonal pattern — may achieve the same "inactive" signal with far less E-ink refresh complexity. Mid-gray solid fill is a single particle state change per pixel; diagonal hatching requires many partial changes per refresh cycle.
- If hatching is required: do NOT use `Canvas` as a standalone composable sibling. Use `Modifier.drawBehind` or `Modifier.drawWithContent` (calling `drawContent()` first, then hatching on top, or hatching first, then `drawContent()`). The draw-behind approach ensures the button's layout and touch area are unaffected.
- Verify that the `Box` wrapping the Fill/Pencil toggles has `Modifier.background(Color.White)` so the inactive state always starts from a white baseline, not from whatever was previously drawn on that screen region.

**Confidence:** HIGH for layout/modifier behavior; MEDIUM for E-ink diagonal artifact risk (inferred from E-ink refresh physics and community reports, not device-specific Kompakt testing).

---

### Pitfall 17: Background Container Around Fill/Pencil Pair Breaks Touch Targets or Clips ButtonMMD

**Change:** Add a subtle visual container (border or background box) around the Fill and Pencil buttons as a pair, visually separating them from the number pad row.

**What goes wrong (outer Box clips inner buttons):** The developer wraps the two `Box` toggles in an outer `Box` with `Modifier.background(...).border(...)`. If the inner buttons have `Modifier.weight(1f)`, their layout is driven by the parent `Row`. Introducing an intermediate `Box` wrapper without explicit size constraints causes the weight modifier to be resolved relative to the wrapper's intrinsic size (which may be 0) rather than the outer `Row`'s available space. The result is both toggle buttons collapsing to zero or minimal width.

**Why it happens:** `Modifier.weight(1f)` is a `RowScope`-scoped modifier. It only functions correctly when the composable that carries it is a direct child of a `Row`. Wrapping the two weight-carrying composables inside a `Box` removes them from direct `Row` scope; the `weight` modifier on the inner items then has no effect and the `Box` sizes to its content (zero or minimal).

**What goes wrong (border adding to layout size):** `Modifier.border(...)` draws a border inside the composable's bounds by default in Compose, not outside. However if the developer uses `Modifier.padding(1.dp).border(1.dp, ...)`, the padding adds to the effective outer size. Combined with `Modifier.weight(1f)` siblings in the same Row, this can cause the bordered pair to be slightly wider than its weight share, pushing the Undo and Get Hint buttons further right and potentially off-screen or clipping them against the Row's end.

**What goes wrong (touch target reduction):** If the outer wrapper `Box` does not propagate touch events to its children by default (which it does by default in Compose, but a developer may add `Modifier.clickable` to the wrapper itself thinking it controls both toggles), the inner clickable regions are shadowed and taps on the Fill/Pencil buttons stop working.

**What goes wrong (background over-draw on E-ink):** A container background drawn over an area that already has a white background is a no-op visually on LCD but triggers an additional partial refresh on E-ink — the display hardware sees a pixel change from white to white as a change and may still issue a refresh depending on the E-ink controller implementation. While this is usually benign, it adds latency to the first render of this layout if the controller does not de-duplicate no-op writes.

**E-ink risk:** Low for the container background itself (solid white or light gray over white). However, if a `border` is used: a 1dp border on E-ink at 216 ppi is a 2-pixel physical stroke. At fast waveform refresh speeds, very thin lines (1–2px) can appear slightly gray rather than black due to insufficient particle travel time. Use 2dp or thicker for any border that must appear fully black.

**Prevention:**
- Do not break the `weight(1f)` chain by introducing an intermediate non-Row container. Instead, apply the background and border directly to a `Row` that wraps just the two toggle composables, and give that inner `Row` a `Modifier.weight(2f)` in the outer `Row`. This keeps all weight-bearing composables as direct children of their respective `Row` and gives the pair a share of the available space equal to two individual buttons.
- Example structure (conceptual): outer `Row { innerRow(weight=2f) { fillToggle(weight=1f); pencilToggle(weight=1f) }; undoButton(weight=1f); hintButton(weight=1f) }`. The inner Row carries the background/border and the fill/pencil boxes carry weight within it.
- Use `Modifier.border(width = 2.dp, color = Color.Black, shape = RectangleShape)` for any border. Do not use `shape = RoundedCornerShape(...)` — rounded corners are rendered via anti-aliasing which produces gray edge pixels on E-ink.
- Do not use `Modifier.shadow(...)` for the container — shadows are gray gradients, which are the canonical E-ink ghosting trigger.
- Verify touch target sizes after the wrapper is added. The `minHeight = 56.dp` constraint must still be satisfied on the toggle buttons inside the inner Row.

**Confidence:** HIGH for layout/weight behavior; MEDIUM for border rendering on E-ink (thin line behavior is inferred from E-ink refresh physics).

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
| v1.1: Pencil mark color on selected cell | drawText color not changing (Pitfall 13) | Bake color into TextStyle at measure time; use pencilStyleSelected variant |
| v1.1: Dynamic pencil mark font size | Wrong unit conversion px→sp (Pitfall 14) | Convert with LocalDensity at composable scope; remember(cellSizePx) the style |
| v1.1: Number pad custom font | ThemeMMD typography override silently ignored (Pitfall 15) | Try TextMMD style parameter first; fall back to Box+clickable if needed |
| v1.1: Diagonal hatching over inactive button | DrawBehind size mismatch + E-ink diagonal artifacts (Pitfall 16) | Apply Modifier.drawBehind after size modifiers; coarse line spacing ≥6px |
| v1.1: Container around Fill/Pencil pair | weight(1f) breaks when wrapped in Box (Pitfall 17) | Use inner Row with weight(2f), not Box; avoid shadow and rounded corners |

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
- Compose drawText API overloads and color parameter: [Exploring text on Canvas using drawText API in Jetpack Compose](https://canopas.com/exploring-text-on-canvas-using-drawtext-api-in-jetpack-compose-402e1285935c) — MEDIUM confidence
- drawText TextLayoutResult overload signature (color = Color.Unspecified default): [Add drawText() function to Canvas component — Google Issue Tracker #190787898](https://issuetracker.google.com/issues/190787898) — MEDIUM confidence
- Compose modifier ordering and constraints: [Constraints and modifier order — Android Developers](https://developer.android.com/develop/ui/compose/layouts/constraints-modifiers) — HIGH confidence (official)
- Compose drawBehind modifier: [Graphics modifiers — Android Developers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers) — HIGH confidence (official)
- Preventing font scaling / sp vs dp in Compose: [Preventing Font Scaling in Jetpack Compose — ProAndroidDev](https://proandroiddev.com/preventing-font-scaling-in-jetpack-compose-8a2cd0f09d23) — HIGH confidence (practitioner report)
- E-ink partial refresh ghosting with complex patterns: [E-Ink Display Integration — Ghosting and Refresh Challenges — Core Electronics Forum](https://forum.core-electronics.com.au/t/e-ink-display-integration-ghosting-and-refresh-challenges/23151) — MEDIUM confidence
