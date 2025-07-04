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
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "Mockingbird"
include(":mockingbird:core")
include(":mockingbird:internal")
include(":mockingbird:processor")
include(":sample")
include(":benchmark:mockk-static:generator")
include(":benchmark:mockk-static:executor")
include(":benchmark:mockingbird:executor")
include(":benchmark:mockingbird:generator")
include(":benchmark:mockk-interface:executor")
include(":benchmark:mockk-interface:generator")
