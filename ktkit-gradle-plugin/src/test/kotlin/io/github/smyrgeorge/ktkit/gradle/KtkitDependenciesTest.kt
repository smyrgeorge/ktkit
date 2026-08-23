package io.github.smyrgeorge.ktkit.gradle

import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KtkitDependenciesTest {

    @Test
    fun `implementationConfiguration prefers commonMainImplementation`() {
        val project = ProjectBuilder.builder().build()
        project.configurations.create("implementation")
        assertEquals("implementation", KtkitDependencies.implementationConfiguration(project))

        project.configurations.create("commonMainImplementation")
        assertEquals("commonMainImplementation", KtkitDependencies.implementationConfiguration(project))
    }

    @Test
    fun `add uses the published coordinates when no matching project exists`() {
        val project = ProjectBuilder.builder().build()
        val implementation = project.configurations.create("implementation")

        KtkitDependencies.add(project, "ktkit", "1.2.3")

        val dependency = implementation.dependencies.single() as ExternalDependency
        assertEquals("io.github.smyrgeorge", dependency.group)
        assertEquals("ktkit", dependency.name)
        assertEquals("1.2.3", dependency.version)
    }

    @Test
    fun `add uses the in-build project when its name and group match`() {
        val root = ProjectBuilder.builder().withName("root").build()
        val ktkitProject = ProjectBuilder.builder().withName("ktkit").withParent(root).build()
        ktkitProject.group = "io.github.smyrgeorge"
        val app = ProjectBuilder.builder().withName("app").withParent(root).build()
        val implementation = app.configurations.create("implementation")

        KtkitDependencies.add(app, "ktkit", "1.2.3")

        val dependency = implementation.dependencies.single() as ProjectDependency
        assertEquals(":ktkit", dependency.path)
    }

    @Test
    fun `add ignores an in-build project of a different group`() {
        val root = ProjectBuilder.builder().withName("root").build()
        val other = ProjectBuilder.builder().withName("ktkit").withParent(root).build()
        other.group = "com.example"
        val app = ProjectBuilder.builder().withName("app").withParent(root).build()
        val implementation = app.configurations.create("implementation")

        KtkitDependencies.add(app, "ktkit", "1.2.3")

        assertTrue(implementation.dependencies.single() is ExternalDependency)
    }

    @Test
    fun `add targets the given configuration`() {
        val project = ProjectBuilder.builder().build()
        project.configurations.create("implementation")
        val jvmMain = project.configurations.create("jvmMainImplementation")

        KtkitDependencies.add(project, "ktkit-sqlx4k-postgres", "1.2.3", "jvmMainImplementation")

        assertEquals(1, jvmMain.dependencies.size)
    }
}
