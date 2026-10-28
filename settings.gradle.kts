pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("/home/edu/tools/android-sdk/extras/google/m2repository") }
        maven { url = uri("/home/edu/tools/android-sdk/extras/android/m2repository") }
    }
}

rootProject.name = "pawa-scanner"
include(":app")
