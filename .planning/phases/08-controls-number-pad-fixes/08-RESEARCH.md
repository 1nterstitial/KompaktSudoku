# Phase 8: Controls & Number Pad Fixes — Research

**Researched:** 2026-03-26
**Domain:** Jetpack Compose layout, typography, borders — all within closed-source MMD constraints
**Confidence:** HIGH (all Compose APIs verified; MMD surface uncertainty documented explicitly)

---

## Summary

Phase 8 makes four small, surgical changes to two files. The Compose APIs needed (FontFamily,
BasicText, Modifier.border, Row with nested weight) are stable and well-documented. The main
uncertainty is whether TextMMD exposes fontFamily and textAlign parameters — the AAR is closed
source and cannot be inspected at compile time. Every requirement has a proven primary path AND a
verified fallback that does not touch TextMMD's unknown API surface.

The most structurally interesting change is CTRL-04: wrapping Fill+Pencil in a shared container
with a border. The pattern requires lifting the two Boxes out of the top-level Row and placing them
in an inner Row that itself receives `weight(2f)` from the outer Row. The inner Boxes then each
take `weight(1f)` inside the inner Row. This preserves equal visual width for all four buttons.

**Primary recommendation:** Implement all four changes in a single plan against two files.
The changes are small, non-overlapping, and share one test run. Splitting into two plans adds
ceremony without safety benefit.

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CTRL-01 | Number pad digit text is vertically centered on its button with a condensed/taller font (`sans-serif-condensed`) | FontFamily(Typeface.create()) API confirmed; BasicText fallback pattern documented |
| CTRL-02 | "Get Hint" button text is vertically centered within the button (two lines, centered) | TextMMD newline + textAlign primary path; Column fallback with two TextMMD calls documented |
| CTRL-03 | Inactive Fill/Pencil mode button displays a solid mid-gray background (`#E0E0E0`); active button retains solid black | Single Color literal swap; no structural change required |
| CTRL-04 | Fill and Pencil buttons are visually grouped with a 1dp black border frame separating them from Undo/Get Hint | Modifier.border import confirmed; nested Row + weight(2f) outer / weight(1f) inner pattern verified |
</phase_requirements>

---

## Project Constraints (from CLAUDE.md)

- **Tech stack:** Kotlin 2.3.20, Jetpack Compose BOM 2026.03.00, AGP 8.9.0
- **SDK:** minSdk 31, targetSdk 31, compileSdk 35
- **MMD:** ButtonMMD and TextMMD from `com.mudita.mmd.components.buttons.ButtonMMD` /
  `com.mudita.mmd.components.text.TextMMD` — prefer over plain Compose components
- **No ripple:** all `clickable` calls must use `indication = null`
- **No animations:** no `Animated*` composables
- **No rounded corners:** use `RectangleShape` exclusively (anti-aliased gray pixels on E-ink)
- **Monochromatic:** only `Color.Black`, `Color.White`, `Color(0xFFE0E0E0)`
- **Architecture:** MVVM + StateFlow — no changes needed for this purely visual phase

---

## Standard Stack

No new dependencies required for this phase. All needed APIs are already on the compile classpath.

### APIs in Scope

| API | Package | Already Imported? | Notes |
|-----|---------|-------------------|-------|
| `Modifier.border()` | `androidx.compose.foundation` | No — new import needed in ControlsRow.kt | Stable since Compose 1.0 |
| `RectangleShape` | `androidx.compose.foundation.shape` | No — new import needed in ControlsRow.kt | Required by border shape param |
| `FontFamily` | `androidx.compose.ui.text.font` | No — new import in NumberPad.kt | Wraps Typeface |
| `Typeface` | `android.graphics` | No — new import in NumberPad.kt | Android framework type |
| `BasicText` | `androidx.compose.foundation.text` | No — import if fallback path chosen | Low-level text composable |
| `TextStyle` | `androidx.compose.ui.text` | Already in GameGrid.kt (not yet in NumberPad.kt) | Used with BasicText |
| `TextAlign` | `androidx.compose.ui.text.style` | Already in LeaderboardScreen.kt (not yet in ControlsRow.kt) | Used for CTRL-02 |
| `Row` (inner, for CTRL-04 grouping) | Already imported in ControlsRow.kt | Yes | Used to wrap Fill+Pencil |
| `fillMaxHeight` | `androidx.compose.foundation.layout` | Not currently in NumberPad.kt | May be needed for CTRL-01 centering |

