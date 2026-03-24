# Phase 1: Puzzle Engine - Research

**Researched:** 2026-03-23
**Domain:** Sudoku puzzle generation, uniqueness verification, technique-based difficulty classification (pure Kotlin)
**Confidence:** MEDIUM — Core algorithms are well-understood (HIGH); Sudoklify difficulty API fidelity is unverified at source-code level (LOW/MEDIUM); fallback path via QQWing is confirmed viable (MEDIUM)

---

## Summary

Phase 1 implements the pure Kotlin puzzle engine with no Android or Compose dependencies. Its sole job is to produce valid, unique Sudoku puzzles at three calibrated difficulty levels. This phase is the highest-risk foundation: if uniqueness or difficulty calibration is wrong, every subsequent phase inherits a broken game.

The core technical challenge is that "difficulty" in the requirements is technique-based (Easy = naked singles only, Medium = hidden pairs/pencil marks, Hard = X-wing/chains), not simply a given-cell-count threshold. This is significantly harder to implement than a count-based difficulty system. The primary library candidate, Sudoklify (1.0.0-beta04), advertises difficulty levels but its internal classification mechanism could not be verified at source-code level — the library uses a seed-based schema selection approach rather than a live solving-technique classifier. Whether Sudoklify's "Easy/Medium/Hard" maps to the project's technique-based definitions must be empirically validated in Wave 0 by generating batches and running a classifier against the output.

Uniqueness verification is non-negotiable and must use an "abort on second solution" solver pattern — the standard backtracking solver that stops at first solution cannot detect ambiguity.

**Primary recommendation:** Use Sudoklify for generation scaffolding; wrap its output with an independent uniqueness verifier and technique classifier written in pure Kotlin. If Sudoklify's difficulty output does not pass empirical validation, replace its difficulty parameter with a custom technique-gated acceptance loop using the same generation backend.

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PUZZ-01 | App generates valid Sudoku puzzles that have exactly one solution | Uniqueness verifier pattern: abort-on-second-solution backtracking solver; confirmed as standard practice |
| PUZZ-02 | Difficulty classified by solving technique: Easy=naked singles; Medium=hidden pairs/pencil marks; Hard=X-wing/chains | Technique classification requires a constraint-propagation solver with technique tracking; Sudoklify's fidelity to this is unverified — empirical validation required in Wave 0 |
| PUZZ-03 | Easy: 36–45 given cells; Medium: 27–35; Hard: 22–27 | Cell counts must be verified per-generation; Sudoklify's exact output ranges are undocumented |
</phase_requirements>

---

## Standard Stack

### Core (Phase 1 scope — pure Kotlin, no Android deps)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Sudoklify (dev.teogor.sudoklify:sudoklify-core) | 1.0.0-beta04 | Puzzle generation with difficulty presets | Only actively maintained Kotlin-native Sudoku generation library; seed-based; KMP-compatible pure Kotlin |
| kotlin-stdlib | 2.3.20 (bundled) | Core language + collections | No extra dependency needed |
| JUnit 4 | 4.13.2 | Unit test runner | Standard for this Android project (Compose test rule requires JUnit 4) |
| kotlinx-coroutines-test | 1.10.2 | Test dispatcher utilities | If any generation is wrapped in coroutines for timing |

### Fallback (if Sudoklify difficulty fails validation)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| QQWing (via LibreSudoku port) | Java source, manually ported | Puzzle generation + technique-based difficulty classification | If Sudoklify's `Difficulty.EASY/MEDIUM/HARD` do not produce technique-appropriate puzzles; QQWing classifies by techniques actually used in its solver |

**Installation (primary):**
```kotlin
// gradle/libs.versions.toml — already present from project STACK.md
sudoklify-core = { group = "dev.teogor.sudoklify", name = "sudoklify-core", version.ref = "sudoklify" }

// build.gradle.kts (app)
implementation(libs.sudoklify.core)
```

**Installation (test):**
```kotlin
testImplementation(libs.junit)
testImplementation(libs.coroutines.test)
```

