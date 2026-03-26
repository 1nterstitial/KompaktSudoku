# Project Research Summary

**Project:** Mudita Kompakt Sudoku — v1.1 Cosmetic UI Fixes
**Domain:** E-ink Android game UI (Compose Canvas + MMD library)
**Researched:** 2026-03-25
**Confidence:** HIGH for Canvas changes; MEDIUM for MMD typography interaction

---

## Executive Summary

The v1.1 milestone is a targeted cosmetic pass on five specific visual problems identified during first real-device play on the Mudita Kompakt. All five changes are confined to the UI layer — `GameGrid.kt`, `ControlsRow.kt`, and `NumberPad.kt` — with no ViewModel, domain model, or persistence changes required. The existing stack (Kotlin 2.3.20, Compose BOM 2026.03.00, MMD 1.0.1) is fully sufficient; no new dependencies are needed.

The highest-confidence fixes are the pencil mark color inversion (Change 1) and the mode button inactive state border (Change 4), both of which follow well-documented E-ink UI patterns with directly applicable Compose APIs. The highest-uncertainty fix is the number pad font/centering change (Change 3), where `TextMMD`'s internal typography implementation is closed-source and the exact parameter surface for `fontSize` and `lineHeight` overrides cannot be verified without a compile-time test. The recommended approach is to try `TextMMD` style parameters first and fall back to the existing `Box + clickable(indication=null)` pattern already established in `ControlsRow.kt` if needed.

The critical cross-cutting constraint for this milestone is E-ink rendering safety: every change must produce only solid black or solid white pixels, must use `Modifier.drawBehind` placed after all size constraints, and must avoid diagonal hatching fine enough to produce dithering artifacts. The PITFALLS research flags diagonal hatching (Change 4 alternative) as the highest E-ink risk in this milestone — the recommended alternative (solid border on inactive state) achieves the same goal with zero ghosting risk and should be preferred over hatching.

---

## Key Findings

### Recommended Stack

No new dependencies are required for v1.1. The existing stack handles all five changes:

- **Compose Canvas / DrawScope** — `drawPencilMarks` in `GameGrid.kt` uses `drawText` with `TextStyle`; color changes are achieved by creating a `pencilStyleSelected` variant with `color = Color.White` and branching at the call site.
- **`Modifier.drawBehind`** — the correct mechanism for drawing the inactive button visual in `ControlsRow.kt`; available in the existing `androidx.compose.ui.draw` package, no version change needed.
- **`Modifier.border` / `Surface(border = BorderStroke(...))`** — for the Fill/Pencil group container (Change 5); `Surface` with `BorderStroke` is already used in `SummaryScreen.kt`.
- **`LocalDensity`** — for converting `cellSizePx` (Float) to `TextUnit` (`.sp`) when computing dynamic pencil mark font size; must be accessed at composable scope inside `BoxWithConstraints`.
- **`LineHeightStyle.Trim.Both`** — Compose 1.4+ API (available in BOM 2026.03.00) for collapsing font metrics padding if `TextMMD` centering cannot be fixed another way.

See `.planning/research/STACK.md` for full version table and rationale.

### Expected Features (v1.1 Scope)

All five items are visual fixes, not new features. They address observed deficiencies in the shipped v1.0 product.

**The five targeted fixes:**
- **Pencil marks white-on-black in selected cell** — currently invisible (black text on black fill); standard Sudoku convention is to invert text color with the cell background.
- **Dynamic pencil mark font sizing** — fixed `9.sp` is fragile; derive from `cellSizePx / 3f` with a `remember(cellSizePx)` `TextStyle` so it adapts correctly.
- **Number pad digit centering** — default MMD typography adds `lineHeight` padding that makes single-digit labels appear vertically low; fix via `LineHeightStyle.Trim.Both` or `lineHeight = fontSize`.
- **Inactive mode button visual differentiation** — white-on-white inactive state is invisible on E-ink; a 1–2dp solid black border makes the button visually distinct without gray fills or animations.
- **Fill/Pencil pair visual grouping** — the Fill and Pencil buttons are semantically linked (mutually exclusive modes) but visually indistinguishable from Undo/Get Hint; a bordered inner `Row` wrapper signals the grouping.

