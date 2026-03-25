# Project Structure

## Directory Layout

```
D:\Development\Claude\
├── app/
│   ├── build.gradle.kts            # App module build config
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/mudita/sudoku/
│       │   │   ├── MainActivity.kt                  # Entry point (stub/shell)
│       │   │   └── puzzle/
│       │   │       ├── PuzzleGenerationException.kt # Domain exception
│       │   │       ├── engine/
│       │   │       │   ├── SudokuValidator.kt        # isValidPlacement() top-level fn
│       │   │       │   ├── UniquenessVerifier.kt     # Abort-on-second-solution solver
│       │   │       │   ├── DifficultyClassifier.kt   # Technique-tier classifier
│       │   │       │   └── SudokuGenerator.kt        # Sudoklify wrapper + 3-gate loop
│       │   │       └── model/
│       │   │           ├── Difficulty.kt             # EASY, MEDIUM, HARD enum
│       │   │           ├── DifficultyConfig.kt       # TechniqueTier, DifficultyConfig, configs
│       │   │           └── SudokuPuzzle.kt           # Immutable puzzle domain model
│       │   └── res/
│       │       └── values/
│       │           ├── strings.xml
│       │           └── themes.xml
│       ├── test/java/com/mudita/sudoku/puzzle/
│       │   ├── SudokuValidatorTest.kt         # 5 unit tests
│       │   ├── UniquenessVerifierTest.kt      # 4 unit tests
│       │   ├── DifficultyClassifierTest.kt    # 5 unit tests
│       │   ├── SudokuGeneratorTest.kt         # 6 unit tests
│       │   └── SudokuEngineIntegrationTest.kt # 8 integration tests (20-puzzle batch)
│       └── androidTest/                       # Empty — no Compose UI tests yet
├── gradle/
│   ├── libs.versions.toml          # Version catalog (all dependencies)
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts                # Root build config
├── settings.gradle.kts             # Module declarations
└── .planning/                      # GSD workflow artifacts (not shipped)
    ├── STATE.md                    # Current project position
    ├── ROADMAP.md                  # Phase definitions and success criteria
    ├── REQUIREMENTS.md             # All 26 v1 requirements
    ├── PROJECT.md                  # Project charter
    ├── codebase/                   # This codebase map
    ├── research/                   # Pre-project research artifacts
    └── phases/
        └── 01-puzzle-engine/       # Phase 1 plans, summaries, verification
```

## Package Organization

Root package: `com.mudita.sudoku`

| Sub-package | Contents | Layer |
|-------------|----------|-------|
| `com.mudita.sudoku` | `MainActivity` | Presentation (entry) |
| `com.mudita.sudoku.puzzle` | `PuzzleGenerationException` | Domain (exceptions) |
| `com.mudita.sudoku.puzzle.engine` | `SudokuValidator`, `UniquenessVerifier`, `DifficultyClassifier`, `SudokuGenerator` | Domain (logic) |
| `com.mudita.sudoku.puzzle.model` | `Difficulty`, `DifficultyConfig`, `SudokuPuzzle` | Domain (models) |

Planned packages (not yet created):
- `com.mudita.sudoku.game` — GameState, ViewModel (Phase 2)
- `com.mudita.sudoku.data` — DataStore repository (Phase 4)
- `com.mudita.sudoku.ui` — Composable screens (Phase 3+)
- `com.mudita.sudoku.ui.navigation` — Navigation graph (Phase 6)

## File Naming Conventions

- Kotlin source files: `PascalCase.kt`, match the primary class/object
- Test files: `{SourceFile}Test.kt` in mirrored package under `src/test/`
- Plan files: `{phase}-{plan-number}-PLAN.md` (e.g., `01-04-PLAN.md`)
- Summary files: `{phase}-{plan-number}-SUMMARY.md`
- Phase-level files: `{phase}-{TYPE}.md` (e.g., `01-VERIFICATION.md`, `01-RESEARCH.md`)

## Key Files

| File | Purpose |
|------|---------|
| `gradle/libs.versions.toml` | Single source of truth for all library versions |
| `app/build.gradle.kts` | Module dependencies, SDK levels, Kotlin compiler options |
| `app/src/main/java/.../puzzle/model/DifficultyConfig.kt` | TechniqueTier enum, DifficultyConfig, EASY/MEDIUM/HARD_CONFIG constants |
| `app/src/main/java/.../puzzle/engine/SudokuGenerator.kt` | Core puzzle generation: Sudoklify wrapper + 3-gate acceptance loop |
| `app/src/test/.../SudokuEngineIntegrationTest.kt` | Batch verification: 60 uniqueness checks, technique classification, timing proxies |
| `.planning/ROADMAP.md` | Phase sequence, goals, success criteria for all 6 phases |
| `.planning/STATE.md` | Current phase, completed plans, accumulated key decisions |
