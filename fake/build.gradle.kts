plugins {
    id("kit.jvm.library")
    id("kit.publish")
}

description = "FakeVisor's glasses, band, Display and assistant, in plain Kotlin: for tests and the visor fake command."

kotlin {
    compilerOptions { optIn.add("com.macedonga.visor.kit.protocol.KitInternal") }
}

dependencies {
    api(project(":kit-server"))
}
