# Mudita UI Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align KompaktSudoku's visual design with Mudita's Mindful Design language — slimmer game header, bolder controls, expanded digit pad, clearer records hierarchy, and heavier typography app-wide.

**Architecture:** Pure UI changes across 8 Kotlin files. No new files, no data/logic changes. Each task targets one screen or component, producing an atomic commit.

**Tech Stack:** Jetpack Compose, Material3, Mudita MMD library, Canvas API

---

### Task 1: Slim down the game screen top bar

**Files:**
- Modify: `app/src/main/java/com/interstitial/sudoku/ui/game/PuzzleTopBar.kt`

- [ ] **Step 1: Reduce the back arrow size and padding**

In `PuzzleTopBar.kt`, change the back arrow TextMMD from `titleLarge` to `titleMedium`, and reduce its padding from `8.dp` to `4.dp` vertical / `6.dp` horizontal:

```kotlin
TextMMD(
    text = "\u2190",
    style = MaterialTheme.typography.titleMedium,
    modifier = Modifier
        .clickable(onClick = onBack)
        .padding(horizontal = 6.dp, vertical = 4.dp)
)
```

- [ ] **Step 2: Build and verify on device**

Run: `./gradlew.bat assembleRelease`

Install APK and verify the header row is visibly slimmer, with the grid shifted up.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/game/PuzzleTopBar.kt
git commit -m "ui: slim down game screen top bar"
```

---

### Task 2: Bold the controls row and thicken borders

**Files:**
- Modify: `app/src/main/java/com/interstitial/sudoku/ui/game/GameScreen.kt`

- [ ] **Step 1: Add FontWeight import**

Add to the imports in `GameScreen.kt`:

```kotlin
import androidx.compose.ui.text.font.FontWeight
```

- [ ] **Step 2: Update the Fill/Notes toggle**

In the `ControlsRow` composable, change the toggle border from `1.dp` to `2.dp`, height from `32.dp` to `34.dp`, and text style from `labelMedium` to `labelLarge` with `FontWeight.Bold`:

```kotlin
        // Fill/Notes segmented toggle
        Row(
            modifier = Modifier
                .weight(2f)
                .height(34.dp)
                .border(2.dp, onSurface)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (inputMode == InputMode.FILL) onSurface else surface)
                    .clickable(onClick = { if (inputMode != InputMode.FILL) onToggleMode() }),
                contentAlignment = Alignment.Center
            ) {
                TextMMD(
                    text = "Fill",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (inputMode == InputMode.FILL) surface else onSurface
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (inputMode == InputMode.NOTES) onSurface else surface)
                    .clickable(onClick = { if (inputMode != InputMode.NOTES) onToggleMode() }),
                contentAlignment = Alignment.Center
            ) {
                TextMMD(
                    text = "Notes",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (inputMode == InputMode.NOTES) surface else onSurface
                )
            }
        }
