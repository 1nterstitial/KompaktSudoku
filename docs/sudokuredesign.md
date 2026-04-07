# KompaktSudoku — Redesign and Implementation Plan

Reviewed: 2026-04-06

## 1. Scope

This document is a full redesign plan for **KompaktSudoku** on the Mudita Kompakt.

It covers:
- current-product diagnosis (from source code review)
- Mudita/MMD design alignment
- gameplay and visual recommendations
- screen-by-screen UX with wireframes
- game interaction redesign
- Compose/front-end architecture with code sketches
- MMD component replacement matrix
- state/persistence adjustments
- records/scoring recommendations
- E-ink implementation guidance
- accessibility
- testing strategy
- phased implementation

This plan is deliberately more ambitious on the **interface and interaction layer** than on the puzzle engine. The current app already has a stronger internal architecture than KompaktClock; the redesign should preserve that strength while changing how the product feels.

---

## 2. Inputs reviewed

### Design language and platform context
- Mudita "Introducing Mudita Mindful Design" blog
  <https://mudita.com/community/blog/introducing-mudita-mindful-design/>
- Mudita developer page for Mindful Design
  <https://developers.mudita.com/resources/mindful-design-introduction/>
- Mudita MMD repository/readme
  <https://github.com/mudita/MMD>

### Target app
- KompaktSudoku repository
  <https://github.com/1nterstitial/KompaktSudoku>

### Reference apps
- CalmCast
  <https://github.com/davidraywilson/CalmCast>
- CalmDirectory
  <https://github.com/davidraywilson/CalmDirectory>
- CalmMusic
  <https://github.com/davidraywilson/CalmMusic>

### Sudoku code and behavior reviewed
- `MainActivity.kt`
- `GameViewModel.kt`
- `DataStoreGameRepository.kt`
- `DataStoreScoreRepository.kt`
- `GameUiState.kt`
- `GameScreen.kt`
- `GameGrid.kt`
- `ControlsRow.kt`
- `NumberPad.kt`
- `MenuScreen.kt`
- `DifficultyScreen.kt`
- `SummaryScreen.kt`
- `LeaderboardScreen.kt`
- `ScoreCalculation.kt`
- `SudokuGenerator.kt`

---

## 3. Executive summary

KompaktSudoku already has the bones of a serious app:

- a real view model
- a real saved-game flow
- puzzle generation and difficulty classification
- a custom canvas grid
- sensible repository boundaries
- one-shot event handling
- an exit-save model that already respects the device

That is the good news.

The problem is that the app still **looks and feels more utilitarian than mindful**. The visual shell and interaction patterns are not yet at the level of the reference apps or at the level implied by Mudita's design language.

The biggest redesign opportunities are:

1. **Menus and support screens are too generic.**
   Main menu, difficulty, summary, and records screens are mostly just button stacks.

2. **The play screen uses space inefficiently.**
   A 10-button horizontal number pad is cramped on a 4.3" portrait device, and the control cluster below the grid still feels dense and heavy.

3. **The product semantics are inconsistent.**
   The README says errors are tracked silently until completion, but the grid code draws wrong-cell indicators immediately. That is both a UX inconsistency and a product-philosophy inconsistency.

4. **Pencil marks are unnecessarily constrained.**
   The current limit of four candidates in a 2×2 mini-grid is a compromise that makes the app feel less complete than it could be.

5. **The app is too score-forward for a mindful device, yet not actually coherent enough about what scoring means.**
   This is the moment to decide whether the app wants to be:
   - score-based and explicit about it, or
   - session-based and calmer about records

### Recommended redesign direction

Treat the Sudoku app like a **paper puzzle desk**:

- the grid is the centerpiece
- controls are large, calm, and physically comfortable
- menus are list-first and typographic
- notes are full-featured
- conflict feedback is rule-based, not answer-leaking
- records feel quiet and humane rather than arcade-like

The backend should remain mostly intact, but the **game semantics around mistakes/records** should be clarified and the UI should be rebuilt around better ergonomic patterns.

---

## 4. What should be preserved

### Keep
- `GameViewModel` as the central state machine
- repository-based save/load design
- save-on-background behavior
- canvas grid approach
- uniqueness/difficulty validation pipeline
- exit confirmation flow concept
- offline/local-only design
- three difficulty tiers
- touch-first model

### Do not preserve as-is
- menu/difficulty/summary screens as button stacks
- 10-across number pad
- 4-note maximum
- "silent error tracking" messaging combined with visible wrong-cell indicators
- score-centric summary UI
- overly generic user-facing copy (`Quit Game`, `Best Scores`, etc.) where calmer language would fit better

---

## 5. Diagnosis of the current app

### 5.1 Architecture strength

This app is already much stronger than KompaktClock at the architecture level.

#### Good decisions already present
- immutable `GameUiState`
- `StateFlow` + one-shot events
- proper repository interfaces
- saved-game resume logic
- completion event pipeline
- puzzle generation off the main thread
- undo stack held outside immutable state
- persistence on lifecycle stop
- custom grid instead of 81 independent boxes

This matters because it means the redesign can be **surgical** rather than destructive.

