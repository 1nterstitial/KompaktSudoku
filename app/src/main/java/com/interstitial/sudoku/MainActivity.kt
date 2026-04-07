package com.interstitial.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.interstitial.sudoku.game.DataStoreGameRepository
import com.interstitial.sudoku.game.DataStoreRecordsRepository
import com.interstitial.sudoku.game.GameViewModel
import com.interstitial.sudoku.game.model.DifficultyRecord
import com.interstitial.sudoku.game.model.GameAction
import com.interstitial.sudoku.game.model.GameEvent
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.interstitial.sudoku.ui.game.GameScreen
import com.interstitial.sudoku.ui.home.HomeScreen
import com.interstitial.sudoku.ui.newpuzzle.NewPuzzleScreen
import com.interstitial.sudoku.ui.records.RecordsScreen
import com.interstitial.sudoku.ui.summary.SummaryScreen
import com.interstitial.sudoku.ui.theme.KompaktSudokuTheme
import com.mudita.mmd.components.snackbar.SnackbarHostStateMMD
import kotlinx.coroutines.launch

sealed class Route {
    data object Home : Route()
    data object NewPuzzle : Route()
    data object Game : Route()
    data class Summary(
        val difficulty: Difficulty,
        val elapsedMs: Long,
        val hintsUsed: Int,
        val isPersonalBest: Boolean
    ) : Route()
    data object Records : Route()
}

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gameRepo = DataStoreGameRepository(applicationContext)
        val recordsRepo = DataStoreRecordsRepository(applicationContext)
        viewModel = GameViewModel(gameRepository = gameRepo, recordsRepository = recordsRepo)

        setContent {
            KompaktSudokuTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                var currentRoute by remember { mutableStateOf<Route>(Route.Home) }
                var hasSavedGame by remember { mutableStateOf(false) }
                var savedDifficulty by remember { mutableStateOf("") }
                var savedCellsLeft by remember { mutableStateOf(0) }
                var records by remember { mutableStateOf(emptyMap<Difficulty, DifficultyRecord>()) }
                val snackbarHostState = remember { SnackbarHostStateMMD() }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    hasSavedGame = gameRepo.hasSavedGame()
                    if (hasSavedGame) {
                        val saved = gameRepo.loadGame()
                        if (saved != null) {
                            savedDifficulty = saved.difficulty.lowercase().replaceFirstChar { it.uppercase() }
                            savedCellsLeft = saved.board.count { it == 0 }
                        }
                    }
                    records = recordsRepo.getAllRecords()
                }

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is GameEvent.Completed -> {
                                currentRoute = Route.Summary(
                                    difficulty = event.difficulty,
                                    elapsedMs = event.elapsedMs,
                                    hintsUsed = event.hintsUsed,
                                    isPersonalBest = event.isPersonalBest
                                )
                                hasSavedGame = false
                                records = recordsRepo.getAllRecords()
                            }
                            is GameEvent.HintUnavailable -> {
                                scope.launch {
                                    snackbarHostState.showSnackbar(event.message)
                                }
                            }
                        }
                    }
                }

                BackHandler(enabled = currentRoute !is Route.Home && currentRoute !is Route.Game) {
                    currentRoute = Route.Home
                }

                when (val route = currentRoute) {
                    is Route.Home -> HomeScreen(
                        hasSavedGame = hasSavedGame,
                        savedGameDifficulty = savedDifficulty,
                        savedGameCellsLeft = savedCellsLeft,
                        onContinue = {
                            viewModel.onAction(GameAction.ResumeGame)
                            currentRoute = Route.Game
                        },
                        onNewPuzzle = { currentRoute = Route.NewPuzzle },
                        onRecords = {
                            scope.launch { records = recordsRepo.getAllRecords() }
                            currentRoute = Route.Records
                        }
                    )
                    is Route.NewPuzzle -> NewPuzzleScreen(
                        isGenerating = state.isGenerating,
                        onDifficultySelected = { difficulty ->
                            viewModel.onAction(GameAction.NewGame(difficulty))
                            currentRoute = Route.Game
                        },
                        onBack = { currentRoute = Route.Home }
                    )
                    is Route.Game -> GameScreen(
                        state = state,
                        snackbarHostState = snackbarHostState,
                        onAction = { action ->
                            viewModel.onAction(action)
                            when (action) {
                                is GameAction.KeepForLater -> {
                                    currentRoute = Route.Home
                                    scope.launch {
                                        hasSavedGame = gameRepo.hasSavedGame()
                                        if (hasSavedGame) {
                                            val saved = gameRepo.loadGame()
                                            if (saved != null) {
                                                savedDifficulty = saved.difficulty.lowercase().replaceFirstChar { it.uppercase() }
                                                savedCellsLeft = saved.board.count { it == 0 }
                                            }
                                        }
                                    }
                                }
                                is GameAction.DiscardPuzzle -> {
                                    currentRoute = Route.Home
                                    hasSavedGame = false
                                }
                                else -> {}
                            }
                        }
                    )
                    is Route.Summary -> SummaryScreen(
                        difficulty = route.difficulty,
                        elapsedMs = route.elapsedMs,
                        hintsUsed = route.hintsUsed,
                        isPersonalBest = route.isPersonalBest,
                        onNewPuzzle = { currentRoute = Route.NewPuzzle },
                        onBackToMenu = { currentRoute = Route.Home },
                        onViewRecords = {
                            scope.launch { records = recordsRepo.getAllRecords() }
                            currentRoute = Route.Records
                        }
                    )
                    is Route.Records -> RecordsScreen(
                        records = records,
                        onBack = { currentRoute = Route.Home }
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveOnStop()
    }
}
