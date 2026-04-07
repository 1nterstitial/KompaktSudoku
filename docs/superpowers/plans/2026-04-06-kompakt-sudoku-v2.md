# KompaktSudoku v2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete Sudoku app for the Mudita Kompakt E-ink device — fresh project with ported puzzle engine, session-centric records, rule-based conflicts, and MMD-exclusive UI.

**Architecture:** Single `GameViewModel` with `StateFlow<GameUiState>`, sealed class navigation in `MainActivity`, two DataStore-backed repositories. Canvas-based grid. No Jetpack Navigation, no animations.

**Tech Stack:** Kotlin 2.3.20, Compose BOM 2026.03.00, MMD 1.0.1, AGP 8.9.0, Gradle 8.11.1, DataStore 1.2.1, kotlinx.serialization 1.8.0, JUnit 4, Mockk, Turbine, Robolectric.

**Working directory:** `D:\Development\KompaktSudoku\sudoku-v2`

**V1 source (read-only reference):** `D:\Development\KompaktSudoku\sudoku-v1`

---

## File Structure

```
sudoku-v2/
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/
│   └── libs.versions.toml
├── gradlew, gradlew.bat, gradle/wrapper/*
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   └── java/com/interstitial/sudoku/
        │       ├── MainActivity.kt
        │       ├── puzzle/
        │       │   ├── PuzzleGenerationException.kt
        │       │   ├── engine/
        │       │   │   ├── SudokuValidator.kt
        │       │   │   ├── UniquenessVerifier.kt
        │       │   │   ├── DifficultyClassifier.kt
        │       │   │   ├── SudokuGenerator.kt
        │       │   │   └── ConflictDetector.kt
        │       │   └── model/
        │       │       ├── Difficulty.kt
        │       │       ├── DifficultyConfig.kt
        │       │       └── SudokuPuzzle.kt
        │       ├── game/
        │       │   ├── GameViewModel.kt
        │       │   ├── GameRepository.kt
        │       │   ├── DataStoreGameRepository.kt
        │       │   ├── RecordsRepository.kt
        │       │   ├── DataStoreRecordsRepository.kt
        │       │   └── model/
        │       │       ├── GameUiState.kt
        │       │       ├── GameAction.kt
        │       │       ├── GameEvent.kt
        │       │       ├── InputMode.kt
        │       │       ├── DifficultyRecord.kt
        │       │       └── PersistedGameState.kt
        │       └── ui/
        │           ├── theme/
        │           │   └── Theme.kt
        │           ├── home/
        │           │   └── HomeScreen.kt
        │           ├── newpuzzle/
        │           │   └── NewPuzzleScreen.kt
        │           ├── game/
        │           │   ├── GameScreen.kt
        │           │   ├── SudokuGrid.kt
        │           │   ├── PuzzleTopBar.kt
        │           │   ├── PuzzleMetaStrip.kt
        │           │   ├── InputModeToggle.kt
        │           │   ├── PuzzleActionRow.kt
        │           │   ├── DigitPad.kt
        │           │   └── LeavePuzzleDialog.kt
        │           ├── summary/
        │           │   └── SummaryScreen.kt
        │           └── records/
        │               └── RecordsScreen.kt
        └── test/
            └── java/com/interstitial/sudoku/
                ├── puzzle/
                │   ├── SudokuValidatorTest.kt
                │   ├── UniquenessVerifierTest.kt
                │   ├── DifficultyClassifierTest.kt
                │   ├── SudokuGeneratorTest.kt
                │   └── ConflictDetectorTest.kt
                └── game/
                    ├── MainDispatcherRule.kt
                    ├── FakeGameRepository.kt
                    ├── FakeRecordsRepository.kt
                    ├── GameViewModelTest.kt
                    └── PersistedGameStateTest.kt
```

---

## Task 1: Project Scaffold — Gradle & Build Files

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Generate Gradle wrapper**

Run from `D:\Development\KompaktSudoku\sudoku-v2`:

```bash
# If gradle is available globally:
gradle wrapper --gradle-version 8.11.1

# If not, copy the wrapper files from v1:
cp -r ../sudoku-v1/gradle/wrapper ./gradle/
cp ../sudoku-v1/gradlew ./
cp ../sudoku-v1/gradlew.bat ./
```

- [ ] **Step 2: Create `gradle/libs.versions.toml`**

```toml
[versions]
activity = "1.8.2"
agp = "8.9.0"
composeBom = "2026.03.00"
coroutines = "1.10.2"
datastore = "1.2.1"
junit = "4.13.2"
kotlin = "2.3.20"
kotlinSerialization = "1.8.0"
lifecycle = "2.9.0"
mmd = "1.0.1"
mockk = "1.13.17"
robolectric = "4.14.1"
sudoklify = "1.0.0-beta04"
turbine = "1.2.0"

[libraries]
# AndroidX / Compose
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

# Kotlin Serialization
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinSerialization" }

# MMD
mmd = { group = "com.mudita", name = "MMD", version.ref = "mmd" }

# Sudoklify
sudoklify-core = { group = "dev.teogor.sudoklify", name = "sudoklify-core", version.ref = "sudoklify" }
sudoklify-presets = { group = "dev.teogor.sudoklify", name = "sudoklify-presets", version.ref = "sudoklify" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 3: Create `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        val githubToken: String? = providers.gradleProperty("githubToken").orNull
        if (githubToken != null) {
            maven {
                url = uri("https://maven.pkg.github.com/mudita/MMD")
                credentials {
                    username = "token"
                    password = githubToken
                }
                content {
                    includeGroup("com.mudita")
                }
            }
        }
    }
}

rootProject.name = "KompaktSudoku"
include(":app")
```

- [ ] **Step 4: Create root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

- [ ] **Step 5: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 6: Create `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.interstitial.sudoku"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.interstitial.sudoku"
        minSdk = 31
        targetSdk = 31
        versionCode = 1
        versionName = "2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    lint {
        disable += "ExpiredTargetSdkVersion"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.sudoklify.core)
    implementation(libs.sudoklify.presets)
    implementation(libs.mmd)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)

    androidTestImplementation(libs.compose.ui.test.junit4)
}
```

- [ ] **Step 7: Create `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="Sudoku"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 8: Verify the build compiles**

Create a minimal `MainActivity.kt` so the project compiles:

```kotlin
package com.interstitial.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mudita.mmd.theme.ThemeMMD
import com.mudita.mmd.theme.eInkColorScheme
import com.mudita.mmd.theme.eInkTypography
import com.mudita.mmd.components.TextMMD

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD(
                colorScheme = eInkColorScheme,
                typography = eInkTypography
            ) {
                TextMMD("KompaktSudoku v2")
            }
        }
    }
}
```

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

**Note:** If MMD imports fail, check the GitHub Packages token in `~/.gradle/gradle.properties`. The v1 project uses `githubToken` property. If MMD import paths differ from what's shown here (e.g., the actual package names may vary), read the MMD library source or v1's working imports to find the correct paths. The key classes needed are: `ThemeMMD`, `eInkColorScheme`, `eInkTypography`, `TextMMD`, `ButtonMMD`, `TopAppBarMMD`, `LazyColumnMMD`, `HorizontalDividerMMD`, `SnackbarHostStateMMD`.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat: scaffold v2 project with Gradle, dependencies, and minimal MainActivity"
```

---

## Task 2: Puzzle Models — Port from v1

**Files:**
- Create: `app/src/main/java/com/interstitial/sudoku/puzzle/model/Difficulty.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/puzzle/model/DifficultyConfig.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/puzzle/model/SudokuPuzzle.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/puzzle/PuzzleGenerationException.kt`

These are direct copies from v1. The package is already `com.interstitial.sudoku.puzzle.*` — no changes needed.

- [ ] **Step 1: Create `Difficulty.kt`**

```kotlin
package com.interstitial.sudoku.puzzle.model

enum class Difficulty {
    EASY, MEDIUM, HARD
}
```

- [ ] **Step 2: Create `DifficultyConfig.kt`**

```kotlin
package com.interstitial.sudoku.puzzle.model

enum class TechniqueTier {
    NAKED_SINGLES_ONLY,
    HIDDEN_PAIRS,
    ADVANCED
}

data class DifficultyConfig(
    val difficulty: Difficulty,
    val minGivens: Int,
    val maxGivens: Int,
    val requiredTechniqueTier: TechniqueTier
)

val EASY_CONFIG = DifficultyConfig(Difficulty.EASY, minGivens = 36, maxGivens = 45, TechniqueTier.NAKED_SINGLES_ONLY)
val MEDIUM_CONFIG = DifficultyConfig(Difficulty.MEDIUM, minGivens = 27, maxGivens = 35, TechniqueTier.NAKED_SINGLES_ONLY)
val HARD_CONFIG = DifficultyConfig(Difficulty.HARD, minGivens = 22, maxGivens = 27, TechniqueTier.ADVANCED)

fun difficultyConfigFor(difficulty: Difficulty): DifficultyConfig = when (difficulty) {
    Difficulty.EASY -> EASY_CONFIG
    Difficulty.MEDIUM -> MEDIUM_CONFIG
    Difficulty.HARD -> HARD_CONFIG
}
```

- [ ] **Step 3: Create `SudokuPuzzle.kt`**

```kotlin
package com.interstitial.sudoku.puzzle.model

