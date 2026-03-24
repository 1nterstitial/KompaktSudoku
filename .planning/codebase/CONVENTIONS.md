# Code Conventions

**Analysis Date:** 2026-03-24

## Naming

**Classes:**
- `PascalCase` throughout. Engine classes are concrete service objects with noun names: `DifficultyClassifier`, `SudokuGenerator`, `UniquenessVerifier`, `SudokuValidator`.
- Exception classes end with `Exception`: `PuzzleGenerationException`.
- Data-holder classes use plain nouns: `SudokuPuzzle`, `DifficultyConfig`.

**Enums:**
- Enum type names are `PascalCase` singular nouns: `Difficulty`, `TechniqueTier`.
- Enum constants are `SCREAMING_SNAKE_CASE`: `EASY`, `MEDIUM`, `HARD`, `NAKED_SINGLES_ONLY`, `HIDDEN_PAIRS`, `ADVANCED`.

**Functions:**
- `camelCase` for all function names.
- Boolean-returning functions use verb phrases that read as questions: `hasUniqueSolution`, `isValidPlacement`, `meetsRequirements`.
- Action functions use verb phrases: `generatePuzzle`, `classifyTechniqueTier`, `classifyBoard`, `solveWithNakedSingles`.

**Properties and variables:**
- `camelCase` for all local variables and class properties.
- `val` strongly preferred over `var`; `var` only when mutation is required (e.g., `progress`, `count`, loop boards).
- Boolean flags use `is`/`has` prefix where the name is a noun: `progress` is an exception (it's a flag tracking loop state).

**Top-level constants:**
- `SCREAMING_SNAKE_CASE` for package-level `val` constants: `EASY_CONFIG`, `MEDIUM_CONFIG`, `HARD_CONFIG`.

**Parameters:**
- Descriptive single-word or short camelCase names: `board`, `index`, `digit`, `difficulty`, `config`, `puzzle`.
- Injected collaborators named after their type (lowercased): `verifier`, `classifier`.

## Kotlin Idioms

**`data class` for value objects:**
- Used for `SudokuPuzzle` and `DifficultyConfig`. Derived properties (`givenCount`) are given defaults in the primary constructor rather than being computed in the body.
- `IntArray` fields inside `data class` require manual `equals`/`hashCode` override (Kotlin's structural equality does not apply to arrays). Pattern seen in `SudokuPuzzle`:
  ```kotlin
  override fun equals(other: Any?): Boolean { ... board.contentEquals(other.board) ... }
  override fun hashCode(): Int { var result = board.contentHashCode(); result = 31 * result + ...; return result }
  ```

**`init` blocks for invariant enforcement:**
- `require(condition) { "message with ${value}" }` is the standard precondition check inside `init` blocks. Used in `SudokuPuzzle` to validate board/solution sizes and givenCount range.

**`enum class` for sealed finite sets:**
- Simple enums without companion objects or properties: `Difficulty`, `TechniqueTier`.
- `when` exhaustive matching used with enums (e.g., `difficultyConfigFor`, `pickSudoklifyDifficulty`).

**Top-level functions:**
- Pure utility functions that belong to the package but not a class are defined at the top level: `isValidPlacement` in `SudokuValidator.kt`, `difficultyConfigFor` in `DifficultyConfig.kt`.
- A thin wrapper class (`SudokuValidator`) delegates to the top-level function where a class instance is needed for injection.

**`repeat(n)` for counted loops:**
- Used in `generatePuzzle` (`repeat(maxAttempts)`) in preference to `for (i in 0 until n)` when the index is not needed.

**`run {}` for early-exit inside lambdas:**
- `return@repeat` used inside `repeat` lambdas to skip an iteration (equivalent to `continue`).

**`copyOf()` for defensive array copies:**
- Internal methods that may mutate a board always receive `board.copyOf()` to avoid side effects on the caller's data.

**Companion import aliasing:**
- External enum clashes resolved with `import ... as`: `import dev.teogor.sudoklify.components.Difficulty as SudoklifyDifficulty`.

**`@OptIn` for experimental APIs:**
- Applied at the class level: `@OptIn(ExperimentalSudoklifyApi::class)` on `SudokuGenerator`.

**`open` for test-double extensibility:**
- Classes and methods marked `open` specifically to enable anonymous subclass overrides in tests. Comment explains intent: `// Declared open to allow test doubles in SudokuGeneratorTest.`

**`internal` for package-visible testing:**
- Methods that should not form the public API but must be tested are marked `internal`: `classifyBoard`, `solveWithNakedSingles`, `solveWithHiddenSingles`, `countSolutions`.

## Compose Patterns

No Composable functions exist yet. `MainActivity` extends `Activity` (not `ComponentActivity`) and has a stub `onCreate`. Compose is declared in the build but not yet used in source.

When Composables are added, the declared stack requires:
- `ButtonMMD` and `TextMMD` from the MMD library instead of plain Compose `Button`/`Text`.
- No ripple overrides (E-ink constraint).
- `collectAsStateWithLifecycle` for StateFlow observation.

## Domain Model Patterns

**Package layout:**
- `puzzle/model/` — pure data types and configuration: `Difficulty`, `TechniqueTier`, `DifficultyConfig`, `SudokuPuzzle`.
- `puzzle/engine/` — stateless services that operate on domain models: `DifficultyClassifier`, `SudokuGenerator`, `UniquenessVerifier`, `SudokuValidator`.
- `puzzle/` — exceptions that cross engine and model: `PuzzleGenerationException`.

**Board representation:**
- All boards are flat `IntArray(81)`. Cell at (row, col) is at `index = row * 9 + col`. Empty cells are `0`, given/placed digits are `1–9`. This convention is enforced uniformly across all engine classes and documented in KDoc on every function that accepts a board.

**Config objects as data:**
- `DifficultyConfig` is a `data class` used as a value object. The three instances (`EASY_CONFIG`, `MEDIUM_CONFIG`, `HARD_CONFIG`) are package-level `val` constants, not a map or factory. New difficulties would require adding a new constant and a `when` branch in `difficultyConfigFor`.

**Injection via constructor parameters with defaults:**
- Engine classes accept collaborators as constructor parameters with sensible defaults: `SudokuGenerator(verifier = UniquenessVerifier(), classifier = DifficultyClassifier(), maxAttempts = 50)`. This enables testing without a DI framework.

## Error Handling

**Custom exception class:**
- `PuzzleGenerationException` extends `Exception` directly with a single `message: String` constructor. No cause wrapping.
- Thrown only at the boundary where a generation loop exhausts all attempts. Internal `tryGenerateCandidate` swallows all exceptions from Sudoklify by returning `null` (retry semantics):
  ```kotlin
  } catch (e: Exception) {
      null  // Retry: generation may occasionally produce unusable results
  }
  ```

**`require()` for constructor preconditions:**
- Invariant violations in domain models throw `IllegalArgumentException` via `require`. Messages include the bad value: `"board must have exactly 81 cells, got ${board.size}"`.

**No `Result` type or sealed error hierarchy yet:**
- Error propagation is exception-based. No `Result<T>` or `sealed class Either` pattern is present.

## Comments

**KDoc on all public and internal functions:**
- Every public function has a KDoc block with `@param` and `@return` tags where the name alone would be ambiguous.
- Class-level KDoc explains the overall algorithm and design decisions (e.g., which techniques are excluded and why).

**Inline comments for non-obvious code:**
- Arithmetic index calculations have end-of-line comments: `// row conflict`, `// column conflict`, `// box conflict`.
- Decision comments explain "why not" alternatives: `// Retry: generation may occasionally produce unusable results`.

**Requirement traceability:**
- PUZZ-01, PUZZ-02, PUZZ-03 requirement IDs appear in gate comments inside `generatePuzzle` and in test comments.

---

*Convention analysis: 2026-03-24*