### No New Gradle Dependencies

All APIs above are in `androidx.compose.*` and `android.graphics.*`, which are already on the
classpath via the existing BOM and compileSdk 35.

---

## Architecture Patterns

### Pattern 1: FontFamily via Typeface.create (CTRL-01)

**What:** Sets a system-named condensed font on a text element.

**Primary path — TextMMD fontFamily param:**
```kotlin
// Source: Android developer docs — system font via Typeface
import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily

TextMMD(
    text = digit.toString(),
    fontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
)
```
Risk: TextMMD is closed-source. If it ignores `fontFamily`, the font renders unchanged.
Detection: visual inspection in emulator or on device. Code compiles either way.

**Fallback path — BasicText inside centered Box:**
```kotlin
// Source: Compose foundation docs + established ControlsRow.kt Box+clickable pattern
import android.graphics.Typeface
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign

ButtonMMD(
    modifier = Modifier.weight(1f).sizeIn(minHeight = 56.dp),
    onClick = { onDigitClick(digit) }
) {
    Box(contentAlignment = Alignment.Center) {
        BasicText(
            text = digit.toString(),
            style = TextStyle(
                fontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL)),
                textAlign = TextAlign.Center,
                color = androidx.compose.ui.graphics.Color.Black
            )
        )
    }
}
```
This pattern keeps ButtonMMD for click handling (E-ink compatible) and only replaces the inner
TextMMD with BasicText for typography control.

**Decision rule for executor:** Start with the TextMMD primary path. If visual inspection confirms
font is ignored, switch to the BasicText fallback. Document the result as a CLAUDE.md convention.

### Pattern 2: Two-Line Centered Label (CTRL-02)

**What:** Splits "Get Hint" across two centered lines inside a ButtonMMD slot.

**Primary path — TextMMD with newline and textAlign:**
```kotlin
// Source: LeaderboardScreen.kt already passes textAlign = TextAlign.Center to TextMMD
import androidx.compose.ui.text.style.TextAlign

ButtonMMD(...) {
    TextMMD(text = "Get\nHint", textAlign = TextAlign.Center)
}
```
Evidence from codebase: `LeaderboardScreen.kt` line 54–58 passes `textAlign = TextAlign.Center`
to TextMMD successfully (compiled and runs). This is HIGH confidence the param is accepted.

**Fallback path — Column with two TextMMD calls:**
```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment

ButtonMMD(...) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextMMD(text = "Get")
        TextMMD(text = "Hint")
    }
}
```
The Column approach is guaranteed to work but produces two separate composable nodes. Tests using
`onNodeWithText("Get Hint")` will break — tests would need updating to `onNodeWithText("Get")`.

**Decision rule:** The existing TextMMD(textAlign = ...) usage in LeaderboardScreen.kt confirms the
param exists on TextMMD. Use the primary path. No test changes required if `"Get\nHint"` is used
because the node's merged text content will still contain "Get Hint".

### Pattern 3: Inactive Background Color Swap (CTRL-03)

**What:** Change inactive mode toggle background from `Color.White` to `Color(0xFFE0E0E0)`.

This is a one-line change per Box. No structural change. No new imports needed.

```kotlin
// Fill Box — before
.background(if (inputMode == InputMode.FILL) Color.Black else Color.White)

// Fill Box — after
.background(if (inputMode == InputMode.FILL) Color.Black else Color(0xFFE0E0E0))
```

Same change applies to the Pencil Box. Active state (`Color.Black`) is untouched.

Text color for inactive state remains `Color.Black` — correct contrast on `#E0E0E0` background.

### Pattern 4: Border Frame for Fill+Pencil Group (CTRL-04)

**What:** Wrap the Fill and Pencil Boxes in a shared container that carries a `Modifier.border()`,
while preserving the outer Row's equal-width allocation.