#### Recommendation
Do not rewrite the generator or the whole view model unless a specific semantic change requires it.

---

### 5.2 Menu and support screens

#### Current state
`MenuScreen`, `DifficultyScreen`, `SummaryScreen`, and `LeaderboardScreen` are competent but generic.

They are mostly:
- heading text
- full-width buttons
- large empty vertical stretches

#### Why this feels below the reference quality
The reference apps show:
- more deliberate hierarchy
- calmer row-based navigation
- better use of dividers
- more nuanced summaries in list items
- clearer distinction between navigation rows and primary actions

#### Recommendation
Move these support screens toward **list-first, row-first structure**, especially:
- home menu
- difficulty chooser
- records screen

Buttons should remain for clear primary actions, but not as the only screen grammar.

---

### 5.3 Game screen

#### Current strengths
- grid is centered as the main content
- controls are logically separated from the grid
- no animation clutter
- MMD components are already used in several places
- selected cell inversion is clear

#### Current problems
- the top "difficulty bar" is too thin and uninformative
- the controls row still feels like a control cluster, not a carefully paced interaction strip
- the horizontal 1–9 + erase pad is too cramped for the device
- there is no refined meta layer for "cells left", "mode", or session status
- hint behavior is random rather than intentional
- error semantics are contradictory
- notes are limited in a way that experienced Sudoku players will notice immediately

---

### 5.4 Error semantics inconsistency

#### What the code and README imply today
README says:
- errors tracked silently
- revealed only at completion

Grid code does:
- compute `isError` using `board[i] != solution[i]`
- draw an inset border for wrong entries immediately

#### Why this matters
This is not just a bug in wording. It creates a deeper design problem:

- if wrong values are shown immediately, the app is assistive
- if mistakes are "silent", the app is more self-guided
- if score is based on hidden wrong entries, that score may feel arbitrary
- if score is based on visible immediate mistakes, then the current copy is wrong

#### Recommendation
Choose one coherent direction and implement it fully.

##### Recommended direction
Use **rule-based conflict feedback**, not **solution-based mistake feedback**.

That means:
- show duplicate conflicts in rows/columns/boxes if the current board violates Sudoku rules
- do **not** mark a value wrong simply because it differs from the hidden solution
- hints and completion can still use the solution internally

This is fairer, calmer, and more puzzle-authentic.

---

### 5.5 Records and scoring

#### Current state
- score formula: `100 - errorCount * 10 - hintCount * 5`, floored at 0
- one best score stored per difficulty
- summary screen is score-centric
- records screen is really a "best score table"

#### Why this is the right time to revisit
A Mudita-first product is not necessarily anti-records, but it should be careful about:
- hidden penalties
- over-gamification
- unclear performance metrics

#### Recommendation
Change the product from **score-centric** to **session-centric**.

##### Preferred model
Track:
- completed count
- best completion time
- best no-hint completion time
- last completed date

Display:
- `Records`, not `Leaderboard`
- `Puzzle complete`, not `You scored X` as the emotional center

##### Compatibility fallback
If you need to preserve the current scoring model for a first redesign pass:
- move score out of the emotional center of the UI
- clarify error semantics
- show score as one line in the summary, not the headline of the experience

---

## 6. Redesign thesis

### 6.1 Core idea

KompaktSudoku should feel like **solving on paper with a careful digital helper**, not like operating a compact control panel.

That implies:
- the grid remains central
- the keypad becomes more comfortable
- every support screen becomes calmer
- feedback becomes clearer and less contradictory
- records stop shouting

### 6.2 Product principles

1. **The puzzle is the hero.**
   The grid should dominate the experience.

2. **Controls must fit the finger, not the layout convenience of the developer.**
   On this device, a 10-across keypad is simply too compressed.

3. **Assistance should be explicit, not accidental.**
   Hints should feel chosen, not random.

4. **Rules over revelation.**
   Conflict highlighting should reflect Sudoku legality, not leak the hidden answer.

5. **Menus should read like printed menus.**
   Strong labels, secondary summaries, thin dividers, little ornament.

6. **Completion should feel calm.**
   The app should acknowledge success without becoming arcade-like.

---

## 7. MMD design rules

These are the non-negotiable rules extracted from the Mudita Mindful Design blog, the Zeroheight design system, and the Calm* app implementations:

1. **One clear purpose per screen.** Game screen = play. Menu = choose. Records = review.
2. **Monochromatic only.** `eInkColorScheme`. Black, white, and structural grays from `outlineVariant`.
3. **No animations.** Zero. Cell selection is instant. Number placement is instant. No fade, no bounce, no scale, no ripple, no transitions, no animated progress indicators.
4. **Dividers over shadows.** `HorizontalDividerMMD` to separate sections. Never use elevation, shadow, or `Card` with tonalElevation.
5. **MMD components exclusively.** `ButtonMMD`, `TextMMD`, `TopAppBarMMD`, `LazyColumnMMD`, `TextFieldMMD`, `SwitchMMD`, `HorizontalDividerMMD`, `CircularProgressIndicatorMMD`, `SnackbarHostStateMMD`. No fallback to raw Material3.
6. **Large touch targets.** ≥48dp minimum, ≥56dp preferred for primary actions.
7. **`eInkTypography`.** Headlines 28sp SemiBold. Body 18sp. Increased line height and letter spacing. Do not override below MMD defaults.
8. **Text-first information.** "Notes" not just ✏️. "Undo" not just ↩️. Icons accompany text; they don't replace it.
9. **Instant feedback.** Cell selection: border change, no animation. Number fill: digit appears, no transition.
10. **Single-tap interactions only.** No long-press, no swipe, no drag.
11. **Full-width elements.** Minimize partial E-ink screen refreshes.
12. **List-based navigation.** `LazyColumnMMD` for vertical scrolling. No carousels, no grids of small items.

