plugins {
    id("kit.android.library")
    id("kit.publish")
}

description = "Lets an Android app use Meta glasses through Visor: their state, camera, Neural Band and Display."

android {
    namespace = "com.macedonga.visor.kit"
    buildFeatures { aidl = true }
}

kotlin {
    compilerOptions { optIn.add("com.macedonga.visor.kit.protocol.KitInternal") }
}

dependencies {
    api(project(":kit-protocol"))
    api(libs.kotlinx.coroutines.core)
    // RequestScopes is an ActivityResultContract.
    api(libs.androidx.activity)
    implementation(libs.kotlinx.coroutines.android)
}

/** Writes the library's version into the code, for the Hello that tells Visor which kit an app runs. */
abstract class KitVersionSource : DefaultTask() {
    @get:Input
    abstract val kitVersion: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun write() {
        val file = outputDir.file("com/macedonga/visor/kit/KitVersion.kt").get().asFile
        file.parentFile.mkdirs()
        file.writeText("package com.macedonga.visor.kit\n\ninternal const val KIT_VERSION: String = \"${kitVersion.get()}\"\n")
    }
}

val kitVersionSource = tasks.register<KitVersionSource>("kitVersionSource") {
    kitVersion.set(version.toString())
}

androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(kitVersionSource, KitVersionSource::outputDir)
    }
}
