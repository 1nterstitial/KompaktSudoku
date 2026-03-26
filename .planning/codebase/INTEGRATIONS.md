# External Integrations

**Analysis Date:** 2026-03-25

## APIs & External Services

**None at runtime.** The application has no network calls, no REST APIs, no web sockets, and no third-party cloud services. All data is local.

**Build-time only — MMD Package Registry:**
- Mudita Mindful Design library is fetched from GitHub Packages at build time
  - Registry URL: `https://maven.pkg.github.com/mudita/MMD`
  - Auth: `githubToken` Gradle property (user-level `~/.gradle/gradle.properties`, not in repo)
  - Group: `com.mudita`
  - Configured in: `settings.gradle.kts`
  - Fallback behavior: build fails at dependency resolution if MMD is not cached and token absent

## Data Storage

**Databases:**
- None — no SQLite, no Room

**DataStore (Primary Persistence):**
- Two separate `DataStore<Preferences>` instances (single active instance per file per process)
  - `game_state` store — in-progress game board persistence
    - Extension property: `Context.gameDataStore` in `DataStoreGameRepository.kt`
    - Key: `in_progress_game` (`stringPreferencesKey`) — stores `PersistedGameState` as JSON
    - Implementation: `app/src/main/java/com/mudita/sudoku/game/DataStoreGameRepository.kt`
  - `score_state` store — per-difficulty high scores
    - Extension property: `Context.scoreDataStore` in `DataStoreScoreRepository.kt`
    - Keys: `high_score_easy`, `high_score_medium`, `high_score_hard` (all `intPreferencesKey`)
    - Implementation: `app/src/main/java/com/mudita/sudoku/game/DataStoreScoreRepository.kt`
- Both stores are scoped to `applicationContext` and instantiated lazily in `MainActivity.kt`

**File Storage:**
- Not used — no file I/O beyond DataStore's managed files

**Caching:**
- None beyond DataStore's internal behavior

## Authentication & Identity

**Auth Provider:** None — no user accounts, no login, no identity

## Serialization (DataStore Integration)

**Library:** `kotlinx.serialization-json` 1.8.0

**Serialized type:** `PersistedGameState` at `app/src/main/java/com/mudita/sudoku/game/model/PersistedGameState.kt`
- Annotated `@Serializable`
- Fields: `board: List<Int>`, `solution: List<Int>`, `givenMask: List<Boolean>`, `difficulty: String`, `selectedCellIndex: Int?`, `pencilMarks: List<List<Int>>`, `errorCount: Int`, `hintCount: Int`, `isComplete: Boolean`
- `inputMode`, `isLoading`, and `undoStack` are intentionally NOT persisted (reset to defaults on resume)

**Encoding/decoding pattern** (in `DataStoreGameRepository.kt`):
```kotlin
Json.encodeToString(state.toPersistedState())    // save
Json.decodeFromString<PersistedGameState>(json)  // load
```
Corrupt or missing data returns `null` (graceful degradation — player starts fresh).

## Monitoring & Observability

**Error Tracking:** None — no Sentry, Crashlytics, or equivalent

**Logs:** Standard Android `Logcat` only; no structured logging framework

**Analytics:** None

## CI/CD & Deployment

**Hosting:** Not configured — no `Dockerfile`, no cloud platform config files detected

**CI Pipeline:** Not configured — no `.github/workflows/`, no Bitrise/CircleCI config detected

**Distribution:** APK sideloaded to Mudita Kompakt device directly

## Environment Configuration

**Required at build time:**
- `githubToken` — GitHub PAT with `read:packages` scope, set in `~/.gradle/gradle.properties` (user-level, outside the repo)

**No runtime environment variables** — Android apps do not use `.env` files; all configuration is compiled into the APK or read from DataStore at runtime

**Secrets location:** User-level Gradle properties only (`~/.gradle/gradle.properties`); nothing secret is committed to the repository

## Webhooks & Callbacks

**Incoming:** None

**Outgoing:** None

## Puzzle Generation (Local Third-Party Library)

**Sudoklify** (`dev.teogor.sudoklify:sudoklify-core` + `sudoklify-presets`, 1.0.0-beta04) — resolved from Maven Central at build time; runs entirely on-device at runtime with no network calls.

- Entry point: `app/src/main/java/com/mudita/sudoku/puzzle/engine/SudokuGenerator.kt`
- API used: `SudoklifyArchitect { loadPresetSchemas() }`, `constructSudoku { seed; type; difficulty }`, `generateGridWithGivens()`, `rawPuzzle.solution`
- Seeds: random `Long` in `1L..Long.MAX_VALUE` range, converted via `.toSeed()`
- Dimension: `Dimension.NineByNine` (fixed — no other board sizes used)
- Difficulty mapping: EASY → VERY_EASY or EASY; MEDIUM → MEDIUM; HARD → HARD or VERY_HARD
- Note: Sudoklify's built-in difficulty tiers do not reliably map to the project's technique-based difficulty classification; the project layers its own `DifficultyClassifier` and `UniquenessVerifier` gates on top

## External Repository Summary

| Repository | URL | Required For |
|------------|-----|-------------|
| Google | `https://dl.google.com/dl/android/maven2/` | AndroidX, AGP |
| Maven Central | `https://repo1.maven.org/maven2/` | Kotlin, Sudoklify, Coroutines, Turbine, Mockk, Robolectric |
| GitHub Packages (Mudita) | `https://maven.pkg.github.com/mudita/MMD` | MMD library only |

---

*Integration audit: 2026-03-25*
