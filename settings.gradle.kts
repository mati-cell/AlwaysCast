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

rootProject.name = "AlwaysCast"

include(":app")
include(":core:common")
include(":core:model")
include(":core:network")
include(":core:proxy")
include(":core:cast")
include(":core:designsystem")
include(":feature:cast")
