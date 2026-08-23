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

    /**
     * The sqlx4k database driver (it also determines the SQL dialect of the code generator).
     * Required.
     */
    public abstract val driver: Property<Driver>

    /** The package the generated sources are placed in. Required. */
    public abstract val outputPackage: Property<String>

    /**
     * The source sets whose code the sqlx4k code generator processes. Defaults to `commonMain`
     * (generated once, visible to every target). Also supported: a target's main source set of a
     * multiplatform project (`jvmMain`, `macosArm64Main`, ...) and `main` for plain JVM projects.
     *
     * NOTE: must be configured in the first `sqlx4k { }` block — the KSP wiring derives from it.
     */
    public abstract val sourceSets: ListProperty<String>

    /**
     * Whether to add the PGMQ integration (`ktkit-sqlx4k-pgmq`). Defaults to false.
     * PGMQ runs on Postgres — enabling it with a non-PostgreSQL [driver] fails the build.
     */
    public abstract val pgmq: Property<Boolean>

    internal val extraArgs: LinkedHashMap<String, String> = LinkedHashMap()

    /** Passes an additional argument to the sqlx4k KSP code generator. */
    public fun arg(key: String, value: String) {
        extraArgs[key] = value
    }

    init {
        pgmq.convention(false)
        sourceSets.convention(listOf("commonMain"))
    }
}