**Confirmed anti-features to avoid in this milestone:**
- No gray fills, gradients, or alpha < 1.0 (dithers on E-ink partial refresh)
- No animations or animated state transitions
- No diagonal hatching finer than 6–8px spacing (partial refresh artifact risk)
- No `RoundedCornerShape` on any border (anti-aliased edges produce gray pixels on E-ink)
- No `Card` elevation or `Modifier.shadow` (shadow requires gray levels)

See `.planning/research/FEATURES.md` for full E-ink rendering reference table.

### Architecture Approach

All five changes are localized to three files in the `ui/game/` layer. No MVVM boundary is crossed; no state changes are required in `GameViewModel`. The Canvas-based `GameGrid` and the MMD-component-based `ControlsRow`/`NumberPad` require different implementation strategies because they operate in different rendering models (DrawScope vs Compose layout tree).

**Affected files and change type:**

| File | Changes | Rendering model |
|------|---------|----------------|
| `GameGrid.kt` | Changes 1 + 2 (pencil mark color + font sizing) | `DrawScope` / Canvas |
| `ControlsRow.kt` | Changes 4 + 5 + Get Hint centering sub-fix | Compose layout + `drawBehind` modifier |
| `NumberPad.kt` | Change 3 (number pad font centering) | MMD component (`TextMMD` inside `ButtonMMD`) |

**Key structural constraints:**
- Changes 1 and 2 must be implemented together — both touch `pencilStyle` declarations and the `drawPencilMarks` call site. Doing them separately creates a broken intermediate state.
- Changes 4 and 5 must be implemented together — both modify the Fill/Pencil `Box` composables and their surrounding layout in `ControlsRow.kt`.
- `pencilStyle` TextStyle objects must be declared at composable scope (not inside the DrawScope lambda) and wrapped in `remember(cellSizePx)` to avoid per-frame reallocation.
- The `weight(1f)` on Fill and Pencil boxes must remain valid within their immediate `Row` parent. The Change 5 wrapper must be a `Row` with `weight(2f)`, not a `Box` — a `Box` wrapper breaks `RowScope`-scoped weight modifiers.

See `.planning/research/ARCHITECTURE.md` for precise line references and code structure.

### Critical Pitfalls

**v1.1-specific pitfalls (Pitfalls 13–17):**

1. **`drawText` color not baked into TextStyle at measure time (Pitfall 13)** — The `drawText(textLayoutResult, topLeft)` overload ignores runtime color; color must be in the `TextStyle` passed to `textMeasurer.measure()`. The existing `digitStyleSelected*` pattern in `GameGrid.kt` is the correct model to follow. Do not use `BlendMode` or the `drawText(color=...)` overload as a workaround.

2. **Pixel-to-sp unit conversion error for pencil font size (Pitfall 14)** — `cellSizePx / 3f` is a pixel value; passing it directly as `.sp` produces a wildly oversized font (~40px at Kompakt density). Convert using `with(LocalDensity.current) { targetPx.toSp() }` at composable scope. Apply a `0.55–0.7×` safety margin and enforce a `7sp` floor for E-ink legibility.

3. **ThemeMMD typography silently overriding `TextMMD` style parameters (Pitfall 15)** — `ButtonMMD` injects its own `ProvideTextStyle` internally, which has higher specificity than any outer override. If `TextMMD` does not accept `fontSize` or `lineHeight` parameters that take effect, fall back to the `Box + clickable(indication=null)` pattern from `ControlsRow.kt`. Do not attempt `LocalTextStyle` composition local overrides above `ButtonMMD`.

4. **`drawBehind` modifier order causes size mismatch (Pitfall 16)** — `Modifier.drawBehind { }` must be placed after all size-constraining modifiers in the chain so the `DrawScope.size` reflects the constrained layout size. If using hatching, keep line spacing >= 6px; prefer the solid border approach instead.