**The weight scope problem:** `Modifier.weight()` is a `RowScope`-scoped extension. It can only
be called on a direct child of a Row. The Fill and Pencil Boxes currently have `.weight(1f)` in
the outer Row's RowScope. Moving them inside a wrapper means:
- The wrapper itself gets `.weight(2f)` from the outer Row (occupies 2 of 4 equal shares)
- The inner Row hosting Fill and Pencil uses its own RowScope for each Box's `.weight(1f)`

```kotlin
// Source: Compose Row/RowScope official documentation + search verification

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RectangleShape

// Outer Row — four slots: [grouped Fill+Pencil][Undo][Hint]
Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    // Fill+Pencil group — takes 2 equal shares of 4 total
    Row(
        modifier = Modifier
            .weight(2f)
            .sizeIn(minHeight = 56.dp)
            .border(width = 1.dp, color = Color.Black, shape = RectangleShape)
    ) {
        // Fill Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (inputMode == InputMode.FILL) Color.Black else Color(0xFFE0E0E0))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { if (inputMode != InputMode.FILL) onToggleMode() },
            contentAlignment = Alignment.Center
        ) {
            TextMMD(
                text = "Fill",
                color = if (inputMode == InputMode.FILL) Color.White else Color.Black
            )
        }
        // Pencil Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (inputMode == InputMode.PENCIL) Color.Black else Color(0xFFE0E0E0))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { if (inputMode != InputMode.PENCIL) onToggleMode() },
            contentAlignment = Alignment.Center
        ) {
            TextMMD(
                text = "Pencil",
                color = if (inputMode == InputMode.PENCIL) Color.White else Color.Black
            )
        }
    }

    // Undo — takes 1 share
    ButtonMMD(modifier = Modifier.weight(1f).sizeIn(minHeight = 56.dp), onClick = onUndo) {
        TextMMD(text = "Undo")
    }

    // Hint — takes 1 share
    ButtonMMD(modifier = Modifier.weight(1f).sizeIn(minHeight = 56.dp), onClick = onHint, enabled = canRequestHint) {
        TextMMD(text = "Get\nHint", textAlign = TextAlign.Center)
    }
}
```

**Notes on CTRL-04 implementation:**
- `weight(2f)` on the group container, NOT `weight(1f)` — otherwise Fill+Pencil together would
  take one share and Undo/Hint each take one share, making the group half the visual width it had.
- `fillMaxHeight()` on each inner Box is needed because the inner Row may not propagate the outer
  `sizeIn(minHeight = 56.dp)` height constraint inward automatically. The explicit `fillMaxHeight()`
  tells each Box to match its Row parent's measured height.
- `sizeIn(minHeight = 56.dp)` moves to the inner Row (the group container), not the individual Boxes.
- The existing `Arrangement.spacedBy(8.dp)` on the outer Row creates the 8dp gap between the
  Fill+Pencil group border and the Undo button — no additional spacing needed.
- Border thickness: UI-SPEC says 1dp. STATE.md research flag says "default to 2dp for new borders
  in this milestone". Use 1dp per UI-SPEC (it is the spec'd value). Document as device-verify item.

### Anti-Patterns to Avoid

- **Applying weight(1f) to both the group wrapper AND its children from the outer Row:** The group
  container must use `weight(2f)` from the outer Row; its children use `weight(1f)` from their
  inner Row.
- **RoundedCornerShape on border:** Produces anti-aliased gray pixels on E-ink. Use `RectangleShape`.
- **Adding indication/ripple:** `indication = null` must be preserved on all clickable modifiers.
- **Using Column fallback for CTRL-02 without updating tests:** `onNodeWithText("Get Hint")` would
  fail; tests look for the merged text. If Column is used, tests must be split to `onNodeWithText("Get")`.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Condensed font | Custom font asset + res/font pipeline | `FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))` | `sans-serif-condensed` = Roboto Condensed, already bundled in every AOSP device; no asset needed |