**Alternatives Considered:**

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Sudoklify | Custom backtracking generator | Custom gives full control but requires ~500 LOC of domain logic to achieve proper technique-gated difficulty. Sudoklify saves that work if its difficulty output is acceptable. |
| Sudoklify | QQWing Java port | QQWing has documented technique-based classification but requires Java-to-Kotlin porting effort. LibreSudoku (production app) already did this — extractable as reference. |

---

## Architecture Patterns

### Recommended Structure (Phase 1 scope)

```
domain/
├── engine/
│   ├── SudokuGenerator.kt         # Wraps Sudoklify; returns SudokuPuzzle
│   ├── SudokuSolver.kt            # Backtracking solver; used for uniqueness check + hints
│   ├── UniquenessVerifier.kt      # Runs abort-on-second-solution check on any puzzle
│   ├── DifficultyClassifier.kt    # Technique classifier; validates output matches requested level
│   ├── DifficultyConfig.kt        # Data class: technique tier + cell count range per Difficulty
│   └── SudokuValidator.kt         # Row/col/box constraint checker (used in solver)
```

No Android imports. No ViewModel. No DataStore. Pure Kotlin objects only, testable with plain JUnit on the JVM.

### Pattern 1: Sudoklify Generation Wrapper

**What:** Call Sudoklify via `SudoklifyArchitect.constructSudoku(spec)`, then run the output through `UniquenessVerifier` and `DifficultyClassifier`. Reject and regenerate if either check fails.

**When to use:** Always. Sudoklify handles the combinatorial generation; the wrappers enforce the project's invariants.

```kotlin
// Source: Sudoklify README + project research
val spec = SudokuSpec {
    seed = Random.nextLong().toSeed()
    type = Dimension.NineByNine
    difficulty = when (difficulty) {
        Difficulty.EASY   -> dev.teogor.sudoklify.Difficulty.EASY
        Difficulty.MEDIUM -> dev.teogor.sudoklify.Difficulty.MEDIUM
        Difficulty.HARD   -> dev.teogor.sudoklify.Difficulty.HARD
    }
}
val rawPuzzle = SudoklifyArchitect(sudokuSchemas) { }.constructSudoku(spec)
// rawPuzzle.generateGridWithGivens() returns the board with 0s for empty cells
```

**Note:** `SudoklifyArchitect` requires a `SudokuSchemas` instance. The `sudoklify-presets` module provides pre-built schemas — use `AllSudokuSchemas` or the 9x9 preset rather than building schemas manually.

### Pattern 2: Abort-on-Second-Solution Uniqueness Verifier

**What:** A backtracking solver modified to count solutions up to 2. If count reaches 2, abort. Only accept puzzles where count == 1.

**When to use:** Mandatory after every puzzle generation. Never skip.

```kotlin
// Source: dlbeer.co.nz/articles/sudoku.html — verified algorithmic pattern
class UniquenessVerifier {
    fun hasUniqueSolution(board: IntArray): Boolean {
        var solutionCount = 0
        solve(board.copyOf()) { solutionCount++ }
        return solutionCount == 1
    }

    private fun solve(board: IntArray, onSolution: () -> Unit) {
        val emptyIndex = board.indexOf(0)
        if (emptyIndex == -1) {
            onSolution()
            return
        }
        // CRITICAL: stop after finding 2 solutions — never search the full tree needlessly
        var count = 0
        for (digit in 1..9) {
            if (isValid(board, emptyIndex, digit)) {
                board[emptyIndex] = digit
                solve(board, onSolution)
                board[emptyIndex] = 0
                if (++count >= 2) return  // abort on second solution found
            }
        }
    }
}
```

### Pattern 3: Technique-Based Difficulty Classifier

**What:** A constraint-propagation solver that tracks which techniques it needed to apply. Compares the required technique set against the expected set for the requested difficulty tier.

**When to use:** After generation, to gate whether the puzzle is accepted for the requested difficulty.

