package io.github.smyrgeorge.ktkit.gradle.openapi

import io.github.smyrgeorge.ktkit.gradle.BuildConfig
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenApiTest {

    @Test
    fun `options are enabled by default`() {
        val project = ProjectBuilder.builder().build()
        val options = project.objects.newInstance(OpenApiOptions::class.java)

        assertTrue(options.enabled.get())
        options.enabled.set(false)
        assertFalse(options.enabled.get())
    }

    @Test
    fun `the integration points at the matching compiler plugin artifact`() {
        val integration = OpenApi()

        // Must match OpenApiCompilerPluginRegistrar.pluginId in the compiler-plugin module.
        assertEquals("io.github.smyrgeorge.ktkit.openapi", integration.getCompilerPluginId())
        val artifact = integration.getPluginArtifact()
        assertEquals("io.github.smyrgeorge", artifact.groupId)
        assertEquals("ktkit-openapi-compiler-plugin", artifact.artifactId)
        assertEquals(BuildConfig.VERSION, artifact.version)
    }
}
