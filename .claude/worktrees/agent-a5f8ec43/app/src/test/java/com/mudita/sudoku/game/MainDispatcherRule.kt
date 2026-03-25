package com.mudita.sudoku.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit 4 TestWatcher that replaces Dispatchers.Main with an UnconfinedTestDispatcher
 * for the duration of each test.
 *
 * Usage:
 *   @get:Rule val mainDispatcherRule = MainDispatcherRule()
 *
 * This allows ViewModel tests to run synchronously on the JVM without needing
 * a running Android Looper for Dispatchers.Main.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: kotlinx.coroutines.test.TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
