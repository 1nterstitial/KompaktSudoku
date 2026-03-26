# Technology Stack

**Analysis Date:** 2026-03-25

## Languages

**Primary:**
- Kotlin 2.3.20 - All application and test code under `app/src/`

**Secondary:**
- None — no Java source files present

## Runtime

**Environment:**
- Android (AOSP 12 / API 31) — Mudita Kompakt E-ink device target

**JVM Target:**
- Java 17 (`sourceCompatibility`, `targetCompatibility`, and `compilerOptions.jvmTarget` all set to `JVM_17` in `app/build.gradle.kts`)

**Package Manager:**
- Gradle 8.11.1 (via wrapper at `gradle/wrapper/gradle-wrapper.properties`)
- Version catalog: `gradle/libs.versions.toml`
- Lockfile: Not present (no `gradle.lockfile`)

## Frameworks

**Core:**
- Jetpack Compose BOM 2026.03.00 — pins all Compose library versions (`app/build.gradle.kts` line 56)
  - `compose-ui` (via BOM)
  - `compose-material3` (via BOM)
  - `compose-ui-tooling-preview` (via BOM)
- AndroidX Activity Compose 1.8.2 — `ComponentActivity.setContent` entry point (`MainActivity.kt`)
- AndroidX Lifecycle ViewModel Compose 2.9.0 — `viewModels {}` delegation, `collectAsStateWithLifecycle`
- AndroidX Lifecycle Runtime Compose 2.9.0 — `collectAsStateWithLifecycle` extension

**Puzzle Generation:**
- Sudoklify Core `dev.teogor.sudoklify:sudoklify-core` 1.0.0-beta04 — puzzle generation via `SudoklifyArchitect`, `constructSudoku` DSL
- Sudoklify Presets `dev.teogor.sudoklify:sudoklify-presets` 1.0.0-beta04 — `loadPresetSchemas()` for preset puzzle schemas

**UI Design System:**
- Mudita Mindful Design (MMD) `com.mudita:MMD` 1.0.1 — `ThemeMMD`, `ButtonMMD`, `TextMMD`, E-ink optimized components; actively used in all UI screens

**Persistence:**
- DataStore Preferences `androidx.datastore:datastore-preferences` 1.2.1 — actively used for game state and high scores
- Kotlinx Serialization JSON `org.jetbrains.kotlinx:kotlinx-serialization-json` 1.8.0 — `@Serializable` on `PersistedGameState`; `Json.encodeToString` / `Json.decodeFromString`

**Testing:**
- JUnit 4.13.2 — unit test runner
- MockK 1.13.17 — Kotlin-native mocking
- Turbine 1.2.0 — Flow/StateFlow emission testing (`app.cash.turbine:turbine`)
- Robolectric 4.14.1 — JVM-based Android/Compose test execution without a device
- Kotlinx Coroutines Test 1.10.2 — `runTest`, `UnconfinedTestDispatcher`
- Compose UI Test JUnit4 (via BOM) — `createComposeRule`, Compose interaction testing

**Build/Dev:**
- Android Gradle Plugin (AGP) 8.9.0 — `com.android.application` plugin
- Kotlin Compose plugin 2.3.20 — `org.jetbrains.kotlin.plugin.compose`
- Kotlin Serialization plugin 2.3.20 — `org.jetbrains.kotlin.plugin.serialization`

## Key Dependencies

**Critical:**
- `com.mudita:MMD:1.0.1` — all UI uses MMD components; hosted on GitHub Packages (not Maven Central); requires `githubToken` in `~/.gradle/gradle.properties`
- `dev.teogor.sudoklify:sudoklify-core:1.0.0-beta04` — only Kotlin-native puzzle generation library with calibrated difficulty levels; beta status is a real risk
- `androidx.datastore:datastore-preferences:1.2.1` — sole persistence mechanism for game state and high scores
- `org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0` — JSON encoding of `PersistedGameState` for DataStore

**Infrastructure:**
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2` — coroutines runtime for IO dispatch, StateFlow
- `androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0` — ViewModel injection into Compose via `viewModel()`

## Configuration

**Environment:**
- `githubToken` — required in `~/.gradle/gradle.properties` (user-level, not in repo) to authenticate against `https://maven.pkg.github.com/mudita/MMD` for MMD resolution
- No `.env` files; all Android config is in `gradle.properties` and `app/build.gradle.kts`

**Build:**
- `gradle/libs.versions.toml` — version catalog (all versions centralized here)
- `app/build.gradle.kts` — module-level build config
- `build.gradle.kts` — root-level plugin declarations
- `settings.gradle.kts` — repository configuration including conditional MMD GitHub Packages block
- `gradle.properties` — JVM args (`-Xmx2048m`), AndroidX flag, Kotlin code style, non-transitive R class

## SDK Levels

| Setting | Value |
|---------|-------|
| `minSdk` | 31 |
| `targetSdk` | 31 |
| `compileSdk` | 35 |

## Platform Requirements

**Development:**
- Android SDK installed (referenced via `local.properties`)
- GitHub Personal Access Token with `read:packages` scope set as `githubToken` in `~/.gradle/gradle.properties`

**Production:**
- Mudita Kompakt device running AOSP 12 (API 31)
- Single APK distribution (no Play Store); `versionCode = 1`, `versionName = "1.0"`
- Minification disabled (`isMinifyEnabled = false` in release build type)

---

*Stack analysis: 2026-03-25*