---

## 8. Component replacement matrix

Every non-MMD component in the current app must be replaced:

| Current | Replace With | Notes |
|---------|-------------|-------|
| `Text(...)` | `TextMMD(...)` | All non-Canvas text throughout the app |
| `Button(...)` | `ButtonMMD(...)` | Number pad, action bar, menu buttons |
| `AlertDialog(...)` | Full-screen composable | Pause menu, forfeit confirm, completion |
| `TopAppBar(...)` | `TopAppBarMMD(...)` | All screens with navigation |
| `LazyColumn(...)` | `LazyColumnMMD(...)` | Menu, records, city search results |
| `Divider(...)` | `HorizontalDividerMMD(...)` | Between all sections and list items |
| `Snackbar` / `SnackbarHost` | `SnackbarHostStateMMD()` | Confirmations ("Record saved", etc.) |
| Any `animateXAsState` | Remove entirely | Cell selection, number placement — all instant |
| Any `Crossfade` / `AnimatedVisibility` | Remove entirely | Screen transitions — instant swap |
| `Card` / elevation | Flat `Box` with border | If used for grouping |
| Ripple indications | Already disabled by `ThemeMMD` | Verify no manual re-enabling anywhere |
| `CircularProgressIndicator` | `CircularProgressIndicatorMMD()` | Puzzle generation loading |
| Scroll-wheel / fling pickers | Tap-based controls | Not currently used, but guard against adding |

---

## 9. Screen-by-screen implementation plan

### 9.1 Home screen

#### Goal
Make the home screen feel like an elegant starting place, not a button menu.

#### Layout
- top safe-area padding
- left-aligned or centered title `Sudoku`
- conditional first row: `Continue puzzle`
- row: `New puzzle`
- row: `Records`
- thin dividers

#### Row summaries
- `Continue puzzle`
  `Medium · 31 cells left`
- `New puzzle`
  `Choose a fresh easy, medium, or hard board`
- `Records`
  `View completed puzzles and best times`

#### Wireframe

```
┌─────────────────────────────┐
│                             │
│   Sudoku                    │  ← screenTitle, left-aligned
│                             │
│ ─────────────────────────── │  ← HorizontalDividerMMD 2dp
│                             │
│   Continue puzzle           │  ← rowTitle
│   Medium · 31 cells left    │  ← rowMeta (secondary text)
│                             │
│ ─────────────────────────── │  ← HorizontalDividerMMD 1dp
│                             │
│   New puzzle                │
│   Choose easy, medium,      │
│   or hard                   │
│                             │
│ ─────────────────────────── │
│                             │
│   Records                   │
│   Completed puzzles and     │
│   best times                │
│                             │
│ ─────────────────────────── │
└─────────────────────────────┘
```

#### Interaction
- row taps navigate
- no icons required
- no big stacked button block
- uses `LazyColumnMMD` for the list (consistency with reference apps)
- "Continue puzzle" row only shown when a saved game exists

---

### 9.2 New Puzzle / Difficulty screen

#### Goal
Replace the current "three equal buttons and a back button" layout with a screen that explains the choice calmly.

#### Layout
- `TopAppBarMMD` with back + title `New puzzle`
- three rows, each with a descriptive secondary line
- optional contextual record line under each difficulty

#### Recommended copy
- **Easy** — more givens, shorter sessions
- **Medium** — balanced deduction
- **Hard** — fewer givens, deeper focus

#### Loading state
After tap, show a simple full-screen loading state:
- `Preparing puzzle…`
- secondary line with chosen difficulty

No spinner. Static text only.

---

### 9.3 Game screen: overall structure

This is the centerpiece of the redesign.

#### Recommended layout order (top to bottom)
1. Top app bar
2. Meta strip
3. Grid
4. Mode toggle
5. Action row
6. Digit pad

#### Why this order
- title and navigation first
- a small amount of context next
- the puzzle remains central
- mode stays close to the grid
- utility actions are separated from digits
- keypad stays physically comfortable

#### Vertical space budget (800×480 portrait)

| Section | Height | Notes |
|---------|--------|-------|
| Status bar (system) | ~24dp | Android system |
| Top app bar | ~56dp | `TopAppBarMMD` |
| Meta strip | ~32dp | Cells left, optional mode indicator |
| Divider | 2dp | |
| Sudoku grid | ~396dp | 9 cells × ~44dp each |
| Divider | 2dp | |
| Mode toggle | ~48dp | Fill / Notes segmented control |
| Action row | ~48dp | Undo, Erase, Hint |
| Divider | 2dp | |
| Digit pad (3×3) | ~168dp | 3 rows × 56dp |
| Bottom padding | ~16dp | |
| **Total** | ~794dp | Fits within 800dp |