| Two-line button label | Custom layout composable | `"\n"` character in text string | Compose handles newlines in text natively |
| Border with shape | Canvas drawRect | `Modifier.border()` | Foundation API handles clipping and stroke correctly |

---

## Common Pitfalls

### Pitfall 1: weight() scope error on inner Boxes
**What goes wrong:** Applying `.weight(1f)` to a Box that is inside an inner Row but trying to
reference the outer Row's RowScope — or forgetting that `.weight(2f)` is needed on the group container.
**Why it happens:** weight() is a RowScope/ColumnScope extension. Moving Boxes into a nested Row
changes their scope.
**How to avoid:** Group container gets `Modifier.weight(2f)` from outer Row's scope. Inner Boxes
get `Modifier.weight(1f)` from inner Row's scope.
**Warning signs:** Compile error "Unresolved reference: weight" or group occupying half the expected width.

### Pitfall 2: onNodeWithText("Get Hint") test failure after CTRL-02
**What goes wrong:** Existing tests in ControlsRowTest.kt look for the button by its text content.
If the Column fallback is used (two separate TextMMD nodes), `onNodeWithText("Get Hint")` will fail
because no single node has that merged text.
**Why it happens:** Compose UI test matcher merges text within a semantics subtree, but two sibling
nodes both lack the full string.
**How to avoid:** Use `TextMMD(text = "Get\nHint")` (single node, newline approach) — merged text
is "Get\nHint" or "Get Hint" depending on how Compose reports it. OR update tests to use
`onNodeWithText("Get")` if Column fallback is chosen.
**Warning signs:** ControlsRowTest failing with "Node with text 'Get Hint' not found".

### Pitfall 3: fillMaxHeight() missing on inner Boxes
**What goes wrong:** Fill and Pencil Boxes do not expand to fill the inner Row's height, leaving
a gap at the bottom of the group where the border is visible without background fill.
**Why it happens:** Without explicit height constraint, Box height wraps its content (TextMMD).
**How to avoid:** Add `Modifier.fillMaxHeight()` on each inner Box.
**Warning signs:** Background color does not fill to the border edge; white strip visible at bottom.

### Pitfall 4: TextMMD silently ignoring fontFamily
**What goes wrong:** The CTRL-01 change compiles and runs but digits still render in the default
Roboto font (not condensed).
**Why it happens:** TextMMD is a closed-source AAR; it may not pass fontFamily through to Compose
text rendering.
**How to avoid:** Verify visually on emulator or device. Switch to BasicText fallback if ignored.
**Warning signs:** Digit labels look identical before and after the change; no condensed appearance.

---

## Code Examples

### Complete Import List for ControlsRow.kt (new imports only)

```kotlin
import androidx.compose.foundation.border           // Modifier.border()
import androidx.compose.foundation.layout.fillMaxHeight  // Box fillMaxHeight
import androidx.compose.foundation.shape.RectangleShape  // border shape
import androidx.compose.ui.text.style.TextAlign     // CTRL-02 textAlign param
```

Existing imports that remain unchanged:
- `androidx.compose.foundation.background`
- `androidx.compose.foundation.clickable`
- `androidx.compose.foundation.interaction.MutableInteractionSource`
- `androidx.compose.foundation.layout.Arrangement`
- `androidx.compose.foundation.layout.Box`
- `androidx.compose.foundation.layout.Row`
- `androidx.compose.foundation.layout.fillMaxWidth`
- `androidx.compose.foundation.layout.sizeIn`
- `androidx.compose.runtime.Composable`
- `androidx.compose.runtime.remember`
- `androidx.compose.ui.Alignment`
- `androidx.compose.ui.Modifier`
- `androidx.compose.ui.graphics.Color`
- `androidx.compose.ui.unit.dp`
- `com.mudita.mmd.components.buttons.ButtonMMD`
- `com.mudita.mmd.components.text.TextMMD`
- `com.mudita.sudoku.game.model.InputMode`

### Complete Import List for NumberPad.kt (new imports — primary TextMMD path)

```kotlin
import android.graphics.Typeface                    // Typeface.create()
import androidx.compose.ui.text.font.FontFamily     // FontFamily(Typeface)
```

