package io.github.smyrgeorge.ktorlib.publish

import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@Suppress("unused")
class PublishConventions : Plugin<Project> {

    private val descriptions: Map<String, String> = mapOf(
    )

    override fun apply(project: Project) {
        project.plugins.apply("com.vanniktech.maven.publish")
        project.extensions.configure<MavenPublishBaseExtension> {
            // sources publishing is always enabled by the Kotlin Multiplatform plugin
            configure(
                KotlinMultiplatform(
                    // whether to publish a sources jar
                    sourcesJar = true,
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
                url.set("https://github.com/smyrgeorge/ktorlib")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/smyrgeorge/ktorlib/blob/main/LICENSE")
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
                    url.set("https://github.com/smyrgeorge/ktorlib")
                    connection.set("scm:git:https://github.com/smyrgeorge/ktorlib.git")
                    developerConnection.set("scm:git:git@github.com:smyrgeorge/ktorlib.git")
                }
            }

            // Configure publishing to Maven Central
            publishToMavenCentral()

            // Enable GPG signing for all publications
            signAllPublications()
        }
    }
}