**Technique tiers (from requirements PUZZ-02):**

| Difficulty | Required Techniques | Forbidden "Easy" Bypass |
|------------|--------------------|-----------------------|
| Easy | Naked singles only | No hidden singles, no pairs |
| Medium | Hidden singles, naked/hidden pairs | No X-wing, no chains |
| Hard | X-wing, chains required | Must not be solvable by Medium techniques |

**Implementation approach:**

1. Implement a human-style constraint propagation solver with technique counters
2. Easy: attempt to solve using only naked-single elimination. If board solves completely, it qualifies
3. Medium: run naked singles first; if stuck, try hidden singles and naked pairs. If solves, qualifies. If still stuck, reject (too hard)
4. Hard: solve must require at least one X-wing or chain step after simpler techniques are exhausted

**Performance note:** A constraint solver with technique tracking is more expensive than a pure backtracking solver. For Hard puzzles, limit attempts: generate up to N candidates and accept the first that passes; log average attempt count during development.

### Pattern 4: DifficultyConfig Data Class

**What:** Encapsulates the cell-count range and technique tier for each difficulty level.

```kotlin
// Source: Project REQUIREMENTS.md + ARCHITECTURE.md research
data class DifficultyConfig(
    val difficulty: Difficulty,
    val minGivens: Int,
    val maxGivens: Int,
    val requiredTechniqueTier: TechniqueTier
)

enum class TechniqueTier { NAKED_SINGLES_ONLY, HIDDEN_PAIRS, ADVANCED }

val EASY_CONFIG   = DifficultyConfig(Difficulty.EASY,   36, 45, TechniqueTier.NAKED_SINGLES_ONLY)
val MEDIUM_CONFIG = DifficultyConfig(Difficulty.MEDIUM, 27, 35, TechniqueTier.HIDDEN_PAIRS)
val HARD_CONFIG   = DifficultyConfig(Difficulty.HARD,   22, 27, TechniqueTier.ADVANCED)
```

### Anti-Patterns to Avoid

- **Difficulty = givens count only:** Never accept a puzzle purely because it has 27 cells given. It must also pass technique classification. (Pitfall 7 from PITFALLS.md)
- **Stopping solver at first solution:** A solver that returns on finding solution #1 cannot detect multi-solution puzzles. Always count to 2 with abort. (Pitfall 3 from PITFALLS.md)
- **Generating without a retry loop:** Generation may fail technique classification multiple times before finding a valid puzzle. The generator must have a retry loop with a reasonable attempt limit (e.g., 50 attempts) and surface an error if the limit is exceeded.
- **Holding Android context in the engine:** `SudokuGenerator` must not import any `android.*` or Compose class. This makes JVM-side unit tests possible and keeps Phase 1 independently verifiable.
- **Storing solution separately from the board:** The solution grid must travel with the puzzle board in the domain model, not held only in generator memory. (Anti-Pattern 4 from ARCHITECTURE.md)

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Puzzle combinatorial generation | Custom grid filler from scratch | Sudoklify core | 2.4 trillion+ seed-based variations, tested KMP library; custom backtracking is ~300 LOC minimum with subtle bugs |
| Unique solution guarantee | Manual "check looks right" | Abort-on-second-solution solver | Multi-solution puzzles are the single most-cited integrity failure in Sudoku game reviews; must be algorithmic |
| Technique detection for Medium/Hard | Heuristic "enough cells removed" | Constraint-propagation solver with technique counters | Cell count correlates weakly with technique requirement; 26-clue puzzles can be trivial naked-singles if cells are distributed favorably |

**Key insight:** The uniqueness verifier and technique classifier are the two pieces the project must own — they encode the project's contract with the player. Everything else can be delegated to Sudoklify.

---

## Common Pitfalls

