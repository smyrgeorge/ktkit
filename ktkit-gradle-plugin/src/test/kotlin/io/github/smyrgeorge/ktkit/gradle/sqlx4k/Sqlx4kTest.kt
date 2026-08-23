package io.github.smyrgeorge.ktkit.gradle.sqlx4k

import io.github.smyrgeorge.ktkit.gradle.BuildConfig
import io.github.smyrgeorge.ktkit.gradle.KtkitExtension
import io.github.smyrgeorge.ktkit.gradle.KtkitGradlePlugin
import io.github.smyrgeorge.ktkit.gradle.assertEvaluationFails
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Sqlx4kTest {

    // ---------------------------------------------------------------------------------------
    // Driver
    // ---------------------------------------------------------------------------------------

    @Test
    fun `drivers carry the codegen dialect and the sqlx4k artifact`() {
        assertEquals("mysql" to "sqlx4k-mysql", Driver.MySQL.dialect to Driver.MySQL.artifact)
        assertEquals("postgresql" to "sqlx4k-postgres", Driver.PostgreSQL.dialect to Driver.PostgreSQL.artifact)
        assertEquals("sqlite" to "sqlx4k-sqlite", Driver.SQLite.dialect to Driver.SQLite.artifact)
        assertEquals("sqlite" to "sqlx4k-sqlite-cipher", Driver.SQLiteCipher.dialect to Driver.SQLiteCipher.artifact)
    }

    // ---------------------------------------------------------------------------------------
    // Sqlx4kOptions
    // ---------------------------------------------------------------------------------------

    @Test
    fun `options have sensible defaults`() {
        val project = ProjectBuilder.builder().build()
        val options = project.objects.newInstance(Sqlx4kOptions::class.java)

        assertFalse(options.driver.isPresent) // required — no default
        assertFalse(options.outputPackage.isPresent)
        assertFalse(options.pgmq.get())
        assertEquals(listOf("commonMain"), options.sourceSets.get())
    }

    @Test
    fun `the driver values are exposed in the dsl scope`() {
        val project = ProjectBuilder.builder().build()
        val options = project.objects.newInstance(Sqlx4kOptions::class.java)

        assertEquals(Driver.MySQL, options.MySQL)
        assertEquals(Driver.PostgreSQL, options.PostgreSQL)
        assertEquals(Driver.SQLite, options.SQLite)
        assertEquals(Driver.SQLiteCipher, options.SQLiteCipher)
    }

    @Test
    fun `extra args are collected in order`() {
        val project = ProjectBuilder.builder().build()
        val options = project.objects.newInstance(Sqlx4kOptions::class.java)

        options.arg("b", "2")
        options.arg("a", "1")
        assertEquals(listOf("b" to "2", "a" to "1"), options.extraArgs.toList())
    }

    // ---------------------------------------------------------------------------------------
    // Sqlx4k
    // ---------------------------------------------------------------------------------------

    @Test
    fun `source sets map to the matching ksp configurations`() {
        assertEquals("kspCommonMainMetadata", Sqlx4k.kspConfigurationName("commonMain"))
        assertEquals("ksp", Sqlx4k.kspConfigurationName("main"))
        assertEquals("kspJvm", Sqlx4k.kspConfigurationName("jvmMain"))
        assertEquals("kspMacosArm64", Sqlx4k.kspConfigurationName("macosArm64Main"))
        assertFailsWith<IllegalStateException> { Sqlx4k.kspConfigurationName("weird") }
    }

    @Test
    fun `the codegen processor is registered on the ksp configuration`() {
        val project = ProjectBuilder.builder().build()
        // Created manually — on a real KMP project KSP creates it.
        val metadataKsp = project.configurations.create("kspCommonMainMetadata")
        project.enableSqlx4k {
            it.driver.set(Driver.PostgreSQL)
            it.outputPackage.set("com.example.generated")
        }

        val codegen = metadataKsp.dependencies.single() as ExternalDependency
        assertEquals("io.github.smyrgeorge", codegen.group)
        assertEquals("sqlx4k-codegen", codegen.name)
        assertEquals(BuildConfig.SQLX4K_VERSION, codegen.version)
    }

    @Test
    fun `source sets are final once the module is enabled`() {
        val project = ProjectBuilder.builder().build()
        val extension = project.enableSqlx4k {
            it.driver.set(Driver.PostgreSQL)
            it.outputPackage.set("com.example.generated")
        }

        assertFailsWith<IllegalStateException> { extension.sqlx4k.sourceSets.set(listOf("jvmMain")) }
    }

    @Test
    fun `empty source sets are rejected immediately`() {
        val project = ProjectBuilder.builder().build()
        val e = assertFailsWith<IllegalArgumentException> {
            project.enableSqlx4k { it.sourceSets.set(emptyList()) }
        }
        assertTrue("'sourceSets' must not be empty" in e.message.orEmpty())
    }

    @Test
    fun `a missing outputPackage fails the evaluation`() {
        val project = ProjectBuilder.builder().build()
        project.enableSqlx4k { it.driver.set(Driver.PostgreSQL) }
        assertEvaluationFails(project, "'outputPackage' must be set")
    }

    @Test
    fun `a missing driver fails the evaluation`() {
        val project = ProjectBuilder.builder().build()
        project.enableSqlx4k { it.outputPackage.set("com.example.generated") }
        assertEvaluationFails(project, "'driver' must be set")
    }

    @Test
    fun `pgmq requires the postgresql driver`() {
        val project = ProjectBuilder.builder().build()
        project.enableSqlx4k {
            it.driver.set(Driver.SQLite)
            it.outputPackage.set("com.example.generated")
            it.pgmq.set(true)
        }
        assertEvaluationFails(project, "'pgmq' requires the PostgreSQL driver")
    }

    @Test
    fun `a source set without a matching ksp configuration fails the evaluation`() {
        val project = ProjectBuilder.builder().build()
        project.enableSqlx4k {
            it.driver.set(Driver.PostgreSQL)
            it.outputPackage.set("com.example.generated")
            it.sourceSets.set(listOf("jvmMain"))
        }
        assertEvaluationFails(project, "no KSP configuration 'kspJvm' exists")
    }

    /** Applies the ktkit plugin and enables sqlx4k with dependency additions disabled. */
    private fun Project.enableSqlx4k(configure: (Sqlx4kOptions) -> Unit): KtkitExtension {
        pluginManager.apply(KtkitGradlePlugin::class.java)
        val extension = extensions.getByType(KtkitExtension::class.java)
        extension.addDependencies.set(false) // bare test projects have no implementation configuration
        extension.sqlx4k { configure(it) }
        return extension
    }
}
