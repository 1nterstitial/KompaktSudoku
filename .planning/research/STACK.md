# Technology Stack

**Project:** Mudita Kompakt Sudoku
**Researched:** 2026-03-23
**Platform target:** Mudita Kompakt (MuditaOS K — AOSP 12, Android API 31, MediaTek Helio A22, 3 GB RAM)

---

## Recommended Stack

### Core Framework

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Kotlin | 2.3.20 | Primary language | Latest stable (March 2026); K2 compiler active by default; required for Compose Compiler plugin model where Compose version tracks Kotlin version directly |
| Jetpack Compose BOM | 2026.03.00 | Compose library version management | Latest stable BOM as of March 2026; pins all Compose libraries (ui 1.10.5, material3 1.4.0, runtime 1.10.5) to a tested-together set; removes manual version juggling |
| Compose Material 3 | 1.4.0 (via BOM) | UI components base layer | MMD builds on top of Material 3; required for ThemeMMD to function correctly |
| Android Gradle Plugin | 8.9.0 | Build tooling | Most recent stable at time of project start (March 2025 release); AGP 9.x requires Gradle 9.3+ which is a heavier migration; use 8.9.0 for a stable, proven build environment on Android 12 target |
| Gradle | 8.11.1 | Build system | Required minimum for AGP 8.9.0; use version catalog (libs.versions.toml) |

**Confidence:** MEDIUM — Kotlin 2.3.20 and Compose BOM 2026.03.00 confirmed via official Android Developers docs and Kotlin Blog. AGP 8.9.0 confirmed via Android Developers release notes. AGP 9.x was documented but skipped deliberately (see Alternatives Considered).

---

### MMD Library (Mandatory)

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Mudita Mindful Design (MMD) | 1.0.1 | All UI components | Required by PROJECT.md — not optional. Provides ThemeMMD (monochromatic E-ink color scheme + typography), ButtonMMD, TextMMD, text fields, switches, tabs, app bars. Version 1.0.1 (Feb 2026) includes naming convention refactors and compatibility improvements over 1.0.0 |

**Integration:**
```kotlin
// settings.gradle.kts — MMD is published to Maven Central
repositories {
    mavenCentral()
}

// build.gradle.kts (app module)
dependencies {
    implementation("com.mudita:MMD:1.0.1")
}
```

**Critical MMD constraints the rest of the stack must respect:**
- Ripple effects are disabled by default in ThemeMMD — do not re-enable them. Any `indication` override that restores ripple will cause E-ink ghosting.
- MMD provides `eInkColorScheme` (monochromatic) — do not use `dynamicColorScheme` or any color-tinted scheme. The device display cannot render color.
- Typography is tuned for E-ink readability — do not override font sizes below MMD defaults.
- Use `ButtonMMD` and `TextMMD` throughout; do not fall back to plain Compose `Button` or `Text` which lack E-ink optimizations.

**Confidence:** MEDIUM — Library existence and version confirmed via GitHub (github.com/mudita/MMD). Internal build versions (compileSdk, minSdk) could not be extracted from the repository's build files via available tooling; assumptions below are based on AOSP 12 (API 31) target.

---

### Architecture & State Management

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Jetpack ViewModel | via AndroidX (BOM) | State holder, business logic host | Standard Android architecture component; survives configuration changes; required by PROJECT.md's MVVM + StateFlow constraint |
| Kotlin Coroutines | 1.10.2 | Async execution, Flow foundation | Latest stable; DataStore and StateFlow both require coroutines; lightweight on Helio A22 CPU |
| Kotlin Flow / StateFlow | bundled with coroutines | UI state stream | MVVM pattern mandated by PROJECT.md; StateFlow is a hot flow that Compose observes directly via `collectAsStateWithLifecycle` |
| Lifecycle (collectAsStateWithLifecycle) | via AndroidX BOM | Lifecycle-aware state collection | Prevents collecting state when app is backgrounded; preferred over plain `collectAsState` for Android |

**Architecture pattern:** Single-module, layered MVVM.

