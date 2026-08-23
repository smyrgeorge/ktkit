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

### OpenAPI generation (`ktkit-gradle-plugin` + `ktkit-openapi-compiler-plugin`)

A Kotlin compiler plugin (similar
to [Ktor's OpenAPI spec generation](https://ktor.io/docs/openapi-spec-generation.html))
that generates the OpenAPI 3.1 specification of your REST handlers at compile time — no reflection, works on every KMP
target (JVM and Native).

Apply the Gradle plugin to the module (s) containing your REST handlers. The plugin is the single
entry point of a ktkit service build: it attaches the ktkit compiler plugins, automatically applies
the `kotlinx.serialization` and `log4k` Gradle plugins, adds the ktkit core dependency, and exposes
the `ktkit { }` DSL where the optional modules/integrations are enabled and configured from one
place:

```kotlin
plugins {
    org.jetbrains.kotlin.multiplatform // or org.jetbrains.kotlin.jvm
    id("io.github.smyrgeorge.ktkit") version "x.y.z"
}

ktkit {
    // Whether enabling a module also adds its ktkit library dependencies
    // (at the versions the plugin was built with). Defaults to true.
    addDependencies = true

    openApi {
        enabled = true // default
    }

    // Optional: database access via sqlx4k — applies KSP, registers the sqlx4k code generator
    // on commonMain (wiring the generated sources and task ordering), and adds `ktkit-sqlx4k`
    // plus the dialect's driver.
    sqlx4k {
        driver = PostgreSQL // required (also: MySQL, SQLite, SQLiteCipher)
        outputPackage = "com.example.generated"
        // Where the sqlx4k-annotated code lives — also: "main" (plain JVM), "jvmMain", ...
        sourceSets = listOf("commonMain") // default
        pgmq = false           // PGMQ integration (`ktkit-sqlx4k-pgmq`) — PostgreSQL only
        // arg("key", "value") // extra sqlx4k codegen (KSP) arguments
    }

    // Optional: package the jvm target as a runnable, self-contained jar (configures `jvmJar`).
    jar {
        mainClass = "com.example.MainKt"
        // archiveFileName = "<project-name>.jar" // default
        // duplicatesStrategy = EXCLUDE           // default
    }
}
```

The compiler plugin statically analyzes every concrete `AbstractRestHandler` and bakes an OpenAPI fragment into the
class (a generated `openApiSpec()` override). At runtime, the framework merges the fragments of all registered handlers
and serves:

- `GET /api/docs` — interactive Swagger UI
- `GET /api/docs/openapi.json` — the merged OpenAPI 3.1 document

(the `/api/docs` base path is configurable via `openApi.basePath`)

The analysis infers, per route:

- Full paths (the `uri()` prefix applied to each `GET`/`POST`/... call) and success status codes (including explicit
  `onSuccessHttpStatusCode` arguments)
- Path/query/header parameters from `pathVariable`/`queryParam`/`header` usage — `asInt()`, `asBooleanOrNull()`, ...
  determine the type, and `*OrNull` conversions mark a parameter optional
- Request bodies from `body<T>()` and response schemas from the handler's return type (unwrapping `Either`/`Result`/
  `Flow`), generated from `@Serializable` classes (`@SerialName`, `@Transient`, default values, nullability, enums and
  sealed hierarchies with the `@type`
  discriminator are respected)
- The standard `ApiError` error responses: 400 when the route has inputs, 401/403 for authenticated (non-anonymous)
  handlers, and the ktkit error types constructed directly inside the route lambda — errors raised deeper in your
  services are not visible to the static analysis; document those with a `Response:` KDoc tag
- Ktor `route("...") { }` groups nested inside `routes()` (their path segments prefix the documented paths)

Route metadata can be enriched in two ways. The primary, type-checked way is the `@OpenApi` annotation placed directly
above the route call:

```kotlin
@OpenApi(
    summary = "Returns a single user by id.",
    description = "A longer, multi-line description.",
    tags = ["users"],
    params = [
        OpenApiParam(name = "id", location = "path", type = "int", description = "The id of the user."),
        OpenApiParam(name = "verbose", type = "boolean", description = "Whether to include details."),
    ],
    responses = [OpenApiResponse(code = 404, description = "The user was not found.")],
)
GET("/{id}") {
    // your code goes here...
}
```

When the annotation is absent, the compiler plugin falls back to a KDoc comment above the route call (when both are
present, the annotation wins — they are never merged):

```kotlin
/**
 * Returns a single user by id.
 *
 * A longer, multi-line description.
 * Tag: users
 * Path: id [Int] The id of the user.
 * Query: verbose [Boolean] Whether to include details.
 * Response: 404 The user was not found.
 */
GET("/{id}") {
    // your code goes here...
}
```

Supported KDoc tags: `Tag:`, `Path:`, `Query:`, `Header:`, `Body:`, `Response:`/`Responses:`, `Description:`,
`Deprecated:`, `OperationId:`, `Security: none` and `Ignore:`. The `@OpenApi` annotation covers the same metadata with
type-checked parameters. A whole handler can be excluded with `@OpenApiIgnore`.

The documentation endpoints are enabled by default, **served without authentication** (disable them via
`openApi.enabled = false` if your API is not meant to be publicly documented), and configurable via the application
configuration:

```kotlin
Application(
    name = "my-app",
    conf = Application.Conf(
        openApi = Application.Conf.OpenApi(
            enabled = true,
            basePath = "/api/docs", // where the UI and <basePath>/openapi.json are served
            title = "My API",       // defaults to the application name
            version = "1.0.0",
            description = "...",
            servers = listOf("https://api.example.com"), // defaults to http://<host>:<port>
            theme = Application.Conf.OpenApi.Theme.AUTO, // AUTO (follows the OS), LIGHT or DARK
            // Self-host the Swagger UI assets (defaults to the unpkg CDN):
            // swaggerUiCss = "/assets/swagger-ui.css",
            // swaggerUiJs = "/assets/swagger-ui-bundle.js",
        )
    ),
)
```

**Known limitations:**

- `uri()` and `routes()` must be implemented in the same Gradle module as the concrete handler (or an intermediate base
  class of the same module) — the compiler plugin cannot read function bodies from dependency modules. An unresolvable
  `uri()` produces a compile warning and the per-route paths are used as-is.
- The fragment baked into a handler is refreshed when the handler's file is recompiled. Kotlin's incremental compilation
  tracks the types a handler references, but an edit that changes only a KDoc comment in a *different*
  file (e.g. a base class) may require a clean build to be picked up.
- Route paths and parameter names must be compile-time string constants; dynamic values produce a warning and the route
  (or parameter) is skipped.
- Response types that are not `@Serializable` (or use custom serializers) are documented as free-form objects, with a
  compile warning.

## Ergonomics (Arrow + context-parameters)

The example module shows how Arrow's `Raise` and Kotlin context parameters keep service code compact while preserving
explicitness around errors and execution context:

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
parameters manually. The context is propagated in two ways at once: via `CoroutineContext` and via context parameters in
function signatures.

## Example

Check the example application [here](example/src/commonMain/kotlin/io/github/smyrgeorge/ktkit/example).

## Building & Development

### Build

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
