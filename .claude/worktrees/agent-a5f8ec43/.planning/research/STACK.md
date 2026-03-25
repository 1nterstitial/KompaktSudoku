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
