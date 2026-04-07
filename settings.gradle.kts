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
