import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.publish.PublishingExtension

plugins {
    id("com.vanniktech.maven.publish")
    id("org.jetbrains.dokka")
}

group = "com.macedonga.visor"
version = providers.gradleProperty("kitVersion").get()

mavenPublishing {
    val javadoc = JavadocJar.Dokka("dokkaGeneratePublicationHtml")
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        configure(KotlinJvm(javadocJar = javadoc, sourcesJar = SourcesJar.Sources()))
    }
    pluginManager.withPlugin("com.android.library") {
        configure(AndroidSingleVariantLibrary(javadocJar = javadoc, sourcesJar = SourcesJar.Sources(), variant = "release"))
    }

    coordinates("com.macedonga.visor", project.name, version.toString())
    publishToMavenCentral(automaticRelease = true)
    // Only the release workflow holds the key. Without it, publishToMavenLocal still works for a local check.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()

    pom {
        name.set(project.name)
        description.set(provider { project.description })
        inceptionYear.set("2026")
        url.set("https://visor.macedonga.com/developers/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("macedonga")
                name.set("Marco Ceccon")
                url.set("https://github.com/macedonga")
            }
        }
        scm {
            url.set("https://github.com/macedonga/visor-kit")
            connection.set("scm:git:https://github.com/macedonga/visor-kit.git")
            developerConnection.set("scm:git:ssh://git@github.com/macedonga/visor-kit.git")
        }
    }
}

// A copy on GitHub Packages, so the repository's page lists the libraries; Maven Central stays the one to use.
// The release workflow gives GitHubPackagesUsername and GitHubPackagesPassword.
extensions.configure<PublishingExtension> {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/macedonga/visor-kit")
            credentials(PasswordCredentials::class)
        }
    }
}
