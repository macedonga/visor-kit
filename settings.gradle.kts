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

rootProject.name = "vdk"

apply(from = "gradle/pins.settings.gradle.kts")

// Project names are the artifact ids, so a build that includes this one (Visor, with includeBuild) gets them
// substituted for the published coordinates without any mapping.
include(":vdk-protocol")
project(":vdk-protocol").projectDir = file("protocol")
include(":vdk")
project(":vdk").projectDir = file("android")
include(":vdk-testing")
project(":vdk-testing").projectDir = file("testing")
// Not published: the `vdk` command for a computer, the checks that keep the wire contract compatible, and
// the sample app.
include(":cli")
project(":cli").projectDir = file("tools/cli")
include(":contract")
project(":contract").projectDir = file("tools/contract")
include(":sample")
project(":sample").projectDir = file("samples/android")
