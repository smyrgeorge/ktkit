package io.github.smyrgeorge.ktkit.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A Gradle plugin that facilitates the application and configuration of ktkit compiler plugins
 * within a Gradle project. This plugin creates the `ktkit` extension for build scripts, which
 * allows users to configure specific ktkit compiler plugins, such as the OpenAPI plugin.
 *
 * The `ktkit` extension provides options to manage the behavior of the ktkit compiler plugins
 * as part of the Kotlin compilation process.
 *
 * When applied, this plugin:
 * - Creates the `ktkit` DSL extension to enable configuration of ktkit compiler plugins.
 * - Applies the [OpenApiSubplugin] to attach the ktkit OpenAPI compiler plugin to the project's
 *   Kotlin compilations.
 */
public class KtkitGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("ktkit", KtkitExtension::class.java)
        target.pluginManager.apply(OpenApiSubplugin::class.java)
    }
}