If BasicText fallback is needed (CTRL-01 fallback path only):
```kotlin
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
```

---

## Plan Decomposition

**Recommendation: 1 plan, 2 files.**

Rationale:
- All four requirements are small (1–15 lines each)
- The changes are non-overlapping: CTRL-01 is isolated to NumberPad.kt; CTRL-02/03/04 are in
  ControlsRow.kt
- A single build-and-test cycle validates all four changes together
- The test suite (ControlsRowTest + NumberPadTest) already exists and covers both files
- Splitting into two plans adds a checkpoint boundary with no safety value — neither plan can
  break the other (different files, same test suite)

Plan should have 2 tasks:
1. ControlsRow.kt — CTRL-02 (Get Hint two lines), CTRL-03 (inactive gray background), CTRL-04
   (Fill+Pencil border group). These three share the same file and the CTRL-04 refactor subsumes
   the CTRL-03 color change since the background modifier is being rewritten anyway.
2. NumberPad.kt — CTRL-01 (condensed font). Isolated to one file, one composable change.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (4.13.2) + Robolectric 4.14.1 + Compose UI Test |
| Config file | `app/build.gradle.kts` — `testOptions.unitTests.isIncludeAndroidResources = true` |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.mudita.sudoku.ui.game.ControlsRowTest" --tests "com.mudita.sudoku.ui.game.NumberPadTest"` |
| Full suite command | `./gradlew :app:testDebugUnitTest` |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CTRL-01 | Condensed font applied to digit buttons | Visual only (font rendering is device/emulator) | `./gradlew :app:testDebugUnitTest --tests "*.NumberPadTest"` (structure test passes; font verified visually) | ✅ NumberPadTest.kt |
| CTRL-02 | "Get Hint" renders as two lines centered | Visual only (line break rendering) | `./gradlew :app:testDebugUnitTest --tests "*.ControlsRowTest"` (existing text tests pass) | ✅ ControlsRowTest.kt |
| CTRL-03 | Inactive mode button has #E0E0E0 background | Visual only (background color not testable via text matcher) | `./gradlew :app:testDebugUnitTest --tests "*.ControlsRowTest"` (toggle behavior unchanged) | ✅ ControlsRowTest.kt |
| CTRL-04 | Fill+Pencil are visually grouped with border | Visual only (border rendering) | `./gradlew :app:testDebugUnitTest --tests "*.ControlsRowTest"` (click behavior unchanged) | ✅ ControlsRowTest.kt |

**Important note:** The structural changes for CTRL-04 (nested Row wrapping Fill+Pencil) preserve
all existing ControlsRowTest behaviors: `onNodeWithText("Fill")` and `onNodeWithText("Pencil")`
still find their nodes; `performClick()` still triggers; `assertHeightIsAtLeast(56.dp)` still passes
because `sizeIn(minHeight = 56.dp)` moves to the inner Row container and both Boxes inherit it.

The `onNodeWithText("Get Hint")` matcher in ControlsRowTest.kt line 127 — there is no test for this
text directly. The Undo test checks `onNodeWithText("Undo")`. No test exists for the Hint button
text, so the "Get\nHint" change causes no test failures.

### Sampling Rate

- **Per task commit:** quick run command above
- **Per wave merge:** full suite command
- **Phase gate:** full suite green before `/gsd:verify-work`

### Wave 0 Gaps

None — existing ControlsRowTest.kt and NumberPadTest.kt cover all behavioral requirements for
this phase. New tests for visual properties (font, color, border) are manual-only and not feasible
to automate via Compose UI Test without pixel-level screenshot comparison.

---

## Environment Availability

Step 2.6: SKIPPED (no new external dependencies — all APIs are in existing compile classpath; no
new services, tools, or runtimes are introduced in this phase).

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `Typeface.create()` in View system | `FontFamily(Typeface.create())` in Compose | Compose 1.0+ | Wraps Android Typeface in Compose type |
| `BorderStroke` on Material Surface | `Modifier.border()` in foundation | Compose 1.0+ | More composable; works on any composable, not just Material |

