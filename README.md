# KtKit

![Build](https://github.com/smyrgeorge/ktkit/actions/workflows/ci.yml/badge.svg)
![Maven Central](https://img.shields.io/maven-central/v/io.github.smyrgeorge/ktkit)
![GitHub License](https://img.shields.io/github/license/smyrgeorge/ktkit)
![GitHub commit activity](https://img.shields.io/github/commit-activity/w/smyrgeorge/ktkit)
![GitHub issues](https://img.shields.io/github/issues/smyrgeorge/ktkit)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.0-blue.svg?logo=kotlin)](http://kotlinlang.org)

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

KtKit is a Kotlin multiplatform toolkit designed to speed up server-side application development with Ktor.
It brings together several libraries into a cohesive set of tools that handle the repetitive aspects of backend
development.

> [!NOTE]
> **Early Stage Project**: KtKit is actively evolving. APIs may change between versions as we refine the abstractions
> based on real-world usage. Production use is possible but expect some breaking changes. Feedback and contributions are
> highly appreciated!

**What it does (today):**

- Provides a small application bootstrap around Ktor with DI, JSON, and auto-registered REST handlers
- Standardizes request handling with tracing, auth/permissions hooks, and RFC 9457-style API errors
- Exposes basic health and metrics endpoints for services built on the toolkit
- Offers TOML configuration loading with environment-variable interpolation and file/resource merging
- Adds convenience helpers for retries, JSON/TOML utilities, and KMP-friendly file/http/process access
- Uses Arrow (Raise/Either) and Kotlin context parameters to keep error handling and context passing lightweight

**Planned features:**

- [ ] Integration with Arrow's resilience libraries (e.g. Retry, Resource, Circuit Breaker)
- [ ] Write extensive examples
- [ ] Write extensive tests
- [ ] Write extensive documentation
- [ ] BearerPrincipalExtractor for JWT authentication
- [x] sqlx4k integration
- [x] PGMQ integration
- [ ] Extend this list with more ideas 🧐!

## Modules and features

### Core (`ktkit`)

- `Application` wrapper for Ktor server startup/shutdown, JSON setup, Koin DI, and routing
- `AbstractRestHandler` with typed request helpers, `ExecContext` propagation, and error mapping
- Built-in `/api/status/health` and `/api/status/metrics` endpoints
- Error model (`ErrorSpec`/`ApiError`) aligned with RFC 9457 conventions
- Config loader for TOML with environment substitution and layered overrides

#### Security: X-Real-Name Header Authentication

> [!WARNING]
> The `XRealNamePrincipalExtractor` and `XRealNameRestClient` use a base64-encoded JSON header (`x-real-name`) to
> identify the authenticated user. **This mechanism is not safe to expose directly to the internet.**

This pattern assumes a trusted reverse proxy or API gateway sits in front of your application and:

1. Authenticates the user (e.g., via OAuth, JWT, or session cookies)
2. Strips any incoming `x-real-name` header from client requests
3. Sets the `x-real-name` header with the authenticated user's information before forwarding

If your application is exposed directly to the internet without such a proxy, any client can forge the header and
impersonate any user. Only use this extractor when your application runs behind a trusted infrastructure layer that
controls this header.

#### TOML Configuration Loading

`ConfigPropertiesToml` loads and deserializes TOML files into `@Serializable` data classes. It supports environment
variable interpolation (`${VAR_NAME}`), loading from resources or the filesystem, and merging a base config with an
override file (override values take precedence).

The `load()` method reads `application.toml` from resources as the base, then looks for an override in this order:
`application.toml`, `config/application.toml`, `application.local.toml`, `config/application.local.toml`.

```kotlin
// Define your config as a @Serializable data class
@Serializable
data class AppConfig(val server: ServerConfig, val database: DatabaseConfig)
@Serializable
data class ServerConfig(val host: String, val port: Int)
@Serializable
data class DatabaseConfig(val url: String, val maxConnections: Int)
```

```toml
# src/commonMain/resources/application.toml (base config)
[server]
host = "localhost"
port = 8080

[database]
url = "postgresql://${DB_HOST}/mydb"
maxConnections = 10
```

```toml
# config/application.local.toml (local override)
[database]
maxConnections = 50
```

```kotlin
// Auto-load with layered overrides
val config: AppConfig = ConfigPropertiesToml.load()

// Or load from a specific file
val config: AppConfig = ConfigPropertiesToml.loadFromFileSystem("path/to/config.toml")

// Or merge two files explicitly
val config: AppConfig = ConfigPropertiesToml.loadFromFileSystem(
    base = "base.toml",
    override = "override.toml"
)
```

### Ktor HTTP Client (`ktkit-ktor-httpclient`)

A multiplatform REST client abstraction built on Ktor's HttpClient with functional error handling via Arrow's `Raise`.

- `HttpClientFactory` for creating pre-configured HttpClient instances with timeouts, connection pooling, and JSON setup
- `AbstractRestClient` base class with typed HTTP methods (GET, POST, PUT, PATCH, DELETE, etc.)
- Built-in implementations: `BearerRestClient` (Bearer token auth) and `XRealNameRestClient` (X-Real-Name header auth)
- Error handling via sealed `RestClientErrorSpec` hierarchy

### sqlx4k integration (`ktkit-sqlx4k`)

> A coroutine-first SQL toolkit with compile-time query validations for Kotlin Multiplatform. PostgreSQL,
> MySQL/MariaDB, and SQLite supported.

- `DatabaseService` helpers for error mapping and traced transactions
- `AuditableRepository` hooks for `createdAt/createdBy/updatedAt/updatedBy`
- `AuditableDatabaseService` interface for services with auditable entities and a `save()` extension
- `JsonSupport` utility for JSON column serialization with sqlx4k's `ValueEncoder` system

### PGMQ integration (`ktkit-sqlx4k-pgmq`)

> A lightweight message queue. Like AWS SQS and RSMQ but on Postgres.

- `Pgmq` wrapper and `AbstractPgmqEventHandler` with trace/user propagation
- Consumer lifecycle helpers with retry + shutdown handling

## Ergonomics (Arrow + context-parameters)

The example module shows how Arrow's `Raise` and Kotlin context parameters keep service code compact while
preserving explicitness around errors and execution context:

```kotlin
class TestService(
    override val db: Driver,
    override val repo: TestRepository,
) : AuditableDatabaseService<Test> {
    val log = Logger.of(this::class)

    context(_: ExecContext, _: QueryExecutor)
    private suspend fun findAll(): List<Test> = db { repo.findAll() }

    context(_: ExecContext, _: Transaction)
    suspend fun test(): List<Test> {
        log.info { "Fetching all tests" }
        return findAll().also {
            log.info { "Fetched ${it.size} tests" }
        }
    }
}
```

The execution context is a coroutine context element that also implements Arrow's `Raise` and log4k's tracing context:

```kotlin
class ExecContext(
    val reqId: String,
    val reqTs: Instant,
    val principal: Principal,
    // Only a part of the context is presented here.
    // Check the documentation for more information.
) : Raise<ErrorSpec>, TracingContext by tracing, CoroutineContext.Element
```

This lets handlers and services raise domain errors, access tracing, and carry request metadata without threading
parameters manually. The context is propagated in two ways at once: via `CoroutineContext` and via context
parameters in function signatures.

## Example

Check the example application [here](example/src/commonMain/kotlin/io/github/smyrgeorge/ktkit/example).

## Building & Development

### Build

```bash
# Build all modules, for Jvm and your current platform
./gradlew build

# Build all modules for all supported platforms
./gradlew build -Ptargets=all
#
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
