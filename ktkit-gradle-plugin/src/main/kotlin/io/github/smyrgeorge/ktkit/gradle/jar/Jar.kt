package io.github.smyrgeorge.ktkit.gradle.jar

import io.github.smyrgeorge.ktkit.gradle.KtkitExtension
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.bundling.Jar as JarTask

/**
 * Configures the `jvmJar` task as a runnable, self-contained ("fat") jar when
 * `ktkit { jar { } }` is used: the runtime dependencies are bundled in and the `Main-Class`
 * manifest attribute is set. See [KtkitExtension.jar].
 */
internal object Jar {

    private const val JVM_JAR_TASK = "jvmJar"
    private const val JVM_RUNTIME_CLASSPATH = "jvmRuntimeClasspath"

    fun apply(project: Project, ktkit: KtkitExtension) {
        val options = ktkit.jar
        options.archiveFileName.convention("${project.name}.jar")

        // The options are read after the build script is fully evaluated, so later
        // re-configuration is still picked up.
        project.afterEvaluate {
            require(options.mainClass.isPresent) {
                "ktkit { jar { } }: 'mainClass' must be set."
            }
            val jar = try {
                project.tasks.named(JVM_JAR_TASK, JarTask::class.java)
            } catch (e: UnknownTaskException) {
                throw IllegalStateException(
                    "ktkit { jar { } }: task '$JVM_JAR_TASK' not found — " +
                            "the block requires the Kotlin multiplatform jvm() target.", e
                )
            }
            jar.configure { task ->
                task.archiveFileName.set(options.archiveFileName)
                task.manifest.attributes(mapOf("Main-Class" to options.mainClass.get()))
                // Include the runtime dependencies in the jar — a self-contained jar, similar
                // to what the Shadow plugin produces.
                task.from(project.configurations.named(JVM_RUNTIME_CLASSPATH).map { configuration ->
                    configuration.map { file -> if (file.isDirectory) file else project.zipTree(file) }
                })
                task.duplicatesStrategy = options.duplicatesStrategy.get()
            }
        }
    }
}
