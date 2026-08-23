import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("com.vanniktech.maven.publish")
}

val descriptions = mapOf(
    "ktkit" to "A comprehensive Kotlin multiplatform toolkit for building server applications with Ktor.",
    "ktkit-ktor-httpclient" to "A comprehensive Kotlin multiplatform toolkit for building server applications with Ktor.",
    "ktkit-gradle-plugin" to "Gradle plugin that wires the ktkit OpenAPI Kotlin compiler plugin onto every Kotlin compilation.",
    "ktkit-compiler-openapi" to "Kotlin compiler plugin for ktkit: compile-time OpenAPI specification generation for REST handlers.",
    "ktkit-sqlx4k" to "A comprehensive Kotlin multiplatform toolkit for building server applications with Ktor.",
    "ktkit-sqlx4k-pgmq" to "A comprehensive Kotlin multiplatform toolkit for building server applications with Ktor.",
    "ktkit-sqlx4k-postgres" to "A comprehensive Kotlin multiplatform toolkit for building server applications with Ktor.",
)

configure<MavenPublishBaseExtension> {
    // Gradle plugin modules publish the plugin jar plus its marker; everything else is Kotlin
    // Multiplatform (JVM-only modules are still applied via the `multiplatform` Kotlin plugin).
    if (pluginManager.hasPlugin("java-gradle-plugin")) {
        configure(
            GradlePlugin(
                javadocJar = JavadocJar.Empty(),
                sourcesJar = SourcesJar.Sources()
            )
        )
    } else {
        configure(
            KotlinMultiplatform(
                // whether to publish a sources jar
                sourcesJar = SourcesJar.Sources()
            )
        )
    }

    coordinates(
        groupId = project.group as String,
        artifactId = project.name,
        version = project.version as String
    )

    pom {
        name.set(project.name)
        description.set(descriptions[project.name] ?: error("Missing description for ${project.name}"))
        url.set("https://github.com/smyrgeorge/ktkit")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/smyrgeorge/ktkit/blob/main/LICENSE")
            }
        }

        developers {
            developer {
                id.set("smyrgeorge")
                name.set("Yorgos S.")
                email.set("smyrgoerge@gmail.com")
                url.set("https://smyrgeorge.github.io/")
            }
        }

        scm {
            url.set("https://github.com/smyrgeorge/ktkit")
            connection.set("scm:git:https://github.com/smyrgeorge/ktkit.git")
            developerConnection.set("scm:git:git@github.com:smyrgeorge/ktkit.git")
        }
    }

    // Configure publishing to Maven Central
    publishToMavenCentral()

    if (providers.gradleProperty("RELEASE_SIGNING_ENABLED").getOrElse("true").toBoolean()) {
        signAllPublications()
    }
}
