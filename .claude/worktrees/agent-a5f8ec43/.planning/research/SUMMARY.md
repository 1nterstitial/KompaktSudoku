# Project Research Summary

**Project:** Mudita Kompakt Sudoku
**Domain:** Mobile puzzle game (E-ink Android, offline, mindful design)
**Researched:** 2026-03-23
**Confidence:** MEDIUM (core Android patterns HIGH; MMD-specific and Sudoklify details MEDIUM)

## Executive Summary

This is a Jetpack Compose Sudoku game for the Mudita Kompakt — an E-ink Android phone running a de-Googled AOSP 12 (API 31). The platform constraint is the defining architectural fact: every UI and rendering decision must account for E-ink display physics (ghosting, partial refresh latency, monochromatic output). The recommended approach is MVVM + StateFlow in a single-module layered architecture, with the Mudita Mindful Design (MMD) library wrapping all UI components to ensure E-ink compatibility from the start. The puzzle engine (generation, validation, difficulty classification) is pure Kotlin with no Android dependencies, making it independently testable and the first thing to build.

The most important product decisions are already made by the platform and the mindful ethos: no timers, no animations, no ads, no color, no Google Services. What remains is execution. The feature set is well-bounded — puzzle generation with genuine difficulty levels, a silent error-tracking game loop, pencil marks, pause/resume persistence, hint with score penalty, and a per-difficulty local leaderboard. These nine features, in that priority order, define the full v1 scope. Everything else is explicitly deferred.

The key risks are concentrated in two areas: E-ink rendering correctness (ghosting from accumulated partial refreshes and full-grid recomposition on cell interaction) and puzzle engine integrity (unique-solution guarantee and technique-based difficulty classification). Both risks must be resolved in the foundation phase before any UI work is layered on top, because retrofitting correct E-ink behavior and correct difficulty semantics after the UI is built is expensive.

---

## Key Findings

### Recommended Stack

The stack is built around the mandatory MMD 1.0.1 library, Kotlin 2.3.20 with the K2 compiler, Jetpack Compose BOM 2026.03.00 (Material3 1.4.0), and AGP 8.9.0. These are the current stable versions as of March 2026. AGP 9.x is deliberately skipped — it requires a Gradle 9.3+ migration with no benefit for this project. The full version catalog is pinned in STACK.md.

For puzzle generation, Sudoklify (dev.teogor.sudoklify:sudoklify-core:1.0.0-beta04) is the recommended library — the only actively maintained Kotlin-native Sudoku generator with built-in difficulty levels and seed-based generation. Its beta status is the one real risk; QQWing (used by LibreSudoku) is the documented fallback if Sudoklify proves unsuitable. Persistence is DataStore Preferences 1.2.1 with kotlinx.serialization 1.8.0 for JSON encoding — SharedPreferences and Room are both explicitly rejected for this workload.

**Core technologies:**
- Kotlin 2.3.20 + Compose BOM 2026.03.00: primary language and UI framework — current stable, K2 compiler active
- MMD 1.0.1: mandatory UI component library — provides ThemeMMD, ButtonMMD, TextMMD, E-ink color scheme
- Sudoklify 1.0.0-beta04: puzzle generation — only option with calibrated difficulty levels in Kotlin
- DataStore Preferences 1.2.1: persistence — async, coroutine-native, no ANR risk on constrained hardware
- kotlinx.serialization 1.8.0: GameState JSON encoding — compile-time checked, Kotlin-native
- Kotlin Coroutines 1.10.2 + StateFlow: async and state management — mandated by project requirements
- SDK: minSdk 31, targetSdk 31, compileSdk 35 — Kompakt is AOSP 12 (API 31), stay conservative on targetSdk

### Expected Features

See FEATURES.md for the full feature dependency tree, anti-feature rationale, and phase-specific notes.