---

### 9.4 Game screen: top app bar

#### Layout
- `TopAppBarMMD`
- back button (opens leave dialog)
- title: `Easy puzzle`, `Medium puzzle`, or `Hard puzzle`
- no extra icons unless absolutely needed

#### Why change the current top area
The current difficulty bar floats with little context. A real top bar gives the screen a stronger frame and matches the reference apps.

---

### 9.5 Game screen: meta strip

#### Purpose
Provide one line of calm session context without turning the screen into a dashboard.

#### Recommended content
- `31 cells left`
- optionally surface `Notes mode` indicator only when active

#### Timer visibility
Track elapsed time internally, but do **not** show it prominently during play by default. Visible timers push puzzle play toward speed pressure. Surface it in summary/records instead.

---

### 9.6 Game screen: grid

#### 9.6.1 Keep the canvas strategy

The current canvas-based grid is the right architectural choice:
- lower visual overhead
- better control over borders and note rendering
- easier E-ink tuning
- cleaner than 81 composables

Keep it.

#### 9.6.2 Grid visual rules

##### Borders
- outer border stronger than internal borders
- 3×3 block borders: 3dp stroke, `MaterialTheme.colorScheme.onSurface` (black)
- cell borders: 1dp stroke, `MaterialTheme.colorScheme.outlineVariant` (gray)
- no gray anti-aliased decorative lines

##### Digits
- given digits: Bold, `onSurface`
- player digits: Regular or Medium weight, `onSurface`
- all digits centered with stable typography

##### Selected cell
Keep the selected-cell inversion model. Recommended:
- selected cell = solid black fill
- selected digit/notes = white

##### Conflicts (NEW — replaces solution-based error display)
Replace "wrong against solution" styling with "invalid by Sudoku rule" styling:
- duplicate value in row/column/box → conflict
- conflict cells get an inset border or corner mark
- selected conflict cell must remain legible

##### Same-digit highlight
Optional: cells containing the same digit as the selected cell get a subtle `surfaceVariant` fill. If it causes E-ink ghosting during rapid cell switching, remove it.

##### Do not add
- row/column shading
- animations
- color cues
- heavy ghost-prone effects

#### 9.6.3 Grid color token mapping

| Grid Element | MMD Token | Renders As |
|-------------|-----------|------------|
| Box borders (3×3) | `onSurface` | Black, 3dp |
| Cell borders | `outlineVariant` | Dark gray, 1dp |
| Given digits | `onSurface` | Black, Bold |
| Player digits | `onSurface` | Black, Regular weight |
| Pencil marks | `onSurfaceVariant` | Medium gray |
| Selected cell fill | `onSurface` | Black (inverted) |
| Selected cell text | `surface` | White |
| Conflict indicator | `onSurface` | Inset corner mark, black |
| Background | `surface` | White |
| Disabled pad button | `outlineVariant` | Gray text |

#### 9.6.4 Canvas implementation sketch

```kotlin
@Composable
fun SudokuGrid(
    state: GameUiState,
    onCellTap: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface

    var lastTapTime by remember { mutableLongStateOf(0L) }

    Canvas(modifier = modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                val now = System.currentTimeMillis()
                if (now - lastTapTime > 200) {   // E-ink debounce
                    lastTapTime = now
                    val cellSize = size.width / 9f
                    val col = (offset.x / cellSize).toInt().coerceIn(0, 8)
                    val row = (offset.y / cellSize).toInt().coerceIn(0, 8)
                    onCellTap(row, col)
                }
            }
        }
    ) {
        // 1. Background fills (selected cell, same-digit cells)
        // 2. Grid lines (1dp cells, 3dp boxes)
        // 3. Digits (given Bold, player Regular)
        // 4. Pencil marks (3×3 micro-grid)
        // 5. Conflict indicators (inset corner marks)
        // All colors from onSurface, outline, outlineVariant, surface
    }
}
```

The 200ms debounce prevents phantom double-taps during E-ink refresh cycles (~100-200ms).

#### 9.6.5 Full 1–9 notes

##### Recommendation
Replace the current 4-note maximum with full candidate support.

##### Why
A four-note limit is a gameplay compromise that advanced users will notice immediately. It makes the app feel less complete than the rest of its architecture deserves.

##### New note model
Allow `Set<Int>` with up to 9 values.

##### Rendering approach
Use a 3×3 micro-grid inside the cell:
```
1 2 3
4 5 6
7 8 9
```

##### Typography
Use dynamically scaled small text derived from cell size. Keep it crisp and high-contrast.

##### Hardware caveat
Prototype this on the actual Kompakt hardware early. If 3×3 note legibility is unacceptable, the fallback should be:
- keep full candidate support in state
- render a reduced visual indicator in non-selected cells
- render the full notes more prominently for the selected cell

But the first attempt should absolutely be full 1–9 notes.

---

### 9.7 Game screen: mode toggle