```
ui/          — Compose screens + composables (stateless where possible)
viewmodel/   — GameViewModel, MenuViewModel (StateFlow exposure)
domain/      — SudokuBoard, SudokuCell, GameState, Scoring (pure Kotlin, no Android deps)
data/        — GameRepository (DataStore persistence)
```

**Confidence:** HIGH — MVVM + StateFlow is the official Google-recommended pattern for Compose apps; confirmed by Android Developers architecture documentation and PROJECT.md requirement.

---

### Sudoku Puzzle Generation

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Sudoklify (dev.teogor.sudoklify:sudoklify-core) | 1.0.0-beta04 | Puzzle generation with difficulty levels | Only actively maintained Kotlin-native sudoku generation library with built-in difficulty levels (Easy, Medium, Hard, Expert); seed-based generation gives 2.4 trillion+ variations; KMP-compatible pure Kotlin so no JNI overhead on the Helio A22 |

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.teogor.sudoklify:sudoklify-core:1.0.0-beta04")
}
```

**Usage pattern:**
```kotlin
val spec = SudokuSpec {
    seed = Random.nextLong()
    difficulty = Difficulty.MEDIUM
    type = SudokuType.Sudoku9x9
}
val puzzle = SudoklifyArchitect.constructSudoku(spec)
```

**Why not custom implementation:** A backtracking generator is ~200 lines of Kotlin but guarantees of unique solutions and calibrated difficulty by revealed-cell count alone are insufficient for "Medium = harder solving logic" (as required by PROJECT.md). Sudoklify handles this. The beta label is acceptable — the library is stable in practice and the alternative is significantly more domain work.

**Confidence:** MEDIUM — Version and Maven coordinates confirmed via GitHub (github.com/teogor/sudoklify). Beta status is a real caveat; no production usage data available for this specific library. If Sudoklify proves unsuitable, fall back to embedding a modified QQWing port (used by LibreSudoku, a production Compose sudoku app).

---

### Local Persistence

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| DataStore Preferences | 1.2.1 | Game state persistence (pause/resume) and high score storage | Async, coroutine-native, no main-thread ANR risk. Official Google replacement for SharedPreferences. Integrates cleanly with StateFlow. Version 1.2.1 confirmed from official Android Developers docs. |

```kotlin
// build.gradle.kts
dependencies {
    implementation("androidx.datastore:datastore-preferences:1.2.1")
}
```

**Do NOT use SharedPreferences.** It blocks the main thread on write, which on a resource-constrained device (Helio A22, 3 GB LPDDR3) risks frame drops on E-ink refresh cycles. DataStore's coroutine-based writes avoid this entirely.

**Do NOT use Room.** A relational database is overkill for this app's storage needs: three high scores (one per difficulty) and one in-progress game state. Room adds ~200 KB to APK and code generation complexity for no benefit. If storage requirements grow significantly in a future version, migrate to Room then.

**Data to persist:**
- `in_progress_game`: Serialized `GameState` as JSON string (board array, selected cell, error count, hints used)
- `high_score_easy`, `high_score_medium`, `high_score_hard`: Int scores

**Serialization:** Use `kotlinx.serialization` (see Supporting Libraries) to convert `GameState` to/from JSON string for DataStore storage.

**Confidence:** HIGH — DataStore 1.2.1 confirmed from official Android documentation. Recommendation against SharedPreferences is well-established in the Android developer community with multiple authoritative sources.

---

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| kotlinx.serialization | 1.8.0 | JSON serialization of GameState for DataStore | Use the `@Serializable` annotation on domain models; encode/decode via `Json.encodeToString` / `Json.decodeFromString` |
| Kotlinx Coroutines Test | 1.10.2 | Testing coroutines and Flow | All ViewModel unit tests; use `runTest` and `UnconfinedTestDispatcher` |
| JUnit 4 | 4.13.2 | Unit test runner | Standard for Android; Compose test rule (`createComposeRule`) requires JUnit 4; do not switch to JUnit 5 (Compose test rule is JUnit 4 incompatible with JUnit 5 runner without adapter) |
| Mockk | 1.13.x | Mocking in unit tests | Kotlin-idiomatic mocking; prefer over Mockito for Kotlin `suspend` functions and coroutine flows |
| Turbine | 1.2.0 | Testing StateFlow / Flow emissions | Replaces manual flow collection in tests; `flow.test { ... }` syntax is clean and reliable |
| Compose UI Test (JUnit 4) | via BOM | Compose UI testing | Integration tests for board rendering, cell selection interaction |
| Robolectric | 4.14.x | JVM-based Compose UI testing | Run Compose UI tests without an emulator; dramatically faster CI; required for testing on a headless machine without a Kompakt device |

---

## SDK Levels

| Setting | Value | Rationale |
|---------|-------|-----------|
| `minSdk` | 31 | MuditaOS K is AOSP 12 (API 31); setting lower serves no purpose and prevents use of API 31 features. **Do not set below 31** — the Kompakt cannot run older versions. |
| `targetSdk` | 31 | Match the device's OS level. Setting targetSdk 33+ enables behavior changes (notification permissions, etc.) that may be undefined on de-Googled AOSP 12. Stay at 31 until Mudita ships a higher API update. |
| `compileSdk` | 35 | Compile against a recent SDK to get IDE warnings and deprecation notices without changing runtime behavior. SDK 35 is stable and available in AGP 8.9.0. |

**Confidence:** MEDIUM — AOSP 12 / API 31 confirmed by multiple independent sources (NotebookCheck, search results referencing "Android 12"). The specific API level 31 (not 32) for Android 12 is based on the known Android versioning table (Android 12 = API 31, Android 12L = API 32). Using targetSdk 31 is a conservative choice that can be raised once Mudita publishes confirmed API level documentation.

---

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| AGP | 8.9.0 | 9.1.0 (latest) | AGP 9.x requires Gradle 9.3.1 — a major Gradle upgrade with potential build script breaking changes. No feature in 9.x is needed for this app. Avoid unnecessary migration risk. |
| Storage | DataStore | SharedPreferences | Synchronous writes risk ANR on constrained hardware; deprecated by Google for new development |
| Storage | DataStore | Room | Relational DB overkill for 3 high scores + 1 game state; adds build complexity with no benefit |
| Puzzle Generation | Sudoklify | Custom backtracking | Custom backtracking cannot trivially achieve calibrated "solving complexity" difficulty without substantial domain work; Sudoklify provides this built-in |
| Puzzle Generation | Sudoklify | QQWing port | QQWing is Java-origin, not idiomatic Kotlin, and requires manual porting; acceptable fallback if Sudoklify proves problematic |
| Testing | JUnit 4 | JUnit 5 | Compose test rule (`createComposeRule`) has JUnit 4 lifecycle dependency; JUnit 5 + Compose requires a compatibility layer; not worth the friction |
| Mocking | Mockk | Mockito | Mockito has limited support for Kotlin `suspend` functions and coroutines; Mockk is the Kotlin-native choice |
| Serialization | kotlinx.serialization | Gson | Gson is not Kotlin-aware (no null safety, no data class support without reflection); kotlinx.serialization is compile-time checked |
| Architecture | MVVM + StateFlow | MVI | MVI adds a formal Intent/Action layer useful for complex event sourcing; for a single-screen game with simple state, the added abstraction is overhead |

---

## Installation Reference

```toml
# gradle/libs.versions.toml
[versions]
agp = "8.9.0"
kotlin = "2.3.20"
composeBom = "2026.03.00"
coroutines = "1.10.2"
datastore = "1.2.1"
sudoklify = "1.0.0-beta04"
kotlinxSerialization = "1.8.0"
mmd = "1.0.1"
mockk = "1.13.14"
turbine = "1.2.0"
junit = "4.13.2"
robolectric = "4.14.1"