**Must have (table stakes):**
- Puzzle generation — valid, unique, difficulty-classified (without this, nothing else functions)
- Three difficulty levels (Easy / Medium / Hard) — classified by solving technique required, not just clue count
- Cell selection + number entry — core interaction loop; must feel immediate on E-ink
- Pencil marks / candidate notes — required for Medium/Hard puzzles to be solvable
- Undo — essential on E-ink where accidental taps are frequent
- Silent error tracking + completion detection — the closed game loop
- Completion summary with error-based score — the satisfying feedback moment
- Pause and resume — E-ink users put devices down for hours; state loss is fatal to trust
- Puzzle uniqueness guarantee — exactly one valid solution per puzzle

**Should have (differentiators):**
- Error-based scoring (not time-based) — aligns with mindful, no-pressure ethos
- Silent error reveal at completion only — encourages reflection, not reactive correction
- Per-difficulty local high score table — personal progression, motivates replay
- Hint with score penalty (cell-reveal) — keeps game flowing without disqualifying the player
- MMD/E-ink native UI — app feels designed for the device, not ported

**Defer (v2+):**
- Strategy-revealing hints (teaching hints) — high complexity; cell-reveal is sufficient for v1
- Statistics tracking across sessions — nice to have, not required for satisfying first run
- Advanced difficulty tuning beyond three tiers — three levels cover the user need

### Architecture Approach

The architecture is single-module MVVM with Unidirectional Data Flow: UI layer (Compose + MMD screens) collects `StateFlow` from ViewModels and emits events up; ViewModels own game logic and coordinate with a data layer (DataStore repositories). The puzzle engine sits outside the ViewModel as pure Kotlin with no Android dependencies. Four screens — MenuScreen, GameScreen, SummaryScreen, LeaderboardScreen — map to three ViewModels (GameViewModel, MenuViewModel, LeaderboardViewModel). See ARCHITECTURE.md for full data flow diagrams, data class definitions, and the suggested build order.

**Major components:**
1. Puzzle Engine (SudokuGenerator, SudokuSolver, SudokuValidator, DifficultyConfig) — pure Kotlin, no Android deps, testable with plain JUnit; generates unique puzzles, validates moves, delivers hints
2. Domain Model (SudokuCell, SudokuBoard, GameState) — immutable data classes with @Serializable; solution grid embedded in SudokuBoard so hints and error reveal work after pause/resume
3. GameViewModel — owns full game lifecycle via StateFlow<GameUiState>; receives sealed GameEvent types from UI; launches coroutines in viewModelScope
4. Data Layer (GameStateRepository, HighScoreRepository over DataStore) — exposes Flow to ViewModels; writes on every significant state change using applicationScope to survive ViewModel destruction
5. UI Layer (Compose + MMD) — stateless composables (SudokuGrid, NumberPad, screens); no business logic; all components are MMD variants wrapped in ThemeMMD

### Critical Pitfalls

See PITFALLS.md for full prevention strategies, detection methods, and phase assignments.