5. **`weight(1f)` breaking when wrapped in `Box` (Pitfall 17)** — `Modifier.weight(n)` is `RowScope`-scoped and only works when the composable is a direct child of a `Row`. The Change 5 wrapper must be `Row(modifier = Modifier.weight(2f))`, not `Box`. Use `border(2.dp, Color.Black, RectangleShape)` — not rounded corners, not shadow.

---

## Implications for Roadmap

This milestone is a single execution phase with three natural implementation groups based on file ownership and dependency coupling. The ordering below minimizes risk by front-loading zero-uncertainty changes and leaving the MMD API question for last.

### Group 1: GameGrid Changes (Changes 1 + 2 together)

**Rationale:** `GameGrid.kt` is entirely self-contained — pure Compose Canvas APIs, no MMD component involvement, no API uncertainty. Changes 1 and 2 share the same code region (`pencilStyle` declarations and `drawPencilMarks` call site) and must be done in a single edit pass. Implementing these first gives immediately verifiable results on device before any MMD-adjacent work begins.

**Delivers:** Visible pencil marks on selected cells (white-on-black); robust dynamic font sizing that adapts to grid area.

**Key implementation decisions:**
- Define `pencilStyleNormal` (black) and `pencilStyleSelected` (white) at composable scope with `remember(cellSizePx)`.
- Move `pencilStyle` declarations inside `BoxWithConstraints` (after `cellSizePx` is computed).
- Use `with(LocalDensity.current) { (cellSizePx / 3f * 0.55f).toSp() }` for font sizing; apply `maxOf(7f, computed).sp` floor.
- Branch on `cell.isSelected` at the `drawPencilMarks` call site.

**Avoids:** Pitfall 13 (color baked at measure time), Pitfall 14 (correct px-to-sp at composable scope).

**Research flag:** None — standard Compose Canvas, fully documented, source directly readable.

---

### Group 2: ControlsRow Changes (Changes 4 + 5 + Get Hint sub-fix)

**Rationale:** Changes 4 and 5 touch overlapping code in `ControlsRow.kt` and must be implemented together. The Get Hint centering fix (explicit `\n` in the TextMMD string) is a one-liner in the same file — most cleanly done before the layout restructure so the diff is independently readable. The `Box + clickable(indication=null)` pattern is already established in this file, so there is no new pattern risk.

**Delivers:** Visually distinct inactive mode button; Fill/Pencil pair clearly grouped as a unit; "Get Hint" text consistently two-line-centered.

**Key implementation decisions:**
- Get Hint: change `TextMMD(text = "Get Hint")` to `TextMMD(text = "Get\nHint", textAlign = TextAlign.Center)`.
- Change 4 (inactive state): apply `Modifier.drawBehind { ... }` after all size modifiers; draw a solid 1dp (or 2dp if 1dp appears gray on device) black border, NOT diagonal hatching.
- Change 4: also apply a matching border to the active state (`background(Color.Black).border(1.dp, Color.Black)`) so button size does not shift on mode toggle.
- Change 5: wrap Fill + Pencil boxes in `Row(modifier = Modifier.weight(2f).border(2.dp, Color.Black, RectangleShape))`.
- Inner Fill and Pencil boxes keep `weight(1f)` referencing the new inner Row scope.

**Avoids:** Pitfall 16 (drawBehind after size modifiers; no diagonal hatching), Pitfall 17 (inner Row not Box; RectangleShape; no shadow; 2dp border).

**Research flag:** Device verification needed to confirm border renders fully black at fast waveform speeds. If 1dp appears gray, use 2dp throughout.

---

### Group 3: NumberPad Font Centering (Change 3)

**Rationale:** This is the highest-uncertainty change due to the closed-source `TextMMD` component. Placed last so the other four changes are shipped and verifiable before iterating on this one. The fallback path is well-understood and already proven in `ControlsRow.kt`.

