# ktkit-sqlx4k

Database support for ktkit services, built on [sqlx4k](https://github.com/smyrgeorge/sqlx4k). This module glues sqlx4k
into the ktkit programming model: the `ExecContext`, the `Raise<ErrorSpec>` error channel, and log4k tracing.

For the full sqlx4k feature list see [smyrgeorge/sqlx4k](https://github.com/smyrgeorge/sqlx4k).

## How the integration works

### 1. Enable it via the Gradle plugin

The `sqlx4k { }` block of the [ktkit Gradle plugin](../README.md#gradle-plugin-ktkit-gradle-plugin) wires everything:
it applies KSP and registers the sqlx4k code generator on the configured source sets, and adds `ktkit-sqlx4k` plus the
driver's artifact to the project.

```kotlin
ktkit {
    sqlx4k {
        driver = PostgreSQL // also: MySQL, SQLite, SQLiteCipher
        generatedCodePackage = "com.example.generated"
    }
}
```

### 2. Declare an entity

Entities are plain `@Table` data classes. Implementing ktkit's `Auditable<ID>` opts the entity into the auto-managed
audit columns:

```kotlin
@Table("test")
data class Test(
    @Id
    override val id: Int = 0,
    override var createdAt: Instant = Clock.System.now(),
    override var createdBy: Uuid = SYSTEM_USER.id,
    override var updatedAt: Instant = createdAt,
    override var updatedBy: Uuid = createdBy,
    val test: String,
) : Auditable<Int>
```

### 3. Declare a repository

Repositories are `@Repository` interfaces extending `AuditableRepository<T>`. The sqlx4k code generator produces the
implementation (`TestRepositoryImpl`) into the `generatedCodePackage`, validating the `@Query` SQL at compile time:

```kotlin
@Repository
interface TestRepository : AuditableRepository<Test> {
    @Query("SELECT * FROM test")
    context(context: QueryExecutor)
    suspend fun findAll(): DbResult<List<Test>>
}
```

`AuditableRepository` contributes the ktkit behavior on top of sqlx4k's CRUD repository:

- `preInsertHook`/`preUpdateHook` fill `createdAt`/`createdBy`/`updatedAt`/`updatedBy` from the authenticated principal
  of the current `ExecContext`.
- `aroundQuery` wraps every query in a tracing span (named `db.<Repository>.<method>`, tagged with the statement text)
  and records failures on the span.

### 4. Call it from a service

Services implement `DatabaseService` — or `AuditableDatabaseService<T>`, which additionally provides the `save()`
extension for auditable entities. Two helpers connect sqlx4k results to the ktkit error model:

- `db { }` unwraps a `DbResult<T>`, mapping database failures to ktkit's `DatabaseError` and raising them through the
  `Raise<ErrorSpec>` context parameter — no exceptions, no manual `Either` plumbing.
- `withTransaction { }` runs a block inside a database transaction, wrapped in a `db.transaction` tracing span.

```kotlin
class TestService(
    override val db: Driver,
    override val repo: TestRepository,
) : AuditableDatabaseService<Test> {

    context(_: Raise<ErrorSpec>, _: QueryExecutor)
    suspend fun findAll(): List<Test> = db { repo.findAll() }
}

// From a REST handler:
testService.withTransaction {
    testService.findAll()
}
```

### 5. Wire it up

The generated repository implementation is a plain object — register it in the application's DI together with the
driver:

```kotlin
di {
    single { db }.bind<Driver>()
    single { TestRepositoryImpl }.bind<TestRepository>()
    singleOf(::TestService)
}
```

### JSON columns

`JsonSupport` builds a sqlx4k `ValueEncoderRegistry` from `@Serializable` classes (including sealed hierarchies), so
entity properties can be persisted as JSON columns. The default configuration uses snake-case names, ignores unknown
keys, and encodes defaults while omitting explicit nulls.

## Companion modules

- [`ktkit-sqlx4k-pgmq`](../ktkit-sqlx4k-pgmq) — [PGMQ](https://github.com/pgmq/pgmq) (message queue on Postgres)
  support: the `Pgmq` wrapper and `AbstractPgmqEventHandler` with trace/user propagation and consumer lifecycle helpers.
  Enabled via `sqlx4k { extensions(Pgmq) }` in the Gradle plugin (PostgreSQL only).
- [`ktkit-sqlx4k-postgres`](../ktkit-sqlx4k-postgres) — JVM-only PostgreSQL helpers based on r2dbc (e.g.
  `PostgresJsonSupport`); added automatically by the Gradle plugin for JVM targets when the driver is `PostgreSQL`.

A complete working setup lives in the [example module](../example).