#### Copy
Use `Fill` and `Notes`, not `Fill` and `Pencil`.

`Notes` is clearer for more users and feels less UI-jargon-heavy.

#### Layout
A two-segment full-width control:
- selected state: black fill with white text (inverted `ButtonMMD`)
- unselected state: bordered or light neutral fill

---

### 9.8 Game screen: action row

#### Actions
- `Undo`
- `Erase`
- `Hint`

#### Layout
Three equal-width `ButtonMMD` in a dedicated row below the mode toggle. Each with TEXT label. Height: 48dp.

#### Behavior
- `Undo` disabled if undo stack empty
- `Erase` disabled if no editable cell is selected
- `Hint` disabled if no hint target is valid

#### Why separate from the keypad
Keeping utility actions separate makes the digit pad simpler and easier to hit correctly.

---

### 9.9 Game screen: hint redesign

#### Current issue
Hints reveal a random unresolved cell. That is mechanically workable but not particularly intentional.

#### Recommended redesign
Make hinting **selected-cell-first**.

#### Behavior
- if the selected cell is a valid hint target, reveal that cell
- if no eligible cell is selected, show a lightweight prompt via `SnackbarHostStateMMD`: `Select a cell to reveal`
- if the selected cell is already correct, disable hint or clarify that no reveal is needed

#### Why
This makes hints feel deliberate and respectful rather than arbitrary.

#### Product decision
Keep hints non-undoable. That is reasonable and easy to explain.

---

### 9.10 Game screen: digit pad

#### Current problem
A 10-across row is too cramped for the device.

#### Recommended redesign
Use a **3×3 keypad** for digits 1–9.

#### Layout
```
┌──────┬──────┬──────┐
│  1   │  2   │  3   │
├──────┼──────┼──────┤
│  4   │  5   │  6   │
├──────┼──────┼──────┤
│  7   │  8   │  9   │
└──────┴──────┴──────┘
```

- 3 columns, 3 rows
- each button: `ButtonMMD`, ≥56dp height, generous spacing
- stable numeric text, ideally condensed/tabular
- erase stays in the action row, not in the digit pad
- when a digit has been placed 9 times, its button becomes disabled (`outlineVariant` text)

#### Why this is better
This is the single most important ergonomic improvement for actual solving. The layout mirrors the Sudoku 3×3 box structure, creating visual coherence between the grid and the input.

---

### 9.11 Leave Puzzle dialog

#### Copy redesign
Replace:
- `Save and Exit` → `Keep for later`
- `Quit Game` → `Discard puzzle`

#### Layout
- centered rectangular dialog
- title: `Leave puzzle?`
- primary action: `Keep for later`
- secondary action: `Discard puzzle`
- no scrim-dismiss

Timer pauses when this dialog is shown.

---

### 9.12 Summary screen

#### Goal
Completion should feel satisfying but quiet.

#### Layout
```
┌─────────────────────────────┐
│       TopAppBarMMD          │
│       "Puzzle complete"     │
├─────────────────────────────┤
│                             │
│   Difficulty     Hard       │
│ ─────────────────────────── │
│   Time           45:12      │
│ ─────────────────────────── │
│   Hints used     1          │
│                             │
│   New personal record       │  ← only if applicable
│                             │
│ ─────────────────────────── │
│                             │
│   [ New puzzle ]            │  ← ButtonMMD, inverted
│                             │
│ ─────────────────────────── │
│                             │
│   [ Back to menu ]          │  ← ButtonMMD
│                             │
│ ─────────────────────────── │
│                             │
│   [ View records ]          │  ← ButtonMMD (tertiary)
│                             │
└─────────────────────────────┘
```

- `Puzzle complete` is the emotional headline, not a score number
- stats as label-value pairs, left-aligned labels, right-aligned values
- `HorizontalDividerMMD` between rows
- no animations, no confetti

#### If you keep the current score model (compatibility path)
Show Errors, Hints, Score — but make `Puzzle complete` the headline, not `Score 85`.

---

### 9.13 Records screen

#### Rename
Use `Records`, not `Best Scores` or `Leaderboard`.

#### Recommended record model
Per difficulty:
- puzzles completed
- best completion time
- best no-hint time
- last completed date (optional)

#### Layout
Sectioned rows using `LazyColumnMMD`:
```
  Easy
  ─────────────────────────
  Completed       12
  Best time       09:42
  Best no-hint    11:15
  ─────────────────────────
  Medium
  ─────────────────────────
  Completed       8
  Best time       22:30
  Best no-hint    —
  ─────────────────────────
  Hard
  ...
```

#### Empty states
Show em dash `—` for missing records. Do not show fake zeros.

---

## 10. Theme implementation

### 10.1 Theme.kt

```kotlin
@Composable
fun KompaktSudokuTheme(content: @Composable () -> Unit) {
    ThemeMMD(
        colorScheme = eInkColorScheme,
        typography = eInkTypography,
        content = content
    )
}
```

No customizations beyond MMD defaults. The Calm* apps prove that `eInkColorScheme` + `eInkTypography` out of the box is the correct foundation.

### 10.2 Typography roles

