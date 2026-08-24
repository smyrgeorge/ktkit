# KtKit

![Build](https://github.com/smyrgeorge/ktkit/actions/workflows/ci.yml/badge.svg)
![Maven Central](https://img.shields.io/maven-central/v/io.github.smyrgeorge/ktkit)
![GitHub License](https://img.shields.io/github/license/smyrgeorge/ktkit)
![GitHub commit activity](https://img.shields.io/github/commit-activity/w/smyrgeorge/ktkit)
![GitHub issues](https://img.shields.io/github/issues/smyrgeorge/ktkit)
[![Kotlin](https://img.shields.io/badge/kotlin-2.4.10-blue.svg?logo=kotlin)](http://kotlinlang.org)

![](https://img.shields.io/static/v1?label=&message=Platforms&color=grey)
![](https://img.shields.io/static/v1?label=&message=Jvm&color=blue)
![](https://img.shields.io/static/v1?label=&message=Linux&color=blue)
![](https://img.shields.io/static/v1?label=&message=macOS&color=blue)
![](https://img.shields.io/static/v1?label=&message=Windows&color=blue)

A comprehensive Kotlin multiplatform toolkit for building server applications with Ktor.

📖 [Documentation](https://smyrgeorge.github.io/ktkit/)

🏠 [Homepage](https://smyrgeorge.github.io/) (under construction)

## Table of Contents

- [Overview](#overview)
- [Usage](#usage)
- [Ergonomics](#ergonomics)
- [Modules and features](#modules-and-features)
    - [Gradle plugin (ktkit-gradle-plugin)](#gradle-plugin-ktkit-gradle-plugin)
    - [Application bootstrap & configuration](#application-bootstrap--configuration)
    - [Kotlinx serialization](#kotlinx-serialization)
    - [API errors (RFC 9457)](#api-errors-rfc-9457)
    - [Security & permissions](#security--permissions)
    - [Logging, tracing & metrics (log4k)](#logging-tracing--metrics-log4k)
    - [Health & metrics endpoints](#health--metrics-endpoints)
    - [TOML Configuration Loading](#toml-configuration-loading)
    - [Database support (sqlx4k)](#database-support-sqlx4k)
    - [Queue support (PGMQ)](#queue-support-pgmq)
    - [OpenAPI generation (ktkit-compiler-openapi)](#openapi-generation-ktkit-compiler-openapi)
- [Example](#example)
- [Building & Development](#building--development)
    - [Build](#build)
    - [Docker Setup](#docker-setup)
- [Contributing](#contributing)
- [License](#license)
- [Related Projects](#related-projects)
- [Author](#author)

## Overview

KtKit is a Kotlin multiplatform toolkit designed to speed up server-side application development with Ktor. It brings
together several libraries into a cohesive set of tools that handle the repetitive aspects of backend development.

> [!NOTE]
> **Early Stage Project**: KtKit is actively evolving. APIs may change between versions as we refine the abstractions
> based on real-world usage. Production use is possible but expect some breaking changes. Feedback and contributions are
> highly appreciated!

## Usage

```kotlin
implementation("io.github.smyrgeorge:ktkit:x.y.z")
```

## Ergonomics

The example module shows how Arrow's `Raise` and Kotlin context parameters keep handler and service code compact while
preserving explicitness around errors and execution context.

A REST handler extends one of the handler base classes (here `XRealNameRestHandler`), declares its base path in `uri()`
and its routes in `routes()`. Typed helpers (`pathVariable`, `queryParam`, `body<T>()`) parse the inputs, the return
value is serialized as the response, and raised errors map to RFC 9457 `ApiError` responses. Handlers are
auto-registered by the application
(from [TestRestHandler.kt](example/src/commonMain/kotlin/io/github/smyrgeorge/ktkit/example/test/TestRestHandler.kt)):

```kotlin
class TestRestHandler(
    private val testService: TestService
) : XRealNameRestHandler() {
    override fun String.uri(): String = "/api/v1/test$this"

    override fun Route.routes() {
        GET("") {
            log.info { "Hello, ${user.username}!" }
            testService.withTransaction {
                testService.test().map { it.toDto() }
            }
        }
    }
}
```

The service the handler calls uses context parameters for the error channel (`Raise<ErrorSpec>`), the execution context,
and the database scope (`QueryExecutor`/`Transaction`)
(from [TestService.kt](example/src/commonMain/kotlin/io/github/smyrgeorge/ktkit/example/test/TestService.kt)):

```kotlin
class TestService(
    override val db: Driver,
    override val repo: TestRepository,
) : AuditableDatabaseService<Test> {
    val log = Logger.of(this::class)

    context(_: Raise<ErrorSpec>, _: QueryExecutor)
    private suspend fun findAll(): List<Test> = db { repo.findAll() }

    context(_: ExecContext, _: Raise<ErrorSpec>, _: Transaction)
    suspend fun test(): List<Test> {
        log.info { "Fetching all tests" }
        return findAll().also {
            log.info { "Fetched ${it.size} tests" }
        }
    }
}
```

The execution context is a coroutine context element that also carries log4k's tracing context:

```kotlin
class ExecContext(
    val reqId: String,
    val reqTs: Instant,
    val principal: Principal,
    val tracing: TracingContext,
    // Only a part of the context is presented here.
    // Check the documentation for more information.
) : TracingContext by tracing, CoroutineContext.Element
```

This lets handlers and services carry request metadata and tracing without threading parameters manually, while domain
errors are raised through the `Raise<ErrorSpec>` context parameter. The context is propagated in two ways at once: via
`CoroutineContext` and via context parameters in function signatures.

## Modules and features

### Gradle plugin (ktkit-gradle-plugin)

The Gradle plugin is the single entry point of a ktkit service build. A typical service build script (see
the [example module](example/build.gradle.kts)):

```kotlin
plugins {
    kotlin("multiplatform") // or kotlin("jvm")
    id("io.github.smyrgeorge.ktkit") version "x.y.z"
}

kotlin {
    jvm()
    macosArm64 { binaries { executable() } }
    // Include other targets as needed
}

ktkit {
    // Optional: database access via sqlx4k package
    sqlx4k {
        driver = PostgreSQL // also: MySQL, SQLite, SQLiteCipher
        generatedCodePackage = "io.github.smyrgeorge.ktkit.example.generated"
        extensions(Pgmq) // sqlx4k extensions; Pgmq (`ktkit-sqlx4k-pgmq`) is PostgreSQL only
    }
    // Optional: package the jvm target as a runnable, self-contained ("fat") jar (configures `jvmJar`).
    jar {
        mainClass = "io.github.smyrgeorge.ktkit.example.MainKt"
    }
}
```

The full list of options of each `ktkit { }` block is documented in its options file:

| Block         | Options file                                                                                                         | Description                                                                                               |
|---------------|----------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `ktkit { }`   | [KtkitExtension.kt](ktkit-gradle-plugin/src/main/kotlin/io/github/smyrgeorge/ktkit/gradle/KtkitExtension.kt)         | The top-level extension: common options (e.g. `addDependencies`) and the entry point of the blocks below. |
| `jar { }`     | [JarOptions.kt](ktkit-gradle-plugin/src/main/kotlin/io/github/smyrgeorge/ktkit/gradle/jar/JarOptions.kt)             | Packages the jvm target as a runnable, self-contained ("fat") jar.                                        |
| `openApi { }` | [OpenApiOptions.kt](ktkit-gradle-plugin/src/main/kotlin/io/github/smyrgeorge/ktkit/gradle/openapi/OpenApiOptions.kt) | The compile-time OpenAPI generation (the ktkit OpenAPI compiler plugin). Enabled by default.              |
| `sqlx4k { }`  | [Sqlx4kOptions.kt](ktkit-gradle-plugin/src/main/kotlin/io/github/smyrgeorge/ktkit/gradle/sqlx4k/Sqlx4kOptions.kt)    | Database access via sqlx4k: the driver, the generated-code package, and the code-generator settings.      |

### Application bootstrap & configuration

The `Application` wrapper is the entry point of a ktkit service: it manages the Ktor server lifecycle
(startup/shutdown), sets up JSON and the Koin DI container, and auto-registers every REST handler bound as
`AbstractRestHandler`. Tracing, logging, and DI are configured from one place:

```kotlin
Application(
    name = "ExampleApplication",
    conf = Application.Conf(host = "localhost", port = 8080),
    configure = {
        logging {
            // Configure logging.
            level = Level.INFO
            // Log in JSON format:
            // SimpleJsonConsoleLoggingAppender.install()
        }
        tracing {
            // Configure tracing.
        }
        json {
            // Configure JSON serialization.
        }
        ktor {
            // Additional Ktor configuration.
        }
        di {
            single { db }.bind<Driver>()
            singleOf(::TestRestHandler) { bind<AbstractRestHandler>() }
            singleOf(::TestService)
        }
    },
    postConfigure = {
        // After configuration, perform any necessary post-configuration tasks.
    }
).start()
```

A complete bootstrap lives in the example module:
[ExampleApplication.kt](example/src/commonMain/kotlin/io/github/smyrgeorge/ktkit/example/ExampleApplication.kt).

### Kotlinx serialization

Everything that crosses the wire is a `@Serializable` class: REST request/response bodies, the RFC 9457 error responses,
the TOML configuration, and the JSON database columns. There is no reflection involved, so serialization works
identically on JVM and Native targets. The Gradle plugin applies the `kotlinx.serialization` compiler plugin
automatically.

### API errors (RFC 9457)

Domain errors are typed `ErrorSpec` values (e.g. `NotFound`, `Unauthorized`, `Forbidden`, `MissingParameter`,
`DatabaseError`), raised through the `Raise<ErrorSpec>` context parameter instead of thrown. At the REST boundary they
are serialized as [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html) problem-details responses (`ApiError`:
`type`, `title`, `status`, `detail`, plus the `requestId` and `data` extensions), so every service built on the toolkit
reports errors in the same format.

### Security & permissions

Authentication and authorization are built into the request pipeline of `AbstractRestHandler`.

**Authentication** — `AbstractRestHandler` takes a `PrincipalExtractor`, which resolves the authenticated `Principal`
from the incoming request. When the extractor yields no principal, the handler falls back to its `defaultUser` (that is
how `AnonymousRestHandler` works — it defaults to the anonymous principal); otherwise the request is rejected with a 401
`Unauthorized`. Ready-made handler base classes wire the extractor for you (e.g. `XRealNameRestHandler` uses
`XRealNamePrincipalExtractor`).

The `PrincipalExtractor` implementations:

| Extractor                     | Source                                                                                                                                | Description                                                                                                             |
|-------------------------------|---------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| `XRealNamePrincipalExtractor` | [XRealNamePrincipalExtractor.kt](ktkit/src/commonMain/kotlin/io/github/smyrgeorge/ktkit/api/auth/impl/XRealNamePrincipalExtractor.kt) | Decodes a base64-encoded JSON `Principal` from the `x-real-name` header, set by a trusted reverse proxy or API gateway. |
| `BearerPrincipalExtractor`    | *planned*                                                                                                                             | JWT Bearer-token authentication (see the planned features).                                                             |

> [!WARNING]
> The `x-real-name` mechanism is **not safe to expose directly to the internet**. It assumes a trusted reverse proxy or
> API gateway in front of the application that authenticates the user, strips any incoming `x-real-name` header, and
> sets it with the authenticated user's information before forwarding. Without such a proxy, any client could forge the
> header and impersonate any user.

**Permissions** — authorization runs after authentication in two layers, and both must pass (a failure responds with a
403 `Forbidden` API error):

- **Role-based** — a `Principal` carries a set of roles; the handler constructor accepts `hasRole`, `hasAnyRole` and
  `hasAllRoles` constraints, enforced on every route of the handler.
- **Custom predicates** — both the handler constructor and every route call accept a
  `permissions: HttpContext.() -> Boolean` function (handler-level and route-level predicates are combined with AND).

```kotlin
class AdminRestHandler : XRealNameRestHandler(
    hasRole = "admin", // Enforced on every route of the handler.
) {
    override fun String.uri(): String = "/api/v1/admin$this"

    override fun Route.routes() {
        GET("/reports", permissions = { user.hasRole("reports:read") }) {
            // ...
        }
    }
}
```

### Logging, tracing & metrics (log4k)

Observability is built on [log4k](https://github.com/smyrgeorge/log4k) — a multiplatform logging library with tracing
and metrics. The `ExecContext` carries the request's tracing context end to end, so log lines and spans are correlated
automatically across REST handlers, database transactions and queries, and PGMQ messages. The log4k annotations
(`@Traced`, `@Timed`, `@Logged`) instrument service methods declaratively.

### Health & metrics endpoints

Every application serves two unauthenticated status endpoints out of the box:

- `GET /api/status/health` — application name, status, start time, and uptime
- `GET /api/status/metrics` — the log4k metrics registry in OpenMetrics line format (VM memory and processor gauges are
  collected by default; register your own via log4k's `Meter`)

### TOML Configuration Loading

`ConfigPropertiesToml` loads TOML files into `@Serializable` data classes, with environment-variable interpolation
(`${VAR_NAME}`) and layered overrides: `load()` reads `application.toml` from resources as the base, then merges the
first override found among `application.toml`, `config/application.toml`, `application.local.toml` and
`config/application.local.toml` (override values win).

```toml
# src/commonMain/resources/application.toml
[database]
url = "postgresql://${DB_HOST}/mydb"
maxConnections = 10
```

```kotlin
@Serializable
data class AppConfig(val database: DatabaseConfig)

val config: AppConfig = ConfigPropertiesToml.load()
```

For the full API (loading from a specific file, explicit base/override merging), see
[ConfigPropertiesToml.kt](ktkit/src/commonMain/kotlin/io/github/smyrgeorge/ktkit/api/props/ConfigPropertiesToml.kt).

### Database support (sqlx4k)

Database access is built on [sqlx4k](https://github.com/smyrgeorge/sqlx4k) — a coroutine-first SQL toolkit for Kotlin
Multiplatform with compile-time query validation. PostgreSQL, MySQL/MariaDB and SQLite are supported, on JVM and Native
targets alike.

The `ktkit-sqlx4k` module glues sqlx4k into the toolkit: `@Table` entities with auto-managed audit columns (`createdAt`/
`createdBy`/`updatedAt`/`updatedBy`), `@Repository` interfaces implemented at compile time by the sqlx4k code generator,
traced transactions and queries, error mapping into the ktkit error model, and JSON-column encoders for `@Serializable`
classes. PGMQ (a message queue on Postgres) is available through `ktkit-sqlx4k-pgmq`.

- How the integration works: [ktkit-sqlx4k](ktkit-sqlx4k/README.md)
- The full sqlx4k feature list: [smyrgeorge/sqlx4k](https://github.com/smyrgeorge/sqlx4k)

### Queue support (PGMQ)

Message-queue support is built on [PGMQ](https://github.com/pgmq/pgmq) — a lightweight message queue on Postgres, like
AWS SQS and RSMQ — via sqlx4k's PGMQ client (PostgreSQL only).

The `ktkit-sqlx4k-pgmq` module glues it into the toolkit: the `Pgmq` wrapper and `AbstractPgmqEventHandler`, which
propagates tracing and the authenticated user through message headers (in both directions — `send` attaches them,
consuming restores them into a fresh `ExecContext`), and manages the consumer lifecycle with retries and graceful
shutdown. Enabled via `sqlx4k { extensions(Pgmq) }` in the Gradle plugin.

- How the integration works: [ktkit-sqlx4k-pgmq](ktkit-sqlx4k-pgmq/README.md)
- The PGMQ project: [pgmq/pgmq](https://github.com/pgmq/pgmq)

### OpenAPI generation (ktkit-compiler-openapi)

A Kotlin compiler plugin that generates the OpenAPI 3.1 specification of your REST handlers at compile time — no
reflection, works on every KMP target (JVM and Native). It is attached automatically by
the [Gradle plugin](#gradle-plugin-ktkit-gradle-plugin) (turn it off with `ktkit { openApi { enabled = false } }`).

At runtime the framework merges the generated fragments of all registered handlers and serves the interactive Swagger UI
at `GET /api/docs`, and the merged OpenAPI 3.1 document at `GET /api/docs/openapi.json`.

- What the plugin provides (analysis rules, `@OpenApi` metadata, configuration, limitations):
  [ktkit-compiler-openapi](ktkit-compiler-openapi/README.md)

## Example

Check the example application [here](example/src/commonMain/kotlin/io/github/smyrgeorge/ktkit/example).

## Building & Development

### Build

On a clean checkout (and after every version bump), bootstrap the build first — it publishes the ktkit Gradle plugin and
the OpenAPI compiler plugin to mavenLocal, which the example module needs before the main build can even configure
(see [bootstrap.sh](scripts/bootstrap.sh)):

```bash
./scripts/bootstrap.sh
```

Then build as usual:

```bash
./gradlew build
```

### Docker Setup

The project includes a `docker-compose.yml` for PostgreSQL:

```bash
docker-compose up -d
```

## Contributing

This is an open-source project. Contributions are welcome!

## License

Check the repository for license information.

## Related Projects

- [log4k](https://github.com/smyrgeorge/log4k) – Multiplatform logging with tracing
- [sqlx4k](https://github.com/smyrgeorge/sqlx4k) – Multiplatform database access

## Author

Yorgos S. ([@smyrgeorge](https://github.com/smyrgeorge))
