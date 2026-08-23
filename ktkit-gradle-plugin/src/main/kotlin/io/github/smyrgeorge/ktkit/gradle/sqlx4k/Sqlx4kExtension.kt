package io.github.smyrgeorge.ktkit.gradle.sqlx4k

/**
 * An sqlx4k extension, enabled through `ktkit { sqlx4k { extensions(...) } }`.
 *
 * Parameterless extensions are Kotlin objects (e.g. [Pgmq]); extensions requiring configuration
 * are added as classes whose constructor takes the options.
 */
public sealed interface Sqlx4kExtension {

    /** The PGMQ integration (`ktkit-sqlx4k-pgmq`) — message queueing on Postgres. PostgreSQL only. */
    public object Pgmq : Sqlx4kExtension
}
