pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "visor-kit"

apply(from = "gradle/pins.settings.gradle.kts")

// Project names are the artifact ids, so a build that includes this one (Visor, with includeBuild) gets them
// substituted for the published coordinates without any mapping.
include(":kit-protocol")
project(":kit-protocol").projectDir = file("protocol")
include(":kit")
project(":kit").projectDir = file("android")
include(":kit-testing")
project(":kit-testing").projectDir = file("testing")
// Not published: the `visor` command for a computer, the checks that keep the wire contract compatible, and
// the sample app.
include(":cli")
project(":cli").projectDir = file("tools/cli")
include(":contract")
project(":contract").projectDir = file("tools/contract")
include(":sample")
project(":sample").projectDir = file("samples/android")
