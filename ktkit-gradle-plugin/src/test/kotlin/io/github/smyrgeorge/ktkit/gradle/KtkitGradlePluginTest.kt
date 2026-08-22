package io.github.smyrgeorge.ktkit.gradle

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KtkitGradlePluginTest {

    @Test
    fun `applying the plugin registers the extension and the openApi subplugin`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KtkitGradlePlugin::class.java)

        assertNotNull(project.extensions.findByName("ktkit"))
        assertTrue(project.plugins.any { it is OpenApiSubplugin })
    }

    @Test
    fun `openApi is enabled by default and configurable via the dsl`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KtkitGradlePlugin::class.java)

        val extension = project.extensions.getByType(KtkitExtension::class.java)
        assertTrue(extension.openApi.enabled.get())

        extension.openApi { it.enabled.set(false) }
        assertFalse(extension.openApi.enabled.get())
    }

    @Test
    fun `the openApi subplugin points at the matching compiler plugin artifact`() {
        val subplugin = OpenApiSubplugin()
        assertEquals("io.github.smyrgeorge.ktkit.openapi", subplugin.getCompilerPluginId())
        val artifact = subplugin.getPluginArtifact()
        assertEquals("io.github.smyrgeorge", artifact.groupId)
        assertEquals("ktkit-openapi-compiler-plugin", artifact.artifactId)
        assertEquals(BuildConfig.VERSION, artifact.version)
    }
}
