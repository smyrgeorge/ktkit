package io.github.smyrgeorge.ktkit.gradle

import io.github.smyrgeorge.ktkit.gradle.openapi.OpenApi
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KtkitGradlePluginTest {

    @Test
    fun `applying the plugin registers the extension and the openApi integration`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KtkitGradlePlugin::class.java)

        assertNotNull(project.extensions.findByName("ktkit"))
        assertTrue(project.plugins.any { it is OpenApi })
        // Modules are opt-in: without ktkit { sqlx4k { } }, nothing sqlx4k-related is wired.
        assertFalse(project.pluginManager.hasPlugin("com.google.devtools.ksp"))
        // The serialization/log4k plugins are applied only once a Kotlin plugin is present.
        assertFalse(project.pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.serialization"))
        assertFalse(project.pluginManager.hasPlugin("io.github.smyrgeorge.log4k"))
    }

    @Test
    fun `the serialization and log4k plugins are applied once a kotlin plugin is present`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KtkitGradlePlugin::class.java)

        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        assertTrue(project.pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.serialization"))
        assertTrue(project.pluginManager.hasPlugin("io.github.smyrgeorge.log4k"))
    }

    @Test
    fun `the core dependencies are added after evaluation`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KtkitGradlePlugin::class.java)
        val implementation = project.configurations.create("implementation")

        project.evaluateNow()

        val notations = implementation.dependencies
            .filterIsInstance<ExternalDependency>()
            .map { "${it.group}:${it.name}:${it.version}" }
        assertTrue("io.github.smyrgeorge:ktkit:${BuildConfig.VERSION}" in notations)
        assertTrue("io.github.smyrgeorge:log4k-context:${BuildConfig.LOG4K_VERSION}" in notations)
    }

    @Test
    fun `the core dependencies prefer commonMainImplementation when it exists`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KtkitGradlePlugin::class.java)
        val implementation = project.configurations.create("implementation")
        val commonMain = project.configurations.create("commonMainImplementation")

        project.evaluateNow()

        assertTrue(implementation.dependencies.isEmpty())
        assertEquals(2, commonMain.dependencies.size)
    }

    @Test
    fun `addDependencies=false disables the core dependencies`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(KtkitGradlePlugin::class.java)
        val implementation = project.configurations.create("implementation")
        project.extensions.getByType(KtkitExtension::class.java).addDependencies.set(false)

        project.evaluateNow()

        assertTrue(implementation.dependencies.isEmpty())
    }

    @Test
    fun `the core ktkit dependency prefers a matching in-build project`() {
        val root = ProjectBuilder.builder().withName("root").build()
        val ktkitProject = ProjectBuilder.builder().withName("ktkit").withParent(root).build()
        ktkitProject.group = "io.github.smyrgeorge"
        val app = ProjectBuilder.builder().withName("app").withParent(root).build()

        app.pluginManager.apply(KtkitGradlePlugin::class.java)
        val implementation = app.configurations.create("implementation")

        app.evaluateNow()

        val projects = implementation.dependencies.filterIsInstance<ProjectDependency>().map { it.path }
        assertEquals(listOf(":ktkit"), projects)
        // log4k-context has no matching project — published coordinates are used.
        assertTrue(implementation.dependencies.filterIsInstance<ExternalDependency>()
            .any { it.name == "log4k-context" })
    }
}
