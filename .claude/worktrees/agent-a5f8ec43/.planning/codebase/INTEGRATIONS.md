# Integrations

**Analysis Date:** 2026-03-24

## Sudoklify (Puzzle Generation)

- **Version:** 1.0.0-beta04
- **Coordinates:**
  - `dev.teogor.sudoklify:sudoklify-core:1.0.0-beta04`
  - `dev.teogor.sudoklify:sudoklify-presets:1.0.0-beta04`
- **Purpose:** Seed-based Sudoku puzzle generation with difficulty levels and preset schemas
- **Usage:** Actively used in `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt`

  Key import paths used:
  ```kotlin
  import dev.teogor.sudoklify.ExperimentalSudoklifyApi
  import dev.teogor.sudoklify.SudoklifyArchitect
  import dev.teogor.sudoklify.components.Difficulty as SudoklifyDifficulty
  import dev.teogor.sudoklify.components.Dimension
  import dev.teogor.sudoklify.components.toSeed
  import dev.teogor.sudoklify.presets.loadPresetSchemas
  import dev.teogor.sudoklify.puzzle.generateGridWithGivens
  ```

  Usage pattern in `SudokuGenerator.tryGenerateCandidate()`:
  ```kotlin
  @OptIn(ExperimentalSudoklifyApi::class)
  val architect = SudoklifyArchitect { loadPresetSchemas() }
  val rawPuzzle = architect.constructSudoku {
      seed = Random.nextLong(1L, Long.MAX_VALUE).toSeed()
      type = Dimension.NineByNine
      this.difficulty = sudoklifyDifficulty  // SudoklifyDifficulty enum value
  }
  val boardGrid: List<List<Int>> = rawPuzzle.generateGridWithGivens()
  val solutionGrid: List<List<Int>> = rawPuzzle.solution
  ```

  Difficulty mapping (`SudokuGenerator.pickSudoklifyDifficulty()`):
  - `Difficulty.EASY` → randomly picks `SudoklifyDifficulty.VERY_EASY` or `EASY`
  - `Difficulty.MEDIUM` → `SudoklifyDifficulty.MEDIUM` only
  - `Difficulty.HARD` → randomly picks `SudoklifyDifficulty.HARD` or `VERY_HARD`

- **Notes:**
  - API is annotated `@ExperimentalSudoklifyApi` — requires `@OptIn` at call sites
  - Beta status (1.0.0-beta04): no stable release exists; breaking changes are possible
  - Sudoklify's built-in difficulty classification does not match the project's technique-based tiers — the project's `DifficultyClassifier` is used as the authoritative gate instead
  - `loadPresetSchemas()` comes from the `-presets` artifact (separate dependency)
  - Seeds must be strictly positive (`1L..Long.MAX_VALUE`) — zero seed causes errors

---

## Mudita Mindful Design (MMD)

- **Version:** 1.0.1
- **Coordinates:** `com.mudita:mmd:1.0.1`
- **Repository:** GitHub Packages — `https://maven.pkg.github.com/mudita/MMD` (configured in `settings.gradle.kts`)
- **Purpose:** E-ink-optimised UI components: `ThemeMMD`, `ButtonMMD`, `TextMMD`, app bars, switches, tabs, text fields
- **Usage:** Declared in `gradle/libs.versions.toml` and `app/build.gradle.kts`; NOT yet imported in any source file — UI phases have not started
- **Notes:**
  - `ThemeMMD` provides `eInkColorScheme` (monochromatic) — do NOT use `dynamicColorScheme` or any tinted scheme
  - Ripple effects are disabled by default — do NOT re-enable via `indication` overrides (causes E-ink ghosting)
  - Typography is tuned for E-ink — do NOT override font sizes below MMD defaults
  - Use `ButtonMMD` and `TextMMD` throughout; do NOT fall back to plain Compose `Button` or `Text`
  - Requires GitHub Packages authentication to resolve at build time

---

## Jetpack Compose BOM

- **Version:** 2026.03.00
- **Coordinates:** `androidx.compose:compose-bom:2026.03.00`
- **Purpose:** Version alignment for all Compose libraries (eliminates manual version management)
- **Usage:** Applied as `platform(libs.compose.bom)` in `app/build.gradle.kts` for both `implementation` and `androidTestImplementation` scopes
- **Pinned library versions (from BOM 2026.03.00):**
  - `androidx.compose.ui:ui` — 1.10.5
  - `androidx.compose.material3:material3` — 1.4.0
  - `androidx.compose.ui:ui-test-junit4` — 1.10.5
