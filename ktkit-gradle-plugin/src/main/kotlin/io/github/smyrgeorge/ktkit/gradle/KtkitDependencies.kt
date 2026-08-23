package io.github.smyrgeorge.ktkit.gradle

import org.gradle.api.Project

/** Adds the ktkit-managed dependencies (see [KtkitExtension.addDependencies]). */
internal object KtkitDependencies {

    const val GROUP: String = "io.github.smyrgeorge"

    /** The implementation configuration of the main sources: commonMain (KMP) or main (JVM). */
    fun implementationConfiguration(project: Project): String =
        if (project.configurations.findByName("commonMainImplementation") != null) "commonMainImplementation"
        else "implementation"

    /**
     * Adds an implementation dependency on a `io.github.smyrgeorge` artifact.
     *
     * When the running build itself contains the matching project — building the ktkit repository
     * (its example module) or a fork — the project is used instead of the published coordinates,
     * so the build always consumes the current sources rather than a previously published
     * artifact. External builds never contain such projects and get the published coordinates.
     */
    fun add(
        project: Project,
        name: String,
        version: String,
        configuration: String = implementationConfiguration(project),
    ) {
        val local = project.findProject(":$name")?.takeIf { it.group == GROUP }
        project.dependencies.add(configuration, local ?: "$GROUP:$name:$version")
    }
}
