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
        // MMD (Mudita Mindful Design) — hosted on GitHub Packages
        // Requires githubToken in ~/.gradle/gradle.properties with read:packages scope
        // See: https://github.com/mudita/MMD
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
        } else {
            // Fail fast if MMD is not available from any source.
            // When githubToken is absent, only Maven Central or the Gradle cache can provide com.mudita:MMD.
            // If neither has it, the build will fail at dependency resolution
            // with a "could not find com.mudita:MMD:1.0.1" error — that is the expected behavior.
            // To fix: add githubToken=ghp_YOUR_TOKEN to ~/.gradle/gradle.properties
            logger.warn(
                "WARNING: githubToken not set in gradle.properties. " +
                "MMD (com.mudita:MMD) will only resolve from Maven Central or Gradle cache. " +
                "If MMD is not available from either source, add githubToken=<GitHub PAT with read:packages> " +
                "to ~/.gradle/gradle.properties"
            )
        }
    }
}

rootProject.name = "MuditaSudoku"
include(":app")