[libraries]
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose" }
lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose" }
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
sudoklify-core = { group = "dev.teogor.sudoklify", name = "sudoklify-core", version.ref = "sudoklify" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
mmd = { group = "com.mudita", name = "MMD", version.ref = "mmd" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

```kotlin
// build.gradle.kts (app module) — key sections
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 31
        targetSdk = 31
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.sudoklify.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mmd)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)

    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
```

---

## Confidence Summary

| Area | Confidence | Notes |
|------|------------|-------|
| Kotlin version (2.3.20) | MEDIUM | Confirmed via Kotlin Blog and official AGP docs; latest at time of research |
| Compose BOM (2026.03.00) | HIGH | Confirmed via official Android Developers BOM mapping page |
| MMD version (1.0.1) | MEDIUM | Confirmed via GitHub releases page; internal build requirements (minSdk etc.) not extractable from repository files with available tooling |
| AGP version (8.9.0) | HIGH | Confirmed via official Android Developers release notes |
| DataStore (1.2.1) | HIGH | Confirmed via official Android documentation |
| Sudoklify (1.0.0-beta04) | MEDIUM | Version confirmed via GitHub; beta status is a real risk; no alternative with equivalent difficulty calibration exists on Maven Central |
| Android API 31 target | MEDIUM | AOSP 12 = API 31 confirmed via multiple sources; Mudita has not published a formal developer spec page |
| Coroutines (1.10.2) | MEDIUM | Confirmed via Maven Central and GitHub releases search |

---

## Sources

- [Mudita MMD GitHub Repository](https://github.com/mudita/MMD) — MMD version, components, design philosophy
- [Android Developers — Compose BOM Mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) — BOM 2026.03.00 library versions
- [Android Developers — Use a BOM](https://developer.android.com/develop/ui/compose/bom) — BOM usage, Kotlin 2.0+ compiler model
- [Android Developers — AGP Release Notes](https://developer.android.com/build/releases/about-agp) — AGP 9.1.0 and 8.x versions, Gradle requirements
- [Android Developers — DataStore](https://developer.android.com/topic/libraries/architecture/datastore) — DataStore Preferences 1.2.1
- [Kotlin Blog — Kotlin 2.3.0 Released](https://blog.jetbrains.com/kotlin/2025/12/kotlin-2-3-0-released/) — Kotlin version timeline
- [Kotlin Blog — Kotlin 2.3.20 Released](https://blog.jetbrains.com/kotlin/2026/03/kotlin-2-3-20-released/) — Latest stable Kotlin
- [Sudoklify GitHub (teogor)](https://github.com/teogor/sudoklify) — Version, Maven coordinates, difficulty API
- [LibreSudoku GitHub](https://github.com/kaajjo/LibreSudoku) — QQWing fallback reference, production Compose sudoku app
- [Mudita Forum — What Android API will the Kompakt support?](https://forum.mudita.com/t/what-android-api-will-the-kompakt-support/7376) — API level context
- [NotebookCheck — Mudita Kompakt E Ink Phone](https://www.notebookcheck.net/Mudita-Kompakt-E-Ink-Phone-A-minimalist-privacy-focused-phone-powered-by-Android.911587.0.html) — Hardware specs, AOSP 12 confirmation
- [Atipik — DataStore vs SharedPreferences 2025](https://www.atipik.ch/en/blog/android-jetpack-datastore-vs-sharedpreferences) — DataStore recommendation context

---

---

# v1.1 Rendering API Reference

**Added:** 2026-03-25
**Scope:** Specific Compose/Canvas/Material 3 APIs for 5 cosmetic rendering tasks in v1.1
**Overall confidence:** HIGH (all primary recommendations verified against official Android documentation)

## Context

The existing codebase at v1.0 uses:
- Canvas-based `GameGrid` with `TextMeasurer` + `drawText` + `drawRect` + `drawLine` in DrawScope
- `ButtonMMD` + `TextMMD` for all non-canvas UI components
- `ThemeMMD` (monochromatic, ripple disabled, E-ink tuned)
- No new dependencies needed for any of the 5 tasks below

No new Gradle dependencies are required for v1.1. All APIs are present in the existing stack.

---

## Task 1: Pencil-mark text in white on selected (black) cells

### Problem
`GameGrid.kt` lines 96–100 define `pencilStyle` with `color = Color.Black`. When a cell is selected, `drawSelectedCell` fills the background with solid black, making black pencil marks invisible.

### API: `DrawScope.drawText` `color` parameter

`drawText` has a `color` parameter that **overrides** the color stored in the `TextLayoutResult` at draw time. The existing `TextLayoutResult` does not need to be re-measured.

**Exact stable signature (androidx.compose.ui.graphics.drawscope):**
```kotlin
fun DrawScope.drawText(
    textLayoutResult: TextLayoutResult,
    color: Color = Color.Unspecified,   // pass Color.White to override
    topLeft: Offset = Offset.Zero,
    alpha: Float = Float.NaN,
    shadow: Shadow? = null,
    textDecoration: TextDecoration? = null,
    drawStyle: DrawStyle? = null,
    blendMode: BlendMode = DrawScope.DefaultBlendMode,
)
```

When `color != Color.Unspecified`, the draw call ignores the color baked into the `TextLayoutResult` and uses the supplied color. No re-measure step, no new `TextStyle` object per call.

**Recommended change in `drawPencilMarks`:**
- Add `isSelected: Boolean` parameter
- Pass `color = if (isSelected) Color.White else Color.Unspecified` at the `drawText` call site
- The existing `pencilStyle` declaration (`color = Color.Black`) does not need to change

```kotlin
// Example call site after the change:
drawText(
    textLayoutResult = measured,
    topLeft = Offset(x, y),
    color = if (isSelected) Color.White else Color.Unspecified
)
```

**Confidence:** HIGH — signature verified from official Android Developers documentation. Already in use in `GameGrid.kt` without the color parameter; adding it is non-breaking.

**E-ink note:** No animation or frame-rate change. A single-color swap at draw time; safe for E-ink.

---

## Task 2: Dynamic pencil-mark font sized to fit 4 marks in a 2x2 layout

### Problem
`pencilStyle` uses `fontSize = 9.sp` fixed. The v1.1 requirement changes pencil mark layout from a 3x3 (9-digit) mini-grid to a 2x2 layout (up to 4 marks per cell). Each mark must fit in one quadrant of the cell. Cell size is dynamic (computed from `gridSizePx / 9f`), so font size must be derived from the available pixel space.

### API: `TextMeasurer.measure` + `Density.toSp`

`TextMeasurer` is already imported and used in `GameGrid.kt`. Font size can be computed from `cellSizePx` at composition time using `LocalDensity`.

**Recommended approach (heuristic, O(1)):**
```kotlin
val density = LocalDensity.current
val subCellPx = cellSizePx / 2f                          // one quadrant width
val pencilFontSp = with(density) { (subCellPx * 0.65f).toSp() }  // ~65% fill factor

val pencilStyle = remember(cellSizePx) {
    TextStyle(
        fontSize = pencilFontSp,
        fontWeight = FontWeight.Normal,
        color = Color.Black
    )
}
```

The `0.65f` factor gives visual breathing room. For a standard Roboto digit, rendered width is approximately 55–65% of the font em-square. Wrap in `remember(cellSizePx)` to avoid recomputing on every recomposition.

**Alternative: binary search (O(log n), higher precision):**
```kotlin
fun computeFittingFontSp(
    textMeasurer: TextMeasurer,
    targetPx: Float,
    density: Density
): TextUnit {
    var lo = 4f
    var hi = with(density) { targetPx.toSp().value }
    repeat(8) {                       // 8 iterations gives < 1sp precision
        val mid = (lo + hi) / 2f
        val measured = textMeasurer.measure("8", TextStyle(fontSize = mid.sp))
        if (measured.size.width <= targetPx && measured.size.height <= targetPx) lo = mid
        else hi = mid
    }
    return lo.sp
}

// At composable level:
val pencilFontSp = remember(cellSizePx, textMeasurer) {
    computeFittingFontSp(textMeasurer, cellSizePx / 2f, density)
}
```

Use the binary search approach if the 0.65 heuristic visually clips digits on the physical device.

**Confidence:** HIGH — `TextMeasurer.measure` is a stable API since Compose 1.4; BOM 2026.03.00 is well beyond that. `Density.toSp()` is a standard Compose unit conversion.

**E-ink note:** Font size computed once at composition time when `cellSizePx` changes. No per-frame recomputation; safe.

---

## Task 3: Taller/thinner font for number pad digit buttons

### Problem
`NumberPad.kt` uses `ButtonMMD` with `TextMMD(text = digit.toString())`. MMD default typography is readable but not visually condensed. Goal: a taller/thinner (condensed) appearance for digit labels.

### API: `DeviceFontFamilyName("sans-serif-condensed")`

**`sans-serif-condensed`** is defined in AOSP's `data/fonts/fonts.xml` as Roboto with `wdth=75` (75% of standard width). It ships in the platform fonts partition of all standard AOSP 12 builds — not in Google apps — so it is present on de-Googled MuditaOS K unless Mudita explicitly stripped the condensed Roboto variant (which would be unusual).

`DeviceFontFamilyName` is a **stable** API — the `@ExperimentalTextApi` annotation was removed in a Compose UI framework commit. No opt-in annotation is required with BOM 2026.03.00.

**Option A (recommended): use device system font**
```kotlin
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val condensedFamily = FontFamily(
    Font(DeviceFontFamilyName("sans-serif-condensed"), weight = FontWeight.Normal)
)
```

Use inside `ButtonMMD` content lambda with a standard Compose `Text` (since `TextMMD` may not expose a `fontFamily` parameter):

```kotlin
ButtonMMD(
    modifier = Modifier.weight(1f).sizeIn(minHeight = 56.dp),
    onClick = { onDigitClick(digit) }
) {
    Text(
        text = digit.toString(),
        style = TextStyle(
            fontFamily = condensedFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black
        )
    )
}
```

**Note on using `Text` vs `TextMMD` inside `ButtonMMD`:** `ButtonMMD` accepts a standard Compose content lambda. Placing a plain `Text` composable inside it does not re-enable ripple or violate E-ink constraints. The ripple suppression is applied at the `ThemeMMD` level (theme-wide `LocalIndication`), not per-composable. Using `Text` here for font control is safe.

**Option B (fallback): bundle the font file**

If `DeviceFontFamilyName("sans-serif-condensed")` renders as the default `sans-serif` on the Kompakt (device font partition differs), bundle the font:

1. Download `RobotoCondensed-Regular.ttf` from Google Fonts (OFL licensed — same file AOSP bundles)
2. Place at `app/src/main/res/font/roboto_condensed.ttf`
3. Use: `FontFamily(Font(R.font.roboto_condensed, FontWeight.Normal))`

APK size impact: ~60 KB. No build configuration changes needed.

**Recommendation:** Ship with Option A. Add physical device verification to the execution plan. Prepare Option B font file in case of fallback.

**Do NOT use:** `androidx.compose.ui.text.googlefonts` (downloadable fonts) — requires Google Play Services, absent on MuditaOS K.

**Confidence:** MEDIUM — `sans-serif-condensed` confirmed in AOSP 12 `fonts.xml` via AOSP mirror. De-Googled device may omit it (LOW probability but unverified). `DeviceFontFamilyName` stabilization confirmed via Android Googlesource commit. Option B is HIGH confidence.

**E-ink note:** Font family change has no E-ink impact. Static text, no animation.

---

## Task 4: Diagonal hatching / crosshatch pattern drawn on a Canvas rect

### Problem
Draw a fill pattern of parallel diagonal lines over a rectangular area within `DrawScope`. Needed for visual state distinction (e.g., used/disabled cell or button region).

### API: `DrawScope.clipRect` + `drawLine` loop

`PathEffect.dashPathEffect` creates dashed single paths — it does not tile a fill pattern. Diagonal hatching requires looping `drawLine` calls. Using `clipRect` keeps line math simple — draw extended lines, let the clip do the bounding.

**45-degree diagonal hatch (top-left to bottom-right), using `clipRect`:**
```kotlin
import androidx.compose.ui.graphics.drawscope.clipRect

fun DrawScope.drawDiagonalHatch(
    topLeft: Offset,
    rectSize: Size,
    spacingPx: Float,      // e.g. 6.dp.toPx() — space between lines
    strokeWidthPx: Float,  // e.g. 1.dp.toPx()
    color: Color = Color.Black
) {
    clipRect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + rectSize.width,
        bottom = topLeft.y + rectSize.height
    ) {
        // Sweep lines from -(height) to +(width) to cover full diagonal range
        var offset = -rectSize.height
        while (offset < rectSize.width) {
            drawLine(
                color = color,
                start = Offset(topLeft.x + offset, topLeft.y + rectSize.height),
                end = Offset(topLeft.x + offset + rectSize.height, topLeft.y),
                strokeWidth = strokeWidthPx
            )
            offset += spacingPx
        }
    }
}
```

**For crosshatch:** Call a second time with `start`/`end` reversed on one axis (135-degree lines):
```kotlin
// 135-degree lines:
drawLine(
    color = color,
    start = Offset(topLeft.x + offset, topLeft.y),
    end = Offset(topLeft.x + offset + rectSize.height, topLeft.y + rectSize.height),
    strokeWidth = strokeWidthPx
)
```

`clipRect` is in `androidx.compose.ui.graphics.drawscope` — stable, no API-level requirement, no new imports beyond what `GameGrid.kt` already uses.

**Confidence:** HIGH — `drawLine`, `clipRect`, and `DrawScope` are core stable Compose Canvas APIs. The geometry is pure math with no library dependency.

**E-ink note:** Use a spacing of 4–8 dp minimum. Very dense lines (< 2dp spacing) may cause E-ink ghosting from complex partial refresh patterns. Draw this pattern only on state change, not continuously.

---

## Task 5: Subtle background for the inactive mode button + frame separating the Fill/Pencil pair

### Problem
`ControlsRow.kt` currently uses `Color.White` as the inactive button background. Two improvements needed:
1. A perceptible light-gray background on the inactive `Fill` or `Pencil` box
2. A visible border frame around the Fill+Pencil pair to visually separate it from the number pad row

### Part A: Subtle inactive background color

**Recommended:** `Color(0xFFE0E0E0)` — 12% gray from white.

On monochromatic E-ink, this is the minimum gray value that renders visually distinct from `Color.White` without requiring a heavy full-page refresh. Values lighter than `0xFFE8E8E8` may be indistinguishable on panels with high gamma.

```kotlin
// In ControlsRow.kt, replace the inactive branch:
.background(
    if (inputMode == InputMode.FILL) Color.Black
    else Color(0xFFE0E0E0)
)
```

**Do not use** `MaterialTheme.colorScheme.surfaceVariant` or tonal elevation values. `ThemeMMD` uses a monochromatic `eInkColorScheme` where the primary color is black/white. Tonal elevation applies a primary-color tint that produces an unpredictable shade and may conflict with MMD's intent. An explicit literal color is more predictable and verifiable on device.

**Confidence:** HIGH for the API (`Modifier.background(Color)` — stable, already used in `ControlsRow.kt`). MEDIUM for the specific shade — needs physical device verification. Start with `0xFFE0E0E0`; if not visible, try `0xFFCCCCCC`.

### Part B: Frame around the Fill/Pencil pair

**Recommended: `Modifier.border`**

Wrap the two mode-toggle `Box` composables in their own `Row` and apply `Modifier.border`:

```kotlin
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.RectangleShape

// Outer Row in ControlsRow:
Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

    // Fill + Pencil pair wrapped in a bordered Row:
    Row(
        modifier = Modifier
            .weight(2f)
            .border(width = 1.dp, color = Color.Black, shape = RectangleShape)
    ) {
        // Fill Box (weight 1f, no border here)
        // Pencil Box (weight 1f, no border here)
    }

    // Undo ButtonMMD (weight 1f)
    // Hint ButtonMMD (weight 1f)
}
```

`Modifier.border` draws a stroke border inset to the composable boundary. `RectangleShape` produces a sharp rectangular frame. Use `RoundedCornerShape(2.dp)` if a slight radius is preferred.

**Alternative: `Modifier.drawBehind` for bottom-only separator**

If the design intent is only a separator line between the controls row and the number pad (not a full box frame around Fill/Pencil), use `drawBehind` on the outer `Row`:

```kotlin
Modifier.drawBehind {
    drawLine(
        color = Color.Black,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = 1.dp.toPx()
    )
}
```

**Recommendation:** Use `Modifier.border` if the requirement is a frame around the pair. Use `Modifier.drawBehind` with a single line if the requirement is just a row separator.

**Confidence:** HIGH — `Modifier.border` and `Modifier.drawBehind` are stable Compose foundation APIs with no API-level requirements.

**E-ink note:** A 1dp border on a static layout triggers one E-ink refresh at state change. No ghosting risk.

---

## Summary Table

| Task | Primary API | Key Call | Confidence | Device Verify |
|------|-------------|----------|------------|---------------|
| 1 — Pencil marks white on selected | `DrawScope.drawText(color=)` | `drawText(measured, color = Color.White, topLeft = ...)` | HIGH | No |
| 2 — Dynamic pencil font size | `TextMeasurer.measure` + `Density.toSp` | `remember(cellSizePx) { (cellSizePx / 2f * 0.65f).toSp() }` | HIGH | No |
| 3 — Condensed number pad font | `DeviceFontFamilyName("sans-serif-condensed")` | `FontFamily(Font(DeviceFontFamilyName(...)))` | MEDIUM | YES — verify on Kompakt |
| 4 — Diagonal hatching | `clipRect` + `drawLine` loop | Loop-based hatch inside `clipRect { }` | HIGH | No |
| 5a — Subtle inactive background | `Modifier.background(Color)` | `Color(0xFFE0E0E0)` | MEDIUM | YES — verify shade on E-ink |
| 5b — Frame around pair | `Modifier.border` | `border(1.dp, Color.Black, RectangleShape)` | HIGH | No |

---

## No New Dependencies Required

All APIs for v1.1 tasks are present in the existing stack. No changes to `build.gradle.kts`, `libs.versions.toml`, or any Gradle files are needed.

The only conditional addition is the bundled font fallback for Task 3 (Option B): `app/src/main/res/font/roboto_condensed.ttf`. This requires no build configuration change — `res/font/` resources are automatically compiled by AGP.

---

## Physical Device Verification Flags

- **Task 3:** Run a test screen with `DeviceFontFamilyName("sans-serif-condensed")` before finalizing. If the font renders identically to the default sans-serif, switch to the bundled `res/font/roboto_condensed.ttf`.
- **Task 5a shade:** Verify `Color(0xFFE0E0E0)` is perceptibly distinct from `Color.White` on the E-ink panel. If not, try `0xFFCCCCCC` (20% gray). If the panel cannot render intermediate gray at all, substitute a 1dp border outline on the inactive button box instead of a gray fill.

---

## v1.1 Sources

- [Android Developers — Graphics in Compose (DrawScope, drawText)](https://developer.android.com/develop/ui/compose/graphics/draw/overview) — `drawText` `color` parameter; `clipRect`; `drawLine`
- [Android Developers — Work with fonts in Compose](https://developer.android.com/develop/ui/compose/text/fonts) — `DeviceFontFamilyName`, `FontFamily`, custom font resources
- [Android Developers — DeviceFontFamilyName API reference](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/font/DeviceFontFamilyName) — stable API confirmation
- [AOSP fonts.xml (aosp-mirror/platform_frameworks_base)](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/data/fonts/fonts.xml) — `sans-serif-condensed` defined as Roboto `wdth=75` in AOSP 12
- [DeviceFontFamilyName stabilization commit (Android Googlesource)](https://android.googlesource.com/platform/frameworks/support/+/0490e25b6d1a7e57410d3c80f77720542d3a6150%5E!/) — `@ExperimentalTextApi` removal confirmed
- [ProAndroidDev — Auto-sizing Text with TextMeasurer](https://proandroiddev.com/auto-sizing-text-in-jetpack-compose-with-basictext-effbc41502fa) — binary search font sizing pattern
- [ProAndroidDev — Dot Dash Design: Lines in Compose with PathEffect](https://proandroiddev.com/dot-dash-design-c30928484f79) — PathEffect vs manual drawLine for patterns
- [Android Developers — Graphics modifiers (Modifier.border, drawBehind)](https://developer.android.com/develop/ui/compose/graphics/draw/modifiers) — border and drawBehind APIs
