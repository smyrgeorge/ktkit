package io.github.smyrgeorge.ktkit.gradle.jar

import io.github.smyrgeorge.ktkit.gradle.KtkitExtension
import io.github.smyrgeorge.ktkit.gradle.KtkitGradlePlugin
import io.github.smyrgeorge.ktkit.gradle.assertEvaluationFails
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JarTest {

    @Test
    fun `options have sensible defaults`() {
        val project = ProjectBuilder.builder().build()
        val options = project.objects.newInstance(JarOptions::class.java)

        assertEquals(DuplicatesStrategy.EXCLUDE, options.duplicatesStrategy.get())
        assertFalse(options.mainClass.isPresent) // required — no default
        assertFalse(options.archiveFileName.isPresent) // convention applied when the module is enabled
    }

    @Test
    fun `the duplicates strategies are exposed in the dsl scope`() {
        val project = ProjectBuilder.builder().build()
        val options = project.objects.newInstance(JarOptions::class.java)

        assertEquals(DuplicatesStrategy.INCLUDE, options.INCLUDE)
        assertEquals(DuplicatesStrategy.EXCLUDE, options.EXCLUDE)
        assertEquals(DuplicatesStrategy.WARN, options.WARN)
        assertEquals(DuplicatesStrategy.FAIL, options.FAIL)
        assertEquals(DuplicatesStrategy.INHERIT, options.INHERIT)
    }

    @Test
    fun `an explicit archive name overrides the convention`() {
        val project = ProjectBuilder.builder().withName("my-service").build()
        val extension = project.enableJar {
            it.mainClass.set("com.example.MainKt")
            it.archiveFileName.set("custom.jar")
        }

        assertEquals("custom.jar", extension.jar.archiveFileName.get())
    }

    @Test
    fun `a missing mainClass fails the evaluation`() {
        val project = ProjectBuilder.builder().build()
        project.enableJar { }
        assertEvaluationFails(project, "'mainClass' must be set")
    }

    @Test
    fun `a missing jvmJar task fails the evaluation`() {
        val project = ProjectBuilder.builder().build()
        project.enableJar { it.mainClass.set("com.example.MainKt") }
        assertEvaluationFails(project, "task 'jvmJar' not found")
    }

    /** Applies the ktkit plugin and enables the jar module with dependency additions disabled. */
    private fun Project.enableJar(configure: (JarOptions) -> Unit): KtkitExtension {
        pluginManager.apply(KtkitGradlePlugin::class.java)
        val extension = extensions.getByType(KtkitExtension::class.java)
        extension.addDependencies.set(false) // bare test projects have no implementation configuration
        extension.jar { configure(it) }
        return extension
    }
}