### Pitfall 1: Sudoklify Difficulty Does Not Match Technique Criteria
**What goes wrong:** Sudoklify's `Difficulty.EASY` generates a puzzle that requires hidden pairs to solve. The app ships with "Easy" puzzles that feel Medium. Player trust is broken.
**Why it happens:** Sudoklify uses a schema/seed selection model for difficulty, not a live technique-classification loop. Its internal definition of "Easy" may differ from PUZZ-02's technique-based definition.
**How to avoid:** In Wave 0 validation, generate 20+ puzzles per difficulty level and run the technique classifier against each. If pass rate < 95%, do not use Sudoklify's difficulty parameter — use it only for generation and apply a custom technique gate on the output.
**Warning signs:** Generated "Easy" puzzles require pencil marks. Generated "Hard" puzzles are solvable by naked singles alone.

### Pitfall 2: Abort-on-Second-Solution Pattern Not Implemented Correctly
**What goes wrong:** The verifier finds one solution and returns `true`. Multi-solution puzzles reach the player.
**Why it happens:** The solver was built with "find first solution" termination, not "count to 2 and abort." A single-line logic error.
**How to avoid:** Unit test with known multi-solution puzzles (e.g., an empty board minus 16 cells has many solutions). Assert the verifier returns `false`.
**Warning signs:** Verification always returns `true` regardless of input.

### Pitfall 3: Generation Timeout on Hard Puzzles
**What goes wrong:** Hard puzzle generation takes 5–10 seconds on the Helio A22 because the technique classifier rejects many candidates before finding one that requires X-wing.
**Why it happens:** Hard technique requirements are rare in the solution space. The generator-classifier loop may run dozens of iterations.
**How to avoid:** Pre-profile the iteration count during development (log attempts per success). If > 30 attempts on average, consider a pool-seeded approach: pre-generate 10 Hard puzzles at first launch and cache them, refreshing in the background. Generation must complete in < 2 seconds per PUZZ-03 success criteria.
**Warning signs:** `generate(HARD)` takes > 2 seconds on a physical Kompakt or emulated Helio A22.

### Pitfall 4: Given-Count Range Not Enforced Post-Generation
**What goes wrong:** Sudoklify removes cells based on its own difficulty spec, which may produce 30 givens for "Hard" instead of the required 22–27.
**Why it happens:** Sudoklify's difficulty-to-cell-count mapping is not publicly documented; it may not align with PUZZ-03.
**How to avoid:** After generation, assert `givenCount in config.minGivens..config.maxGivens`. If out of range, regenerate. Log the distribution during Wave 0 validation.
**Warning signs:** Consistently generating Hard puzzles with 30+ givens.

### Pitfall 5: Minimum Clue Bound Violated
**What goes wrong:** Generator produces a 16-clue puzzle (mathematically impossible to have a unique solution). Uniqueness verifier will fail, causing an infinite retry loop.
**Why it happens:** If the generation approach removes cells without bounding to a minimum, it can fall below 17 givens — the proven minimum for a uniquely solvable 9×9 Sudoku.
**How to avoid:** Hard lower bound: never generate puzzles with fewer than 17 givens. The project's Hard minimum is 22, which safely exceeds this.
**Warning signs:** Uniqueness verifier always returns `false` for Hard puzzles below 22 givens.

---

## Code Examples

### SudoklifyArchitect Usage (verified from README)
```kotlin
// Source: github.com/teogor/sudoklify README
val sudokuSpec = SudokuSpec {
    seed = 2024L.toSeed()
    type = Dimension.NineByNine
    difficulty = Difficulty.EASY
}
// SudoklifyArchitect requires SudokuSchemas — use sudoklify-presets module
val puzzle = SudoklifyArchitect(schemas) { }.constructSudoku(sudokuSpec)
val boardWithGivens: Array<IntArray> = puzzle.generateGridWithGivens()
```

