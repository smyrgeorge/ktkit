package io.github.smyrgeorge.ktkit.publish

import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SourcesJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@Suppress("unused")
class PublishConventions : Plugin<Project> {

    private val descriptions: Map<String, String> = mapOf(
        "ktkit" to "A comprehensive Kotlin multiplatform toolkit for building server applications with Ktor.",
        "ktkit-ktor-httpclient" to "A comprehensive Kotlin multiplatform toolkit for building server applications with Ktor.",
        "ktkit-spring-webclient" to "A comprehensive Kotlin multiplatform toolkit for building server applications with Ktor.",
        "ktkit-sqlx4k" to "A comprehensive Kotlin multiplatform toolkit for building server applications with Ktor.",
        "ktkit-sqlx4k-pgmq" to "A comprehensive Kotlin multiplatform toolkit for building server applications with Ktor.",
    )

    override fun apply(project: Project) {
        project.plugins.apply("com.vanniktech.maven.publish")
        project.extensions.configure<MavenPublishBaseExtension> {
            // sources publishing is always enabled by the Kotlin Multiplatform plugin
            configure(
                KotlinMultiplatform(
                    // whether to publish a sources jar
                    sourcesJar = SourcesJar.Sources()
                )
            )
            coordinates(
                groupId = project.group as String,
                artifactId = project.name,
                version = project.version as String
            )

            pom {
                name.set(project.name)
                description.set(descriptions[project.name] ?: error("Missing description for $project.name"))
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

            // Enable GPG signing for all publications
            signAllPublications()
        }
    }
}
