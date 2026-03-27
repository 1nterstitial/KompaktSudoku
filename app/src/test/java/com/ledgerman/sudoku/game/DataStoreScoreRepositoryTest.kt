package com.ledgerman.sudoku.game

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ledgerman.sudoku.puzzle.model.Difficulty
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataStoreScoreRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createRepository(): DataStoreScoreRepository {
        val dataStore = PreferenceDataStoreFactory.create {
            tempFolder.newFile("test_score_state.preferences_pb")
        }
        return DataStoreScoreRepository(dataStore)
    }

    @Test
    fun `getBestScore returns null when no score saved`() = runTest {
        val repo = createRepository()
        assertNull(repo.getBestScore(Difficulty.EASY))
    }

    @Test
    fun `saveBestScore then getBestScore returns saved value`() = runTest {
        val repo = createRepository()
        repo.saveBestScore(Difficulty.EASY, 85)
        assertEquals(85, repo.getBestScore(Difficulty.EASY))
    }

    @Test
    fun `scores are stored per difficulty independently`() = runTest {
        val repo = createRepository()
        repo.saveBestScore(Difficulty.EASY, 90)
        repo.saveBestScore(Difficulty.MEDIUM, 70)

        assertEquals(90, repo.getBestScore(Difficulty.EASY))
        assertEquals(70, repo.getBestScore(Difficulty.MEDIUM))
        assertNull(repo.getBestScore(Difficulty.HARD))
    }

    @Test
    fun `saveBestScore overwrites previous value`() = runTest {
        val repo = createRepository()
        repo.saveBestScore(Difficulty.EASY, 50)
        repo.saveBestScore(Difficulty.EASY, 90)
        assertEquals(90, repo.getBestScore(Difficulty.EASY))
    }
}
