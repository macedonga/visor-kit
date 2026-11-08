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
    implementation(project(":kit-fake"))
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    runtimeOnly(libs.slf4j.nop)
}

// The docs ride in the command for `visor docs` and `visor docs-mcp`, with an index of their pages.
val docsResources = tasks.register<Sync>("docsResources") {
    from(rootProject.file("docs")) {
        include("*.md", "llms.txt")
        into("docs")
    }
    into(layout.buildDirectory.dir("generated/docs"))
    doLast {
        val dir = destinationDir.resolve("docs")
        dir.resolve("index.txt").writeText(dir.list().orEmpty().filter { it.endsWith(".md") }.sorted().joinToString("\n"))
    }
}

sourceSets.main { resources.srcDir(docsResources) }

// The version the command reports, from the build.
tasks.jar {
    manifest { attributes("Implementation-Version" to version) }
}
