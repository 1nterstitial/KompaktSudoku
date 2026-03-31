package com.interstitial.sudoku.puzzle.engine

import com.interstitial.sudoku.puzzle.PuzzleGenerationException
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.interstitial.sudoku.puzzle.model.SudokuPuzzle
import com.interstitial.sudoku.puzzle.model.difficultyConfigFor
import dev.teogor.sudoklify.ExperimentalSudoklifyApi
import dev.teogor.sudoklify.SudoklifyArchitect
import dev.teogor.sudoklify.components.Difficulty as SudoklifyDifficulty
import dev.teogor.sudoklify.components.Dimension
import dev.teogor.sudoklify.components.toSeed
import dev.teogor.sudoklify.presets.loadPresetSchemas
import dev.teogor.sudoklify.puzzle.generateGridWithGivens
import kotlin.random.Random

/**
 * Generates valid, uniquely-solvable Sudoku puzzles at a specified difficulty level.
 *
 * Uses Sudoklify for raw puzzle generation, then gates acceptance with:
 *   1. [UniquenessVerifier] — ensures exactly one solution (PUZZ-01)
 *   2. Given-count range check per [DifficultyConfig] (PUZZ-03)
 *   3. [DifficultyClassifier] — ensures the correct technique tier (PUZZ-02)
 *
 * @param verifier Uniqueness verifier (injected for testability).
 * @param classifier Technique-tier classifier (injected for testability).
 * @param maxAttempts Maximum attempts per generate call before throwing [PuzzleGenerationException].
 */
@OptIn(ExperimentalSudoklifyApi::class)
class SudokuGenerator(
    private val verifier: UniquenessVerifier = UniquenessVerifier(),
    private val classifier: DifficultyClassifier = DifficultyClassifier(),
    private val maxAttempts: Int = 50
) {

    /**
     * Generates a [SudokuPuzzle] at the requested [difficulty].
     *
     * @throws [PuzzleGenerationException] if no valid puzzle is found within [maxAttempts] attempts.
     */
    fun generatePuzzle(difficulty: Difficulty): SudokuPuzzle {
        val config = difficultyConfigFor(difficulty)
        var lastRejectionReason = "unknown"

        repeat(maxAttempts) {
            val candidate = tryGenerateCandidate(difficulty)
                ?: run { lastRejectionReason = "Sudoklify generation returned null"; return@repeat }

            // Gate 1: uniqueness (PUZZ-01)
            if (!verifier.hasUniqueSolution(candidate.board)) {
                lastRejectionReason = "multiple solutions"
                return@repeat
            }

            // Gate 2: given-count range (PUZZ-03)
            if (candidate.givenCount !in config.minGivens..config.maxGivens) {
                lastRejectionReason = "givenCount ${candidate.givenCount} not in ${config.minGivens}..${config.maxGivens}"
                return@repeat
            }

            // Gate 3: technique tier (PUZZ-02)
            if (!classifier.meetsRequirements(candidate, config)) {
                lastRejectionReason = "technique tier mismatch"
                return@repeat
            }

            return candidate
        }

        throw PuzzleGenerationException(
            "Failed to generate $difficulty puzzle after $maxAttempts attempts. Last rejection: $lastRejectionReason"
        )
    }

    /**
     * Calls Sudoklify to produce one raw candidate puzzle board.
     *
     * Sudoklify's difficulty classification does not reliably map to the project's technique-based
     * tiers (Pitfall 1 in RESEARCH.md). To maximise acceptance rate, we randomly sample from all
     * available Sudoklify difficulty levels and rely exclusively on [DifficultyClassifier] (Gate 3)
     * and the given-count range (Gate 2) to select the correct tier. Using only the matching
     * Sudoklify difficulty causes < 5% acceptance rates for MEDIUM and HARD.
     *
     * Returns null if Sudoklify throws or produces an unusable result.
     */
    private fun tryGenerateCandidate(difficulty: Difficulty): SudokuPuzzle? {
        return try {
            // Sample across all Sudoklify difficulties to increase acceptance rate for each tier.
            // Our technique classifier (Gate 3) acts as the authoritative difficulty gate.
            val sudoklifyDifficulty = pickSudoklifyDifficulty(difficulty)

            // SudoklifyArchitect takes a lambda returning SudokuSchemas (factory function pattern)
            val architect = SudoklifyArchitect { loadPresetSchemas() }

            // constructSudoku takes a SudokuSpec.Builder lambda (DSL-style configuration)
            // toSeed() requires a strictly positive Long; use nextLong with explicit positive range
            val rawPuzzle = architect.constructSudoku {
                seed = Random.nextLong(1L, Long.MAX_VALUE).toSeed()
                type = Dimension.NineByNine
                this.difficulty = sudoklifyDifficulty
            }

            // generateGridWithGivens() returns List<List<Int>> (9 rows × 9 cols); flatten to 81-element IntArray
            val boardGrid: List<List<Int>> = rawPuzzle.generateGridWithGivens()
            val board = IntArray(81) { i -> boardGrid[i / 9][i % 9] }

            // rawPuzzle.solution is List<List<Int>> — the complete solution grid
            val solutionGrid: List<List<Int>> = rawPuzzle.solution
            val solution = IntArray(81) { i -> solutionGrid[i / 9][i % 9] }

            SudokuPuzzle(
                board = board,
                solution = solution,
                difficulty = difficulty
            )
        } catch (e: Exception) {
            null  // Retry: generation may occasionally produce unusable results
        }
    }

    /**
     * Maps the requested project [Difficulty] to the Sudoklify difficulty tier(s) that produce
     * puzzles in the correct given-count range and technique tier.
     *
     * Empirical data (10 puzzles per tier, RESEARCH.md Pitfall 1):
     *   VERY_EASY → NAKED_SINGLES_ONLY 100%, givens avg 42, range 38–45
     *   EASY      → NAKED_SINGLES_ONLY 100%, givens avg 38, range 37–40
     *   MEDIUM    → NAKED_SINGLES_ONLY 40%, ADVANCED 60%, givens avg 30, range 26–32
     *   HARD      → ADVANCED 100%, givens avg 25, range 23–26
     *   VERY_HARD → ADVANCED 100%, givens avg 26, range 24–28
     *
     * Mapping:
     *   EASY   → VERY_EASY+EASY (38–45 givens, NAKED_SINGLES_ONLY → matches EASY_CONFIG)
     *   MEDIUM → MEDIUM only (26–32 givens; 40% are NAKED_SINGLES_ONLY in MEDIUM range → MEDIUM_CONFIG)
     *   HARD   → HARD+VERY_HARD (23–28 givens, ADVANCED → matches HARD_CONFIG range 22–27)
     */
    private fun pickSudoklifyDifficulty(difficulty: Difficulty): SudoklifyDifficulty =
        when (difficulty) {
            Difficulty.EASY   -> listOf(SudoklifyDifficulty.VERY_EASY, SudoklifyDifficulty.EASY).random()
            Difficulty.MEDIUM -> SudoklifyDifficulty.MEDIUM  // Only source of 27–35 givenCount puzzles
            Difficulty.HARD   -> listOf(SudoklifyDifficulty.HARD, SudoklifyDifficulty.VERY_HARD).random()
        }
}