1. **Whole-grid recomposition on every cell interaction** — Model each cell as its own @Stable data class; pass cell-level state; use `key(cellIndex)` in grid rendering; verify with Layout Inspector before layering features. Address in foundation.
2. **E-ink ghosting from accumulated partial refreshes** — Use ThemeMMD throughout; zero animations (no AnimatedVisibility, no animate*AsState); explicit solid background on all composables. Address in foundation, verify on physical hardware.
3. **Puzzle generator producing multiple-solution puzzles** — After each clue removal, run a modified solver configured to abort on second solution found; only accept removal if exactly one solution exists. Non-negotiable invariant, not an optimisation.
4. **Touch targets too small for E-ink interaction model** — Minimum 56dp per cell (not Android's 48dp standard); number pad below grid to preserve cell size. Establish sizing constants before building number pad.
5. **Difficulty implemented as clue count only** — Integrate technique classification: Easy = naked singles only; Medium = hidden singles/naked pairs required; Hard = advanced elimination required. Implement alongside the generator.

---

## Implications for Roadmap

Based on combined research, the architecture's own build-order analysis and the pitfall phase assignments converge on the same six-phase structure. The ordering is driven by dependency chains and risk front-loading.

### Phase 1: Puzzle Engine
**Rationale:** Zero dependencies on other phases; highest algorithmic risk (unique-solution guarantee, technique-based difficulty classification). Getting this wrong late is the most expensive possible mistake. Pure Kotlin means it is fully testable without Android tooling.
**Delivers:** SudokuGenerator (backtracking + uniqueness check), SudokuSolver, SudokuValidator, DifficultyConfig with technique classification gates, unit test suite
**Addresses:** Puzzle generation, unique puzzles, three difficulty levels (FEATURES.md table stakes)
**Avoids:** Multiple-solution puzzles (Pitfall 3), difficulty-as-clue-count-only (Pitfall 7)
**Research flag:** NEEDS RESEARCH — difficulty classification by required solving technique is non-trivial; algorithm research required before implementation

### Phase 2: Game State and ViewModel Skeleton
**Rationale:** The ViewModel contract defines what the UI consumes. Building the UI against a defined state shape prevents rework. Domain models must be in place before persistence and UI work diverges.
**Delivers:** SudokuCell/SudokuBoard/GameState data classes with @Serializable, GameEvent sealed class, GameViewModel with StateFlow<GameUiState>, input handling wiring
**Addresses:** Cell selection, number entry, error tracking, undo (FEATURES.md)
**Avoids:** Mutable state in composables (Architecture anti-pattern 1), error counting on given cells (Pitfall 8 — GIVEN/PLAYER/HINT cell type enum defined here)
**Research flag:** Standard patterns — MVVM + StateFlow is well-documented; skip research-phase

### Phase 3: Core Game UI (E-ink)
**Rationale:** Playability on device validates the touch/grid UX before investing in save/restore plumbing. E-ink rendering correctness (ghosting, recomposition) must be verified on physical hardware before any other feature is layered on.
**Delivers:** ThemeMMD root wrapper, SudokuGrid custom composable, NumberPad (ButtonMMD), GameScreen assembling both, end-to-end playable (no persistence yet)
**Addresses:** MMD/E-ink native UI, cell selection + number entry feel, pencil marks (FEATURES.md differentiators)
**Avoids:** Whole-grid recomposition (Pitfall 1), E-ink ghosting (Pitfall 2), touch targets too small (Pitfall 4), ripple/elevation artifacts (Pitfall 9)
**Research flag:** NEEDS VALIDATION ON HARDWARE — touch target sizing and ghosting behavior cannot be fully verified in emulator; physical Kompakt device testing required

### Phase 4: Persistence
**Rationale:** Pause/resume is a table stakes feature for E-ink users. Persistence must be in place before scoring is wired up, because completion flow needs the leaderboard write to be atomic.
**Delivers:** DataStore setup, GameStateRepository implementation, pause/resume serialization (JSON via kotlinx.serialization), HighScoreRepository, LeaderboardViewModel
**Addresses:** Pause and resume, per-difficulty high score table (FEATURES.md)
**Avoids:** Game state lost on process death (Pitfall 6 — write on every change using applicationScope), corrupt DataStore on relaunch
**Research flag:** Standard patterns — DataStore + kotlinx.serialization is well-documented; skip research-phase

### Phase 5: Scoring and Completion
**Rationale:** Depends on Phases 2 and 4. Error counting and hint logic live in GameViewModel (Phase 2); score write to leaderboard requires HighScoreRepository (Phase 4). Post-game error reveal and score computation are the culmination of the silent tracking design.
**Delivers:** Error counting in GameViewModel, hint logic (cell-reveal), post-game error reveal, score computation (with floor of 0), SummaryScreen, LeaderboardScreen, completion navigation
**Addresses:** Silent error tracking, completion summary, error-based score, hint with penalty, per-difficulty high score (FEATURES.md)
**Avoids:** Negative score (Pitfall 5 — max(0, score) floor), duplicate leaderboard entries (Pitfall 10), hint on already-correct cell (Pitfall 11)
**Research flag:** Standard patterns — scoring formula is a product decision, not a technical unknown; skip research-phase

### Phase 6: Menu and Navigation
**Rationale:** Navigation is easiest to assemble once all destination screens exist. MenuViewModel reads persisted in-progress game flag (Phase 4) to show the Resume option.
**Delivers:** Compose Navigation graph, MenuScreen (difficulty picker, resume option), MenuViewModel, full app flow (Menu → Game → Summary → Leaderboard)
**Addresses:** Graceful handling of incomplete puzzles on relaunch (FEATURES.md differentiator)
**Avoids:** No pitfalls introduced at this phase beyond standard navigation patterns
**Research flag:** Standard patterns — Compose Navigation is well-documented; skip research-phase

### Phase Ordering Rationale

- Puzzle Engine first because it has no dependencies and carries the highest domain risk; a broken uniqueness guarantee or miscalibrated difficulty discovered in Phase 5 would require rewriting Phase 1 work
- Game state before UI because the ViewModel contract defines the UI's data contract; inverting this causes interface rework
- Core UI before persistence because on-device E-ink rendering validation is the second-highest risk and should be confronted early, not after the full persistence layer is built
- Persistence before scoring because the completion flow requires an atomic leaderboard write; wiring scoring without a working repository is a dead end
- Menu last because it is pure assembly of already-built screens; all navigation targets must exist first

### Research Flags

Phases needing deeper research during planning:
- **Phase 1 (Puzzle Engine):** Difficulty classification by required solving technique requires algorithm research. The Sudoklify library handles this if it proves reliable; if not, a custom classifier needs a documented algorithm (e.g., dlbeer's constraint-propagation approach). Research the Sudoklify API before committing to the implementation plan.
- **Phase 3 (Core Game UI):** E-ink rendering behavior on the Mudita Kompakt cannot be fully characterized without a physical device. Touch target sizing, ghosting threshold (how many partial refreshes before full-panel flash is needed), and the MMD waveform mode API (if it exists) all need hands-on validation.

Phases with standard patterns (skip research-phase):
- **Phase 2 (Game State / ViewModel):** MVVM + StateFlow + immutable data classes is the official Google-recommended pattern with extensive documentation
- **Phase 4 (Persistence):** DataStore Preferences + kotlinx.serialization JSON is a well-trodden path with official documentation
- **Phase 5 (Scoring / Completion):** No novel algorithms; standard ViewModel state transitions and repository writes
- **Phase 6 (Menu / Navigation):** Compose Navigation graph is well-documented; no novel patterns required

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | MEDIUM | Core Android stack (Compose BOM, AGP, DataStore) confirmed HIGH via official docs. MMD 1.0.1 confirmed via GitHub but internal build requirements (minSdk, compileSdk) not extractable. Sudoklify confirmed MEDIUM — beta status, no production usage data. API 31 target confirmed MEDIUM — AOSP 12 = API 31 well-established but Mudita has not published a formal developer spec. |
| Features | HIGH | Core Sudoku feature landscape is well-documented across academic papers (difficulty classification), production apps (LibreSudoku, Good Sudoku), and official sources. E-ink-specific UX constraints are MEDIUM — community reports and forum posts, not first-party documentation. |
| Architecture | HIGH | MVVM + StateFlow + Compose is the official Google-recommended pattern with extensive documentation. MMD integration pattern confirmed MEDIUM via CalmDirectory reference app. Data class designs are standard Kotlin patterns. |
| Pitfalls | HIGH | E-ink ghosting and recomposition pitfalls confirmed by practitioner reports and official Compose performance docs. Puzzle uniqueness pitfall is mathematical fact. Touch target sizing from Mudita Forum developer report (MEDIUM — single source). |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- **MMD waveform API:** It is unknown whether MMD exposes a programmatic full-panel refresh trigger. If it does not, the app cannot control the ghosting threshold beyond using solid backgrounds and no animations. Investigate the MMD GitHub source during Phase 3.
- **Sudoklify difficulty API fidelity:** The library claims technique-based difficulty levels, but this has not been verified against actual puzzle samples. Validate in Phase 1 by generating 20+ puzzles per difficulty level and hand-solving or running a technique classifier against them.
- **Kompakt display density and available layout area:** The device is 800x480 px at ~216 ppi, but the MMD navigation bar's padding (~20px overhead noted by one developer) reduces available vertical space. Measure actual usable area on the physical device in Phase 3 before finalising the grid-plus-numberpad layout.
- **targetSdk 31 behavioral implications:** Staying at targetSdk 31 avoids Android 13+ permission behavior changes, which is correct for this device. However, if Mudita ships an OS update to a higher API level, this should be revisited. Flag for post-launch review.

---

## Sources

### Primary (HIGH confidence)
- [Android Developers — Compose BOM Mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) — BOM 2026.03.00 library versions
- [Android Developers — AGP Release Notes](https://developer.android.com/build/releases/about-agp) — AGP 8.9.0 / 9.x versions and Gradle requirements
- [Android Developers — DataStore](https://developer.android.com/topic/libraries/architecture/datastore) — DataStore 1.2.1, corruption handler, kotlinx.serialization integration
- [Android Developers — Compose Architecture](https://developer.android.com/develop/ui/compose/architecture) — MVVM + UDF patterns
- [Android Developers — Compose Performance Best Practices](https://developer.android.com/develop/ui/compose/performance/bestpractices) — recomposition stability, derivedStateOf
- [Kotlin Blog — Kotlin 2.3.20 Released](https://blog.jetbrains.com/kotlin/2026/03/kotlin-2-3-20-released/) — latest stable Kotlin version
- [Mathematics of Sudoku — Wikipedia](https://en.wikipedia.org/wiki/Mathematics_of_Sudoku) — 17-clue minimum, unique solution requirement
- [Generating difficult Sudoku puzzles quickly — dlbeer.co.nz](https://dlbeer.co.nz/articles/sudoku.html) — technique-based difficulty generation algorithm
- [Sudoku Puzzles Generating: from Easy to Evil (academic paper)](https://zhangroup.aporc.org/images/files/Paper_3485.pdf) — difficulty classification by required techniques
- [Mudita Kompakt sideloading — official Mudita Forum](https://forum.mudita.com/t/sideloading-apps-on-mudita-kompakt-what-you-need-to-know/7178) — AOSP constraints, no GMS

### Secondary (MEDIUM confidence)
- [Mudita MMD GitHub Repository](https://github.com/mudita/MMD) — MMD version 1.0.1, component names, design philosophy
- [CalmDirectory MMD reference app](https://github.com/davidraywilson/CalmDirectory) — real MMD integration (ButtonMMD, TopAppBarMMD, SwitchMMD confirmed)
- [Sudoklify GitHub (teogor)](https://github.com/teogor/sudoklify) — version 1.0.0-beta04, Maven coordinates, difficulty API
- [LibreSudoku GitHub](https://github.com/kaajjo/LibreSudoku) — QQWing fallback reference, production Compose Sudoku app
- [Debugging Compose in Sudoku app — christopherward.medium.com](https://christopherward.medium.com/debugging-and-fixing-a-huge-jetpack-compose-performance-problem-in-my-sudoku-solver-app-8f67fa229dc2) — recomposition pitfall (directly applicable practitioner report)
- [NotebookCheck — Mudita Kompakt](https://www.notebookcheck.net/Mudita-Kompakt-E-Ink-Phone-A-minimalist-privacy-focused-phone-powered-by-Android.911587.0.html) — hardware specs, AOSP 12 confirmation
- [Mudita Forum — one week as a developer](https://forum.mudita.com/t/one-week-with-the-mudita-kompakt-as-a-software-developer/8518) — MMD navigation bar padding, touch target sizing context
- [Good Sudoku press kit](https://www.playgoodsudoku.com/presskit/) — mindful design, teaching hints as differentiator
- [BOOX E-ink ghosting guide](https://onyxboox.medium.com/how-to-optimize-ghosting-on-color-e-ink-screen-fa0b9b77a171) — ghosting prevention patterns
- [AnkiDroid E-ink ghosting issue #16117](https://github.com/ankidroid/Anki-Android/issues/16117) — community-confirmed E-ink rendering failure mode

### Tertiary (LOW confidence / needs validation)
- Sudoklify difficulty calibration — beta library, technique classification not independently verified against puzzle samples; validate in Phase 1
- MMD waveform refresh API existence — referenced in community discussion but not confirmed in library source; investigate in Phase 3

---
*Research completed: 2026-03-23*
*Ready for roadmap: yes*
