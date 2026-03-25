<!-- GSD:project-start source:PROJECT.md -->
## Project

**Mudita Kompakt Sudoku**

A Sudoku game for the Mudita Kompakt E-ink Android device, built with Kotlin and the Mudita Mindful Design (MMD) library. Players choose from three difficulty levels, solve puzzles with touch input, and earn an error-based score at completion. High scores are stored persistently per difficulty, and in-progress games can be paused and resumed later.

**Core Value:** A fully playable Sudoku experience that feels native on the Mudita Kompakt's E-ink display — responsive touch input, high-contrast grid, and smooth puzzle flow without display artifacts.

### Constraints

- **Tech Stack**: Kotlin + Jetpack Compose + MMD library — required for Mudita Kompakt compatibility
- **Display**: 800×480 E-ink — large touch targets, high contrast, no animations, instant feedback
- **Architecture**: MVVM with StateFlow — aligns with MMD's recommended pattern
- **Storage**: Local only — no network, no backend
<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->
## Technology Stack

## Recommended Stack
### Core Framework
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Kotlin | 2.3.20 | Primary language | Latest stable (March 2026); K2 compiler active by default; required for Compose Compiler plugin model where Compose version tracks Kotlin version directly |
| Jetpack Compose BOM | 2026.03.00 | Compose library version management | Latest stable BOM as of March 2026; pins all Compose libraries (ui 1.10.5, material3 1.4.0, runtime 1.10.5) to a tested-together set; removes manual version juggling |
| Compose Material 3 | 1.4.0 (via BOM) | UI components base layer | MMD builds on top of Material 3; required for ThemeMMD to function correctly |
| Android Gradle Plugin | 8.9.0 | Build tooling | Most recent stable at time of project start (March 2025 release); AGP 9.x requires Gradle 9.3+ which is a heavier migration; use 8.9.0 for a stable, proven build environment on Android 12 target |
| Gradle | 8.11.1 | Build system | Required minimum for AGP 8.9.0; use version catalog (libs.versions.toml) |
### MMD Library (Mandatory)
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Mudita Mindful Design (MMD) | 1.0.1 | All UI components | Required by PROJECT.md — not optional. Provides ThemeMMD (monochromatic E-ink color scheme + typography), ButtonMMD, TextMMD, text fields, switches, tabs, app bars. Version 1.0.1 (Feb 2026) includes naming convention refactors and compatibility improvements over 1.0.0 |
- Ripple effects are disabled by default in ThemeMMD — do not re-enable them. Any `indication` override that restores ripple will cause E-ink ghosting.
- MMD provides `eInkColorScheme` (monochromatic) — do not use `dynamicColorScheme` or any color-tinted scheme. The device display cannot render color.
- Typography is tuned for E-ink readability — do not override font sizes below MMD defaults.
- Use `ButtonMMD` and `TextMMD` throughout; do not fall back to plain Compose `Button` or `Text` which lack E-ink optimizations.
### Architecture & State Management
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Jetpack ViewModel | via AndroidX (BOM) | State holder, business logic host | Standard Android architecture component; survives configuration changes; required by PROJECT.md's MVVM + StateFlow constraint |
| Kotlin Coroutines | 1.10.2 | Async execution, Flow foundation | Latest stable; DataStore and StateFlow both require coroutines; lightweight on Helio A22 CPU |
| Kotlin Flow / StateFlow | bundled with coroutines | UI state stream | MVVM pattern mandated by PROJECT.md; StateFlow is a hot flow that Compose observes directly via `collectAsStateWithLifecycle` |
| Lifecycle (collectAsStateWithLifecycle) | via AndroidX BOM | Lifecycle-aware state collection | Prevents collecting state when app is backgrounded; preferred over plain `collectAsState` for Android |
### Sudoku Puzzle Generation
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Sudoklify (dev.teogor.sudoklify:sudoklify-core) | 1.0.0-beta04 | Puzzle generation with difficulty levels | Only actively maintained Kotlin-native sudoku generation library with built-in difficulty levels (Easy, Medium, Hard, Expert); seed-based generation gives 2.4 trillion+ variations; KMP-compatible pure Kotlin so no JNI overhead on the Helio A22 |
### Local Persistence
| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| DataStore Preferences | 1.2.1 | Game state persistence (pause/resume) and high score storage | Async, coroutine-native, no main-thread ANR risk. Official Google replacement for SharedPreferences. Integrates cleanly with StateFlow. Version 1.2.1 confirmed from official Android Developers docs. |
- `in_progress_game`: Serialized `GameState` as JSON string (board array, selected cell, error count, hints used)
- `high_score_easy`, `high_score_medium`, `high_score_hard`: Int scores
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
## SDK Levels
| Setting | Value | Rationale |
|---------|-------|-----------|
| `minSdk` | 31 | MuditaOS K is AOSP 12 (API 31); setting lower serves no purpose and prevents use of API 31 features. **Do not set below 31** — the Kompakt cannot run older versions. |
| `targetSdk` | 31 | Match the device's OS level. Setting targetSdk 33+ enables behavior changes (notification permissions, etc.) that may be undefined on de-Googled AOSP 12. Stay at 31 until Mudita ships a higher API update. |
| `compileSdk` | 35 | Compile against a recent SDK to get IDE warnings and deprecation notices without changing runtime behavior. SDK 35 is stable and available in AGP 8.9.0. |
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
## Installation Reference
# gradle/libs.versions.toml
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
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->
## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->
## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:workflow-start source:GSD defaults -->
## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->



<!-- GSD:profile-start -->
## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
