package io.github.smyrgeorge.ktkit.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * A Kotlin compiler plugin support class that integrates the ktkit OpenAPI compiler plugin
 * into the Kotlin compilation process. This is used for enabling and managing the OpenAPI
 * plugin functionality through the Gradle build scripts.
 *
 * The OpenAPI plugin is configured using the `ktkit` extension defined by the `KtkitGradlePlugin`.
 *
 * Responsibilities:
 * - Matches the plugin ID to connect with the corresponding compiler plugin module.
 * - Defines the artifact details (group ID, artifact ID, and version) for resolving the compiler plugin.
 * - Determines whether the plugin is applicable to the Kotlin compilations based on the project configuration.
 * - Provides additional configuration options for the compilation process, if necessary.
 *
 * This class ensures that the OpenAPI compiler plugin is conditionally applied based on
 * the `openApi` configuration of the `ktkit` extension.
 */
internal class OpenApiSubplugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        // Configuration lives on the `ktkit` extension, created by KtkitGradlePlugin.
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val extension = kotlinCompilation.target.project.extensions.findByType(KtkitExtension::class.java)
        return extension?.openApi?.enabled?.get() ?: true
    }

    // Must match `OpenApiCompilerPluginRegistrar.pluginId` in the compiler-plugin module.
    override fun getCompilerPluginId(): String = COMPILER_PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = COMPILER_PLUGIN_GROUP,
        artifactId = COMPILER_PLUGIN_ARTIFACT,
        version = BuildConfig.VERSION,
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.provider { emptyList() }

    private companion object {
        const val COMPILER_PLUGIN_ID = "io.github.smyrgeorge.ktkit.openapi"
        const val COMPILER_PLUGIN_GROUP = "io.github.smyrgeorge"
        const val COMPILER_PLUGIN_ARTIFACT = "ktkit-openapi-compiler-plugin"
    }
}
