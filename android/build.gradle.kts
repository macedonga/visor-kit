plugins {
    id("vdk.android.library")
    id("vdk.publish")
}

description = "Lets an Android app use Meta glasses through Visor: their state, camera, Neural Band and Display."

android {
    namespace = "com.macedonga.visor.vdk"
    buildFeatures { aidl = true }
}

kotlin {
    compilerOptions { optIn.add("com.macedonga.visor.vdk.protocol.VdkInternal") }
}

dependencies {
    api(project(":vdk-protocol"))
    api(libs.kotlinx.coroutines.core)
    // RequestScopes is an ActivityResultContract.
    api(libs.androidx.activity)
    implementation(libs.kotlinx.coroutines.android)
}

/** Writes the library's version into the code, for the Hello that tells Visor which VDK an app runs. */
abstract class VdkVersionSource : DefaultTask() {
    @get:Input
    abstract val vdkVersion: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun write() {
        val file = outputDir.file("com/macedonga/visor/vdk/VdkVersion.kt").get().asFile
        file.parentFile.mkdirs()
        file.writeText("package com.macedonga.visor.vdk\n\ninternal const val VDK_VERSION: String = \"${vdkVersion.get()}\"\n")
    }
}

val vdkVersionSource = tasks.register<VdkVersionSource>("vdkVersionSource") {
    vdkVersion.set(version.toString())
}

androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(vdkVersionSource, VdkVersionSource::outputDir)
    }
}
