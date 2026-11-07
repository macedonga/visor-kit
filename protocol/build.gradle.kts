plugins {
    id("kit.jvm.library")
    id("kit.publish")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.poko)
}

description = "The VDK's wire contract: its methods, topics, their types and the error codes."

dependencies {
    api(libs.kotlinx.serialization.json)
}

// The module that declares KitInternal uses it everywhere.
kotlin {
    compilerOptions { optIn.add("com.macedonga.visor.kit.protocol.KitInternal") }
}