| Role | Style | Usage |
|------|-------|-------|
| `screenTitle` | `eInkTypography.headlineLarge` (28sp SemiBold) | Screen titles |
| `gridDigitGiven` | Custom ~22sp Bold | Given cells in Canvas |
| `gridDigitPlayer` | Custom ~22sp Regular | Player cells in Canvas |
| `gridNote` | Custom ~10sp Regular | Pencil marks in Canvas |
| `metaStrip` | `eInkTypography.bodyMedium` | Cells-left indicator |
| `rowTitle` | `eInkTypography.titleMedium` | Menu row primary text |
| `rowMeta` | `eInkTypography.bodyMedium` | Menu row secondary text |
| `keypadDigit` | `eInkTypography.titleLarge` | 3×3 digit pad |
| `buttonText` | `eInkTypography.labelLarge` | Action row buttons |

### 10.3 Surfaces and dividers

#### Rules
- use white as the default surface
- use black borders and dividers for structure
- use black fill for selected mode, selected cell, and truly primary emphasis
- avoid shadow and elevation as organizing tools
- use `HorizontalDividerMMD` between every list item and section boundary

### 10.4 Spacing rhythm
- 8dp small gaps
- 12dp row internal padding
- 16dp page padding
- 24dp major section spacing

The app should feel roomy but not empty.

---

## 11. Copy and language

Use calmer, more human language. Microcopy matters a lot on a small, quiet device.

| Current | Replace with |
|---------|-------------|
| `Pencil` | `Notes` |
| `Quit Game` | `Discard puzzle` |
| `Save and Exit` | `Keep for later` |
| `Best Scores` | `Records` |
| `Leaderboard` | `Records` |
| `You scored X` | `Puzzle complete` |
| `New personal best!` | `New personal record` |

---

## 12. Product semantics

### 12.1 Recommended path: calm session records

#### During play
- do not compare entries to the hidden solution for visible mistakes
- do show rule conflicts if duplicates occur
- track elapsed time silently
- track hints used

#### At completion
Show:
- elapsed time
- hint count
- optional "solved without hints" note

#### In records
Store:
- completions count
- best time
- best no-hint time

#### Why this is the best fit
It is easier to explain, less contradictory, more in line with a mindful device, and more respectful of Sudoku as a puzzle rather than a score machine.

### 12.2 Compatibility path: keep score, fix the semantics

If you must preserve score in v1 of the redesign:
- stop drawing solution-based wrong-cell borders during play
- clarify what increases score penalties
- move score to a secondary position in the summary screen
- rename `Best Scores` only if you are actually keeping score as the main record

This path is visually easier, but philosophically weaker.

---

## 13. Front-end architecture plan

### 13.1 Keep the current basic route model

The app only has a handful of top-level screens. A large Navigation Compose migration is not required.

Keep:
- `MainActivity`
- a route enum/sealed class
- view-model-owned game state

But make the screen structure cleaner and more componentized.

If you do use Jetpack Navigation, disable all transition animations:
```kotlin
composable(
    route = "game/{difficulty}",
    enterTransition = { EnterTransition.None },
    exitTransition = { ExitTransition.None }
) { /* ... */ }
```

### 13.2 Break GameScreen into smaller composables

New file structure:
- `PuzzleTopBar.kt`
- `PuzzleMetaStrip.kt`
- `PuzzleGrid.kt` (canvas)
- `InputModeToggle.kt`
- `PuzzleActionRow.kt`
- `DigitPad.kt` (3×3)
- `LeavePuzzleDialog.kt`

This will make future E-ink tuning much easier and reduce the current "one large screen file" problem.

### 13.3 Extend GameUiState

Suggested additions:
- `conflictMask: BooleanArray` or equivalent derived representation
- `elapsedMs: Long`
- `startedAtElapsedRealtimeMs: Long?`
- `hasUndo: Boolean`
- `cellsRemaining: Int` (can be derived, but useful if cached cleanly)

Do not bloat state with UI-only booleans if they can be derived cheaply.

### 13.4 GameViewModel changes

#### Keep
- central state machine
- save/load flow
- completion event pattern
- undo stack outside UI state

#### Change
- remove 4-note cap → `Set<Int>` with up to 9 values
- replace random hinting with selected-cell-first hinting
- compute conflict information from current board legality, not from hidden solution
- track elapsed time robustly across resume
- change completion/records pipeline from score storage to records storage

#### New helper functions
- `buildConflictMask(board: IntArray): BooleanArray`
- `cellsRemaining(board: IntArray): Int`
- `canRevealSelectedCell(...): Boolean`
- `applyHintToSelectedCell(...)`

### 13.5 Persistence

Current saved game state is serialized to JSON in Preferences DataStore. This is acceptable.

Add fields for:
- elapsed time
- start timestamp
- new note model (`Set<Int>` per cell instead of capped list)
- schema version identifier

If the schema becomes significantly richer later, consider Proto DataStore. Not mandatory for the first redesign pass.

### 13.6 Replace ScoreRepository with RecordsRepository

```kotlin
@Serializable
data class DifficultyRecord(
    val completedCount: Int,
    val bestTimeMs: Long?,
    val bestNoHintTimeMs: Long?,
    val lastCompletedAtEpochMs: Long?
)
```

