package com.mudita.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mudita.mmd.ThemeMMD
import com.mudita.sudoku.game.DataStoreGameRepository
import com.mudita.sudoku.game.GameViewModel
import com.mudita.sudoku.game.gameDataStore
import com.mudita.sudoku.ui.game.GameScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /**
     * Repository backed by DataStore scoped to applicationContext.
     * Uses lazy so applicationContext is available (not null at property initialisation time).
     */
    private val repository by lazy {
        DataStoreGameRepository(applicationContext.gameDataStore)
    }

    /**
     * ViewModel created via ViewModelProvider.Factory that injects the DataStoreGameRepository.
     * Using by viewModels { factory } ensures the same instance survives configuration changes.
     * GameScreen receives this ViewModel explicitly (not via the default viewModel() call) so
     * the activity-scoped instance with the real repository is always used.
     */
    private val viewModel: GameViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GameViewModel(repository = repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD {
                GameScreen(viewModel = viewModel)
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