- **Notes:** No explicit versions on individual Compose artifacts — all inherited from BOM

---

## AndroidX Lifecycle / ViewModel

- **Version:** 2.9.0
- **Coordinates:**
  - `androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0`
  - `androidx.lifecycle:lifecycle-runtime-compose:2.9.0`
- **Purpose:** MVVM ViewModel integration with Compose; lifecycle-aware StateFlow collection
- **Usage:** Declared in `app/build.gradle.kts`; not yet imported in source (ViewModel phases not started)
- **Key API when used:** `collectAsStateWithLifecycle()` from `lifecycle-runtime-compose` — preferred over `collectAsState()`

---

## DataStore Preferences

- **Version:** 1.2.1
- **Coordinates:** `androidx.datastore:datastore-preferences:1.2.1`
- **Purpose:** Async, coroutine-native key-value persistence for game state and high scores
- **Usage:** Declared in `app/build.gradle.kts`; not yet imported in source (persistence phase not started)
- **Planned storage keys:**
  - `in_progress_game` — serialized `GameState` JSON string
  - `high_score_easy`, `high_score_medium`, `high_score_hard` — Int scores
- **Notes:** Requires a `Context` to create the `DataStore` instance; typically wired via `Application` or dependency injection

---

## Kotlinx Serialization JSON

- **Version:** 1.8.0
- **Coordinates:** `org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0`
- **Purpose:** Compile-time checked JSON serialization for `GameState` → DataStore persistence
- **Plugin:** `kotlin-serialization` plugin applied in both `build.gradle.kts` (root) and `app/build.gradle.kts`
- **Usage:** Declared in `app/build.gradle.kts`; not yet used in source — planned for `@Serializable` on `GameState` domain model
- **Planned pattern:**
  ```kotlin
  @Serializable
  data class GameState(...)
  // encode: Json.encodeToString(gameState)
  // decode: Json.decodeFromString<GameState>(jsonString)
  ```

---

## Kotlinx Coroutines Android

- **Version:** 1.10.2
- **Coordinates:** `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2`
- **Purpose:** Android-aware coroutine dispatcher (`Dispatchers.Main`), StateFlow foundation
- **Usage:** Declared in `app/build.gradle.kts`; not yet directly imported in source (no ViewModel or async code yet)

---

## JUnit 4 (Test Runner)

- **Version:** 4.13.2
- **Coordinates:** `junit:junit:4.13.2`
- **Purpose:** Unit test runner; required by Compose test rule (`createComposeRule`)
- **Usage:** Actively used — all test files in `app/src/test/` use `org.junit.Test` and `org.junit.Assert.*`
- **Notes:** JUnit 5 is incompatible with `createComposeRule` — do not migrate

---

## Mockk

- **Version:** 1.13.17
- **Coordinates:** `io.mockk:mockk:1.13.17`
- **Purpose:** Kotlin-idiomatic mocking for unit tests, including `suspend` functions and coroutines
- **Usage:** Declared in `app/build.gradle.kts` (`testImplementation`); not yet used in any test file

---

## Turbine

- **Version:** 1.2.0
- **Coordinates:** `app.cash.turbine:turbine:1.2.0`
- **Purpose:** Testing StateFlow and Flow emissions with `flow.test { ... }` syntax
- **Usage:** Declared in `app/build.gradle.kts` (`testImplementation`); not yet used in any test file

---

## Robolectric

- **Version:** 4.14.1
- **Coordinates:** `org.robolectric:robolectric:4.14.1`
- **Purpose:** JVM-based Android/Compose UI testing without emulator; enables headless CI
- **Usage:** Declared in `app/build.gradle.kts` (`testImplementation`); not yet used in any test file

---

## Compose UI Test JUnit4

- **Version:** via Compose BOM 2026.03.00
- **Coordinates:** `androidx.compose.ui:ui-test-junit4`
- **Purpose:** Compose UI integration tests (`createComposeRule`, `onNodeWithText`, etc.)
- **Usage:** Declared in `app/build.gradle.kts` (`androidTestImplementation`); no instrumented tests written yet

---

## External Repository Summary

| Repository | URL | Required For |
|------------|-----|-------------|
| Google | `https://dl.google.com/dl/android/maven2/` | AndroidX, AGP |
| Maven Central | `https://repo1.maven.org/maven2/` | Kotlin, Sudoklify, Coroutines, Turbine, Mockk, Robolectric |
| GitHub Packages (Mudita) | `https://maven.pkg.github.com/mudita/MMD` | MMD library only |

---

*Integration audit: 2026-03-24*
