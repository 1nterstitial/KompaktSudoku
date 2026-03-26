# Architecture — v1.1 UI Integration Points

**Scope:** Cosmetic UI fixes for the existing Mudita Kompakt Sudoku app (v1.1 milestone)
**Researched:** 2026-03-25
**Confidence:** HIGH for Canvas draw loop changes; MEDIUM for TextMMD font parameter; LOW for ButtonMMD textStyle (API not publicly documented)

---

## Context

The existing codebase is a deployed v1.0 app. All five changes target the UI layer only — no ViewModel, no domain model, no persistence. The Canvas-based `GameGrid` and the MMD-wrapped `ControlsRow`/`NumberPad` require different approaches because they operate in fundamentally different rendering models.

---

## Change 1 — Pencil Mark Text Color on Selected Cell

**File:** `app/src/main/java/com/mudita/sudoku/ui/game/GameGrid.kt`
**Function:** `GameGrid` composable (lines 96–100), `DrawScope.drawPencilMarks` (lines 204–222)

### Current state

A single `pencilStyle` `TextStyle` is defined at composable level (lines 96–100):

```kotlin
val pencilStyle = TextStyle(
    fontSize = 9.sp,
    fontWeight = FontWeight.Normal,
    color = Color.Black        // always black — invisible on selected (black-fill) cell
)
```

This style is passed unconditionally into `drawPencilMarks()` at line 157:

```kotlin
drawPencilMarks(
    marks = cell.pencilMarks,
    ...
    style = pencilStyle        // same style regardless of isSelected
)
```

### What needs to change

Two `TextStyle` variants must be defined — one for the normal case, one for the selected case — and the call site at line 155–163 must branch on `cell.isSelected`:

- `pencilStyleNormal`: `color = Color.Black` (current behavior)
- `pencilStyleSelected`: `color = Color.White` (white text on black background)

The branch point is in the `forEach` at lines 152–163. `CellData` already carries `isSelected: Boolean` (line 35), so no model changes are required.

**`drawPencilMarks` signature does not need to change** — the style is passed in from the call site. All changes are localized to the `GameGrid` composable body, between the two `val pencilStyle` declarations (lines 96–100) and the `drawPencilMarks` call site (lines 155–163).

### MMD constraint

None. `drawPencilMarks` is a `DrawScope` extension function using raw Compose Canvas APIs (`drawText`, `TextStyle`). No MMD components are involved. The change is pure Canvas drawing.

### Confidence

HIGH — all code paths visible in source; no hidden dependencies.

---

## Change 2 — Pencil Mark Font Sizing (Fit 2×2 / 4-digit Constraint)

**File:** `app/src/main/java/com/mudita/sudoku/ui/game/GameGrid.kt`
**Function:** `GameGrid` composable (lines 96–100), `DrawScope.drawPencilMarks` (lines 204–222)

### Current state

`pencilStyle` uses a hardcoded `fontSize = 9.sp` (line 97). This is independent of cell size — at smaller display densities or in landscape orientation, 9sp may overflow the sub-cell.

`drawPencilMarks` lays out digits in a **3×3 sub-grid** (lines 212–221):

```kotlin
val subSize = cellSize / 3f          // each sub-cell is 1/3 of cellSizePx
for (digit in 1..9) {
    val subRow = (digit - 1) / 3
    val subCol = (digit - 1) % 3
    // positions each digit in its (subRow, subCol) slot
}
```

The requirement says "fit 4 marks in a 2×2 layout at maximum." This implies changing the layout from 3×3 (9 slots) to 2×2 (4 slots) — which would only show digits 1–4 and break the conventional Sudoku pencil-mark convention where digit _n_ lives at position _n_. The more defensible interpretation is: **the font must be small enough that a full 3×3 of digits fits without clipping**, with the 2×2 framing being a perceptual benchmark (font should look correct when only 4 marks are shown in the corners).

