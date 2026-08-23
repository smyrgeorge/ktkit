package io.github.smyrgeorge.ktkit.gradle

import io.github.smyrgeorge.ktkit.gradle.openapi.OpenApi
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The ktkit Gradle plugin: applies everything a ktkit service needs and exposes the `ktkit { }`
 * DSL to configure the ktkit compiler plugins and modules from one place.
 *
 * When applied, this plugin:
 * - Creates the `ktkit` DSL extension (see [KtkitExtension]).
 * - Applies the [OpenApi] to attach the ktkit OpenAPI compiler plugin to the project's
 *   Kotlin compilations.
 * - Applies the `kotlinx.serialization` and `log4k` Gradle plugins once a Kotlin plugin
 *   (multiplatform or JVM) is present — every ktkit service relies on both.
 * - Adds the ktkit core and log4k-context dependencies (unless [KtkitExtension.addDependencies]
 *   is disabled).
 *
 * Modules (e.g. sqlx4k) are opt-in through the DSL and wire their own plugins, code generation
 * and dependencies — see [KtkitExtension.sqlx4k].
 */
public class KtkitGradlePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val extension = target.extensions.create("ktkit", KtkitExtension::class.java, target)
        target.pluginManager.apply(OpenApi::class.java)

        // Every ktkit service serializes with kotlinx.serialization and logs with log4k.
        KOTLIN_PLUGIN_IDS.forEach { kotlinPluginId ->
            target.pluginManager.withPlugin(kotlinPluginId) {
                target.pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
                target.pluginManager.apply("io.github.smyrgeorge.log4k")
            }
        }

        target.afterEvaluate {
            if (!extension.addDependencies.get()) return@afterEvaluate
            KtkitDependencies.add(target, "ktkit", BuildConfig.VERSION)
            KtkitDependencies.add(target, "log4k-context", BuildConfig.LOG4K_VERSION)
        }
    }

    private companion object {
        val KOTLIN_PLUGIN_IDS = listOf("org.jetbrains.kotlin.multiplatform", "org.jetbrains.kotlin.jvm")
    }
}