data class SudokuPuzzle(
    val board: IntArray,
    val solution: IntArray,
    val difficulty: Difficulty,
    val givenCount: Int = board.count { it != 0 }
) {
    init {
        require(board.size == 81) { "board must have exactly 81 cells, got ${board.size}" }
        require(solution.size == 81) { "solution must have exactly 81 cells, got ${solution.size}" }
        require(solution.none { it == 0 }) { "solution must be fully filled (no zeros)" }
        require(givenCount in 17..81) { "givenCount $givenCount is below the mathematical minimum of 17" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SudokuPuzzle) return false
        return board.contentEquals(other.board) &&
               solution.contentEquals(other.solution) &&
               difficulty == other.difficulty
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + solution.contentHashCode()
        result = 31 * result + difficulty.hashCode()
        return result
    }
}
```

- [ ] **Step 4: Create `PuzzleGenerationException.kt`**

```kotlin
package com.interstitial.sudoku.puzzle

class PuzzleGenerationException(message: String) : Exception(message)
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/puzzle/
git commit -m "feat: port puzzle models from v1 (Difficulty, DifficultyConfig, SudokuPuzzle)"
```

---

## Task 3: Puzzle Engine — Port from v1 with Tests

**Files:**
- Create: `app/src/main/java/com/interstitial/sudoku/puzzle/engine/SudokuValidator.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/puzzle/engine/UniquenessVerifier.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/puzzle/engine/DifficultyClassifier.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/puzzle/engine/SudokuGenerator.kt`
- Create: `app/src/test/java/com/interstitial/sudoku/puzzle/SudokuValidatorTest.kt`
- Create: `app/src/test/java/com/interstitial/sudoku/puzzle/UniquenessVerifierTest.kt`
- Create: `app/src/test/java/com/interstitial/sudoku/puzzle/DifficultyClassifierTest.kt`
- Create: `app/src/test/java/com/interstitial/sudoku/puzzle/SudokuGeneratorTest.kt`

These are direct copies from v1 at `D:\Development\KompaktSudoku\sudoku-v1\app\src\main\java\com\interstitial\sudoku\puzzle\engine\` and their corresponding tests at `D:\Development\KompaktSudoku\sudoku-v1\app\src\test\java\com\interstitial\sudoku\puzzle\`.

- [ ] **Step 1: Copy engine source files**

Copy these files verbatim from v1 (the package is already `com.interstitial.sudoku.puzzle.engine`):
- `SudokuValidator.kt`
- `UniquenessVerifier.kt`
- `DifficultyClassifier.kt`
- `SudokuGenerator.kt`

Read each from `D:\Development\KompaktSudoku\sudoku-v1\app\src\main\java\com\interstitial\sudoku\puzzle\engine\` and write to `D:\Development\KompaktSudoku\sudoku-v2\app\src\main\java\com\interstitial\sudoku\puzzle\engine\`.

- [ ] **Step 2: Copy engine test files**

Copy these files verbatim from v1:
- `SudokuValidatorTest.kt`
- `UniquenessVerifierTest.kt`
- `DifficultyClassifierTest.kt`
- `SudokuGeneratorTest.kt`

Read each from `D:\Development\KompaktSudoku\sudoku-v1\app\src\test\java\com\interstitial\sudoku\puzzle\` and write to `D:\Development\KompaktSudoku\sudoku-v2\app\src\test\java\com\interstitial\sudoku\puzzle\`.

Also check if there's a `SudokuEngineIntegrationTest.kt` — copy it if so.

- [ ] **Step 3: Run ported tests**

```bash
./gradlew test --tests "com.interstitial.sudoku.puzzle.*"
```

Expected: All tests PASS. If any fail, fix import paths or missing dependencies.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/puzzle/engine/ app/src/test/java/com/interstitial/sudoku/puzzle/
git commit -m "feat: port puzzle engine and tests from v1 (generator, validator, uniqueness, classifier)"
```

---

## Task 4: ConflictDetector — New Component

**Files:**
- Create: `app/src/main/java/com/interstitial/sudoku/puzzle/engine/ConflictDetector.kt`
- Create: `app/src/test/java/com/interstitial/sudoku/puzzle/ConflictDetectorTest.kt`

- [ ] **Step 1: Write ConflictDetector tests**

```kotlin
package com.interstitial.sudoku.puzzle

import com.interstitial.sudoku.puzzle.engine.ConflictDetector
import org.junit.Assert.*
import org.junit.Test

class ConflictDetectorTest {

    private fun emptyBoard() = IntArray(81)

    @Test
    fun `empty board has no conflicts`() {
        val mask = ConflictDetector.buildConflictMask(emptyBoard())
        assertTrue(mask.none { it })
    }

    @Test
    fun `board with no duplicates has no conflicts`() {
        val board = emptyBoard()
        board[0] = 1 // row 0, col 0
        board[1] = 2 // row 0, col 1
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask.none { it })
    }

    @Test
    fun `row duplicate marks both cells`() {
        val board = emptyBoard()
        board[0] = 5 // row 0, col 0
        board[3] = 5 // row 0, col 3
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask[0])
        assertTrue(mask[3])
    }

    @Test
    fun `column duplicate marks both cells`() {
        val board = emptyBoard()
        board[0] = 5  // row 0, col 0
        board[27] = 5 // row 3, col 0
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask[0])
        assertTrue(mask[27])
    }

    @Test
    fun `box duplicate marks both cells`() {
        val board = emptyBoard()
        board[0] = 5  // row 0, col 0 (box 0)
        board[10] = 5 // row 1, col 1 (box 0)
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask[0])
        assertTrue(mask[10])
    }

    @Test
    fun `three-way conflict marks all three cells`() {
        val board = emptyBoard()
        board[0] = 5  // row 0, col 0
        board[1] = 5  // row 0, col 1 (row conflict)
        board[9] = 5  // row 1, col 0 (column conflict with 0, box conflict with 0)
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask[0])
        assertTrue(mask[1])
        assertTrue(mask[9])
    }

    @Test
    fun `non-conflicting cells are not marked`() {
        val board = emptyBoard()
        board[0] = 5  // row 0, col 0
        board[3] = 5  // row 0, col 3 (conflict)
        board[40] = 7 // row 4, col 4 (no conflict with anything)
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask[0])
        assertTrue(mask[3])
        assertFalse(mask[40])
    }

    @Test
    fun `conflict cleared after value removed`() {
        val board = emptyBoard()
        board[0] = 5
        board[3] = 5
        val mask1 = ConflictDetector.buildConflictMask(board)
        assertTrue(mask1[0])

        board[3] = 0
        val mask2 = ConflictDetector.buildConflictMask(board)
        assertFalse(mask2[0])
        assertFalse(mask2[3])
    }

    @Test
    fun `conflict cleared after value replaced`() {
        val board = emptyBoard()
        board[0] = 5
        board[3] = 5
        val mask1 = ConflictDetector.buildConflictMask(board)
        assertTrue(mask1[0])

        board[3] = 6
        val mask2 = ConflictDetector.buildConflictMask(board)
        assertFalse(mask2[0])
        assertFalse(mask2[3])
    }

    @Test
    fun `valid complete board has no conflicts`() {
        val board = intArrayOf(
            5,3,4, 6,7,8, 9,1,2,
            6,7,2, 1,9,5, 3,4,8,
            1,9,8, 3,4,2, 5,6,7,
            8,5,9, 7,6,1, 4,2,3,
            4,2,6, 8,5,3, 7,9,1,
            7,1,3, 9,2,4, 8,5,6,
            9,6,1, 5,3,7, 2,8,4,
            2,8,7, 4,1,9, 6,3,5,
            3,4,5, 2,8,6, 1,7,9
        )
        val mask = ConflictDetector.buildConflictMask(board)
        assertTrue(mask.none { it })
    }
}
```

- [ ] **Step 2: Run tests — verify they fail**

```bash
./gradlew test --tests "com.interstitial.sudoku.puzzle.ConflictDetectorTest"
```

Expected: FAIL — `ConflictDetector` class not found.

- [ ] **Step 3: Implement `ConflictDetector.kt`**

```kotlin
package com.interstitial.sudoku.puzzle.engine

object ConflictDetector {

    fun buildConflictMask(board: IntArray): BooleanArray {
        val mask = BooleanArray(81)
        for (i in 0 until 81) {
            if (board[i] == 0) continue
            val row = i / 9
            val col = i % 9
            val boxRow = (row / 3) * 3
            val boxCol = (col / 3) * 3

            // Check row
            for (c in 0 until 9) {
                val j = row * 9 + c
                if (j != i && board[j] == board[i]) {
                    mask[i] = true
                    mask[j] = true
                }
            }
            // Check column
            for (r in 0 until 9) {
                val j = r * 9 + col
                if (j != i && board[j] == board[i]) {
                    mask[i] = true
                    mask[j] = true
                }
            }
            // Check 3x3 box
            for (dr in 0 until 3) {
                for (dc in 0 until 3) {
                    val j = (boxRow + dr) * 9 + (boxCol + dc)
                    if (j != i && board[j] == board[i]) {
                        mask[i] = true
                        mask[j] = true
                    }
                }
            }
        }
        return mask
    }
}
```

- [ ] **Step 4: Run tests — verify they pass**

```bash
./gradlew test --tests "com.interstitial.sudoku.puzzle.ConflictDetectorTest"
```

Expected: All 8 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/puzzle/engine/ConflictDetector.kt app/src/test/java/com/interstitial/sudoku/puzzle/ConflictDetectorTest.kt
git commit -m "feat: add rule-based ConflictDetector with tests"
```

---

## Task 5: Game Data Models

**Files:**
- Create: `app/src/main/java/com/interstitial/sudoku/game/model/InputMode.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/game/model/GameAction.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/game/model/GameEvent.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/game/model/DifficultyRecord.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/game/model/PersistedGameState.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/game/model/GameUiState.kt`

- [ ] **Step 1: Create `InputMode.kt`**

```kotlin
package com.interstitial.sudoku.game.model

