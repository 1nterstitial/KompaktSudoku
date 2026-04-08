# Mudita UI Alignment — Design Spec

Align KompaktSudoku's visual design with Mudita's Mindful Design philosophy as seen in the Kompakt Alarm app. The current app works well functionally but needs typography, spacing, and hierarchy refinements to match Mudita's bolder, more confident aesthetic.

## Reference

The Mudita Kompakt Alarm app establishes these patterns:
- Heavy, bold font weights throughout
- Thick (2dp) header dividers
- Dotted separators between list items
- Clear visual hierarchy with indentation and weight contrast
- Generous spacing and high-contrast black-and-white palette

## Changes

### 1. Game Screen — Slimmer Top Bar

**File:** `PuzzleTopBar.kt`

Reduce vertical space consumed by the header so the grid shifts up.

- Back arrow: `titleLarge` → `titleMedium`
- Row padding: `horizontal = 4.dp` with 8dp arrow padding → `horizontal = 4.dp` with `4.dp` vertical padding on arrow, `6.dp` horizontal
- Cells-remaining text padding stays the same
- Net effect: header loses ~12dp of height, grid moves up

### 2. Game Screen — Bolder Controls Row

**File:** `GameScreen.kt` (ControlsRow composable)

Match the Mudita Alarm app's heavier button typography.

- Toggle and button text: `labelMedium` → `labelLarge` with `FontWeight.Bold`
- Toggle border: `1.dp` → `2.dp`
- Controls row height: `32.dp` → `34.dp`
- Row padding: keep `horizontal = 2.dp, vertical = 2.dp`

### 3. Game Screen — Digit Pad Fills Remaining Space

**File:** `GameScreen.kt` (DigitPad call site) and `DigitPad.kt`

The digit pad currently has a fixed 120dp height, leaving dead space at the bottom. It should expand to fill all remaining vertical space.

- Remove fixed `height = 120.dp` from DigitPad
- At the call site in GameScreen, add `Modifier.weight(1f)` so the pad takes all remaining space after the grid and controls
- The canvas already draws relative to its size, so digit text will scale up with the larger area

### 4. Records Screen — Clearer Hierarchy

**File:** `RecordsScreen.kt`

Difficulty headers (Easy/Medium/Hard) need to read as top-level section headers, with stat rows clearly nested underneath.

**Difficulty headers:**
- Typography: `titleMedium` → `titleLarge` with `FontWeight.ExtraBold`
- Add a 2dp thick divider directly under each header (using `HorizontalDividerMMD(thickness = 2.dp)`) indented to match header padding
- Padding: `16.dp` horizontal, `14.dp` top, `6.dp` bottom

**Stat rows (Completed, Best time, Best no-hint):**
- Left padding: `16.dp` → `32.dp` (indented under the header)
- Separators between stat rows: dotted style instead of solid `HorizontalDividerMMD`
  - Since MMD's divider doesn't support dotted style natively, use a custom composable: `Canvas` drawing a dotted line with `pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))`
  - Dotted separator also indented (start at 32dp)
- Remove the thick 2dp divider between sections; use vertical spacing (8dp gap) instead

### 5. All Screens — Bolder Typography

Match the Mudita Alarm app's confident, heavy font weights across every screen.

**HomeScreen (`HomeScreen.kt`):**
- MenuRow title: add `FontWeight.Bold` to `titleMedium`
- MenuRow subtitle: add `FontWeight.Medium` to `bodyMedium`

**NewPuzzleScreen (`NewPuzzleScreen.kt`):**
- DifficultyRow title: add `FontWeight.Bold` to `titleMedium`
- DifficultyRow subtitle: add `FontWeight.Medium` to `bodyMedium`

**SummaryScreen (`SummaryScreen.kt`):**
- StatRow label: add `FontWeight.Bold` to `bodyLarge`
- StatRow value: `FontWeight.Medium` → `FontWeight.Bold`
- "New personal record" text: add `FontWeight.Bold`

**RecordsScreen (`RecordsScreen.kt`):**
- RecordRow label: add `FontWeight.Bold` to `bodyMedium`
- RecordRow value: `FontWeight.Medium` → `FontWeight.Bold`

**LeavePuzzleDialog (`LeavePuzzleDialog.kt`):**
- Dialog title: add `FontWeight.Bold` to `headlineSmall`
- Button text inherits from ButtonMMD (already bold via MMD defaults)

**TopAppBarMMD titles** (NewPuzzle, Records, Summary):
- Title text: add `FontWeight.Bold` to the TextMMD passed to TopAppBarMMD

## Out of Scope

- Sudoku grid rendering (Canvas-based, already well-styled)
- Color palette changes (stays with MMD defaults)
- Navigation patterns (back arrow, routing)
- Home screen layout structure (already uses thick header divider + menu rows)
- Functional changes

## Files Modified

1. `app/src/main/java/com/interstitial/sudoku/ui/game/PuzzleTopBar.kt`
2. `app/src/main/java/com/interstitial/sudoku/ui/game/GameScreen.kt`
3. `app/src/main/java/com/interstitial/sudoku/ui/game/DigitPad.kt`
4. `app/src/main/java/com/interstitial/sudoku/ui/records/RecordsScreen.kt`
5. `app/src/main/java/com/interstitial/sudoku/ui/home/HomeScreen.kt`
6. `app/src/main/java/com/interstitial/sudoku/ui/newpuzzle/NewPuzzleScreen.kt`
7. `app/src/main/java/com/interstitial/sudoku/ui/summary/SummaryScreen.kt`
8. `app/src/main/java/com/interstitial/sudoku/ui/game/LeavePuzzleDialog.kt`
