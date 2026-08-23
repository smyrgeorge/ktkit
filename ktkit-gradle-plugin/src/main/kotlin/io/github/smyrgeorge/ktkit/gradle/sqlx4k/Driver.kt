package io.github.smyrgeorge.ktkit.gradle.sqlx4k

/**
 * The sqlx4k database drivers of the sqlx4k module (see [Sqlx4kOptions.driver]).
 *
 * @property dialect the SQL dialect identifier passed to the sqlx4k code generator.
 * @property artifact the sqlx4k driver artifact.
 */
public enum class Driver(internal val dialect: String, internal val artifact: String) {
    MySQL("mysql", "sqlx4k-mysql"),
    PostgreSQL("postgresql", "sqlx4k-postgres"),
    SQLite("sqlite", "sqlx4k-sqlite"),
    SQLiteCipher("sqlite", "sqlx4k-sqlite-cipher"),
}