Store a map keyed by difficulty. Introduce `RecordsRepository` / `DataStoreRecordsRepository`.

### 13.7 File-by-file refactor map

#### Keep with significant UI refactor
- `GameScreen.kt`
- `ControlsRow.kt`
- `NumberPad.kt`
- `MenuScreen.kt`
- `DifficultyScreen.kt`
- `SummaryScreen.kt`
- `LeaderboardScreen.kt` (renamed/reworked into Records)

#### Keep with gameplay/state changes
- `GameViewModel.kt`
- `GameUiState.kt`
- `DataStoreGameRepository.kt`

#### Keep mostly intact
- `SudokuGenerator.kt`
- uniqueness/classification infrastructure
- difficulty model

#### New files to introduce
- `RecordsRepository.kt`
- `RecordsScreen.kt`
- `PuzzleTopBar.kt`
- `PuzzleMetaStrip.kt`
- `DigitPadGrid.kt`
- `InputModeToggle.kt`
- `PuzzleActionRow.kt`

---

## 14. Conflict detection

### Recommendation
Compute conflicts from current board legality, not from the hidden solution.

Conflict exists if:
- the same non-zero value appears more than once in a row
- or column
- or 3×3 box

### Why
This gives the player valid Sudoku feedback without answer leakage.

### Testing
Write unit tests for:
- row duplicates
- column duplicates
- box duplicates
- overlapping conflict sets
- cleared conflicts after erase/replace

---

## 15. Elapsed time tracking

### Recommendation
Track elapsed time even if it is not shown prominently during play.

Use:
- `startedAtElapsedRealtimeMs`
- `accumulatedElapsedMs`

### Save/load behavior
When saving an in-progress puzzle:
- persist accumulated time up to save point
- if the puzzle was "running", persist enough to restore correctly

### Recommended policy
Count only active in-app solving time, not hours spent with the phone asleep. That better reflects a calm puzzle session.

---

## 16. E-ink implementation guidance

### 16.1 No animation — still absolute

- no transitions
- no ripple (enforced by `ThemeMMD`)
- no animated progress indicators
- static loading text only

### 16.2 Refresh-aware layout

Reduce perceived refresh harshness by:
- keeping large elements stable in position
- using tabular numerals where values change
- not shifting the keypad or controls between states unnecessarily
- avoiding decorative secondary elements that would redraw for no benefit

### 16.3 Grid redraw discipline

Continue to:
- build cell render info carefully
- avoid unnecessary state churn
- keep draw order deterministic

### 16.4 Minimize recomposition scope

- **Grid is a single Canvas** — only the Canvas redraws, not 81 individual composables
- **Number pad buttons are stable** — they only recompose when a digit's count reaches 9 (disabled state)
- **Action bar recomposes only when mode/undo state toggles** — use `derivedStateOf` or stable lambda references
- **Meta strip recomposes only when cells-remaining count changes** — isolate in its own composable

### 16.5 Touch debouncing

E-ink has ~100-200ms refresh. Double-taps during refresh can register phantom inputs. Implement a 200ms debounce on cell taps (see Canvas implementation sketch in Section 9.6.4).

---

## 17. Accessibility

- Grid cells include content descriptions: "Row 3, Column 5, value 7, given" or "Row 1, Column 2, empty"
- Number pad buttons: "Place digit 4"
- Action buttons: "Toggle notes mode, currently off"
- Selected cell announced: "Selected Row 3, Column 5"
- All interactive touch targets ≥48dp (enforced by layout)
- High contrast inherent to monochromatic scheme
- No reliance on color to convey state

---

## 18. Testing strategy

| Layer | Tool | What |
|-------|------|------|
| Game logic | JUnit 4 + Turbine + Coroutines Test | Digit placement, pencil marks, undo stack, conflict detection, completion detection |
| Conflict detection | JUnit 4 | Row/column/box duplicates, overlapping conflicts, cleared conflicts after erase |
| Puzzle generation | JUnit 4 | Sudoklify wrapper produces valid puzzles at each difficulty; solution matches |
| Serialization | JUnit 4 | `GameState` round-trips with full 1–9 notes, elapsed time |
| Records | JUnit 4 | Record updates on completion, best-time logic, no-hint tracking |
| Selected-cell hint | JUnit 4 | Valid target check, reveal behavior, edge cases |
| UI - Grid | Compose UI Test + Robolectric | Cell tap → selection; digit placement renders; pencil marks render |
| UI - Navigation | Compose UI Test | Menu → Game → Pause → Resume; Completion → Menu |
| E-ink QA | Mudita Kompakt device | Ghosting on rapid cell selection; keypad tap feel; note legibility; divider crispness |

---

## 19. Things to explicitly avoid

Do not add:
- animated hint reveals
- confetti or celebratory motion
- dense score dashboards
- decorative background patterns
- swipe-heavy controls
- gesture-only hidden actions
- fancy shadowed cards
- multiple competing status bars around the puzzle
- scroll-wheel time/number pickers
- horizontal carousels

These would weaken the product.

---