**Delivers:** Vertically centered single-digit labels in the number pad.

**Key implementation decisions — attempt in order:**
1. Try `TextMMD(text = digit.toString(), fontSize = 18.sp)` — if it compiles and centers the text, done.
2. If (1) does not visually center: add `lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)` to the TextStyle.
3. If `TextMMD` ignores the style override: replace `TextMMD` inside `ButtonMMD` with `BasicText` (not plain `Text`) and a full explicit `TextStyle`. Add inline comment: `// UI-01 exception: TextMMD does not expose fontSize for number pad digits`.
4. Do NOT attempt Option C (fully custom `NumberPadButton` with `Box + clickable`) in this milestone — too much structural change for a cosmetic fix.

**Avoids:** Pitfall 15 (no `LocalTextStyle` outer override; no custom font file; no `paddingTop` hacks; no Bold weight).

**Research flag:** Compile-time verification required at the start of execution. Build with Option A first; the build result immediately determines the path.

---

### Phase Ordering Rationale

- **GameGrid first** — zero MMD uncertainty, zero layout complexity, immediately verifiable on device.
- **ControlsRow second** — Changes 4 and 5 are tightly coupled; the existing `indication=null` pattern eliminates new pattern risk.
- **NumberPad last** — only change with compile-time uncertainty about the MMD API surface; isolating it last minimizes iteration blast radius.
- Changes 1+2 are atomically coupled (same TextStyle declarations, same call site) — single edit pass.
- Changes 4+5 are atomically coupled (same Box composables, overlapping layout region) — single edit pass.

### Research Flags

- **Change 3 (NumberPad):** Compile-time test at execution start determines Option A/B/C path. Not a research-phase item — it is an immediate decision point.
- **Changes 4+5 (border thickness):** Device verification to confirm 1dp vs 2dp border appearance at fast waveform. Default to 2dp for E-ink reliability.
- **Change 2 (pencil font size multiplier):** `0.55f` is a design-ratio estimate. Tune on device; verify all 9 pencil marks fit without clipping in a single cell.

Standard patterns (no further research needed):
- **Change 1 (pencil mark color):** Compose `drawText` + `TextStyle(color)` is fully documented. Zero uncertainty.
- **Change 5 (Surface + BorderStroke):** Pattern already in `SummaryScreen.kt`. Zero uncertainty.
- **Get Hint text centering:** Explicit `\n` in TextMMD text string. Trivial.

---

## Open Questions for Device Verification

| Question | Affects | What to Check |
|----------|---------|--------------|
| Does `TextMMD` accept `fontSize` parameter? | Change 3 path selection | Build with `TextMMD(text="1", fontSize=18.sp)` — compile success/failure determines Option A vs B vs C |
| Do 1dp borders render fully black at fast waveform? | Changes 4 + 5 | Visual inspection on device; increase to 2dp if border appears gray |
| Does dynamic pencil font at `cellSizePx / 3f * 0.55f` fit all 9 marks without clipping? | Change 2 | Visual inspection with all 9 pencil marks in one cell |
| Does `lineHeightStyle = LineHeightStyle(trim = Trim.Both)` take effect through `TextMMD`? | Change 3 Option B | Check if digit vertical centering improves on device |
| Does the 6dp spacer between ControlsRow and NumberPad provide sufficient visual separation? | Overall layout polish | Player testing judgment call; adjust to 8dp or 10dp if needed |

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack (no new deps required) | HIGH | All needed APIs exist in current BOM; confirmed |
| Features (5 targeted fixes, scope) | HIGH | All five fixes are directly observed problems with documented solutions |
| Architecture (file/function targets) | HIGH | Source directly readable; all integration points identified by line number |
| Change 1 (drawText color) | HIGH | Confirmed via Google Issue Tracker #242995485 and Compose drawText API docs |
| Change 2 (px-to-sp conversion) | HIGH | Standard density chain; correct formula confirmed in ARCHITECTURE.md |
| Change 3 (TextMMD API surface) | MEDIUM | Closed-source AAR; typography injection inferred from Material 3 internals |
| Change 4 (drawBehind + E-ink) | HIGH layout / MEDIUM E-ink artifact | drawBehind API confirmed; diagonal artifact risk inferred from E-ink physics |
| Change 5 (weight scope + Surface) | HIGH | Compose RowScope weight behavior well-documented; Surface pattern already in codebase |

