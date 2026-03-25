# Tech Stack

**Analysis Date:** 2026-03-24

## Build System

- **Gradle:** 8.11.1 (wrapper: `gradle/wrapper/gradle-wrapper.properties`)
- **Android Gradle Plugin (AGP):** 8.9.0 (`gradle/libs.versions.toml` → `agp = "8.9.0"`)
- **Version catalog:** `gradle/libs.versions.toml` (all dependency versions centralised here)
- **Build scripts:** Kotlin DSL (`.gradle.kts`) — `build.gradle.kts` (root), `app/build.gradle.kts`
- **Repositories:** Google, Maven Central, `https://maven.pkg.github.com/mudita/MMD` (for MMD library)
- **Repository mode:** `FAIL_ON_PROJECT_REPOS` — all repos must be declared in `settings.gradle.kts`

## Language & Runtime

- **Kotlin:** 2.3.20 — primary language, all source files
- **JVM target:** 17 (`compileOptions` + `kotlin.compilerOptions.jvmTarget = JVM_17` in `app/build.gradle.kts`)
- **Kotlin compiler plugins active:**
  - `kotlin-android` — Android integration
  - `kotlin-compose` — Compose compiler (tracks Kotlin version: 2.3.20)
  - `kotlin-serialization` — enables `@Serializable` annotation processing
- **Min Java source/target compatibility:** `JavaVersion.VERSION_17`

## SDK Config

| Setting | Value | Notes |
|---------|-------|-------|
| `compileSdk` | 35 | IDE warnings and deprecation notices |
| `targetSdk` | 31 | Matches MuditaOS K (AOSP 12 / API 31) |
| `minSdk` | 31 | Kompakt cannot run older Android |

## Core Dependencies

### Compose

- **Compose BOM:** `androidx.compose:compose-bom:2026.03.00` — pins all Compose library versions
  - Applied as `platform(libs.compose.bom)` in both `implementation` and `androidTestImplementation`
  - `compose-ui` (no version — from BOM)
  - `compose-material3` (no version — from BOM; Material 3 1.4.0)
  - `compose-ui-tooling-preview` (runtime dep)
  - `compose-ui-tooling` (debug only)
- **Compose enabled via:** `buildFeatures { compose = true }` in `app/build.gradle.kts`

### MMD (Mudita Mindful Design)

- **Version:** 1.0.1
- **Coordinates:** `com.mudita:mmd:1.0.1`
- **Source:** GitHub Packages (`https://maven.pkg.github.com/mudita/MMD`)
- **Status:** Declared in `libs.versions.toml`; NOT yet imported in any source file — scheduled for UI phases

### Lifecycle / ViewModel

- **Version:** 2.9.0 (`lifecycle`)
- `androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0`
- `androidx.lifecycle:lifecycle-runtime-compose:2.9.0` (provides `collectAsStateWithLifecycle`)

### Coroutines

- **Version:** 1.10.2 (`coroutines`)
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2` (runtime)
- `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2` (test only)

### Serialization

- **Version:** 1.8.0 (`kotlinSerialization`)
- `org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0`
- **Status:** Declared; not yet used in source (DataStore/GameState serialization is a future phase)

### DataStore

- **Version:** 1.2.1 (`datastore`)
- `androidx.datastore:datastore-preferences:1.2.1`
- **Status:** Declared; not yet used in source (persistence is a future phase)

### Sudoklify

- **Version:** 1.0.0-beta04 (`sudoklify`)
- `dev.teogor.sudoklify:sudoklify-core:1.0.0-beta04`
- `dev.teogor.sudoklify:sudoklify-presets:1.0.0-beta04`
- **Status:** Actively used — see INTEGRATIONS.md

## Testing Dependencies

| Library | Version | Scope | Coordinates |
|---------|---------|-------|-------------|
| JUnit 4 | 4.13.2 | `testImplementation` | `junit:junit:4.13.2` |
| Kotlinx Coroutines Test | 1.10.2 | `testImplementation` | `org.jetbrains.kotlinx:kotlinx-coroutines-test` |
| Mockk | 1.13.17 | `testImplementation` | `io.mockk:mockk:1.13.17` |
| Turbine | 1.2.0 | `testImplementation` | `app.cash.turbine:turbine:1.2.0` |
| Robolectric | 4.14.1 | `testImplementation` | `org.robolectric:robolectric:4.14.1` |
| Compose UI Test JUnit4 | via BOM | `androidTestImplementation` | `androidx.compose.ui:ui-test-junit4` |

**Note:** Mockk, Turbine, and Robolectric are declared in the version catalog and `app/build.gradle.kts` but not yet used in any test file — they are available for ViewModel and UI testing phases.

## Application Config

- **Application ID:** `com.mudita.sudoku`
- **Root project name:** `MuditaSudoku` (`settings.gradle.kts`)
- **Module:** Single module — `:app`
- **Minify:** Disabled (`isMinifyEnabled = false` in release build type)
- **ProGuard file:** `app/proguard-rules.pro` (referenced but default rules only)
- **Test instrumentation runner:** `androidx.test.runner.AndroidJUnitRunner`

---

*Stack analysis: 2026-03-24*
