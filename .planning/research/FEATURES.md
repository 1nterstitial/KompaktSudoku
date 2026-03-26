# Feature Landscape

**Domain:** Mobile Sudoku game (E-ink, mindful/minimalist)
**Researched:** 2026-03-25 (v1.1 UI cosmetic fixes appended)
**Overall confidence:** HIGH (core Sudoku features well-documented; E-ink specifics MEDIUM)

---

## v1.1 UI Cosmetic Research (2026-03-25)

This section addresses five specific visual behaviors identified from first real-device play on the
Mudita Kompakt (4.3", 800x480, ~216 ppi, monochromatic E-ink). Each item is an observed problem
with a concrete recommended fix.

The existing `GameGrid.kt` uses a single Compose `Canvas` with `drawText` / `TextMeasurer`, and
`ControlsRow.kt` uses `Box + clickable` wrappers with `background()` for mode toggles. Both files
are the primary targets for these changes.

---

### Item 1: Pencil-Mark Digits White-on-Black in a Selected Cell

**The problem**

`GameGrid.kt` defines a single `pencilStyle` with `color = Color.Black`. When a cell is selected
its background is filled solid black. Pencil mark text is drawn after the selected fill — black
text on a black background renders invisible.

**What the standard UX behavior is**

Virtually every Sudoku app that inverts the selected cell (black fill, white main digit) also
inverts the pencil marks in that same cell. The inversion applies to all text within a selected
cell, not just the main digit. This is confirmed by multiple Sudoku apps on both iOS (Paper Sudoku,
Sudoku The Clean One, Good Sudoku) and the open-source Android reference LibreSudoku: when a cell
is selected with a dark highlight, text that would normally be black is rendered white.

**Contrast specification**

E-ink monochromatic displays are binary: a pixel is either fully black or fully white. There is no
partial gray rendering at 216 ppi. The contrast ratio for white text on a pure black background on
E-ink is effectively the maximum possible — no additional contrast specification is needed. The
requirement is simply that the correct color is passed to `drawText`.

**Implementation approach**

Create a `pencilStyleSelected` variant that is identical to `pencilStyle` but with
`color = Color.White`. In `drawPencilMarks`, pass the selected/unselected style based on whether
the cell is selected. The existing `drawPencilMarks` function already receives a `style: TextStyle`
parameter — the caller at line 157 only needs to pass the correct style.

No `BlendMode` or `colorFilter` workarounds are needed. `TextStyle(color = Color.White)` passed to
`TextMeasurer.measure()` produces white-colored glyphs when drawn via `drawText`. This is a
well-established pattern confirmed in official Compose `drawText` API documentation.

**What to avoid**

- Do NOT use `BlendMode.Xor` or `BlendMode.Difference` to "auto-invert" colors — these affect the
  entire draw call including the already-drawn black background rectangle, causing artifacts.
- Do NOT add a white background sub-rect behind pencil marks on selected cells — unnecessary visual
  noise and breaks the clean inversion look.
- A gray intermediate color (e.g., `Color(0xFFAAAAAA)`) would appear washed out on E-ink due to
  dithering artifacts. Use pure `Color.White` only.

**Confidence:** HIGH — standard pattern, directly supported by Compose `drawText` API.

---

### Item 2: Dynamic Pencil-Mark Font Sizing and 3x3 Grid Arrangement

**The problem**

`GameGrid.kt` uses a fixed `9.sp` pencil style in a 3x3 sub-grid layout. The current cell size
on the 800x480 display is approximately `(min(800,480) / 9) = 53px per cell`. Each 3x3 sub-slot is
~17.7px. At 216 ppi, 9sp ≈ 9 * (216/160) ≈ 12.15px of rendered text height. That leaves only
~5.5px padding, which is adequate but fragile — it will break if the device's font scale is changed
or if the grid area changes size.

**Standard Sudoku UX for pencil mark arrangement**

The universal convention across physical Sudoku books, apps, and print puzzles is the **3x3
positional layout**: digit N occupies the sub-cell at row `(N-1)/3`, column `(N-1)%3`. This
means:
- 1,2,3 across the top row
- 4,5,6 in the middle row
- 7,8,9 in the bottom row

This layout is immediately readable without any legend because the position of the digit within the
sub-grid mirrors the digit's position in a standard numpad. The existing code already implements
this correctly.

The alternative layout — placing all marks in the top-left corner in order, like "1 3 7" — is
harder to scan and is used only in very compact print formats. Do not change the 3x3 layout.

**The 2x2 variant (4 candidates max)**

Some apps (notably NYT Sudoku, Good Sudoku) dynamically switch to a 2x2 layout when only 4 or
fewer candidates are present, using a larger font. This is a convenience feature, not a universal
standard. For this project, the 3x3 layout is preferred because:
1. Positional meaning is lost when layout changes dynamically — digit 5 appears at a different
   location when switching from 3x3 to 2x2.
2. The fixed 3x3 grid gives users muscle memory for candidate positions.
3. Implementation simplicity. 3x3 is already correct.

**Font size recommendation**

Instead of a fixed `9.sp`, size the pencil marks dynamically relative to cell size. The well-
established Sudoku design ratio is approximately **1/3 of the main digit size**, with the main
digit occupying roughly 40-45% of the cell height.

At 800x480:
- Grid width = 480px (constrained by height or width, whichever is smaller)
- Cell size = 480 / 9 = ~53.3px
- Main digit target: ~22-24px rendered height (current 20sp ≈ 27px at 216ppi, which is fine)
- Pencil mark target: 1/3 of main digit height ≈ 7-9px rendered height

For robust sizing, compute `pencilSp = (cellSizePx / density) / 9 * 0.85f` where the `/ 9`
gives one sub-slot in dp, and `0.85` reserves margin. At 216ppi density=1.35 (Android reports
density as dpi/160), this yields approximately `(53.3 / 1.35) / 9 * 0.85 ≈ 3.7sp` minimum —
which is below legibility on any display. The floor constraint is 7sp.

**Concrete recommendation**

Use `maxOf(7f, cellSizePx / density / 9f * 0.85f).sp` as the pencil font size, computed at
composable scope where `cellSizePx` is known. This makes the size responsive to grid area while
preventing sub-legible sizes. Pass this into the `pencilStyle` / `pencilStyleSelected` TextStyle.

**What to avoid**

- Fixed `9.sp` with no floor: fragile to density changes.
- Font sizes below 7sp on E-ink: glyphs become indistinguishable at this density. At 216 ppi and
  Android's density scaling, 7sp is approximately 9.5 physical pixels — just above the illegible
  threshold for a single stroke-width digit.
- Serif fonts for pencil marks: serif details at sub-10sp become noise on E-ink. Stick with the
  system sans-serif (Roboto) which MMD already uses.

**Confidence:** MEDIUM — Sizing ratio derived from widely-documented Sudoku design conventions.
Exact pixel floor requires device-side verification.

---

### Item 3: "Taller/Thinner" Digit Font for the Number Pad

**The problem**

`NumberPad.kt` uses `TextMMD` with default font, which inherits from `ThemeMMD`. The default MMD
typography is described as "tuned for E-ink readability" — likely a standard Roboto variant. On
the Kompakt's 800x480 display, the 10-button row (1-9 + erase) each get roughly 48px width at 2dp
gaps. This is adequate for single digits but players reported the text feels vertically low or
not centered.

**What font characteristics work well on E-ink for number pad digits**

E-ink design research converges on these properties for isolated-digit buttons:

1. **Monospaced / tabular figures** — Digits must be the same width to appear optically centered
   in fixed-width buttons. Roboto has "proportional" figures by default — the digit "1" is
   narrower than "8". This causes "1" to appear off-center in its button. Roboto has a tabular
   variant: `FontFeatureSettings("tnum")` in Compose `TextStyle`. However, `TextMMD` does not
   expose `fontFeatureSettings` directly — this requires a workaround.

2. **Medium weight, not Bold** — Bold digits on E-ink compress the stroke spacing and can create
   inter-stroke ghosting (particularly the tight loops in "8" and "0"). FontWeight.Normal or
   FontWeight.Medium is preferred. Current `digitStylePlayer` already uses Normal which is correct.

3. **Sans-serif** — Confirmed best practice for E-ink small-to-medium display text across
   MobileRead forum discussions and E-ink app developer guides. Serif details at small sizes
   produce dithering artifacts on E-ink's dithered gray rendering. Roboto (the MMD default) is
   correct.

4. **Condensed is not needed for 10-button rows** — With 10 buttons across 480px, each button is
   ~46-48px wide. A single digit at 20-24sp (approx 27-32px rendered) fits with visual margin.
   Condensed fonts increase legibility risk on E-ink (tighter strokes) for no benefit here.

5. **Vertical centering** — The most likely cause of the "not centered" perception is that
   `TextMMD` internally adds line spacing / font padding that Compose includes in the text bounding
   box but which E-ink cannot render as gray (it dithers to white, hiding the "top" of the cap
   height). The fix is to use `lineHeight = fontSize` in the `TextStyle` to collapse extra line
   spacing, or to use a `Box` with `contentAlignment = Alignment.Center` and measure manually. If
   `TextMMD` does not accept a `TextStyle` override with `lineHeight`, the center-alignment must
   be handled in the `Box` wrapper. This is the most robust fix on E-ink.

**Concrete recommendation**

The number pad buttons already wrap `TextMMD` inside `ButtonMMD`. The visual centering issue
is most reliably solved not by changing the font but by ensuring the `ButtonMMD` uses
`contentAlignment = Alignment.Center` (already the default for Material 3 `Button`). If the
issue persists, add an explicit `lineHeightStyle` on the TextStyle to eliminate font-metrics
padding: `lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim =
LineHeightStyle.Trim.Both)`. This is a Compose 1.4+ API confirmed available in BOM 2026.03.00
(Compose 1.10.5).

Do not bundle a custom font file for digits. The MMD library's Roboto variant is already
optimal for E-ink. The centering problem is a metrics issue, not a glyph design issue.

**What to avoid**

- Do NOT use a condensed or narrow font variant — risk of illegible thin strokes on E-ink.
- Do NOT use Bold for number pad digits — can cause E-ink ghosting on strokes.
- Do NOT hard-code a `paddingTop` offset to fake-center text — this breaks on different font
  scales and display sizes.
- Do NOT set `lineHeight` lower than `fontSize` — can cause text clipping at top/bottom.

**Confidence:** MEDIUM — Vertical centering via `LineHeightStyle.Trim.Both` is confirmed in
Compose API docs. MMD's internal typography implementation is opaque (closed-source AAR), so the
exact interaction of `TextMMD` with a `TextStyle` override requires device-side verification.

---

### Item 4: Inactive Mode Button Visual Differentiation on E-ink

**The problem**

`ControlsRow.kt` implements Fill and Pencil mode buttons as `Box` composables with
`background(Color.Black)` for the active state and `background(Color.White)` for the inactive
state. On E-ink, a white-background button with black text is visually indistinguishable from any
other white-background button on the same screen — it looks identical to a plain label.

**What patterns work on E-ink for "inactive but tappable"**

E-ink UI design frameworks (eink-ui CSS, Quill, and the withintent.com design guide) converge on
**border-driven visual hierarchy** as the primary E-ink design pattern. Color fills are not
available. The distinguishable states in a pure monochromatic binary system are:

| State | Visual Treatment | Appearance |
|-------|-----------------|------------|
| Active | Solid black fill + white text | Inverted, obvious |
| Inactive (tappable) | White fill + thin black border + black text | Outlined button |
| Disabled (not tappable) | White fill + no border + light gray text | Ghosted label |

The **inactive but tappable** treatment is `OutlinedButton` semantics: white background, 1dp black
border, full-opacity black text. This is the standard pattern confirmed in:
- E-ink CSS UI framework: "borders over fills, whitespace over color, clarity over decoration"
- Material 3 `OutlinedButton` design: medium-emphasis action, border as the visual indicator
- Physical Sudoku app UX convention: mode buttons where exactly one is active at a time visually
  "pop" against outlined inactive peers

**Alternative patterns that do NOT work on monochromatic E-ink**

- **Diagonal hatch / stipple fill** — Requires rendering gray tones. E-ink dithers these as
  alternating black/white pixels at close range, which creates visual noise and ghosting on
  partial refresh. Do not use.
- **Lighter gray fill** — E-ink Carta 1.2 (likely used in Mudita Kompakt) supports 16 gray
  levels in full-refresh mode, but partial refresh (used during game play to avoid screen flash)
  reduces to 2-4 effective gray levels. A "light gray" background that looks right in full refresh
  may render as white or dithered black during partial refresh. Unreliable — avoid.
- **Dashed border** — Requires sub-pixel precision for dashes to appear correct. The dithered
  rendering of E-ink at 216 ppi may render dashes as irregular blobs. Solid border is safer.
- **Reduced opacity text** (e.g., 60% alpha for inactive) — E-ink renders partial opacity as
  dithered gray, which looks messy at small text sizes. Avoid sub-100% opacity for text.

**Concrete recommendation for ControlsRow**

Change the inactive `Box` modifier from `background(Color.White)` to `background(Color.White)` +
`border(width = 1.dp, color = Color.Black)` using `Modifier.border()`. This makes the inactive
button visually distinct (outlined) while the active button remains solid black.

```
// Active
Modifier.background(Color.Black).border(1.dp, Color.Black)

// Inactive (tappable)
Modifier.background(Color.White).border(1.dp, Color.Black)
```

The border on the active button is invisible (black border on black background) but ensures
the button size does not shift between states — without it, the inactive button's border adds 1dp
to its footprint, causing a subtle layout shift on mode toggle.

**What to avoid**

- Stipple / hatch fill backgrounds.
- `alpha < 1.0` on any text or border in E-ink UI.
- Gray background fills that depend on 4-bit or 8-bit gray level rendering during partial refresh.
- Different font sizes between active/inactive states — size shifts are visually jarring on E-ink.

**Confidence:** HIGH — Border-over-fill is the documented E-ink UI design consensus. Compose
`Modifier.border()` is a stable API. The equal-border-on-both-states sizing trick is a
Compose layout best practice (prevents recomposition-triggered size shifts).

---

### Item 5: Button Group Visual Separation (Controls Pair from Number Row)

**The problem**

`GameScreen.kt` lays out `ControlsRow` and `NumberPad` as consecutive composables in a `Column`.
There is no visual separator between the Fill/Pencil pair and the 1-9 number row. On E-ink with
pure white backgrounds, the two rows visually blur together, especially since both contain buttons
with similar height and text styling.

**How E-ink apps typically group related controls**

From E-ink UI framework analysis (eink-ui CSS framework, Quill IoT UI, and physical keyboard
app patterns on Boox/reMarkable):

1. **Spacing gap only** — The simplest approach: add `Arrangement.spacedBy(8dp)` or a `Spacer`
   between the two rows in the `Column`. On E-ink, white space is high-contrast because there is
   nothing to compete with it. A 6-10dp gap between ControlsRow and NumberPad provides clear
   visual grouping without any additional element. This is the lowest-risk approach and matches
   the "whitespace over color" eink design principle.

2. **Thin 1dp horizontal rule** — A `Divider` or `HorizontalDivider` (1dp height, full width,
   black color) between the two rows. Common in E-ink reading apps, calendar apps, and control
   panels (the reMarkable 2 uses thin rules throughout its UI). The rule makes the grouping
   explicit: "these controls belong together; these are separate." The eink-ui CSS framework
   explicitly provides `.eink-divider` and `.eink-divider--strong` for exactly this use case.

3. **Inset background on the group** — Drawing a thin border rectangle around the Fill/Pencil pair
   (like a card or button-group container). This is more visually prominent but requires a
   `Box` wrapper with `border()` modifier around just the mode-toggle buttons. It draws visual
   attention to the grouping and makes it clear the two buttons are semantically linked (they are
   mutually exclusive modes). Material 3 uses this pattern in `SegmentedButton`.

**Concrete recommendation**

Use a combination of option 1 + 3:

- Add a **6dp spacer** between ControlsRow and NumberPad in `GameScreen.kt` (or wherever both
  are composed). This provides breathing room.
- Wrap only the Fill and Pencil `Box` buttons (not Undo/Hint) in a thin-bordered `Row` container
  using `Modifier.border(1.dp, Color.Black)`. This visually signals they are a linked pair
  (mode toggle group) separate from the action buttons (Undo, Hint).

This matches the standard "segmented button" E-ink pattern without introducing any gray fills
or animation. The border on the group container must be `1.dp` — heavier borders create visual
weight that competes with the grid border hierarchy (thick 2.5dp box borders, thin 1dp cell lines).

**What to avoid**

- Colored separators — no color available on monochromatic E-ink.
- Gradient or fade separators — same reason.
- A thick (2dp+) border group container — fights with the grid's visual hierarchy.
- Placing a divider between the Fill button and the Pencil button themselves — they are a group;
  dividing them implies they are separate. The divider or border goes around the pair, not between
  them.
- A `Card` composable with elevation — Compose's `elevation` renders as a drop shadow requiring
  gray levels. On E-ink this either disappears entirely or dithers into pixel noise.

**Confidence:** HIGH — Spacing + border grouping is the universal low-tech solution and directly
implementable with existing Compose primitives. Eink-ui CSS framework confirms this pattern.
The exact gap size (6dp vs 8dp vs 10dp) is a device-side UX judgment call.

---

## E-ink Rendering Reference: What Causes Ghosting

Documenting the failure modes to avoid during v1.1 implementation:

| Technique | E-ink Behavior | Verdict |
|-----------|---------------|---------|
| Solid black fill | Immediate, crisp | Safe |
| Solid white fill | Immediate, crisp | Safe |
| Pure Color.Black text | Crisp at all sizes | Safe |
| Pure Color.White text | Crisp at all sizes | Safe |
| Gray fill (any) | Dithered on partial refresh | Avoid in game UI |
| Partial alpha (< 1.0) | Dithered gray, noisy on small text | Avoid |
| BlendMode effects | May trigger full refresh cycle | Avoid |
| Animations (any) | Frame-by-frame ghosting | Forbidden (UI-02) |
| Ripple/indication | Ghost trail | Forbidden (UI-02) |
| Elevation / shadow | Requires gray levels, unreliable | Avoid |
| Dashed/dotted border | Irregular rendering at 216ppi | Avoid |
| Diagonal hatch fill | Gray-level artifact, noise | Avoid |
| Solid 1dp black border | Crisp, reliable | Safe |
| lineHeight trim (text) | Pure metric calculation, no rendering artifact | Safe |

---

## Table Stakes (v1.0, carried forward)

Features users expect in any Sudoku app. Missing = product feels broken or incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Puzzle generation | Core product — no puzzles, no game | High | Generate-then-verify approach: fill valid grid via backtracking, then remove cells. Difficulty controlled by solving technique required, NOT just cell count. |
| Three difficulty levels | Universal Sudoku convention (Easy / Medium / Hard) | Low | Easy: 36-45 givens, naked singles only. Medium: 27-35, requires pencil marks/pairs. Hard: 22-27, requires X-wing/advanced chains. Exact range less important than technique classification. |
| Cell selection + number entry | Primary input interaction | Medium | Tap cell to select, tap digit to fill. Must feel immediate on E-ink — no animation feedback delay. Large tap targets required (800x480 px means ~89x53px cells minimum). |
| Undo | Universal expectation; misplaced taps happen constantly on touch screens | Low | Single-level undo acceptable; unlimited undo preferred. Critical on E-ink where accidental taps are common due to delayed visual feedback. |
| Pencil marks / candidate notes | Expected by any intermediate-to-advanced solver | Medium | Small digit annotations in cell corners. Toggle between "fill" and "note" input modes. Without this, Medium+ puzzles are unsolvable for most players. |
| Incorrect entry detection | Players expect to know when they've made an error, even if only at the end | Low | This project uses silent tracking (revealed at game end). Still table stakes — the question is WHEN to surface it, not WHETHER to track it. |
| Game completion detection | Automatic puzzle-solved recognition | Low | Validate all 81 cells filled correctly; trigger completion screen. |
| Completion summary | Players need closure and feedback on performance | Low | Show error count, hints used, final score at minimum. This project already specifies this. |
| Pause and resume | Players stop mid-puzzle constantly; state loss = frustration | Medium | Persist full grid state, notes, and error count. On E-ink devices especially — users put devices down for hours. |
| Puzzle uniqueness | Each puzzle must have exactly one valid solution | High | Standard constraint for Sudoku generation. Violation destroys player trust. Verify at generation time. |

---

## Differentiators (v1.0, carried forward)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Error-based scoring (not time-based) | Aligns with mindful ethos — quality over speed | Low | Fewer errors = higher score. Fixed penalty per hint used. |
| Silent error tracking (revealed at end) | Encourages reflection over reactive correction | Low | Contrast: most apps use real-time auto-check. |
| Per-difficulty high score table | Personal progression per level | Low | Local device only. Three separate leaderboards. |
| Hint with score penalty (not disqualification) | Keeps game flowing; penalty is softer than hard-blocking | Low | One hint deducts fixed amount from final score. |
| MMD / E-ink native UI | App feels designed for the device, not ported | Medium | Uses ThemeMMD, high-contrast layout. No ripple, no animations. |
| Distraction-free, no ads / no social | The mindful device has a specific audience | Low | Silence is a feature. |
| Graceful handling of incomplete puzzles on relaunch | User trust on E-ink device where battery cycles happen | Low | Resume prompt on launch. |

---

## Anti-Features (v1.0, carried forward)

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Animations and transitions | E-ink ghosting | Instant state updates only |
| Real-time error highlighting | Removes cognitive engagement | Silent tracking; surface at end |
| Timer / time-based scoring | Creates anxiety; contradicts Mudita philosophy | Error-based scoring only |
| Daily challenges / streaks | Obligation anxiety | Unlimited on-demand generation |
| Global leaderboards / online ranking | No Google Services on MuditaOS K | Local per-difficulty high scores |
| Achievements / badges / XP | Visual noise, extrinsic motivation | Score + personal best is reward |
| Color themes / custom skins | E-ink is monochromatic | Single MMD theme |
| Ad monetization | Violates distraction-free premise | No ads |
| Auto-fill pencil marks | Reduces cognitive challenge | Manual only |
| Cloud save / cross-device sync | No backend | DataStore local persistence |
| Sound effects / haptics | E-ink audience values quiet focus | Silent play |

---

## Feature Dependencies

```
Puzzle generation (unique, solvable)
  └── Difficulty classification (technique-based)
        └── Three difficulty levels (Easy / Medium / Hard)

Cell selection
  └── Number entry
        └── Pencil mark entry (toggle mode)
              └── Silent error tracking
                    └── Completion detection
                          └── Completion summary (errors, hints, score)
                                └── Per-difficulty high score table

Pause state persistence
  └── Resume on relaunch prompt
        └── (depends on: cell state + pencil marks + error count + hint count)

Hint system
  └── Score penalty on hint use
        └── Hint count tracked
              └── (feeds into: completion summary + score calculation)
```

---

## Sources

**v1.1 research sources (2026-03-25):**
- [GameGrid.kt — existing implementation](../app/src/main/java/com/mudita/sudoku/ui/game/GameGrid.kt) — current pencil mark and selected cell rendering (HIGH confidence — primary source)
- [ControlsRow.kt — existing implementation](../app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt) — current button active/inactive pattern (HIGH confidence — primary source)
- [eink-ui CSS UI Framework — marcomattes/eink-css-ui-framework](https://github.com/marcomattes/eink-css-ui-framework) — "borders over fills, whitespace over color, clarity over decoration"; `.eink-divider` pattern (MEDIUM confidence — WebSearch)
- [How to Design for E-Ink Devices — withintent.com](https://www.withintent.com/blog/e-ink-design/) — E-ink UI design principles: static layouts, avoid rescaling, use pagination over scroll (MEDIUM confidence — WebSearch)
- [Support colorFilter/blendMode in DrawScope.drawText — Google Issue Tracker #242995485](https://issuetracker.google.com/issues/242995485) — confirms `drawText` color is set via `TextStyle.color`, not `blendMode` (HIGH confidence — official issue tracker)
- [Draw text in Compose — Android Developers](https://developer.android.com/develop/ui/compose/quick-guides/content/video/draw-text-compose) — `TextMeasurer` + `TextStyle(color)` is the authoritative approach for colored canvas text (HIGH confidence — official docs)
- [Graphics in Compose — Android Developers](https://developer.android.com/develop/ui/compose/graphics/draw/overview) — `BlendMode` options and their rendering behavior on `Canvas` (HIGH confidence — official docs)
- [Fonts for readability on eink — MobileRead Forums](https://www.mobileread.com/forums/showthread.php?t=366520) — community consensus: sans-serif, thicker weight, avoid condensed at small sizes (MEDIUM confidence — community forum)
- [Android App Font Size & Typography Guidelines — learnui.design](https://www.learnui.design/blog/android-material-design-font-size-guidelines.html) — tabular figures, line height metrics (MEDIUM confidence — design reference)
- [Pencilmark — Sudopedia Mirror](http://sudopedia.enjoysudoku.com/Pencilmark.html) — canonical 3x3 positional pencil mark layout ("corner marks" vs "center marks") (HIGH confidence — domain reference)
- [OutlinedButton — Material 3 Compose](https://composables.com/material3/outlinedbutton) — outlined button semantics for medium-emphasis inactive actions (HIGH confidence — official component docs)
- [LineHeightStyle — Android Developers](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/style/LineHeightStyle) — `Trim.Both` API for collapsing font metrics padding in Compose 1.4+ (HIGH confidence — official API reference)

**v1.0 research sources (2026-03-23, carried forward):**
- [Top 10 Best Sudoku Apps 2025 - sudokugames.org](https://www.sudokugames.org/blog/top-10-best-sudoku-apps-2025) — feature landscape survey (MEDIUM confidence)
- [Sudoku Puzzle Difficulty Levels Explained - sudokugames.org](https://www.sudokugames.org/blog/sudoku-puzzle-difficulty-levels) — difficulty classification (MEDIUM confidence)
- [Sudoku Puzzles Generating: from Easy to Evil - zhangroup.aporc.org](https://zhangroup.aporc.org/images/files/Paper_3485.pdf) — difficulty classification by required techniques (HIGH confidence)
- [Good Sudoku press kit - playgoodsudoku.com](https://www.playgoodsudoku.com/presskit/) — design philosophy (MEDIUM confidence)
- [Pencil marks / candidates explanation - sudokuconquest.com](https://www.sudokuconquest.com/blog/sudoku-basics-candidates-pencil-marking) — pencil mark mechanics (HIGH confidence)