### What needs to change

Replace the hardcoded `9.sp` with a value derived from `cellSizePx`. The formula should ensure the tallest digit fits inside `subSize = cellSize / 3f` with margin:

```
pencilFontSizePx = cellSizePx / 3f * 0.55f    // ~55% of sub-cell height
```

This `Px` value must be converted to `Sp` using `LocalDensity` before passing to `TextStyle`, because `TextStyle.fontSize` takes `TextUnit` (`.sp`), not raw pixels:

```kotlin
val density = LocalDensity.current
val pencilFontSizeSp = with(density) { (cellSizePx / 3f * 0.55f).toSp() }
```

`cellSizePx` is computed at line 105 (`val cellSizePx = gridSizePx / 9f`) inside the `BoxWithConstraints` block (line 102), so the font size derivation must also live inside that block, after `cellSizePx` is available. The current `pencilStyle` declarations (lines 76–100) are above `BoxWithConstraints` and would need to move inside it.

**Structural note:** Moving `pencilStyle` (and by extension `digitStyle*`) inside `BoxWithConstraints` is a localized restructure. The `textMeasurer` at line 73 must remain at composable level (it is a `remember` — calling `rememberTextMeasurer()` inside `BoxWithConstraints` is legal but less idiomatic; it is simpler to keep it at the top level).

### MMD constraint

None. Pure Canvas / `TextStyle` / `LocalDensity` — no MMD components involved.

### Dependency on Change 1

Changes 1 and 2 both touch `pencilStyle` declaration and the `drawPencilMarks` call site. They must be made together in a single edit pass to avoid a half-broken intermediate state. Recommended: resolve the `TextStyle` variants (Change 1) and the sizing formula (Change 2) in the same code block.

### Confidence

HIGH for the approach; MEDIUM for the exact `0.55f` multiplier — this should be tuned on device, but the structural approach (derive from `cellSizePx`, convert to `Sp`) is correct.

---

## Change 3 — Number Pad Font (Condensed/Taller)

**File:** `app/src/main/java/com/mudita/sudoku/ui/game/NumberPad.kt`
**Function:** `NumberPad` composable (lines 21–50)
**Relevant call:** `TextMMD(text = digit.toString())` at lines 37 and 47

### Current state

`TextMMD` is called with only `text` — no font, size, or style override. The observed issue (text not vertically centered, appears short/wide) is caused by MMD's default `eInkTypography` body style applying a system-default font with non-ideal metrics for single-digit labels in a 56dp-tall button.

### MMD constraint — MEDIUM confidence

`TextMMD`'s confirmed parameter surface (from all call sites across the codebase):
- `text: String`
- `color: Color`
- `fontWeight: FontWeight`
- `textAlign: TextAlign`
- `modifier: Modifier`

**No `fontSize`, `fontFamily`, or `style`/`textStyle` parameter has been used anywhere in the codebase.** Whether `TextMMD` exposes these parameters is unknown without access to the compiled AAR or source.

The v1.0 `ControlsRow.kt` comment (line 27) explicitly flags this uncertainty: _"ButtonMMD's `colors` parameter availability is unconfirmed at compile time."_ The same caveat extends to `TextMMD`'s font parameters.

### Approach options (ordered by preference)

**Option A — `TextMMD` with `fontSize` parameter (try first)**
If `TextMMD` accepts a `fontSize: TextUnit` parameter (plausible given it wraps Material 3 `Text`), pass `fontSize = 18.sp` with a condensed system font. Zero structural change to `NumberPad.kt`.

```kotlin
TextMMD(text = digit.toString(), fontSize = 18.sp)
```

This must be verified at compile time. If the parameter does not exist, the build fails immediately with an unresolved reference.

