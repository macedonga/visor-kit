import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

description = "Writes the wire contract as OpenRPC and checks it against the last release."

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("com.macedonga.visor.kit.protocol.KitInternal")
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }
}

dependencies {
    implementation(project(":kit-protocol"))
}

val document = rootProject.layout.projectDirectory.file("protocol/api/openrpc.json")

// Writes protocol/api/openrpc.json from kit-protocol.
tasks.register<JavaExec>("updateContract") {
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.macedonga.visor.kit.contract.MainKt")
    args("update", document.asFile.path)
}

// Fails when openrpc.json is stale, or when it drops or changes anything the last release tag has.
val checkContract = tasks.register<JavaExec>("checkContract") {
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.macedonga.visor.kit.contract.MainKt")
    args("check", document.asFile.path, rootProject.layout.projectDirectory.asFile.path)
}

tasks.named("check") { dependsOn(checkContract) }
