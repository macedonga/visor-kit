plugins {
    id("vdk.android.library")
    id("vdk.publish")
}

description = "FakeVisor: Visor inside an app's own process, for tests and for building without glasses."

android {
    namespace = "com.macedonga.visor.vdk.testing"
}

kotlin {
    compilerOptions { optIn.add("com.macedonga.visor.vdk.protocol.VdkInternal") }
}

dependencies {
    api(project(":vdk"))
}
