# ktkit-compiler-openapi

A Kotlin compiler plugin that generates the OpenAPI 3.1 specification of your REST handlers at compile time.

The plugin is attached automatically by the [ktkit Gradle plugin](../README.md#gradle-plugin-ktkit-gradle-plugin) and
can be turned off with `ktkit { openApi { enabled = false } }`.

## How it works

The compiler plugin statically analyzes every concrete `AbstractRestHandler` and bakes an OpenAPI fragment into the
class (a generated `openApiSpec()` override). At runtime, the framework merges the fragments of all registered handlers
and serves:

- `GET /api/docs` — interactive Swagger UI
- `GET /api/docs/openapi.json` — the merged OpenAPI 3.1 document

(the `/api/docs` base path is configurable via `openApi.basePath`, see [Runtime configuration](#runtime-configuration))

## What the analysis infers

Per route:

- Full paths (the `uri()` prefix applied to each `GET`/`POST`/... call) and success status codes (including explicit
  `onSuccessHttpStatusCode` arguments)
- Path/query/header parameters from `pathVariable`/`queryParam`/`header` usage — `asInt()`, `asBooleanOrNull()`, ...
  determine the type, and `*OrNull` conversions mark a parameter optional
- Request bodies from `body<T>()` and response schemas from the handler's return type (unwrapping `Either`/`Result`/
  `Flow`), generated from `@Serializable` classes (`@SerialName`, `@Transient`, default values, nullability, enums and
  sealed hierarchies with the `@type` discriminator are respected)
- The standard `ApiError` error responses: 400 when the route has inputs, 401/403 for authenticated (non-anonymous)
  handlers, 500 on every route (unexpected errors), and the ktkit error types constructed directly inside the route
  lambda — errors raised deeper in your services are not visible to the static analysis and are not documented. The
  error responses are defined once as shared `components.responses` entries (`BadRequest`, `Unauthorized`, ...) and
  referenced from each operation
- Ktor `route("...") { }` groups nested inside `routes()` (their path segments prefix the documented paths)

## Enriching route metadata

Route metadata is provided with the `@OpenApi` annotation placed directly above the route call:

```kotlin
@OpenApi(
    summary = "Returns a single user by id.",
    description = "A longer, multi-line description.",
    tags = ["users"],
)
GET("/{id}") {
    // your code goes here...
}
```

Besides the fields shown above, the annotation supports `deprecated` (marks the operation deprecated, with the given
text as the reason). A single route can be excluded with `@OpenApiIgnore` placed directly above the route call, and a
whole handler by annotating its class.

## Runtime configuration

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

## Known limitations

- `uri()` and `routes()` must be implemented in the same Gradle module as the concrete handler (or an intermediate base
  class of the same module) — the compiler plugin cannot read function bodies from dependency modules. An unresolvable
  `uri()` produces a compile warning and the per-route paths are used as-is.
- The fragment baked into a handler is refreshed when the handler's file is recompiled. Kotlin's incremental compilation
  tracks the types a handler references, but an edit that changes only an `@OpenApi` annotation in a *different*
  file (e.g. a base class) may require a clean build to be picked up.
- Route paths and parameter names must be compile-time string constants; dynamic values produce a warning and the route
  (or parameter) is skipped.
- Response types that are not `@Serializable` (or use custom serializers) are documented as free-form objects, with a
  compile warning.

A complete working setup lives in the [example module](../example).
