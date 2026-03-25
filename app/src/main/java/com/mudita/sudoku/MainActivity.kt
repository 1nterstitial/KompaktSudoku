package com.mudita.sudoku

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
import com.mudita.sudoku.game.DataStoreGameRepository
import com.mudita.sudoku.game.DataStoreScoreRepository
import com.mudita.sudoku.game.GameViewModel
import com.mudita.sudoku.game.gameDataStore
import com.mudita.sudoku.game.model.CompletionResult
import com.mudita.sudoku.game.scoreDataStore
import com.mudita.sudoku.ui.game.GameScreen
import com.mudita.sudoku.ui.game.LeaderboardScreen
import com.mudita.sudoku.ui.game.SummaryScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Top-level screen routing enum.
 *
 * Phase 6 replaces this with NavHost. Leaf composables (SummaryScreen, LeaderboardScreen) must
 * not contain nav-awareness — they receive data as parameters and emit callbacks.
 */
enum class Screen { GAME, SUMMARY, LEADERBOARD }

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
                var currentScreen by remember { mutableStateOf(Screen.GAME) }
                var completionResult by remember { mutableStateOf<CompletionResult?>(null) }
                val leaderboardScores by viewModel.leaderboardScores.collectAsStateWithLifecycle()

                when (currentScreen) {
                    Screen.GAME -> GameScreen(
                        viewModel = viewModel,
                        onCompleted = { result ->
                            // CRITICAL: Set completionResult BEFORE currentScreen.
                            // Compose batches state updates within the same event handler into
                            // a single recomposition, but assignment order still matters:
                            // SummaryScreen is rendered with the LATEST value of completionResult
                            // at the point of recomposition. Setting it first ensures
                            // SummaryScreen never receives a null result (Pitfall 2).
                            completionResult = result
                            currentScreen = Screen.SUMMARY
                        }
                    )
                    Screen.SUMMARY -> SummaryScreen(
                        result = completionResult!!,
                        onViewLeaderboard = {
                            currentScreen = Screen.LEADERBOARD
                        },
                        onNewGame = {
                            viewModel.startNewGame()
                            currentScreen = Screen.GAME
                        }
                    )
                    Screen.LEADERBOARD -> LeaderboardScreen(
                        scores = leaderboardScores,
                        onNewGame = {
                            viewModel.startNewGame()
                            currentScreen = Screen.GAME
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