**Overall confidence:** HIGH for structural approach; MEDIUM for MMD font parameter interaction in Change 3.

### Gaps to Address

- **`TextMMD` font parameter surface** — Cannot be resolved without a compile test. Attempt Option A at the start of Change 3 execution; document the result as a convention note in `CLAUDE.md` (specifically whether `TextMMD` accepts `fontSize` / `lineHeight` overrides).
- **Border render thickness on device** — Accept 2dp as the default for any new border in this milestone. Verify 1dp is acceptable before reducing.
- **Pencil font size multiplier** — `0.55f` is a starting estimate from design ratios. Tune on device; the correct value is the smallest multiplier that produces visible, non-clipping glyphs across all 9 pencil marks in one cell.

---

## Sources

### Primary (HIGH confidence — official docs and direct source inspection)

- `GameGrid.kt` lines 76–168 — direct source reading (pencilStyle declarations, drawPencilMarks call site, drawText overload in use)
- `ControlsRow.kt` lines 44–105 — direct source reading (Box layout, indication=null pattern, MMD caveat comment at line 27)
- `NumberPad.kt` lines 26–50 — direct source reading (TextMMD call sites, confirmed parameter surface)
- `SummaryScreen.kt` line 75 — `Surface(border = BorderStroke(...))` pattern already in codebase
- [Draw text in Compose — Android Developers](https://developer.android.com/develop/ui/compose/quick-guides/content/video/draw-text-compose) — `TextMeasurer` + `TextStyle(color)` authoritative approach
- [Support colorFilter/blendMode in DrawScope.drawText — Google Issue Tracker #242995485](https://issuetracker.google.com/issues/242995485) — confirms color must be in TextStyle at measure time
- [Compose drawBehind modifier — Android Developers](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers) — drawBehind API reference
- [LineHeightStyle — Android Developers](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/style/LineHeightStyle) — Trim.Both API for font metrics padding (Compose 1.4+)
- [Constraints and modifier order — Android Developers](https://developer.android.com/develop/ui/compose/layouts/constraints-modifiers) — modifier ordering and size constraints
- [OutlinedButton — Material 3 Compose](https://composables.com/material3/outlinedbutton) — border-over-fill inactive state semantics

### Secondary (MEDIUM confidence — community and framework analysis)

- [eink-ui CSS UI Framework — marcomattes/eink-css-ui-framework](https://github.com/marcomattes/eink-css-ui-framework) — "borders over fills, whitespace over color" E-ink design consensus; `.eink-divider` pattern
- [Pencilmark — Sudopedia Mirror](http://sudopedia.enjoysudoku.com/Pencilmark.html) — 3x3 positional pencil mark layout is the universal standard
- [Fonts for readability on eink — MobileRead Forums](https://www.mobileread.com/forums/showthread.php?t=366520) — sans-serif, avoid condensed, medium weight for E-ink digits
- [E-ink Display Integration — Ghosting and Refresh Challenges — Core Electronics Forum](https://forum.core-electronics.com.au/t/e-ink-display-integration-ghosting-and-refresh-challenges/23151) — diagonal hatching partial refresh artifact risk
- [MMD GitHub releases page](https://github.com/mudita/MMD/releases) — MMD 1.0.1 version confirmed; TextMMD/ButtonMMD parameter surface not publicly documented

---

*Research completed: 2026-03-25*
*Scope: v1.1 cosmetic UI fixes (5 changes, UI layer only, no new dependencies)*
*Ready for roadmap: yes*