enum class InputMode { FILL, NOTES }
```

- [ ] **Step 2: Create `GameAction.kt`**

This is the sealed class of user-dispatched actions (NOT undo entries — undo entries are an internal ViewModel concept).

```kotlin
package com.interstitial.sudoku.game.model

import com.interstitial.sudoku.puzzle.model.Difficulty

sealed class GameAction {
    data class SelectCell(val index: Int) : GameAction()
    data class PlaceDigit(val digit: Int) : GameAction()
    data class ToggleNote(val digit: Int) : GameAction()
    data object Erase : GameAction()
    data object Undo : GameAction()
    data object Hint : GameAction()
    data object ToggleInputMode : GameAction()
    data class NewGame(val difficulty: Difficulty) : GameAction()
    data object ResumeGame : GameAction()
    data object PausePuzzle : GameAction()
    data object KeepForLater : GameAction()
    data object DiscardPuzzle : GameAction()
}
```

- [ ] **Step 3: Create `GameEvent.kt`**

```kotlin
package com.interstitial.sudoku.game.model

import com.interstitial.sudoku.puzzle.model.Difficulty

sealed class GameEvent {
    data class Completed(
        val difficulty: Difficulty,
        val elapsedMs: Long,
        val hintsUsed: Int,
        val isPersonalBest: Boolean
    ) : GameEvent()

    data class HintUnavailable(val message: String) : GameEvent()
}
```

- [ ] **Step 4: Create `DifficultyRecord.kt`**

```kotlin
package com.interstitial.sudoku.game.model

import kotlinx.serialization.Serializable

@Serializable
data class DifficultyRecord(
    val completedCount: Int = 0,
    val bestTimeMs: Long? = null,
    val bestNoHintTimeMs: Long? = null,
    val lastCompletedEpochMs: Long? = null
)
```

- [ ] **Step 5: Create `PersistedGameState.kt`**

```kotlin
package com.interstitial.sudoku.game.model

import kotlinx.serialization.Serializable

@Serializable
data class PersistedGameState(
    val schemaVersion: Int = 1,
    val board: List<Int>,
    val solution: List<Int>,
    val givens: List<Boolean>,
    val notes: List<List<Int>>,
    val difficulty: String,
    val elapsedMs: Long,
    val hintsUsed: Int,
    val hintedCells: List<Int>
)
```

- [ ] **Step 6: Create `GameUiState.kt`**

```kotlin
package com.interstitial.sudoku.game.model

import com.interstitial.sudoku.puzzle.model.Difficulty

data class GameUiState(
    val board: IntArray = IntArray(81),
    val solution: IntArray = IntArray(81),
    val givens: BooleanArray = BooleanArray(81),
    val notes: Array<Set<Int>> = Array(81) { emptySet() },
    val conflictMask: BooleanArray = BooleanArray(81),
    val selectedCell: Int? = null,
    val inputMode: InputMode = InputMode.FILL,
    val difficulty: Difficulty = Difficulty.EASY,
    val cellsRemaining: Int = 0,
    val elapsedMs: Long = 0,
    val hasUndo: Boolean = false,
    val digitCounts: IntArray = IntArray(9),
    val isComplete: Boolean = false,
    val isPaused: Boolean = false,
    val hintsUsed: Int = 0,
    val hintedCells: Set<Int> = emptySet(),
    val isGenerating: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameUiState) return false
        return board.contentEquals(other.board) &&
            solution.contentEquals(other.solution) &&
            givens.contentEquals(other.givens) &&
            notes.contentDeepEquals(other.notes) &&
            conflictMask.contentEquals(other.conflictMask) &&
            selectedCell == other.selectedCell &&
            inputMode == other.inputMode &&
            difficulty == other.difficulty &&
            cellsRemaining == other.cellsRemaining &&
            elapsedMs == other.elapsedMs &&
            hasUndo == other.hasUndo &&
            digitCounts.contentEquals(other.digitCounts) &&
            isComplete == other.isComplete &&
            isPaused == other.isPaused &&
            hintsUsed == other.hintsUsed &&
            hintedCells == other.hintedCells &&
            isGenerating == other.isGenerating
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + solution.contentHashCode()
        result = 31 * result + givens.contentHashCode()
        result = 31 * result + notes.contentDeepHashCode()
        result = 31 * result + conflictMask.contentHashCode()
        result = 31 * result + (selectedCell?.hashCode() ?: 0)
        result = 31 * result + inputMode.hashCode()
        result = 31 * result + difficulty.hashCode()
        result = 31 * result + cellsRemaining
        result = 31 * result + elapsedMs.hashCode()
        result = 31 * result + hasUndo.hashCode()
        result = 31 * result + digitCounts.contentHashCode()
        result = 31 * result + isComplete.hashCode()
        result = 31 * result + isPaused.hashCode()
        result = 31 * result + hintsUsed
        result = 31 * result + hintedCells.hashCode()
        result = 31 * result + isGenerating.hashCode()
        return result
    }
}
```

- [ ] **Step 7: Verify build compiles**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/game/model/
git commit -m "feat: add game data models (GameUiState, GameAction, GameEvent, DifficultyRecord, PersistedGameState)"
```

---

## Task 6: Persistence Layer — Repositories

**Files:**
- Create: `app/src/main/java/com/interstitial/sudoku/game/GameRepository.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/game/DataStoreGameRepository.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/game/RecordsRepository.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/game/DataStoreRecordsRepository.kt`
- Create: `app/src/test/java/com/interstitial/sudoku/game/PersistedGameStateTest.kt`

- [ ] **Step 1: Write serialization round-trip test**

```kotlin
package com.interstitial.sudoku.game

import com.interstitial.sudoku.game.model.PersistedGameState
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class PersistedGameStateTest {

    @Test
    fun `round trip serialization preserves all fields`() {
        val state = PersistedGameState(
            schemaVersion = 1,
            board = (1..81).toList(),
            solution = (1..81).map { ((it - 1) % 9) + 1 },
            givens = (1..81).map { it % 3 == 0 },
            notes = (1..81).map { if (it % 5 == 0) listOf(1, 3, 7) else emptyList() },
            difficulty = "MEDIUM",
            elapsedMs = 123456L,
            hintsUsed = 2,
            hintedCells = listOf(10, 45)
        )
        val json = Json.encodeToString(PersistedGameState.serializer(), state)
        val restored = Json.decodeFromString(PersistedGameState.serializer(), json)
        assertEquals(state, restored)
    }

    @Test
    fun `notes with full 1-9 candidates survive round trip`() {
        val state = PersistedGameState(
            schemaVersion = 1,
            board = List(81) { 0 },
            solution = List(81) { 1 },
            givens = List(81) { false },
            notes = List(81) { listOf(1, 2, 3, 4, 5, 6, 7, 8, 9) },
            difficulty = "HARD",
            elapsedMs = 0L,
            hintsUsed = 0,
            hintedCells = emptyList()
        )
        val json = Json.encodeToString(PersistedGameState.serializer(), state)
        val restored = Json.decodeFromString(PersistedGameState.serializer(), json)
        assertEquals(9, restored.notes[0].size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9), restored.notes[0])
    }
}
```

- [ ] **Step 2: Run test — verify it passes**

```bash
./gradlew test --tests "com.interstitial.sudoku.game.PersistedGameStateTest"
```

Expected: PASS (the `@Serializable` annotation on `PersistedGameState` should make this work).

- [ ] **Step 3: Create `GameRepository.kt`**

```kotlin
package com.interstitial.sudoku.game

import com.interstitial.sudoku.game.model.PersistedGameState

interface GameRepository {
    suspend fun saveGame(state: PersistedGameState)
    suspend fun loadGame(): PersistedGameState?
    suspend fun clearGame()
    suspend fun hasSavedGame(): Boolean
}
```

- [ ] **Step 4: Create `DataStoreGameRepository.kt`**

```kotlin
package com.interstitial.sudoku.game

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.interstitial.sudoku.game.model.PersistedGameState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "game_state")

class DataStoreGameRepository(private val context: Context) : GameRepository {

    private val key = stringPreferencesKey("in_progress_game")

    override suspend fun saveGame(state: PersistedGameState) {
        val json = Json.encodeToString(PersistedGameState.serializer(), state)
        context.gameDataStore.edit { it[key] = json }
    }

    override suspend fun loadGame(): PersistedGameState? {
        val json = context.gameDataStore.data.map { it[key] }.first() ?: return null
        return try {
            Json.decodeFromString(PersistedGameState.serializer(), json)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearGame() {
        context.gameDataStore.edit { it.remove(key) }
    }

    override suspend fun hasSavedGame(): Boolean {
        return context.gameDataStore.data.map { it.contains(key) }.first()
    }
}
```

- [ ] **Step 5: Create `RecordsRepository.kt`**

```kotlin
package com.interstitial.sudoku.game

import com.interstitial.sudoku.game.model.DifficultyRecord
import com.interstitial.sudoku.puzzle.model.Difficulty

interface RecordsRepository {
    suspend fun getRecord(difficulty: Difficulty): DifficultyRecord
    suspend fun updateRecord(difficulty: Difficulty, record: DifficultyRecord)
    suspend fun getAllRecords(): Map<Difficulty, DifficultyRecord>
}
```

- [ ] **Step 6: Create `DataStoreRecordsRepository.kt`**