**Existing codebase evidence:** `LeaderboardScreen.kt` uses `BorderStroke(1.dp, Color.Black)` on
a Material3 `Surface`. For Phase 8, `Modifier.border()` is preferred because the Fill+Pencil group
is a plain `Row`, not a `Surface`. Both approaches are valid; `Modifier.border()` requires no
Material dependency and aligns with the Box+clickable pattern already established for mode toggles.

---

## Open Questions

1. **Does TextMMD accept fontFamily?**
   - What we know: TextMMD is a closed-source AAR. LeaderboardScreen.kt confirms `fontWeight` and
     `textAlign` params are accepted. `fontFamily` is not confirmed from any existing codebase usage.
   - What's unclear: Whether TextMMD forwards `fontFamily` to its underlying Compose text rendering.
   - Recommendation: Attempt primary path. If ignored, use BasicText fallback. Document result in
     CLAUDE.md after Phase 8 completes.

2. **Does `"Get\nHint"` (newline literal) center correctly in ButtonMMD's content slot?**
   - What we know: Compose renders newlines in text strings as line breaks. `textAlign = TextAlign.Center`
     is accepted by TextMMD (confirmed by LeaderboardScreen.kt usage).
   - What's unclear: Whether ButtonMMD's internal content slot constrains width in a way that makes
     multiline text rendering ambiguous.
   - Recommendation: Primary path is low risk. Use Column fallback only if visual inspection shows
     misalignment. No test changes needed for either path (no test asserts on "Get Hint" text).

3. **Should border be 1dp or 2dp?**
   - What we know: UI-SPEC specifies 1dp. STATE.md research flag says "default to 2dp for all new
     borders in this milestone" because 1dp may render gray on fast E-ink waveform.
   - Recommendation: Implement 1dp per UI-SPEC. Mark as a device-verify item. Escalate to 2dp only
     if device testing shows gray rendering at 1dp.

---

## Sources

### Primary (HIGH confidence)
- Jetpack Compose official docs — `Modifier.border()` API, RectangleShape, weight() scope rules
- [Android Developers — Compose Modifiers List](https://developer.android.com/develop/ui/compose/modifiers-list) — border modifier confirmed
- [Compose Foundation release notes (BOM 2026.03.00)](https://developer.android.com/jetpack/androidx/releases/compose-foundation) — API stability
- Codebase inspection — `LeaderboardScreen.kt` confirms TextMMD accepts `fontWeight`, `textAlign`, `modifier`; `GameGrid.kt` confirms `TextStyle(fontFamily = ...)` is a valid Compose pattern
- Codebase inspection — `ControlsRowTest.kt` and `NumberPadTest.kt` confirm test structure and what the tests actually assert (no "Get Hint" text assertion exists)

### Secondary (MEDIUM confidence)
- [composables.com — border modifier](https://composables.com/foundation/border) — function signatures confirmed: `Modifier.border(width: Dp, color: Color, shape: Shape = RectangleShape)`
- WebSearch verification — `FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))` is the documented Compose way to reference system-named fonts; `sans-serif-condensed` = Roboto Condensed, bundled in AOSP

### Tertiary (LOW confidence — verify on device)
- STATE.md research flag — "1dp may render gray on fast E-ink waveform" — unverified; device testing needed
- TextMMD fontFamily acceptance — unverified; closed-source AAR

---

## Metadata

**Confidence breakdown:**
- CTRL-01 API (FontFamily, BasicText): HIGH — both Compose APIs are stable and verified
- CTRL-01 TextMMD fontFamily acceptance: LOW — closed-source AAR, unverifiable without device test
- CTRL-02 TextMMD textAlign: HIGH — confirmed by existing LeaderboardScreen.kt usage
- CTRL-03 color swap: HIGH — trivial constant change
- CTRL-04 border + weight(2f) pattern: HIGH — RowScope weight scoping rules are documented and confirmed by search
- Test coverage: HIGH — existing tests cover all behavioral requirements; visual-only changes do not need new automated tests

**Research date:** 2026-03-26
**Valid until:** 2026-04-26 (stable APIs; MMD closed-source risk unchanged until Mudita publishes source)