**Option B — Wrap in Compose `BasicText` with custom style (fallback)**
Replace `TextMMD` inside the `ButtonMMD` content lambda with a plain Compose `BasicText` (not `Text`) with an explicit `TextStyle`. `BasicText` is a lower-level primitive that MMD theming does not wrap, so E-ink-specific optimizations from MMD typography would be bypassed — acceptable for single-digit labels where the visual is just a number.

**MMD compliance note:** The project convention mandates `TextMMD` for all user-facing text. Replacing it with `BasicText` inside a `ButtonMMD` is a narrow exception — document it inline (`// UI-01 exception: TextMMD lacks fontSize control for number pad digits`).

**Option C — Custom wrapper composable `NumberPadButton`**
Extract a private `@Composable fun NumberPadButton(digit: Int, ...)` that uses a `Box` + `clickable(indication=null)` pattern (same as `ControlsRow`'s Fill/Pencil toggles) with full `TextStyle` control. This removes `ButtonMMD` from `NumberPad.kt` for the digit buttons, introducing visual inconsistency with other buttons on screen unless the `Box` carefully replicates `ButtonMMD`'s border and padding. **Not recommended for a cosmetic fix milestone** — too much structural change.

### Recommended path

Attempt Option A first. If the build fails, fall back to Option B with an inline comment. Do not attempt Option C in v1.1.

### The "Get Hint" centering sub-issue

The requirement also mentions: _"Get Hint button text centered (two lines, vertically centered)."_ This is in `ControlsRow.kt` at line 102: `TextMMD(text = "Get Hint")`. The hint button uses `ButtonMMD` which has `contentAlignment = Alignment.Center` by default in Compose Material 3. If the text wraps to two lines, vertical centering within the button is a `ButtonMMD` internal layout concern.

Fix options:
1. Replace with `TextMMD(text = "Get\nHint", textAlign = TextAlign.Center)` — explicit newline forces consistent two-line layout.
2. Apply `modifier = Modifier.wrapContentHeight()` on the `TextMMD` — already centered by `ButtonMMD` layout.

Option 1 is simpler and has no compile-time uncertainty. This change is localized to line 102 of `ControlsRow.kt`.

### Confidence

MEDIUM for Option A (TextMMD fontSize unknown), HIGH for Option B (BasicText API confirmed in Compose).

---

## Change 4 — ControlsRow Inactive State (Diagonal Hatching)

**File:** `app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt`
**Function:** `ControlsRow` composable (lines 36–105)
**Relevant blocks:** Fill Box (lines 49–64), Pencil Box (lines 67–82)

### Current state

The active/inactive visual is implemented entirely in the `Box` modifier chain (lines 50–57 for Fill, 68–75 for Pencil):

```kotlin
.background(if (inputMode == InputMode.FILL) Color.Black else Color.White)
```

The inactive button is plain white with black text — no secondary visual distinguishing it from a plain button. The requirement asks for a "subtle background" on the inactive button.

### What needs to change

The `Box` composable already owns the background layer. The cleanest approach is to replace `.background(Color.White)` on the inactive state with a `Canvas` drawn inside the `Box`, overlaid using `Box`'s Z-ordering (children draw on top of each other in declaration order).

**Structural approach: nested Canvas inside existing Box**

The `Box` allows multiple children. Add a `Canvas(modifier = Modifier.matchParentSize())` as the first child that draws diagonal hatching when inactive, then the `TextMMD` as the second child (on top). Active state: the `Box` keeps `Color.Black` background and the canvas draws nothing.

```
Box (background = Color.Black when active, Color.White when inactive)
  ├── Canvas (Modifier.matchParentSize()) — draws hatching when inactive
  └── TextMMD (centered, color = white/black per state)
```

**Hatching implementation** uses `DrawScope.drawLine` in a loop — a diagonal pattern with `color = Color.LightGray` and `strokeWidth = 0.5.dp.toPx()`. This is pure Canvas API with no MMD involvement.

**Alternative: `drawBehind` modifier**
Compose's `Modifier.drawBehind { }` draws into the Box's background layer without adding a child composable. This is cleaner than a nested `Canvas`:

```kotlin
.drawBehind {
    if (isInactive) drawHatchPattern(...)
}
```

`drawBehind` is available in `androidx.compose.ui.draw` — no new dependencies. The hatch drawing logic can be extracted to a private `DrawScope` extension function to keep the modifier chain readable.

**Recommended:** Use `drawBehind` — it is structurally equivalent to the current `.background(...)` modifier and adds no layout complexity.

### MMD constraint

The Fill/Pencil toggles already bypass `ButtonMMD` (using `Box+clickable` per the ControlsRow comment: _"ButtonMMD's `colors` parameter availability is unconfirmed"_). The `drawBehind` change is fully within Compose's drawing modifier layer — MMD has no involvement.

### Confidence

HIGH — `drawBehind` is standard Compose API. Hatch pattern is a trivial Canvas loop.

---

## Change 5 — ControlsRow Fill/Pencil Frame

**File:** `app/src/main/java/com/mudita/sudoku/ui/game/ControlsRow.kt`
**Function:** `ControlsRow` composable (lines 36–105), specifically the outer `Row` (lines 44–46) and the first two `Box` children (lines 49–82)

### Current state

The four elements (Fill, Pencil, Undo, Get Hint) sit in a `Row` with `Arrangement.spacedBy(8.dp)`. All four have equal `weight(1f)`. There is no visual grouping separating the Fill/Pencil pair from Undo/Get Hint.

### What needs to change

Wrap the Fill and Pencil `Box` composables in a shared container with a border/background. The minimal structural change is:

```
Row (existing, fills full width)
  ├── Row or Box (NEW — wraps Fill + Pencil, has border modifier)
  │     ├── Fill Box (existing)
  │     └── Pencil Box (existing)
  ├── Undo ButtonMMD (unchanged)
  └── Get Hint ButtonMMD (unchanged)
```

**Modifier approach on the wrapping Row:**

```kotlin
Row(
    modifier = Modifier
        .weight(2f)                              // occupies 2 of 4 equal slots
        .border(width = 1.dp, color = Color.Black)
        .padding(1.dp)                           // prevent content from touching border
) {
    // Fill Box — weight(1f) within this inner Row
    // Pencil Box — weight(1f) within this inner Row
}
```

The `weight(2f)` on the wrapper and `weight(1f)` on the Undo and Get Hint buttons preserves the existing 1:1:1:1 slot ratio (the 2f wrapper spans the same space as two 1f buttons).

**Inner Box weights must be updated:** Currently Fill and Pencil each have `.weight(1f)` referencing the outer `Row`. After wrapping, they reference the inner `Row` — the value stays `weight(1f)` but the `RowScope` changes. This is a structural change that requires careful re-reading of the layout.

**Alternative: `border` modifier directly on a `Surface`**

Use `Surface(border = BorderStroke(1.dp, Color.Black))` as the wrapping composable instead of a plain `Row`. `Surface` already exists in the codebase (`SummaryScreen.kt` line 75 uses it). This avoids manual border math.

```kotlin
Surface(
    border = BorderStroke(1.dp, Color.Black),
    modifier = Modifier.weight(2f)
) {
    Row {
        // Fill Box (weight 1f)
        // Pencil Box (weight 1f)
    }
}
```

**Recommended:** Use `Surface` with `BorderStroke` — it matches the pattern already in `SummaryScreen.kt` and makes the intent explicit.

### MMD constraint

`Surface` is a Material 3 composable, not an MMD component. It is already used in `SummaryScreen.kt`, so it is an established pattern in the codebase. No MMD compliance issue — MMD's `ThemeMMD` sets the `LocalContentColor` and color scheme, which `Surface` respects.

### Dependency on Change 4

Changes 4 and 5 both modify the Fill/Pencil `Box` composables and their surrounding layout. They must be coordinated:
- Change 4 adds `drawBehind` to the individual Fill and Pencil boxes.
- Change 5 adds a `Surface` wrapper around both boxes.

These do not conflict but touching the same region twice in separate edits risks introducing merge errors. Implement both in a single pass.

### Confidence

HIGH — `Surface` with `BorderStroke` is a confirmed Compose API used in the same codebase.

---

## Build Order

### Independent changes (can be done in any order, no cross-file dependencies)

| Order | Change | File | Risk |
|-------|--------|------|------|
| 1 | Pencil color + font sizing (Changes 1 + 2 together) | `GameGrid.kt` | LOW — pure Canvas, no MMD |
| 2 | Get Hint text centering (explicit newline) | `ControlsRow.kt` line 102 | LOW — trivial text change |
| 3 | Fill/Pencil inactive hatching (Change 4) | `ControlsRow.kt` | LOW — `drawBehind` modifier |
| 4 | Fill/Pencil frame (Change 5) | `ControlsRow.kt` | MEDIUM — layout restructure |
| 5 | NumberPad font (Change 3) | `NumberPad.kt` | MEDIUM — TextMMD API uncertainty |

### Ordering rationale

**Changes 1+2 first:** GameGrid is entirely self-contained. No MMD uncertainty. Verifiable on device immediately after applying. Doing this first gives confidence before tackling the riskier MMD parameter questions.

**Get Hint text fix before layout changes:** It is a one-line change in `ControlsRow.kt`. If done before the layout restructure (Change 5), the diff is cleaner and the change is independently reviewable.

**Change 4 before Change 5:** Hatching (`drawBehind`) is additive and does not alter layout. The Surface wrapper (Change 5) restructures the Row hierarchy. Applying 4 first means the boxes are in their final state before wrapping them.

**Change 3 last:** TextMMD font is the highest-uncertainty item. If Option A (direct `fontSize` parameter) fails to compile, Option B requires a convention exception and a comment. Doing this last means the other four changes are already done and tested, reducing the blast radius of any iteration on Change 3.

---

## Structural Constraints Summary

| Change | MMD Constraint? | Structural Risk | Notes |
|--------|----------------|----------------|-------|
| 1 (pencil color) | None | LOW | Add one TextStyle variant, branch at call site |
| 2 (pencil sizing) | None | LOW-MEDIUM | Move style declarations inside BoxWithConstraints |
| 3 (number pad font) | MEDIUM — TextMMD fontSize unknown | MEDIUM | Verify at compile time; Option B fallback available |
| 4 (inactive hatching) | None | LOW | drawBehind modifier, no layout change |
| 5 (pair frame) | None | MEDIUM | Layout restructure; use Surface+BorderStroke pattern |

---

## Sources

- `GameGrid.kt` — direct source reading, lines 76–168 (draw loop, pencilStyle, drawPencilMarks)
- `ControlsRow.kt` — direct source reading, lines 44–105 (Box layout, indication=null pattern, MMD caveat comment)
- `NumberPad.kt` — direct source reading, lines 26–50
- `SummaryScreen.kt` — Surface+BorderStroke pattern reference, line 75
- `MenuScreen.kt`, `DifficultyScreen.kt`, `SummaryScreen.kt`, `LeaderboardScreen.kt` — TextMMD confirmed parameter surface (text, color, fontWeight, textAlign, modifier; no fontSize seen)
- [Compose drawBehind API](https://developer.android.com/reference/kotlin/androidx/compose/ui/draw/package-summary#(androidx.compose.ui.Modifier).drawBehind(kotlin.Function1)) — HIGH confidence
- [Compose LocalDensity / toPx / toSp](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/package-summary#LocalDensity) — HIGH confidence
- [MMD GitHub releases page](https://github.com/mudita/MMD/releases) — version confirmed (1.0.1), ButtonMMD/TextMMD parameter surface not publicly documented