```kotlin
package com.interstitial.sudoku.game

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.interstitial.sudoku.game.model.DifficultyRecord
import com.interstitial.sudoku.puzzle.model.Difficulty
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.recordsDataStore: DataStore<Preferences> by preferencesDataStore(name = "records")

class DataStoreRecordsRepository(private val context: Context) : RecordsRepository {

    private fun keyFor(difficulty: Difficulty) = stringPreferencesKey("record_${difficulty.name}")

    override suspend fun getRecord(difficulty: Difficulty): DifficultyRecord {
        val json = context.recordsDataStore.data.map { it[keyFor(difficulty)] }.first()
            ?: return DifficultyRecord()
        return try {
            Json.decodeFromString(DifficultyRecord.serializer(), json)
        } catch (e: Exception) {
            DifficultyRecord()
        }
    }

    override suspend fun updateRecord(difficulty: Difficulty, record: DifficultyRecord) {
        val json = Json.encodeToString(DifficultyRecord.serializer(), record)
        context.recordsDataStore.edit { it[keyFor(difficulty)] = json }
    }

    override suspend fun getAllRecords(): Map<Difficulty, DifficultyRecord> {
        return Difficulty.entries.associateWith { getRecord(it) }
    }
}
```

- [ ] **Step 7: Verify build compiles**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/game/ app/src/test/java/com/interstitial/sudoku/game/
git commit -m "feat: add persistence layer (GameRepository, RecordsRepository, DataStore implementations)"
```

---

## Task 7: GameViewModel — Core Logic

**Files:**
- Create: `app/src/main/java/com/interstitial/sudoku/game/GameViewModel.kt`
- Create: `app/src/test/java/com/interstitial/sudoku/game/MainDispatcherRule.kt`
- Create: `app/src/test/java/com/interstitial/sudoku/game/FakeGameRepository.kt`
- Create: `app/src/test/java/com/interstitial/sudoku/game/FakeRecordsRepository.kt`
- Create: `app/src/test/java/com/interstitial/sudoku/game/GameViewModelTest.kt`

This is the largest task. The ViewModel handles: cell selection, digit placement, notes, erase, undo, hints, conflict recomputation, completion detection, elapsed time, save/resume, and records updates.

- [ ] **Step 1: Create test helpers**

`MainDispatcherRule.kt`:
```kotlin
package com.interstitial.sudoku.game

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    private val dispatcher = UnconfinedTestDispatcher()
    override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}
```

`FakeGameRepository.kt`:
```kotlin
package com.interstitial.sudoku.game

import com.interstitial.sudoku.game.model.PersistedGameState

class FakeGameRepository : GameRepository {
    private var saved: PersistedGameState? = null
    override suspend fun saveGame(state: PersistedGameState) { saved = state }
    override suspend fun loadGame(): PersistedGameState? = saved
    override suspend fun clearGame() { saved = null }
    override suspend fun hasSavedGame(): Boolean = saved != null
}
```

`FakeRecordsRepository.kt`:
```kotlin
package com.interstitial.sudoku.game

import com.interstitial.sudoku.game.model.DifficultyRecord
import com.interstitial.sudoku.puzzle.model.Difficulty

class FakeRecordsRepository : RecordsRepository {
    private val records = mutableMapOf<Difficulty, DifficultyRecord>()
    override suspend fun getRecord(difficulty: Difficulty) = records[difficulty] ?: DifficultyRecord()
    override suspend fun updateRecord(difficulty: Difficulty, record: DifficultyRecord) { records[difficulty] = record }
    override suspend fun getAllRecords() = Difficulty.entries.associateWith { getRecord(it) }
}
```

- [ ] **Step 2: Write GameViewModel tests**

```kotlin
package com.interstitial.sudoku.game

import app.cash.turbine.test
import com.interstitial.sudoku.game.model.GameAction
import com.interstitial.sudoku.game.model.GameEvent
import com.interstitial.sudoku.game.model.InputMode
import com.interstitial.sudoku.puzzle.engine.SudokuGenerator
import com.interstitial.sudoku.puzzle.model.Difficulty
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private lateinit var gameRepo: FakeGameRepository
    private lateinit var recordsRepo: FakeRecordsRepository
    private lateinit var viewModel: GameViewModel

    @Before
    fun setup() {
        gameRepo = FakeGameRepository()
        recordsRepo = FakeRecordsRepository()
        viewModel = GameViewModel(
            generator = SudokuGenerator(),
            gameRepository = gameRepo,
            recordsRepository = recordsRepo
        )
    }

    private fun startEasyGame() {
        viewModel.onAction(GameAction.NewGame(Difficulty.EASY))
        // Wait for generation to complete
        val state = viewModel.uiState.value
        assertFalse("Game should have finished generating", state.isGenerating)
        assertTrue("Board should have cells", state.board.any { it != 0 })
    }

    @Test
    fun `selecting a cell updates selectedCell`() {
        startEasyGame()
        viewModel.onAction(GameAction.SelectCell(40))
        assertEquals(40, viewModel.uiState.value.selectedCell)
    }

    @Test
    fun `selecting same cell again deselects`() {
        startEasyGame()
        viewModel.onAction(GameAction.SelectCell(40))
        viewModel.onAction(GameAction.SelectCell(40))
        assertNull(viewModel.uiState.value.selectedCell)
    }

    @Test
    fun `placing digit in fill mode updates board`() {
        startEasyGame()
        val state = viewModel.uiState.value
        // Find first empty non-given cell
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.PlaceDigit(1))
        assertEquals(1, viewModel.uiState.value.board[emptyCell])
    }

    @Test
    fun `placing digit on given cell is no-op`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val givenCell = state.givens.indices.first { state.givens[it] }
        val originalValue = state.board[givenCell]
        viewModel.onAction(GameAction.SelectCell(givenCell))
        viewModel.onAction(GameAction.PlaceDigit(if (originalValue == 1) 2 else 1))
        assertEquals(originalValue, viewModel.uiState.value.board[givenCell])
    }

    @Test
    fun `placing digit clears notes on that cell`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.ToggleInputMode) // switch to Notes
        viewModel.onAction(GameAction.ToggleNote(3))
        viewModel.onAction(GameAction.ToggleNote(7))
        assertTrue(viewModel.uiState.value.notes[emptyCell].containsAll(setOf(3, 7)))

        viewModel.onAction(GameAction.ToggleInputMode) // back to Fill
        viewModel.onAction(GameAction.PlaceDigit(5))
        assertTrue(viewModel.uiState.value.notes[emptyCell].isEmpty())
    }

    @Test
    fun `toggle note adds and removes candidate`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.ToggleInputMode) // Notes mode
        viewModel.onAction(GameAction.ToggleNote(5))
        assertTrue(5 in viewModel.uiState.value.notes[emptyCell])
        viewModel.onAction(GameAction.ToggleNote(5))
        assertFalse(5 in viewModel.uiState.value.notes[emptyCell])
    }

    @Test
    fun `notes support full 1-9 candidates`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.ToggleInputMode)
        for (d in 1..9) viewModel.onAction(GameAction.ToggleNote(d))
        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7, 8, 9), viewModel.uiState.value.notes[emptyCell])
    }

    @Test
    fun `erase removes player digit`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.PlaceDigit(3))
        assertEquals(3, viewModel.uiState.value.board[emptyCell])
        viewModel.onAction(GameAction.Erase)
        assertEquals(0, viewModel.uiState.value.board[emptyCell])
    }

    @Test
    fun `erase on given cell is no-op`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val givenCell = state.givens.indices.first { state.givens[it] }
        val originalValue = state.board[givenCell]
        viewModel.onAction(GameAction.SelectCell(givenCell))
        viewModel.onAction(GameAction.Erase)
        assertEquals(originalValue, viewModel.uiState.value.board[givenCell])
    }

    @Test
    fun `undo restores previous value`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.PlaceDigit(7))
        assertEquals(7, viewModel.uiState.value.board[emptyCell])
        viewModel.onAction(GameAction.Undo)
        assertEquals(0, viewModel.uiState.value.board[emptyCell])
    }

    @Test
    fun `undo with empty stack is no-op`() {
        startEasyGame()
        assertFalse(viewModel.uiState.value.hasUndo)
        viewModel.onAction(GameAction.Undo) // should not crash
    }

    @Test
    fun `toggle input mode switches between FILL and NOTES`() {
        startEasyGame()
        assertEquals(InputMode.FILL, viewModel.uiState.value.inputMode)
        viewModel.onAction(GameAction.ToggleInputMode)
        assertEquals(InputMode.NOTES, viewModel.uiState.value.inputMode)
        viewModel.onAction(GameAction.ToggleInputMode)
        assertEquals(InputMode.FILL, viewModel.uiState.value.inputMode)
    }

    @Test
    fun `hint on empty selected cell reveals solution`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        val expectedValue = state.solution[emptyCell]
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.Hint)
        assertEquals(expectedValue, viewModel.uiState.value.board[emptyCell])
        assertTrue(emptyCell in viewModel.uiState.value.hintedCells)
        assertEquals(1, viewModel.uiState.value.hintsUsed)
    }

    @Test
    fun `hinted cell cannot be edited`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.Hint)
        val hintedValue = viewModel.uiState.value.board[emptyCell]
        viewModel.onAction(GameAction.PlaceDigit(if (hintedValue == 1) 2 else 1))
        assertEquals(hintedValue, viewModel.uiState.value.board[emptyCell])
    }

    @Test
    fun `hint is not undoable`() {
        startEasyGame()
        val state = viewModel.uiState.value
        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        val undoBefore = viewModel.uiState.value.hasUndo
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.Hint)
        assertEquals(undoBefore, viewModel.uiState.value.hasUndo)
    }

    @Test
    fun `digit counts track placements`() {
        startEasyGame()
        val state = viewModel.uiState.value
        // Count initial 1s from givens
        val initialOnes = state.board.count { it == 1 }
        assertEquals(initialOnes, state.digitCounts[0]) // digitCounts[0] = count of digit 1

        val emptyCell = state.board.indices.first { state.board[it] == 0 && !state.givens[it] }
        viewModel.onAction(GameAction.SelectCell(emptyCell))
        viewModel.onAction(GameAction.PlaceDigit(1))
        assertEquals(initialOnes + 1, viewModel.uiState.value.digitCounts[0])
    }
}
```

- [ ] **Step 3: Run tests — verify they fail**

```bash
./gradlew test --tests "com.interstitial.sudoku.game.GameViewModelTest"
```

Expected: FAIL — `GameViewModel` class not found.

- [ ] **Step 4: Implement `GameViewModel.kt`**

```kotlin
package com.interstitial.sudoku.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interstitial.sudoku.game.model.*
import com.interstitial.sudoku.puzzle.engine.ConflictDetector
import com.interstitial.sudoku.puzzle.engine.SudokuGenerator
import com.interstitial.sudoku.puzzle.model.Difficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UndoEntry(
    val cellIndex: Int,
    val previousValue: Int,
    val previousNotes: Set<Int>
)

