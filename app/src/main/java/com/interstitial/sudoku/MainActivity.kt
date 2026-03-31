package com.interstitial.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mudita.mmd.ThemeMMD
import com.interstitial.sudoku.game.DataStoreGameRepository
import com.interstitial.sudoku.game.DataStoreScoreRepository
import com.interstitial.sudoku.game.GameViewModel
import com.interstitial.sudoku.game.gameDataStore
import com.interstitial.sudoku.game.model.CompletionResult
import com.interstitial.sudoku.game.scoreDataStore
import com.interstitial.sudoku.ui.game.DifficultyScreen
import com.interstitial.sudoku.ui.game.GameScreen
import com.interstitial.sudoku.ui.game.LeaderboardScreen
import com.interstitial.sudoku.ui.game.MenuScreen
import com.interstitial.sudoku.ui.game.SummaryScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Top-level screen routing enum.
 *
 * Leaf composables (SummaryScreen, LeaderboardScreen, MenuScreen, DifficultyScreen) must
 * not contain nav-awareness — they receive data as parameters and emit callbacks.
 */
enum class Screen { MENU, DIFFICULTY, GAME, SUMMARY, LEADERBOARD }

class MainActivity : ComponentActivity() {

    /**
     * Repository backed by DataStore scoped to applicationContext.
     * Uses lazy so applicationContext is available (not null at property initialisation time).
     */
    private val repository by lazy {
        DataStoreGameRepository(applicationContext.gameDataStore)
    }

    /**
     * Score repository backed by a separate DataStore file ("score_state").
     *
     * MUST be a separate DataStore instance from [repository] — a single DataStore file can only
     * have one active instance per process. Using two instances on the same file causes corruption.
     */
    private val scoreRepository by lazy {
        DataStoreScoreRepository(applicationContext.scoreDataStore)
    }

    /**
     * ViewModel created via ViewModelProvider.Factory that injects both repositories.
     * Using by viewModels { factory } ensures the same instance survives configuration changes.
     */
    private val viewModel: GameViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GameViewModel(
                    repository = repository,
                    scoreRepository = scoreRepository
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD {
                var currentScreen by remember { mutableStateOf(Screen.MENU) }
                var completionResult by remember { mutableStateOf<CompletionResult?>(null) }
                val leaderboardScores by viewModel.leaderboardScores.collectAsStateWithLifecycle()
                // Reactive: showResumeDialog StateFlow emits true when DataStore load finds a
                // saved game. Collecting here ensures the Resume button appears as soon as the
                // async load completes (Pitfall 1 avoidance).
                val hasSavedGame by viewModel.showResumeDialog.collectAsStateWithLifecycle()

                when (currentScreen) {
                    Screen.MENU -> MenuScreen(
                        hasSavedGame = hasSavedGame,
                        onNewGame = {
                            currentScreen = Screen.DIFFICULTY
                        },
                        onResume = {
                            // MUST call resumeGame() here — clears showResumeDialog and restores
                            // board state from the persisted save (Pitfall 4).
                            viewModel.resumeGame()
                            currentScreen = Screen.GAME
                        },
                        onBestScores = {
                            currentScreen = Screen.LEADERBOARD
                        }
                    )
                    Screen.DIFFICULTY -> DifficultyScreen(
                        onDifficultySelected = { difficulty ->
                            // Call startGame() BEFORE navigating — ensures ViewModel starts
                            // generating the puzzle before GameScreen renders (Pitfall 5).
                            viewModel.startGame(difficulty)
                            currentScreen = Screen.GAME
                        },
                        onBack = {
                            currentScreen = Screen.MENU
                        }
                    )
                    Screen.GAME -> GameScreen(
                        viewModel = viewModel,
                        onCompleted = { result ->
                            // CRITICAL: Set completionResult BEFORE currentScreen.
                            // Compose batches state updates within the same event handler into
                            // a single recomposition, but assignment order still matters:
                            // SummaryScreen is rendered with the LATEST value of completionResult
                            // at the point of recomposition. Setting it first ensures
                            // SummaryScreen never receives a null result (Pitfall 3).
                            completionResult = result
                            currentScreen = Screen.SUMMARY
                        },
                        onBackToMenu = {
                            currentScreen = Screen.MENU
                        }
                    )
                    Screen.SUMMARY -> SummaryScreen(
                        result = completionResult!!,
                        onViewLeaderboard = {
                            currentScreen = Screen.LEADERBOARD
                        },
                        onBackToMenu = {
                            currentScreen = Screen.MENU
                        }
                    )
                    Screen.LEADERBOARD -> LeaderboardScreen(
                        scores = leaderboardScores,
                        onBackToMenu = {
                            currentScreen = Screen.MENU
                        }
                    )
                }
            }
        }
    }

    /**
     * Trigger automatic save when the app moves to the background.
     *
     * Saves on Dispatchers.IO — all save guards (isLoading, isComplete, empty board) live inside
     * saveNow() to keep MainActivity thin (per D-01). No explicit pause button is needed; this
     * lifecycle hook covers the "put device down mid-game" E-ink use case.
     *
     * No per-move saves (per D-02) — only on onStop to minimise DataStore write pressure.
     */
    override fun onStop() {
        super.onStop()
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.saveNow()
        }
    }
}
