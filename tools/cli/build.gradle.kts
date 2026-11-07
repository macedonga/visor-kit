import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

description = "The visor command: Visor's VDK from a computer, through adb."
version = providers.gradleProperty("kitVersion").get()

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjdk-release=17")
        optIn.add("com.macedonga.visor.kit.protocol.KitInternal")
    }
}

application {
    applicationName = "visor"
    mainClass.set("com.macedonga.visor.kit.cli.MainKt")
}

dependencies {
    implementation(project(":kit-protocol"))
}

// The version the command reports, from the build.
tasks.jar {
    manifest { attributes("Implementation-Version" to version) }
}