class GameViewModel(
    private val generator: SudokuGenerator = SudokuGenerator(),
    private val gameRepository: GameRepository,
    private val recordsRepository: RecordsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<GameEvent>()
    val events = _events.asSharedFlow()

    private val undoStack = ArrayDeque<UndoEntry>()

    private var timerStartRealtimeMs: Long? = null

    fun onAction(action: GameAction) {
        when (action) {
            is GameAction.NewGame -> startNewGame(action.difficulty)
            is GameAction.ResumeGame -> resumeGame()
            is GameAction.SelectCell -> selectCell(action.index)
            is GameAction.PlaceDigit -> placeDigit(action.digit)
            is GameAction.ToggleNote -> toggleNote(action.digit)
            is GameAction.Erase -> erase()
            is GameAction.Undo -> undo()
            is GameAction.Hint -> hint()
            is GameAction.ToggleInputMode -> toggleInputMode()
            is GameAction.PausePuzzle -> pausePuzzle()
            is GameAction.KeepForLater -> keepForLater()
            is GameAction.DiscardPuzzle -> discardPuzzle()
        }
    }

    private fun startNewGame(difficulty: Difficulty) {
        _uiState.value = GameUiState(isGenerating = true, difficulty = difficulty)
        undoStack.clear()
        viewModelScope.launch {
            val puzzle = withContext(Dispatchers.Default) {
                generator.generatePuzzle(difficulty)
            }
            val givens = BooleanArray(81) { puzzle.board[it] != 0 }
            val board = puzzle.board.copyOf()
            _uiState.value = GameUiState(
                board = board,
                solution = puzzle.solution.copyOf(),
                givens = givens,
                difficulty = difficulty,
                cellsRemaining = board.count { it == 0 },
                digitCounts = computeDigitCounts(board),
                conflictMask = ConflictDetector.buildConflictMask(board)
            )
            timerStartRealtimeMs = System.currentTimeMillis()
        }
    }

    private fun resumeGame() {
        viewModelScope.launch {
            val persisted = gameRepository.loadGame() ?: return@launch
            val board = persisted.board.toIntArray()
            val givens = persisted.givens.toBooleanArray()
            val notes = Array(81) { persisted.notes[it].toSet() }
            val difficulty = Difficulty.valueOf(persisted.difficulty)
            undoStack.clear()
            _uiState.value = GameUiState(
                board = board,
                solution = persisted.solution.toIntArray(),
                givens = givens,
                notes = notes,
                difficulty = difficulty,
                cellsRemaining = board.count { it == 0 },
                elapsedMs = persisted.elapsedMs,
                hintsUsed = persisted.hintsUsed,
                hintedCells = persisted.hintedCells.toSet(),
                digitCounts = computeDigitCounts(board),
                conflictMask = ConflictDetector.buildConflictMask(board)
            )
            timerStartRealtimeMs = System.currentTimeMillis()
        }
    }

    private fun selectCell(index: Int) {
        val current = _uiState.value
        _uiState.value = current.copy(
            selectedCell = if (current.selectedCell == index) null else index
        )
    }

    private fun placeDigit(digit: Int) {
        val state = _uiState.value
        val cell = state.selectedCell ?: return
        if (state.givens[cell] || cell in state.hintedCells) return

        if (state.inputMode == InputMode.NOTES) {
            toggleNote(digit)
            return
        }

        val prevValue = state.board[cell]
        val prevNotes = state.notes[cell]
        undoStack.addLast(UndoEntry(cell, prevValue, prevNotes))

        val newBoard = state.board.copyOf()
        newBoard[cell] = digit
        val newNotes = state.notes.copyOf()
        newNotes[cell] = emptySet()

        val newCellsRemaining = newBoard.count { it == 0 }
        val newConflictMask = ConflictDetector.buildConflictMask(newBoard)
        val newDigitCounts = computeDigitCounts(newBoard)
        val isComplete = newCellsRemaining == 0 && newConflictMask.none { it }

        _uiState.value = state.copy(
            board = newBoard,
            notes = newNotes,
            cellsRemaining = newCellsRemaining,
            conflictMask = newConflictMask,
            digitCounts = newDigitCounts,
            hasUndo = true,
            isComplete = isComplete
        )

        if (isComplete) onCompletion()
    }

    private fun toggleNote(digit: Int) {
        val state = _uiState.value
        val cell = state.selectedCell ?: return
        if (state.givens[cell] || cell in state.hintedCells) return
        if (state.board[cell] != 0) return // can't add notes to a filled cell

        val currentNotes = state.notes[cell]
        val wasPresent = digit in currentNotes
        undoStack.addLast(UndoEntry(cell, state.board[cell], currentNotes))

        val newNotes = state.notes.copyOf()
        newNotes[cell] = if (wasPresent) currentNotes - digit else currentNotes + digit

        _uiState.value = state.copy(notes = newNotes, hasUndo = true)
    }

    private fun erase() {
        val state = _uiState.value
        val cell = state.selectedCell ?: return
        if (state.givens[cell] || cell in state.hintedCells) return

        val prevValue = state.board[cell]
        val prevNotes = state.notes[cell]
        if (prevValue == 0 && prevNotes.isEmpty()) return

        undoStack.addLast(UndoEntry(cell, prevValue, prevNotes))

        val newBoard = state.board.copyOf()
        newBoard[cell] = 0
        val newNotes = state.notes.copyOf()
        newNotes[cell] = emptySet()

        _uiState.value = state.copy(
            board = newBoard,
            notes = newNotes,
            cellsRemaining = newBoard.count { it == 0 },
            conflictMask = ConflictDetector.buildConflictMask(newBoard),
            digitCounts = computeDigitCounts(newBoard),
            hasUndo = true
        )
    }

    private fun undo() {
        if (undoStack.isEmpty()) return
        val entry = undoStack.removeLast()
        val state = _uiState.value

        val newBoard = state.board.copyOf()
        newBoard[entry.cellIndex] = entry.previousValue
        val newNotes = state.notes.copyOf()
        newNotes[entry.cellIndex] = entry.previousNotes

        _uiState.value = state.copy(
            board = newBoard,
            notes = newNotes,
            cellsRemaining = newBoard.count { it == 0 },
            conflictMask = ConflictDetector.buildConflictMask(newBoard),
            digitCounts = computeDigitCounts(newBoard),
            hasUndo = undoStack.isNotEmpty()
        )
    }

    private fun hint() {
        val state = _uiState.value
        val cell = state.selectedCell
        if (cell == null || state.givens[cell] || cell in state.hintedCells) {
            viewModelScope.launch {
                _events.emit(GameEvent.HintUnavailable("Select a cell to reveal"))
            }
            return
        }
        // If cell already has the correct value, no-op
        if (state.board[cell] == state.solution[cell]) {
            viewModelScope.launch {
                _events.emit(GameEvent.HintUnavailable("Select a cell to reveal"))
            }
            return
        }

        val newBoard = state.board.copyOf()
        newBoard[cell] = state.solution[cell]
        val newNotes = state.notes.copyOf()
        newNotes[cell] = emptySet()

        val newCellsRemaining = newBoard.count { it == 0 }
        val newConflictMask = ConflictDetector.buildConflictMask(newBoard)
        val isComplete = newCellsRemaining == 0 && newConflictMask.none { it }

        _uiState.value = state.copy(
            board = newBoard,
            notes = newNotes,
            hintedCells = state.hintedCells + cell,
            hintsUsed = state.hintsUsed + 1,
            cellsRemaining = newCellsRemaining,
            conflictMask = newConflictMask,
            digitCounts = computeDigitCounts(newBoard),
            isComplete = isComplete
        )
        // Hint does NOT push to undoStack

        if (isComplete) onCompletion()
    }

    private fun toggleInputMode() {
        val state = _uiState.value
        _uiState.value = state.copy(
            inputMode = if (state.inputMode == InputMode.FILL) InputMode.NOTES else InputMode.FILL
        )
    }

    private fun pausePuzzle() {
        updateElapsedTime()
        _uiState.value = _uiState.value.copy(isPaused = true)
    }

    private fun keepForLater() {
        viewModelScope.launch {
            updateElapsedTime()
            saveCurrentGame()
        }
    }

    private fun discardPuzzle() {
        viewModelScope.launch {
            gameRepository.clearGame()
        }
    }

    fun saveOnStop() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.isComplete && state.board.any { it != 0 }) {
                updateElapsedTime()
                saveCurrentGame()
            }
        }
    }

    private suspend fun saveCurrentGame() {
        val state = _uiState.value
        gameRepository.saveGame(
            PersistedGameState(
                board = state.board.toList(),
                solution = state.solution.toList(),
                givens = state.givens.toList(),
                notes = state.notes.map { it.toList() },
                difficulty = state.difficulty.name,
                elapsedMs = state.elapsedMs,
                hintsUsed = state.hintsUsed,
                hintedCells = state.hintedCells.toList()
            )
        )
    }

    private fun updateElapsedTime() {
        val start = timerStartRealtimeMs ?: return
        val now = System.currentTimeMillis()
        val delta = now - start
        _uiState.value = _uiState.value.copy(elapsedMs = _uiState.value.elapsedMs + delta)
        timerStartRealtimeMs = now
    }

    private fun onCompletion() {
        updateElapsedTime()
        timerStartRealtimeMs = null
        val state = _uiState.value
        viewModelScope.launch {
            gameRepository.clearGame()
            val record = recordsRepository.getRecord(state.difficulty)
            val isNewBest = record.bestTimeMs == null || state.elapsedMs < record.bestTimeMs
            val isNewNoHintBest = state.hintsUsed == 0 &&
                (record.bestNoHintTimeMs == null || state.elapsedMs < record.bestNoHintTimeMs)
            val updated = record.copy(
                completedCount = record.completedCount + 1,
                bestTimeMs = if (isNewBest) state.elapsedMs else record.bestTimeMs,
                bestNoHintTimeMs = if (isNewNoHintBest) state.elapsedMs else record.bestNoHintTimeMs,
                lastCompletedEpochMs = System.currentTimeMillis()
            )
            recordsRepository.updateRecord(state.difficulty, updated)
            _events.emit(
                GameEvent.Completed(
                    difficulty = state.difficulty,
                    elapsedMs = state.elapsedMs,
                    hintsUsed = state.hintsUsed,
                    isPersonalBest = isNewBest
                )
            )
        }
    }

    private fun computeDigitCounts(board: IntArray): IntArray {
        val counts = IntArray(9)
        for (v in board) {
            if (v in 1..9) counts[v - 1]++
        }
        return counts
    }
}
```

- [ ] **Step 5: Run tests — verify they pass**

```bash
./gradlew test --tests "com.interstitial.sudoku.game.GameViewModelTest"
```

Expected: All tests PASS. If any fail due to timing or async issues, adjust the test setup. The `UnconfinedTestDispatcher` should make coroutines execute immediately.

**Note:** The `startEasyGame()` helper calls `NewGame` which launches a coroutine. With `UnconfinedTestDispatcher`, the generation should complete synchronously. If `isGenerating` is still true after the action, the generator is running on `Dispatchers.Default` which isn't overridden. In that case, change the ViewModel to accept a `CoroutineDispatcher` parameter for generation, or use `advanceUntilIdle()` in tests. Adjust as needed.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/game/GameViewModel.kt app/src/test/java/com/interstitial/sudoku/game/
git commit -m "feat: implement GameViewModel with full game logic, undo, hints, conflict detection"
```

