package io.github.smyrgeorge.ktkit.gradle.openapi

import io.github.smyrgeorge.ktkit.gradle.BuildConfig
import io.github.smyrgeorge.ktkit.gradle.KtkitExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Attaches the ktkit OpenAPI compiler plugin to the project's Kotlin compilations, generating
 * the `openApiSpec()` overrides of the REST handlers at compile time.
 *
 * Implemented as a [KotlinCompilerPluginSupportPlugin] (what the Kotlin Gradle plugin calls a
 * "subplugin"): it matches the compiler plugin's id, points at the compiler-plugin artifact, and
 * decides applicability per compilation from the `openApi` options of the `ktkit` extension
 * (see [KtkitExtension.openApi]).
 */
internal class OpenApi : KotlinCompilerPluginSupportPlugin {
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
