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

## Usage

```kotlin
implementation("io.github.smyrgeorge:ktkit:x.y.z")
```

## Overview

KtKit is a Kotlin multiplatform toolkit designed to speed up server-side application development with Ktor. It brings
together several libraries into a cohesive set of tools that handle the repetitive aspects of backend development.

> [!NOTE]
> **Early Stage Project**: KtKit is actively evolving. APIs may change between versions as we refine the abstractions
> based on real-world usage. Production use is possible but expect some breaking changes. Feedback and contributions are
> highly appreciated!

**What it does (today):**

- Provides a small application bootstrap around Ktor with DI, JSON, and auto-registered REST handlers
- Standardizes request handling with tracing, auth/permissions hooks, and RFC 9457-style API errors
- Exposes basic health and metrics endpoints for services built on the toolkit
- Generates the OpenAPI specification of your REST handlers at compile time (via a Kotlin compiler plugin) and serves it
  with Swagger UI at `/api/docs`
- Offers TOML configuration loading with environment-variable interpolation and file/resource merging
- Adds convenience helpers for retries, JSON/TOML utilities, and KMP-friendly file/http/process access
- Uses Arrow (Raise/Either) and Kotlin context parameters to keep error handling and context passing lightweight

**Planned features:**

- [x] Gradle Plugin
- [ ] Write extensive examples
- [ ] Write extensive tests
- [ ] Write extensive documentation
- [ ] BearerPrincipalExtractor for JWT authentication
- [x] sqlx4k integration
- [x] PGMQ integration
- [x] OpenApi generation
- [ ] ~~Integration with Arrow's resilience libraries (e.g. Retry, Resource, Circuit Breaker)~~
- [ ] Extend this list with more ideas 🧐!

## Modules and features

### Gradle plugin (`ktkit-gradle-plugin`)

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
        mainClass = "io.github.smyrgeorge.ktkit.example.ExampleApplicationKt"
    }
}
```

The full list of options of each `ktkit { }` block is documented in its options file:

- `ktkit { }` —
  [KtkitExtension.kt](ktkit-gradle-plugin/src/main/kotlin/io/github/smyrgeorge/ktkit/gradle/KtkitExtension.kt)
- `jar { }` —
  [JarOptions.kt](ktkit-gradle-plugin/src/main/kotlin/io/github/smyrgeorge/ktkit/gradle/jar/JarOptions.kt)
- `openApi { }` —
  [OpenApiOptions.kt](ktkit-gradle-plugin/src/main/kotlin/io/github/smyrgeorge/ktkit/gradle/openapi/OpenApiOptions.kt)
- `sqlx4k { }` —
  [Sqlx4kOptions.kt](ktkit-gradle-plugin/src/main/kotlin/io/github/smyrgeorge/ktkit/gradle/sqlx4k/Sqlx4kOptions.kt)

### Ergonomics (Arrow + context-parameters)

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

#### TOML Configuration Loading

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