---

## Task 8: Theme

**Files:**
- Create: `app/src/main/java/com/interstitial/sudoku/ui/theme/Theme.kt`

- [ ] **Step 1: Create `Theme.kt`**

```kotlin
package com.interstitial.sudoku.ui.theme

import androidx.compose.runtime.Composable
import com.mudita.mmd.theme.ThemeMMD
import com.mudita.mmd.theme.eInkColorScheme
import com.mudita.mmd.theme.eInkTypography

@Composable
fun KompaktSudokuTheme(content: @Composable () -> Unit) {
    ThemeMMD(
        colorScheme = eInkColorScheme,
        typography = eInkTypography,
        content = content
    )
}
```

**Note:** The MMD import paths shown here are based on the v1 project. If the actual paths differ (e.g., the package might be `com.mudita.mmd.` or `com.mudita.mindfuldesign.`), check the MMD library source or v1's working imports. The key is: use `ThemeMMD` with `eInkColorScheme` and `eInkTypography` — no customization.

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew assembleDebug
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/theme/
git commit -m "feat: add KompaktSudokuTheme wrapping MMD eInkColorScheme + eInkTypography"
```

---

## Task 9: UI — HomeScreen

**Files:**
- Create: `app/src/main/java/com/interstitial/sudoku/ui/home/HomeScreen.kt`

- [ ] **Step 1: Implement `HomeScreen.kt`**

```kotlin
package com.interstitial.sudoku.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.HorizontalDividerMMD
import com.mudita.mmd.components.TextMMD

@Composable
fun HomeScreen(
    hasSavedGame: Boolean,
    savedGameDifficulty: String,
    savedGameCellsLeft: Int,
    onContinue: () -> Unit,
    onNewPuzzle: () -> Unit,
    onRecords: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(top = 32.dp)) {
        TextMMD(
            text = "Sudoku",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )
        HorizontalDividerMMD(thickness = 2.dp)

        if (hasSavedGame) {
            MenuRow(
                title = "Continue puzzle",
                subtitle = "$savedGameDifficulty \u00B7 $savedGameCellsLeft cells left",
                onClick = onContinue
            )
            HorizontalDividerMMD()
        }

        MenuRow(
            title = "New puzzle",
            subtitle = "Choose easy, medium, or hard",
            onClick = onNewPuzzle
        )
        HorizontalDividerMMD()

        MenuRow(
            title = "Records",
            subtitle = "Completed puzzles and best times",
            onClick = onRecords
        )
        HorizontalDividerMMD()
    }
}

