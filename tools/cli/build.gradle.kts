import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

description = "The vdk command: Visor's VDK from a computer, through adb."
version = providers.gradleProperty("vdkVersion").get()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjdk-release=17")
        optIn.add("com.macedonga.visor.vdk.protocol.VdkInternal")
    }
}

application {
    applicationName = "vdk"
    mainClass.set("com.macedonga.visor.vdk.cli.MainKt")
}

dependencies {
    implementation(project(":vdk-protocol"))
}

// The version the command reports, from the build.
tasks.jar {
    manifest { attributes("Implementation-Version" to version) }
}