## 20. Phased implementation

### Phase 1: support-screen redesign
Goal: get the app visually aligned before touching deep gameplay semantics.

#### Deliverables
- home/menu redesign (list rows, row summaries)
- difficulty redesign (descriptive secondary lines)
- summary redesign (calm completion, `Puzzle complete` headline)
- records screen shell
- calmer copy throughout
- `TopAppBarMMD` on all screens
- `HorizontalDividerMMD` between all list items

---

### Phase 2: game screen ergonomic redesign
Goal: make play physically better.

#### Deliverables
- `TopAppBarMMD` on game screen
- meta strip (`31 cells left`)
- segmented Fill/Notes toggle
- dedicated action row (Undo, Erase, Hint)
- 3×3 digit pad (replacing 10-across row)
- erase moved out of keypad
- all `ButtonMMD` with text labels
- touch debounce on grid

---

### Phase 3: notes and hint overhaul
Goal: improve puzzle interaction quality.

#### Deliverables
- full 1–9 notes (replace 4-note cap)
- 3×3 micro-grid note rendering in Canvas
- selected-cell hinting
- note rendering legibility validation on hardware
- tests for note serialization and UI behavior

---

### Phase 4: semantics cleanup
Goal: make the product coherent.

#### Deliverables
- adopt conflict-based feedback (board legality, not solution comparison)
- remove visible solution-based mistake styling
- implement `RecordsRepository` with session-centric model
- update summary and records screens accordingly
- elapsed time tracking across resume

---

### Phase 5: polish and hardware tuning
Goal: make it feel finished on the Kompakt itself.

#### Deliverables
- on-device font tuning (grid digits, notes, keypad)
- keypad spacing tuning
- note legibility validation
- button target validation (≥48dp, prefer ≥56dp)
- summary/records spacing polish
- full recomposition-scope audit
- manual ghosting QA

---

## 21. Final recommendation

If only one design decision is adopted, it should be this:

> **Rebuild the play screen around a 3×3 keypad, full notes, and conflict-based feedback instead of solution-leaking wrong-cell indicators.**

That single decision fixes the biggest issues at once:
- ergonomics
- visual quality
- product coherence
- puzzle authenticity
- Mudita alignment

Everything else in this document will make the app better, but that one move transforms it from "good prototype" to "designed product".

---

## 22. Build checklist

### Support screens
- [ ] Redesign home/menu as list rows with summaries
- [ ] Redesign difficulty screen with descriptions
- [ ] Redesign summary screen (`Puzzle complete` headline)
- [ ] Rename/rebuild records screen with session-centric model

### MMD component migration
- [ ] Replace all `Text` → `TextMMD`
- [ ] Replace all `Button` → `ButtonMMD`
- [ ] Replace all `TopAppBar` → `TopAppBarMMD`
- [ ] Replace all `Divider` → `HorizontalDividerMMD`
- [ ] Replace all `AlertDialog` → full-screen composables or leave-puzzle dialog
- [ ] Replace all `LazyColumn` → `LazyColumnMMD`
- [ ] Add `SnackbarHostStateMMD` for confirmations
- [ ] Remove ALL animation code (`animateXAsState`, `AnimatedVisibility`, `Crossfade`)
- [ ] Remove all elevation / shadow / `Card` usage
- [ ] Verify no manual ripple re-enabling

### Game shell
- [ ] Add `TopAppBarMMD`
- [ ] Add meta strip
- [ ] Add segmented Fill/Notes toggle
- [ ] Add dedicated action row (Undo, Erase, Hint with text labels)
- [ ] Replace 10-across keypad with 3×3 keypad
- [ ] Move erase to action row
- [ ] Implement 200ms touch debounce on grid
- [ ] Verify all touch targets ≥48dp on Kompakt

### Gameplay
- [ ] Replace random hinting with selected-cell hinting
- [ ] Remove 4-note cap → full 1–9 `Set<Int>`
- [ ] Implement full 1–9 note rendering (3×3 micro-grid in Canvas)
- [ ] Implement conflict detection from board legality
- [ ] Remove visible solution-based mistake styling
- [ ] Disable completed-digit keypad buttons

### Persistence / records
- [ ] Persist full note model
- [ ] Track elapsed time coherently across resume
- [ ] Replace `ScoreRepository` with `RecordsRepository`
- [ ] Migrate existing records safely

### Copy
- [ ] `Pencil` → `Notes`
- [ ] `Quit Game` → `Discard puzzle`
- [ ] `Save and Exit` → `Keep for later`
- [ ] `Best Scores` / `Leaderboard` → `Records`
- [ ] Summary headline: `Puzzle complete`

### QA
- [ ] Conflict detection unit tests
- [ ] Save/resume with elapsed time unit tests
- [ ] Selected-cell hint logic unit tests
- [ ] Full 1–9 notes serialization round-trip tests
- [ ] Record updates on completion tests
- [ ] Generator remains off main thread
- [ ] No screen adds animations or ripples
- [ ] Test on real Kompakt hardware
- [ ] Validate note legibility on hardware
- [ ] Validate keypad hit targets on hardware
- [ ] Validate completion/records flow end-to-end
