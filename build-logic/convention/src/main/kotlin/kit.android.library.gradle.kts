import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts

plugins {
    id("com.android.library")
}

extensions.configure<LibraryExtension> {
    compileSdk = libs.int("compileSdk")

    defaultConfig {
        minSdk = libs.int("minSdk")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val dumpAbi = publicApiRules()

// The published variant's classes: the Kotlin code and the AIDL stubs alike.
extensions.configure<LibraryAndroidComponentsExtension> {
    onVariants(selector().withName("release")) { variant ->
        variant.artifacts.forScope(ScopedArtifacts.Scope.PROJECT)
            .use(dumpAbi)
            .toGet(ScopedArtifact.CLASSES, DumpAbi::jars, DumpAbi::dirs)
    }
}
