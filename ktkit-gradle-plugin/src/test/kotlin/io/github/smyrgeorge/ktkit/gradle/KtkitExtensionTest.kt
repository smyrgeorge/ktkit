package io.github.smyrgeorge.ktkit.gradle

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KtkitExtensionTest {

    private fun extensionOf(project: org.gradle.api.Project): KtkitExtension {
        project.pluginManager.apply(KtkitGradlePlugin::class.java)
        return project.extensions.getByType(KtkitExtension::class.java)
    }

    @Test
    fun `addDependencies defaults to true`() {
        val project = ProjectBuilder.builder().build()
        assertTrue(extensionOf(project).addDependencies.get())
    }

    @Test
    fun `openApi is enabled by default and configurable via the dsl`() {
        val project = ProjectBuilder.builder().build()
        val extension = extensionOf(project)

        assertTrue(extension.openApi.enabled.get())
        extension.openApi { it.enabled.set(false) }
        assertFalse(extension.openApi.enabled.get())
    }

    @Test
    fun `sqlx4k enabling applies the ksp plugin and wires the integration once`() {
        val project = ProjectBuilder.builder().build()
        val extension = extensionOf(project)
        // Created manually so the (single) codegen registration is observable.
        val metadataKsp = project.configurations.create("kspCommonMainMetadata")

        extension.sqlx4k {
            it.driver.set(io.github.smyrgeorge.ktkit.gradle.sqlx4k.Driver.PostgreSQL)
            it.outputPackage.set("com.example.generated")
        }
        assertTrue(project.pluginManager.hasPlugin("com.google.devtools.ksp"))
        assertEquals(1, metadataKsp.dependencies.size)

        // A second block re-configures the options but does not re-wire the integration.
        extension.sqlx4k { it.arg("custom", "value") }
        assertEquals(1, metadataKsp.dependencies.size)
        assertEquals(mapOf("custom" to "value"), extension.sqlx4k.extraArgs)
    }

    @Test
    fun `jar enabling sets the archive-name convention from the project name`() {
        val project = ProjectBuilder.builder().withName("my-service").build()
        val extension = extensionOf(project)

        assertFalse(extension.jar.archiveFileName.isPresent)
        extension.jar { it.mainClass.set("com.example.MainKt") }
        assertEquals("my-service.jar", extension.jar.archiveFileName.get())
    }
}
