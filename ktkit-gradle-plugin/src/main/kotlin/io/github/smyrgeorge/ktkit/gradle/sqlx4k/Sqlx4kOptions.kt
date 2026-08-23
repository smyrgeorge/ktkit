@file:Suppress("PropertyName")

package io.github.smyrgeorge.ktkit.gradle.sqlx4k

import org.gradle.api.provider.ListProperty
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

    internal val extraArgs: LinkedHashMap<String, String> = LinkedHashMap()

    /** Passes an additional argument to the sqlx4k KSP code generator. */
    public fun arg(key: String, value: String) {
        extraArgs[key] = value
    }

    init {
        enabledExtensions.convention(emptyList())
        sourceSets.convention(listOf("commonMain"))
    }
}
