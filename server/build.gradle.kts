plugins {
    id("kit.jvm.library")
    id("kit.publish")
}

description = "The VDK's server engine: it routes apps' calls, streams and feeds, whatever carries them."

kotlin {
    compilerOptions { optIn.add("com.macedonga.visor.kit.protocol.KitInternal") }
}

dependencies {
    api(project(":kit-protocol"))
    api(libs.kotlinx.coroutines.core)
}
