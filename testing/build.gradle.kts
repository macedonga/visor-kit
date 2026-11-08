plugins {
    id("kit.android.library")
    id("kit.publish")
}

description = "FakeVisor: Visor inside an app's own process, for tests and for building without glasses."

android {
    namespace = "com.macedonga.visor.kit.testing"
}

kotlin {
    compilerOptions { optIn.add("com.macedonga.visor.kit.protocol.KitInternal") }
}

dependencies {
    api(project(":kit"))
    api(project(":kit-fake"))
    implementation(libs.kotlinx.coroutines.android)
}
