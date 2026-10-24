pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "AeroLauncher"
include(":jre_lwjgl3glfw")
// The module directory keeps its original name: CI workflows, the JNI/CMake
// paths and the sdlhook build all reference TurtleLauncher/ on disk.
include(":TurtleLauncher")
