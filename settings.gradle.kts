pluginManagement {
    includeBuild("build-logic")
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
    }
}
rootProject.name = "TVLauncher"
include(":app")
include(":benchmark")

include(":core:model")
include(":core:domain")
include(":core:platform")
include(":core:data")
include(":core:designsystem")
include(":feature:home")
include(":feature:settings")
include(":feature:profiles")
include(":feature:search")
