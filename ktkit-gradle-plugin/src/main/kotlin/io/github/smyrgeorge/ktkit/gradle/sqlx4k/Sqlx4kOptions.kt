@file:Suppress("PropertyName")

package io.github.smyrgeorge.ktkit.gradle.sqlx4k

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

/** Options of the sqlx4k module (`ktkit { sqlx4k { } }`). */
public abstract class Sqlx4kOptions {
    // The Driver values, exposed directly in the DSL scope: `driver = PostgreSQL`.
    public val MySQL: Driver = Driver.MySQL
    public val PostgreSQL: Driver = Driver.PostgreSQL
    public val SQLite: Driver = Driver.SQLite
    public val SQLiteCipher: Driver = Driver.SQLiteCipher

    // The parameterless extensions, exposed directly in the DSL scope: `extensions = listOf(Pgmq)`.
    public val Pgmq: Sqlx4kExtension.Pgmq = Sqlx4kExtension.Pgmq

    /**
     * The sqlx4k database driver (it also determines the SQL dialect of the code generator).
     * Required.
     */
    public abstract val driver: Property<Driver>

    /** The package the generated sources are placed in. Required. */
    public abstract val generatedCodePackage: Property<String>

    /**
     * The source sets whose code the sqlx4k code generator processes. Defaults to `commonMain`
     * (generated once, visible to every target). Also supported: a target's main source set of a
     * multiplatform project (`jvmMain`, `macosArm64Main`, ...) and `main` for plain JVM projects.
     *
     * NOTE: must be configured in the first `sqlx4k { }` block — the KSP wiring derives from it.
     */
    public abstract val sourceSets: ListProperty<String>

    /**
     * The enabled sqlx4k extensions, populated via [extensions]. Defaults to none.
     * (Named to avoid Gradle's reserved `extensions` property of decorated/ExtensionAware types.)
     */
    public abstract val enabledExtensions: ListProperty<Sqlx4kExtension>

    /**
     * Enables the given sqlx4k extensions (see [Sqlx4kExtension]), e.g. `extensions(Pgmq)`.
     * [Pgmq] runs on Postgres — enabling it with a non-PostgreSQL [driver] fails the build.
     */
    public fun extensions(vararg extensions: Sqlx4kExtension) {
        enabledExtensions.addAll(extensions.toList())
    }

    /**
     * Arguments passed to the sqlx4k KSP code generator, applied last — after the ones this plugin
     * derives itself ([driver] becomes `dialect`, [generatedCodePackage] becomes `output-package`),
     * so an entry under the same key overrides them. Defaults to none.
     *
     * This is the escape hatch for every sqlx4k code-generator option ktkit does not model; see the
     * sqlx4k README for the full list. The compile-time checks and the SQL optimizations are all on
     * by default and are turned off here:
     *
     * ```kotlin
     * sqlx4k {
     *     driver = PostgreSQL
     *     generatedCodePackage = "com.example.generated"
     *     args = mapOf(
     *         // Checks — fail the build on an inconsistent @Query or entity.
     *         "validate-sql-syntax" to "false",
     *         // Check the full list of options in the sqlx4k:
     *         // https://github.com/smyrgeorge/sqlx4k
     *     )
     * }
     * ```
     *
     * NOTE: assigning replaces the whole map, [arg] adds one entry to it.
     */
    public abstract val args: MapProperty<String, String>

    /** Adds a single argument to [args]. */
    public fun arg(key: String, value: String) {
        args.put(key, value)
    }

    init {
        enabledExtensions.convention(emptyList())
        sourceSets.convention(listOf("commonMain"))
        args.convention(emptyMap())
    }
}