@Composable
private fun MenuRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        TextMMD(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        TextMMD(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
```

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew assembleDebug
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/home/
git commit -m "feat: add HomeScreen with list-based menu rows"
```

---

## Task 10: UI — NewPuzzleScreen

**Files:**
- Create: `app/src/main/java/com/interstitial/sudoku/ui/newpuzzle/NewPuzzleScreen.kt`

- [ ] **Step 1: Implement `NewPuzzleScreen.kt`**

```kotlin
package com.interstitial.sudoku.ui.newpuzzle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.mudita.mmd.components.HorizontalDividerMMD
import com.mudita.mmd.components.TextMMD
import com.mudita.mmd.components.TopAppBarMMD

@Composable
fun NewPuzzleScreen(
    isGenerating: Boolean,
    onDifficultySelected: (Difficulty) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBarMMD(
            title = { TextMMD("New puzzle") },
            navigationIcon = {
                TextMMD(
                    text = "←",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .clickable(onClick = onBack)
                        .padding(16.dp)
                )
            }
        )

        if (isGenerating) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                TextMMD(
                    text = "Preparing puzzle\u2026",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            DifficultyRow(
                title = "Easy",
                subtitle = "More givens, shorter sessions",
                onClick = { onDifficultySelected(Difficulty.EASY) }
            )
            HorizontalDividerMMD()
            DifficultyRow(
                title = "Medium",
                subtitle = "Balanced deduction",
                onClick = { onDifficultySelected(Difficulty.MEDIUM) }
            )
            HorizontalDividerMMD()
            DifficultyRow(
                title = "Hard",
                subtitle = "Fewer givens, deeper focus",
                onClick = { onDifficultySelected(Difficulty.HARD) }
            )
            HorizontalDividerMMD()
        }
    }
}

@Composable
private fun DifficultyRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        TextMMD(text = title, style = MaterialTheme.typography.titleMedium)
        TextMMD(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/newpuzzle/
git commit -m "feat: add NewPuzzleScreen with difficulty rows and loading state"
```

---

## Task 11: UI — Game Screen Components

**Files:**
- Create: `app/src/main/java/com/interstitial/sudoku/ui/game/PuzzleTopBar.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/ui/game/PuzzleMetaStrip.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/ui/game/InputModeToggle.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/ui/game/PuzzleActionRow.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/ui/game/DigitPad.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/ui/game/LeavePuzzleDialog.kt`

- [ ] **Step 1: Create `PuzzleTopBar.kt`**

```kotlin
package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.mudita.mmd.components.TextMMD
import com.mudita.mmd.components.TopAppBarMMD

@Composable
fun PuzzleTopBar(
    difficulty: Difficulty,
    onBack: () -> Unit
) {
    val title = when (difficulty) {
        Difficulty.EASY -> "Easy puzzle"
        Difficulty.MEDIUM -> "Medium puzzle"
        Difficulty.HARD -> "Hard puzzle"
    }
    TopAppBarMMD(
        title = { TextMMD(title) },
        navigationIcon = {
            TextMMD(
                text = "←",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(16.dp)
            )
        }
    )
}
```

- [ ] **Step 2: Create `PuzzleMetaStrip.kt`**

```kotlin
package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.TextMMD

@Composable
fun PuzzleMetaStrip(cellsRemaining: Int, modifier: Modifier = Modifier) {
    TextMMD(
        text = "$cellsRemaining cells left",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
```

- [ ] **Step 3: Create `InputModeToggle.kt`**

```kotlin
package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.game.model.InputMode
import com.mudita.mmd.components.TextMMD

@Composable
fun InputModeToggle(
    currentMode: InputMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(48.dp)
            .border(2.dp, onSurface)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (currentMode == InputMode.FILL) onSurface else surface)
                .clickable(onClick = { if (currentMode != InputMode.FILL) onToggle() }),
            contentAlignment = Alignment.Center
        ) {
            TextMMD(
                text = "Fill",
                style = MaterialTheme.typography.labelLarge,
                color = if (currentMode == InputMode.FILL) surface else onSurface
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (currentMode == InputMode.NOTES) onSurface else surface)
                .clickable(onClick = { if (currentMode != InputMode.NOTES) onToggle() }),
            contentAlignment = Alignment.Center
        ) {
            TextMMD(
                text = "Notes",
                style = MaterialTheme.typography.labelLarge,
                color = if (currentMode == InputMode.NOTES) surface else onSurface
            )
        }
    }
}
```

- [ ] **Step 4: Create `PuzzleActionRow.kt`**

```kotlin
package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.ButtonMMD

@Composable
fun PuzzleActionRow(
    hasUndo: Boolean,
    canErase: Boolean,
    canHint: Boolean,
    onUndo: () -> Unit,
    onErase: () -> Unit,
    onHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ButtonMMD(
            onClick = onUndo,
            enabled = hasUndo,
            modifier = Modifier.weight(1f).height(48.dp)
        ) { com.mudita.mmd.components.TextMMD("Undo") }

        ButtonMMD(
            onClick = onErase,
            enabled = canErase,
            modifier = Modifier.weight(1f).height(48.dp)
        ) { com.mudita.mmd.components.TextMMD("Erase") }

        ButtonMMD(
            onClick = onHint,
            enabled = canHint,
            modifier = Modifier.weight(1f).height(48.dp)
        ) { com.mudita.mmd.components.TextMMD("Hint") }
    }
}
```

- [ ] **Step 5: Create `DigitPad.kt`**

```kotlin
package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.ButtonMMD
import com.mudita.mmd.components.TextMMD

@Composable
fun DigitPad(
    digitCounts: IntArray,
    onDigit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (row in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0..2) {
                    val digit = row * 3 + col + 1
                    val isDisabled = digitCounts[digit - 1] >= 9
                    ButtonMMD(
                        onClick = { onDigit(digit) },
                        enabled = !isDisabled,
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        TextMMD(
                            text = digit.toString(),
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 6: Create `LeavePuzzleDialog.kt`**

```kotlin
package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.ButtonMMD
import com.mudita.mmd.components.TextMMD

@Composable
fun LeavePuzzleDialog(
    onKeepForLater: () -> Unit,
    onDiscard: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            TextMMD(
                text = "Leave puzzle?",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            ButtonMMD(
                onClick = onKeepForLater,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { TextMMD("Keep for later") }

            Spacer(modifier = Modifier.height(12.dp))

            ButtonMMD(
                onClick = onDiscard,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { TextMMD("Discard puzzle") }
        }
    }
}
```

- [ ] **Step 7: Verify build compiles**

```bash
./gradlew assembleDebug
```

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/game/
git commit -m "feat: add game screen components (TopBar, MetaStrip, ModeToggle, ActionRow, DigitPad, LeaveDialog)"
```

---

## Task 12: UI — SudokuGrid Canvas

**Files:**
- Create: `app/src/main/java/com/interstitial/sudoku/ui/game/SudokuGrid.kt`

This is the most complex UI component. Single Canvas with touch detection, grid lines, digits, notes, selection, and conflict indicators.

- [ ] **Step 1: Implement `SudokuGrid.kt`**

```kotlin
package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.sp
import com.interstitial.sudoku.game.model.GameUiState

@OptIn(ExperimentalTextApi::class)
@Composable
fun SudokuGrid(
    state: GameUiState,
    onCellTap: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val textMeasurer = rememberTextMeasurer()
    var lastTapTime by remember { mutableLongStateOf(0L) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime > 200) {
                        lastTapTime = now
                        val cellSize = size.width / 9f
                        val col = (offset.x / cellSize).toInt().coerceIn(0, 8)
                        val row = (offset.y / cellSize).toInt().coerceIn(0, 8)
                        onCellTap(row, col)
                    }
                }
            }
    ) {
        val cellSize = size.width / 9f
        val givenStyle = TextStyle(fontSize = (cellSize * 0.55f).toSp(), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = onSurface)
        val playerStyle = TextStyle(fontSize = (cellSize * 0.55f).toSp(), color = onSurface)
        val selectedStyle = TextStyle(fontSize = (cellSize * 0.55f).toSp(), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = surface)
        val noteStyle = TextStyle(fontSize = (cellSize * 0.2f).toSp(), color = onSurfaceVariant)
        val selectedNoteStyle = TextStyle(fontSize = (cellSize * 0.2f).toSp(), color = surface)

        // 1. Draw cell backgrounds
        for (i in 0 until 81) {
            val r = i / 9
            val c = i % 9
            val x = c * cellSize
            val y = r * cellSize

            if (state.selectedCell == i) {
                drawRect(onSurface, Offset(x, y), Size(cellSize, cellSize))
            }
        }

        // 2. Draw grid lines
        // Cell borders (1dp)
        for (i in 0..9) {
            val pos = i * cellSize
            drawLine(outlineVariant, Offset(pos, 0f), Offset(pos, size.height), strokeWidth = 1f)
            drawLine(outlineVariant, Offset(0f, pos), Offset(size.width, pos), strokeWidth = 1f)
        }
        // Box borders (3dp)
        for (i in 0..3) {
            val pos = i * 3 * cellSize
            drawLine(onSurface, Offset(pos, 0f), Offset(pos, size.height), strokeWidth = 3f)
            drawLine(onSurface, Offset(0f, pos), Offset(size.width, pos), strokeWidth = 3f)
        }

        // 3. Draw digits and notes
        for (i in 0 until 81) {
            val r = i / 9
            val c = i % 9
            val x = c * cellSize
            val y = r * cellSize
            val isSelected = state.selectedCell == i
            val value = state.board[i]

            if (value != 0) {
                val style = when {
                    isSelected -> selectedStyle
                    state.givens[i] || i in state.hintedCells -> givenStyle
                    else -> playerStyle
                }
                val measured = textMeasurer.measure(value.toString(), style)
                drawText(
                    measured,
                    topLeft = Offset(
                        x + (cellSize - measured.size.width) / 2f,
                        y + (cellSize - measured.size.height) / 2f
                    )
                )
            } else {
                // Draw pencil marks in 3x3 micro-grid
                val notes = state.notes[i]
                if (notes.isNotEmpty()) {
                    val noteSize = cellSize / 3f
                    for (d in notes) {
                        val nr = (d - 1) / 3
                        val nc = (d - 1) % 3
                        val style = if (isSelected) selectedNoteStyle else noteStyle
                        val measured = textMeasurer.measure(d.toString(), style)
                        drawText(
                            measured,
                            topLeft = Offset(
                                x + nc * noteSize + (noteSize - measured.size.width) / 2f,
                                y + nr * noteSize + (noteSize - measured.size.height) / 2f
                            )
                        )
                    }
                }
            }

            // 4. Conflict indicators (inset corner marks)
            if (state.conflictMask[i] && value != 0) {
                val inset = cellSize * 0.1f
                val markLen = cellSize * 0.2f
                val markColor = if (isSelected) surface else onSurface
                // Top-left corner mark
                drawLine(markColor, Offset(x + inset, y + inset), Offset(x + inset + markLen, y + inset), strokeWidth = 2f)
                drawLine(markColor, Offset(x + inset, y + inset), Offset(x + inset, y + inset + markLen), strokeWidth = 2f)
                // Bottom-right corner mark
                val bx = x + cellSize - inset
                val by = y + cellSize - inset
                drawLine(markColor, Offset(bx - markLen, by), Offset(bx, by), strokeWidth = 2f)
                drawLine(markColor, Offset(bx, by - markLen), Offset(bx, by), strokeWidth = 2f)
            }
        }
    }
}

private fun Float.toSp(): androidx.compose.ui.unit.TextUnit = (this / 3f).sp
```

**Note:** The `toSp()` helper is approximate — on the actual device, the density ratio will differ. The executing agent should verify that digit sizes look right and adjust the scaling factor. The key constraint is: given digits must be bold, player digits regular weight, and notes must be legible in the 3×3 micro-grid.

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew assembleDebug
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/game/SudokuGrid.kt
git commit -m "feat: add Canvas-based SudokuGrid with digits, notes, selection, and conflict indicators"
```

---

## Task 13: UI — GameScreen Orchestrator

**Files:**
- Create: `app/src/main/java/com/interstitial/sudoku/ui/game/GameScreen.kt`

- [ ] **Step 1: Implement `GameScreen.kt`**

```kotlin
package com.interstitial.sudoku.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.game.model.GameAction
import com.interstitial.sudoku.game.model.GameUiState
import com.interstitial.sudoku.game.model.InputMode
import com.mudita.mmd.components.HorizontalDividerMMD
import com.mudita.mmd.components.SnackbarHostStateMMD

@Composable
fun GameScreen(
    state: GameUiState,
    snackbarHostState: SnackbarHostStateMMD,
    onAction: (GameAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLeaveDialog by remember { mutableStateOf(false) }

    if (showLeaveDialog) {
        LeavePuzzleDialog(
            onKeepForLater = {
                showLeaveDialog = false
                onAction(GameAction.KeepForLater)
            },
            onDiscard = {
                showLeaveDialog = false
                onAction(GameAction.DiscardPuzzle)
            }
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PuzzleTopBar(
                difficulty = state.difficulty,
                onBack = {
                    onAction(GameAction.PausePuzzle)
                    showLeaveDialog = true
                }
            )
            PuzzleMetaStrip(cellsRemaining = state.cellsRemaining)
            HorizontalDividerMMD(thickness = 2.dp)

            SudokuGrid(
                state = state,
                onCellTap = { row, col -> onAction(GameAction.SelectCell(row * 9 + col)) },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            HorizontalDividerMMD(thickness = 2.dp)

            InputModeToggle(
                currentMode = state.inputMode,
                onToggle = { onAction(GameAction.ToggleInputMode) },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            PuzzleActionRow(
                hasUndo = state.hasUndo,
                canErase = state.selectedCell != null &&
                    state.selectedCell !in state.hintedCells.map { it } &&
                    state.selectedCell?.let { !state.givens[it] } == true,
                canHint = state.selectedCell != null,
                onUndo = { onAction(GameAction.Undo) },
                onErase = { onAction(GameAction.Erase) },
                onHint = { onAction(GameAction.Hint) },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDividerMMD()

            DigitPad(
                digitCounts = state.digitCounts,
                onDigit = { digit ->
                    if (state.inputMode == InputMode.NOTES) {
                        onAction(GameAction.ToggleNote(digit))
                    } else {
                        onAction(GameAction.PlaceDigit(digit))
                    }
                },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.statusBarsPadding()
        )
    }
}
```

**Note:** The `SnackbarHost` usage with `SnackbarHostStateMMD` may need adjustment depending on the actual MMD API. Check the MMD library for the correct `SnackbarHost` composable — it might be `SnackbarHostMMD` or the standard `SnackbarHost` accepting `SnackbarHostStateMMD`. Adapt accordingly.

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew assembleDebug
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/game/GameScreen.kt
git commit -m "feat: add GameScreen orchestrating all game sub-components"
```

---

## Task 14: UI — SummaryScreen & RecordsScreen

**Files:**
- Create: `app/src/main/java/com/interstitial/sudoku/ui/summary/SummaryScreen.kt`
- Create: `app/src/main/java/com/interstitial/sudoku/ui/records/RecordsScreen.kt`

- [ ] **Step 1: Implement `SummaryScreen.kt`**

```kotlin
package com.interstitial.sudoku.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.mudita.mmd.components.ButtonMMD
import com.mudita.mmd.components.HorizontalDividerMMD
import com.mudita.mmd.components.TextMMD
import com.mudita.mmd.components.TopAppBarMMD

@Composable
fun SummaryScreen(
    difficulty: Difficulty,
    elapsedMs: Long,
    hintsUsed: Int,
    isPersonalBest: Boolean,
    onNewPuzzle: () -> Unit,
    onBackToMenu: () -> Unit,
    onViewRecords: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBarMMD(
            title = { TextMMD("Puzzle complete") }
        )

        StatRow("Difficulty", difficulty.name.lowercase().replaceFirstChar { it.uppercase() })
        HorizontalDividerMMD()
        StatRow("Time", formatTime(elapsedMs))
        HorizontalDividerMMD()
        StatRow("Hints used", hintsUsed.toString())
        HorizontalDividerMMD()

        if (isPersonalBest) {
            TextMMD(
                text = "New personal record",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
            HorizontalDividerMMD(thickness = 2.dp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        ButtonMMD(
            onClick = onNewPuzzle,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp)
        ) { TextMMD("New puzzle") }

        Spacer(modifier = Modifier.height(8.dp))

        ButtonMMD(
            onClick = onBackToMenu,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp)
        ) { TextMMD("Back to menu") }

        Spacer(modifier = Modifier.height(8.dp))

        ButtonMMD(
            onClick = onViewRecords,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp)
        ) { TextMMD("View records") }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextMMD(text = label, style = MaterialTheme.typography.bodyLarge)
        TextMMD(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
```

- [ ] **Step 2: Implement `RecordsScreen.kt`**

```kotlin
package com.interstitial.sudoku.ui.records

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.interstitial.sudoku.game.model.DifficultyRecord
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.mudita.mmd.components.HorizontalDividerMMD
import com.mudita.mmd.components.TextMMD
import com.mudita.mmd.components.TopAppBarMMD

@Composable
fun RecordsScreen(
    records: Map<Difficulty, DifficultyRecord>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBarMMD(
            title = { TextMMD("Records") },
            navigationIcon = {
                TextMMD(
                    text = "←",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.clickable(onClick = onBack).padding(16.dp)
                )
            }
        )

        for (difficulty in Difficulty.entries) {
            val record = records[difficulty] ?: DifficultyRecord()
            DifficultySection(difficulty, record)
        }
    }
}

@Composable
private fun DifficultySection(difficulty: Difficulty, record: DifficultyRecord) {
    TextMMD(
        text = difficulty.name.lowercase().replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
    HorizontalDividerMMD()
    RecordRow("Completed", if (record.completedCount > 0) record.completedCount.toString() else "\u2014")
    HorizontalDividerMMD()
    RecordRow("Best time", record.bestTimeMs?.let { formatTime(it) } ?: "\u2014")
    HorizontalDividerMMD()
    RecordRow("Best no-hint", record.bestNoHintTimeMs?.let { formatTime(it) } ?: "\u2014")
    HorizontalDividerMMD(thickness = 2.dp)
}

@Composable
private fun RecordRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextMMD(text = label, style = MaterialTheme.typography.bodyMedium)
        TextMMD(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
```

- [ ] **Step 3: Verify build compiles**

```bash
./gradlew assembleDebug
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/ui/summary/ app/src/main/java/com/interstitial/sudoku/ui/records/
git commit -m "feat: add SummaryScreen and RecordsScreen"
```

---

## Task 15: MainActivity — Navigation & Wiring

**Files:**
- Modify: `app/src/main/java/com/interstitial/sudoku/MainActivity.kt`

- [ ] **Step 1: Implement full `MainActivity.kt`**

Replace the minimal placeholder with the full navigation wiring:

```kotlin
package com.interstitial.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.interstitial.sudoku.game.DataStoreGameRepository
import com.interstitial.sudoku.game.DataStoreRecordsRepository
import com.interstitial.sudoku.game.GameViewModel
import com.interstitial.sudoku.game.model.GameAction
import com.interstitial.sudoku.game.model.GameEvent
import com.interstitial.sudoku.puzzle.model.Difficulty
import com.interstitial.sudoku.ui.game.GameScreen
import com.interstitial.sudoku.ui.home.HomeScreen
import com.interstitial.sudoku.ui.newpuzzle.NewPuzzleScreen
import com.interstitial.sudoku.ui.records.RecordsScreen
import com.interstitial.sudoku.ui.summary.SummaryScreen
import com.interstitial.sudoku.ui.theme.KompaktSudokuTheme
import com.mudita.mmd.components.SnackbarHostStateMMD
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
                var records by remember { mutableStateOf(emptyMap<Difficulty, com.interstitial.sudoku.game.model.DifficultyRecord>()) }
                val snackbarHostState = remember { SnackbarHostStateMMD() }
                val scope = rememberCoroutineScope()

                // Check for saved game on launch
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

                // Observe game events
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
```

**Note:** The `SnackbarHostStateMMD` constructor and `showSnackbar` API may differ from standard Material3. Check the MMD library for the correct API. If `SnackbarHostStateMMD` doesn't exist as a standalone class, use the standard `SnackbarHostState` from Material3 — this is the one area where MMD may not have a custom equivalent.

- [ ] **Step 2: Verify build compiles**

```bash
./gradlew assembleDebug
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/interstitial/sudoku/MainActivity.kt
git commit -m "feat: wire MainActivity with sealed class routing and full navigation"
```

---

## Task 16: Run All Tests & Final Verification

**Files:** None (verification only)

- [ ] **Step 1: Run all unit tests**

```bash
./gradlew test
```

Expected: All tests PASS. Fix any failures.

- [ ] **Step 2: Run full build**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Verify no animations or ripple re-enabling**

Search the codebase for prohibited patterns:

```bash
grep -r "animateXAsState\|AnimatedVisibility\|Crossfade\|animateContentSize\|ripple\|indication" app/src/main/
```

Expected: No matches.

- [ ] **Step 4: Verify no raw Material3 components**

Search for `Text(` or `Button(` that aren't `TextMMD(` or `ButtonMMD(`:

```bash
grep -rn "import androidx.compose.material3.Text$\|import androidx.compose.material3.Button$" app/src/main/
```

Expected: No matches (all text/button usage should be via MMD).

**Note:** Some composables may use `MaterialTheme.typography` or `MaterialTheme.colorScheme` — that's fine, those are theme queries not components. The rule is specifically about `Text()`, `Button()`, `TopAppBar()`, `Divider()`, `LazyColumn()` composables.

- [ ] **Step 5: Commit any fixes**

```bash
git add -A
git commit -m "fix: address test failures and component compliance issues"
```

Only create this commit if there were actual fixes needed.

---

## MMD API Adaptation Notes

The code in this plan uses MMD import paths based on the v1 project and MMD documentation. The actual API surface of MMD 1.0.1 may differ in:

1. **Import paths** — might be `com.mudita.mmd.components.*` or `com.mudita.mmd.theme.*` or something else. Check v1's working imports or the MMD library source.
2. **`TextMMD` parameters** — may not accept `color` or `fontWeight` directly. Might need to pass them via `style` parameter instead.
3. **`ButtonMMD` content** — may expect a different content lambda signature.
4. **`TopAppBarMMD`** — parameter names for `title` and `navigationIcon` may differ.
5. **`SnackbarHostStateMMD`** — may not exist; fall back to standard `SnackbarHostState` if needed.
6. **`LazyColumnMMD`** — referenced in the spec but not used in the current screens (they're short enough for regular `Column`). If scroll is needed, add it.
7. **`HorizontalDividerMMD`** — the `thickness` parameter may be named differently.

The executing agent should check v1's imports and adapt. The key principle: use every MMD component available, never fall back to raw Material3 equivalents.