### Uniqueness Verifier Core Loop (verified algorithm from dlbeer.co.nz)
```kotlin
// Source: dlbeer.co.nz/articles/sudoku.html — abort-on-second-solution pattern
fun countSolutions(board: IntArray, limit: Int = 2): Int {
    val idx = board.indexOf(0)
    if (idx == -1) return 1  // complete solution found
    var count = 0
    for (d in 1..9) {
        if (isValidPlacement(board, idx, d)) {
            board[idx] = d
            count += countSolutions(board, limit)
            board[idx] = 0
            if (count >= limit) return count  // abort early
        }
    }
    return count
}

fun hasUniqueSolution(puzzle: IntArray): Boolean = countSolutions(puzzle.copyOf()) == 1
```

### Constraint Validator (row/col/box)
```kotlin
// Source: standard Sudoku constraint logic — HIGH confidence (mathematical)
fun isValidPlacement(board: IntArray, index: Int, digit: Int): Boolean {
    val row = index / 9
    val col = index % 9
    val boxRow = (row / 3) * 3
    val boxCol = (col / 3) * 3
    for (i in 0..8) {
        if (board[row * 9 + i] == digit) return false       // row check
        if (board[i * 9 + col] == digit) return false       // col check
        val br = boxRow + i / 3
        val bc = boxCol + i % 3
        if (board[br * 9 + bc] == digit) return false       // box check
    }
    return true
}
```