```

- [ ] **Step 3: Update the action buttons**

In the same `ControlsRow`, change all three `ButtonMMD` heights from `32.dp` to `34.dp`, and their label text from `labelMedium` to `labelLarge` with `FontWeight.Bold`:

```kotlin
        // Action buttons
        ButtonMMD(
            onClick = onUndo,
            enabled = hasUndo,
            modifier = Modifier.weight(1f).height(34.dp)
        ) { TextMMD("Undo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }

        ButtonMMD(
            onClick = onErase,
            enabled = canErase,
            modifier = Modifier.weight(1f).height(34.dp)
        ) { TextMMD("Erase", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }

        ButtonMMD(
            onClick = onHint,
            enabled = canHint,
            modifier = Modifier.weight(1f).height(34.dp)
        ) { TextMMD("Hint", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
```

- [ ] **Step 4: Build and verify on device**

Run: `./gradlew.bat assembleRelease`

Verify controls row has visibly bolder text and thicker toggle border.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/game/GameScreen.kt
git commit -m "ui: bold controls row text and thicken borders"
```

---

### Task 3: Expand digit pad to fill remaining space

**Files:**
- Modify: `app/src/main/java/com/interstitial/sudoku/ui/game/DigitPad.kt`
- Modify: `app/src/main/java/com/interstitial/sudoku/ui/game/GameScreen.kt`

- [ ] **Step 1: Remove fixed height from DigitPad**

In `DigitPad.kt`, remove the `.height(120.dp)` from the Canvas modifier so it only has `fillMaxWidth`, padding, and pointerInput. The height will be controlled by the parent:

```kotlin
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .pointerInput(Unit) {
```

Also remove the unused `import androidx.compose.foundation.layout.height` if it becomes unused.

- [ ] **Step 2: Add weight(1f) at the GameScreen call site**

In `GameScreen.kt`, change the DigitPad invocation to pass `Modifier.weight(1f)`:

```kotlin
            // Compact digit pad
            DigitPad(
                digitCounts = state.digitCounts,
                onDigit = { digit ->
                    if (state.inputMode == InputMode.NOTES) {
                        onAction(GameAction.ToggleNote(digit))
                    } else {
                        onAction(GameAction.PlaceDigit(digit))
                    }
                },
                modifier = Modifier.weight(1f)
            )
```

- [ ] **Step 3: Build and verify on device**

Run: `./gradlew.bat assembleRelease`

Verify the digit pad now fills all remaining vertical space below the controls row, with no dead space at the bottom. Digits should be larger and easier to tap.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/game/DigitPad.kt app/src/main/java/com/interstitial/sudoku/ui/game/GameScreen.kt
git commit -m "ui: expand digit pad to fill remaining vertical space"
```

---

### Task 4: Redesign Records screen hierarchy

**Files:**
- Modify: `app/src/main/java/com/interstitial/sudoku/ui/records/RecordsScreen.kt`

- [ ] **Step 1: Add Canvas and dotted-line imports**

Add these imports to `RecordsScreen.kt`:

```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.sp
```

- [ ] **Step 2: Create DottedDivider composable**

Add a private composable at the bottom of the file, before `formatTime`:

```kotlin
@Composable
private fun DottedDivider(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        )
    }
}
```

- [ ] **Step 3: Update DifficultySection**

Replace the entire `DifficultySection` composable with the new hierarchy — bold header with thick underline, indented stat rows with dotted separators, spacing between sections:

```kotlin
@Composable
private fun DifficultySection(difficulty: Difficulty, record: DifficultyRecord) {
    TextMMD(
        text = difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
    )
    HorizontalDividerMMD(
        thickness = 2.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    RecordRow("Completed", if (record.completedCount > 0) record.completedCount.toString() else "\u2014")
    DottedDivider(modifier = Modifier.padding(start = 32.dp, end = 16.dp))
    RecordRow("Best time", record.bestTimeMs?.let { formatTime(it) } ?: "\u2014")
    DottedDivider(modifier = Modifier.padding(start = 32.dp, end = 16.dp))
    RecordRow("Best no-hint", record.bestNoHintTimeMs?.let { formatTime(it) } ?: "\u2014")
    Spacer(modifier = Modifier.height(8.dp))
}
```

- [ ] **Step 4: Update RecordRow with indentation and bold text**

Replace the `RecordRow` composable with indented padding and bold font:

```kotlin
@Composable
private fun RecordRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 32.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextMMD(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        TextMMD(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
```

- [ ] **Step 5: Add Spacer import if not present**

Ensure the imports include:

```kotlin
import androidx.compose.foundation.layout.Spacer
```

- [ ] **Step 6: Build and verify on device**

Run: `./gradlew.bat assembleRelease`

Verify: difficulty headers are large and extra bold with thick underline, stat rows are indented with dotted separators, spacing between sections instead of thick dividers.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/records/RecordsScreen.kt
git commit -m "ui: redesign Records screen with clearer hierarchy"
```

---

### Task 5: Bold typography on HomeScreen

**Files:**
- Modify: `app/src/main/java/com/interstitial/sudoku/ui/home/HomeScreen.kt`

- [ ] **Step 1: Add FontWeight import**

```kotlin
import androidx.compose.ui.text.font.FontWeight
```

- [ ] **Step 2: Update MenuRow typography**

Replace the `MenuRow` composable with bold title and medium-weight subtitle:

```kotlin
@Composable
private fun MenuRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        TextMMD(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        TextMMD(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
```

- [ ] **Step 3: Build and verify on device**

Run: `./gradlew.bat assembleRelease`

Verify menu row titles are bolder.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/home/HomeScreen.kt
git commit -m "ui: bold HomeScreen menu row typography"
```

---

### Task 6: Bold typography on NewPuzzleScreen

**Files:**
- Modify: `app/src/main/java/com/interstitial/sudoku/ui/newpuzzle/NewPuzzleScreen.kt`

- [ ] **Step 1: Add FontWeight import**

```kotlin
import androidx.compose.ui.text.font.FontWeight
```

- [ ] **Step 2: Bold the TopAppBar title**

Change the TopAppBarMMD title:

```kotlin
        TopAppBarMMD(
            title = { TextMMD("New puzzle", fontWeight = FontWeight.Bold) },
```

- [ ] **Step 3: Update DifficultyRow typography**

Replace the `DifficultyRow` composable with bold title and medium-weight subtitle:

```kotlin
@Composable
private fun DifficultyRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        TextMMD(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextMMD(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
```

- [ ] **Step 4: Build and verify on device**

Run: `./gradlew.bat assembleRelease`

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/newpuzzle/NewPuzzleScreen.kt
git commit -m "ui: bold NewPuzzleScreen typography"
```

---

### Task 7: Bold typography on SummaryScreen

**Files:**
- Modify: `app/src/main/java/com/interstitial/sudoku/ui/summary/SummaryScreen.kt`

- [ ] **Step 1: Bold the TopAppBar title**

Change the TopAppBarMMD title:

```kotlin
        TopAppBarMMD(
            title = { TextMMD("Puzzle complete", fontWeight = FontWeight.Bold) }
        )
```

- [ ] **Step 2: Update StatRow and personal best text**

Replace the `StatRow` composable with bold label and value:

```kotlin
@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextMMD(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        TextMMD(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}
```

Also update the "New personal record" text:

```kotlin
        if (isPersonalBest) {
            TextMMD(
                text = "New personal record",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
            HorizontalDividerMMD(thickness = 2.dp)
        }
```

- [ ] **Step 3: Build and verify on device**

Run: `./gradlew.bat assembleRelease`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/summary/SummaryScreen.kt
git commit -m "ui: bold SummaryScreen typography"
```

---

### Task 8: Bold typography on LeavePuzzleDialog and Records TopAppBar

**Files:**
- Modify: `app/src/main/java/com/interstitial/sudoku/ui/game/LeavePuzzleDialog.kt`
- Modify: `app/src/main/java/com/interstitial/sudoku/ui/records/RecordsScreen.kt`

- [ ] **Step 1: Bold the dialog title**

In `LeavePuzzleDialog.kt`, add the import:

```kotlin
import androidx.compose.ui.text.font.FontWeight
```

Update the title text:

```kotlin
            TextMMD(
                text = "Leave puzzle?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
```

- [ ] **Step 2: Bold the Records TopAppBar title**

In `RecordsScreen.kt`, update the TopAppBarMMD title:

```kotlin
        TopAppBarMMD(
            title = { TextMMD("Records", fontWeight = FontWeight.Bold) },
```

- [ ] **Step 3: Build and verify on device**

Run: `./gradlew.bat assembleRelease`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/game/LeavePuzzleDialog.kt app/src/main/java/com/interstitial/sudoku/ui/records/RecordsScreen.kt
git commit -m "ui: bold dialog and Records TopAppBar typography"
```

---

### Task 9: Final build and verification

- [ ] **Step 1: Clean build**

Run: `./gradlew.bat clean assembleRelease`

Ensure zero errors.

- [ ] **Step 2: Verify all screens on device**

Check each screen against the design spec:
- Home: bold menu titles
- New Puzzle: bold title bar and difficulty rows
- Game: slim header, bold controls, expanded digit pad
- Records: clear hierarchy with indented dotted rows
- Summary: bold stat rows and personal record text
- Leave dialog: bold title

- [ ] **Step 3: Commit plan doc**

```bash
git add docs/superpowers/plans/2026-04-07-mudita-ui-alignment.md
git commit -m "docs: add Mudita UI alignment implementation plan"
```
