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
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ForkSure"
include(":app")

// The secrets plugin fails configuration when the gitignored local.properties is missing.
// GitHub's automatic dependency submission has none, so create it there only; elsewhere a
// missing file should still fail loudly instead of building without the API key.
if (System.getenv("GITHUB_ACTIONS") == "true") {
    file("local.properties").let { if (!it.exists()) it.createNewFile() }
}
