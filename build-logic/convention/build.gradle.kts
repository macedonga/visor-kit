plugins {
    `kotlin-dsl`
}

group = "com.macedonga.visor.vdk.buildlogic"

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.poko.gradle.plugin)
    compileOnly(libs.maven.publish.gradle.plugin)
    compileOnly(libs.dokka.gradle.plugin)
    compileOnly(libs.kotlin.abi.tools.api)
}
