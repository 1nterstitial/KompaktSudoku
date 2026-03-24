package com.mudita.sudoku.puzzle.model

enum class TechniqueTier {
    NAKED_SINGLES_ONLY,   // Easy: solvable by naked singles alone
    HIDDEN_PAIRS,         // Medium: requires hidden singles or naked/hidden pairs
    ADVANCED              // Hard: requires X-wing or chain techniques
}

data class DifficultyConfig(
    val difficulty: Difficulty,
    val minGivens: Int,
    val maxGivens: Int,
    val requiredTechniqueTier: TechniqueTier
)

val EASY_CONFIG   = DifficultyConfig(Difficulty.EASY,   minGivens = 36, maxGivens = 45, TechniqueTier.NAKED_SINGLES_ONLY)
val MEDIUM_CONFIG = DifficultyConfig(Difficulty.MEDIUM, minGivens = 27, maxGivens = 35, TechniqueTier.HIDDEN_PAIRS)
val HARD_CONFIG   = DifficultyConfig(Difficulty.HARD,   minGivens = 22, maxGivens = 27, TechniqueTier.ADVANCED)

fun difficultyConfigFor(difficulty: Difficulty): DifficultyConfig = when (difficulty) {
    Difficulty.EASY   -> EASY_CONFIG
    Difficulty.MEDIUM -> MEDIUM_CONFIG
    Difficulty.HARD   -> HARD_CONFIG
}
