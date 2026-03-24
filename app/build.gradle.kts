plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mudita.sudoku"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mudita.sudoku"
        minSdk = 31
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    testOptions {
        unitTests {
            // Required for Robolectric to access merged Android resources and manifests
            // (e.g., ComponentActivity registration for Compose UI tests with createComposeRule)
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
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose UI
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Activity (ComponentActivity + setContent)
    implementation(libs.androidx.activity.compose)

    // Lifecycle / ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.coroutines.android)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Sudoklify — puzzle generation
    implementation(libs.sudoklify.core)
    implementation(libs.sudoklify.presets)

    // MMD — Mudita Mindful Design E-ink UI components (D-09)
    // compileOnly(libs.mmd) — commented out because both known MMD repos are inaccessible:
    //   GitHub Packages (maven.pkg.github.com/mudita/MMD) returns 401 Unauthorized
    //   JFrog instance (mudita.jfrog.io/artifactory/mmd-release) appears deactivated
    //
    // Local stubs for MMD components (ThemeMMD, ButtonMMD, TextMMD) are provided in:
    //   app/src/main/java/com/mudita/mmd/MmdComponents.kt
    // These allow the project to compile and tests to run without credentials.
    // Replace with: implementation(libs.mmd) once the AAR is accessible, and delete the stubs.
    // compileOnly(libs.mmd)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.compose.ui.test.manifest)

    // Compose UI tests
    androidTestImplementation(libs.compose.ui.test.junit4)
}