### Generation Retry Loop
```kotlin
// Pattern: retry with attempt limit
fun generatePuzzle(difficulty: Difficulty, maxAttempts: Int = 50): SudokuPuzzle {
    val config = difficultyConfigFor(difficulty)
    repeat(maxAttempts) {
        val candidate = sudoklifyGenerator.generate(difficulty)
        if (!uniquenessVerifier.hasUniqueSolution(candidate.board)) return@repeat
        if (candidate.givenCount !in config.minGivens..config.maxGivens) return@repeat
        if (!techniqueClassifier.meetsRequirements(candidate, config.requiredTechniqueTier)) return@repeat
        return candidate
    }
    throw PuzzleGenerationException("Failed after $maxAttempts attempts for $difficulty")
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Difficulty = clue count only | Difficulty = technique required to solve | ~2015 in serious apps | Meaningful difficulty tiers; player expectation met |
| Single-solution assumption | Abort-on-second-solution verification | Standard since 2010s | Non-negotiable correctness requirement |
| Static puzzle bank | On-device seed-based generation | Post-2020 (Sudoklify model) | Unlimited variety without storage; eliminates "seen this before" problem |
| JNI-wrapped C solver (Android) | Pure Kotlin KMP solver | 2022+ with KMP maturity | No JNI overhead, JVM-testable, simpler build |

**Deprecated/outdated:**
- Using SharedPreferences or Bundle to pass the puzzle board: use domain data classes with the solution embedded from generation time
- Using `AsyncTask` for background generation: use `withContext(Dispatchers.Default)` coroutines

---

## Open Questions

1. **Sudoklify technique fidelity**
   - What we know: Sudoklify has `Difficulty.EASY/MEDIUM/HARD/EXPERT` and uses schema selection, not live technique classification
   - What's unclear: Whether its output satisfies "Easy = naked singles only, Medium = hidden pairs required, Hard = X-wing required" as specified in PUZZ-02
   - Recommendation: Wave 0 validation task — generate 20 puzzles per difficulty, run technique classifier, measure pass rate. If < 95%, use Sudoklify for generation scaffolding only, and apply custom technique gate

2. **Sudoklify given-count output ranges**
   - What we know: PUZZ-03 requires Easy=36–45, Medium=27–35, Hard=22–27
   - What's unclear: What cell counts Sudoklify's presets actually produce per difficulty
   - Recommendation: Log `givenCount` during Wave 0 validation; enforce range check regardless

3. **Generation performance on Helio A22**
   - What we know: dlbeer.co.nz reports ~596ms on a 1.66 GHz Atom N450 for standard generation. Helio A22 has 4x Cortex-A53 @ 2.0 GHz — likely comparable or slightly faster for sequential work
   - What's unclear: How many technique-classifier iterations are needed for Hard puzzles specifically
   - Recommendation: Measure on emulator or device during development; if Hard generation exceeds 2 seconds, implement background pre-generation pool

4. **Sudoklify `SudokuSchemas` setup requirement**
   - What we know: `SudoklifyArchitect` throws `EmptySudokuSchemasException` if schemas are empty; the `sudoklify-presets` module provides pre-built schemas
   - What's unclear: Whether the presets module dependency must be added separately or if it is bundled with `sudoklify-core`
   - Recommendation: Add `dev.teogor.sudoklify:sudoklify-presets:1.0.0-beta04` as a separate dependency; verify at project build time

---

## Environment Availability

Step 2.6: SKIPPED for this phase. Phase 1 is pure Kotlin logic with no external service dependencies. The only tool required is the Kotlin compiler + Gradle build system, which are verified as present in the Android Studio / command-line build environment when the project is set up. The `sudoklify-core` and `sudoklify-presets` dependencies are fetched from Maven Central at build time — no special tool installation required.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Maven Central (network) | Sudoklify dependency fetch | ✓ (assumed; standard Android dev) | — | Download JAR manually |
| JDK 17+ | Kotlin/Gradle build | Not checked (no JDK on this machine) | — | Install JDK 17 via Android Studio |
| Android Studio / IntelliJ | IDE for development | Not checked (no IDE CLI on this machine) | — | Use command-line Gradle |

**Note:** This is a planning machine, not the development machine. No build tools are expected here. The developer's machine with Android Studio is assumed to have JDK 17+, Gradle 8.11.1, and AGP 8.9.0 available per the project STACK.md.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (4.13.2) — pure JVM, no Android or Robolectric needed for Phase 1 |
| Config file | None — standard JUnit 4 with Gradle `test` task |
| Quick run command | `./gradlew :app:test --tests "*.engine.*" -x lint` |
| Full suite command | `./gradlew :app:test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PUZZ-01 | 20 generated puzzles all have exactly one solution | Unit (parameterized) | `./gradlew :app:test --tests "*.UniquenessVerifierTest"` | Wave 0 |
| PUZZ-01 | Known multi-solution board returns false from verifier | Unit | `./gradlew :app:test --tests "*.UniquenessVerifierTest"` | Wave 0 |
| PUZZ-02 | Easy puzzles solvable with naked singles only | Unit | `./gradlew :app:test --tests "*.DifficultyClassifierTest"` | Wave 0 |
| PUZZ-02 | Medium puzzles require hidden pairs/pencil marks | Unit | `./gradlew :app:test --tests "*.DifficultyClassifierTest"` | Wave 0 |
| PUZZ-02 | Hard puzzles require advanced techniques | Unit | `./gradlew :app:test --tests "*.DifficultyClassifierTest"` | Wave 0 |
| PUZZ-03 | Easy: given count in 36–45 range across 20 puzzles | Unit (statistical) | `./gradlew :app:test --tests "*.SudokuGeneratorTest"` | Wave 0 |
| PUZZ-03 | Medium: given count in 27–35 range across 20 puzzles | Unit (statistical) | `./gradlew :app:test --tests "*.SudokuGeneratorTest"` | Wave 0 |
| PUZZ-03 | Hard: given count in 22–27 range across 20 puzzles | Unit (statistical) | `./gradlew :app:test --tests "*.SudokuGeneratorTest"` | Wave 0 |
| PUZZ-01/02/03 | generation() completes in < 2 seconds for all difficulties | Unit (timing) | `./gradlew :app:test --tests "*.GenerationPerformanceTest"` | Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :app:test --tests "*.engine.*" -x lint`
- **Per wave merge:** `./gradlew :app:test`
- **Phase gate:** Full suite green (all PUZZ-01, PUZZ-02, PUZZ-03 tests pass) before phase sign-off

### Wave 0 Gaps
- [ ] `src/test/java/…/engine/UniquenessVerifierTest.kt` — covers PUZZ-01
- [ ] `src/test/java/…/engine/DifficultyClassifierTest.kt` — covers PUZZ-02
- [ ] `src/test/java/…/engine/SudokuGeneratorTest.kt` — covers PUZZ-03 cell count ranges + 20-puzzle batch
- [ ] `src/test/java/…/engine/GenerationPerformanceTest.kt` — covers < 2s timing requirement
- [ ] `src/test/java/…/engine/SudokuValidatorTest.kt` — covers row/col/box constraint logic

---

## Project Constraints (from CLAUDE.md)

| Constraint | Source | Impact on Phase 1 |
|------------|--------|-------------------|
| Kotlin + Jetpack Compose + MMD required | CLAUDE.md / PROJECT.md | Phase 1 is pure Kotlin — no Compose or MMD in engine code |
| MVVM + StateFlow architecture | CLAUDE.md | Phase 1 creates the domain layer; ViewModel wraps it in Phase 2 |
| Local only — no network, no backend | CLAUDE.md | Puzzle generation is fully on-device; no API calls |
| E-ink display: no animations, instant feedback | CLAUDE.md | Not applicable to Phase 1 (no UI) |
| GSD workflow enforcement | CLAUDE.md | Execute all changes via `/gsd:execute-phase` |
| All UI via MMD components | CLAUDE.md | Not applicable to Phase 1 (no UI) |

---

## Sources

### Primary (HIGH confidence)
- [dlbeer.co.nz — Generating Difficult Sudoku Puzzles Quickly](https://dlbeer.co.nz/articles/sudoku.html) — uniqueness verifier abort-on-second-solution pattern; generation algorithm; performance on Atom N450 (~596ms)
- [Mathematics of Sudoku — Wikipedia](https://en.wikipedia.org/wiki/Mathematics_of_Sudoku) — 17-clue minimum for unique solutions; mathematical facts
- Project PITFALLS.md — Pitfall 3 (multi-solution), Pitfall 7 (difficulty as clue count only): HIGH confidence, derived from multiple verified sources

### Secondary (MEDIUM confidence)
- [Sudoklify GitHub (teogor/sudoklify)](https://github.com/teogor/sudoklify) — SudokuSpec builder API, Difficulty enum values (EASY/MEDIUM/HARD/EXPERT), `generateGridWithGivens()` method, module structure (core/presets/solver), 1.0.0-beta04 release contents
- [QQWing GitHub (stephenostermiller/qqwing)](https://github.com/stephenostermiller/qqwing) — Difficulty enum (SIMPLE/EASY/INTERMEDIATE/EXPERT), technique-based classification approach
- [LibreSudoku GitHub (kaajjo)](https://github.com/kaajjo/LibreSudoku) — QQWing integration pattern via `QQWingController`, `GameDifficulty` enum, parallel generation threading model
- [SudokuPuzzles.net — Difficulty Levels Explained](https://www.sudokupuzzles.net/blog/sudoku-difficulty-levels-explained) — Technique tiers: Easy=naked+hidden singles, Medium=naked/hidden pairs, Hard=X-wing/pointing pairs

### Tertiary (LOW confidence — unverified at source level)
- Sudoklify internal difficulty-to-cell-count mapping — not documented; must be measured empirically
- Sudoklify technique classification fidelity — not verifiable from README or available source files; must be validated in Wave 0

## Metadata

**Confidence breakdown:**
- Standard stack: MEDIUM — Sudoklify version and API confirmed from README; internal difficulty semantics unverified
- Architecture: HIGH — Backtracking patterns, uniqueness verifier, constraint validator are standard CS; verified from authoritative sources
- Pitfalls: HIGH — Multi-solution bug and difficulty-as-count-only are verified failure modes from domain literature and production app research
- Technique classifier design: MEDIUM — Technique progression (naked singles → hidden pairs → X-wing) is well-established Sudoku theory; exact implementation approach is designed from first principles

**Research date:** 2026-03-23
**Valid until:** 2026-06-23 (stable domain; Sudoklify beta status means check for new releases before building)
