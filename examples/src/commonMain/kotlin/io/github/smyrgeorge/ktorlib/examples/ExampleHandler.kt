package io.github.smyrgeorge.ktorlib.examples

import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.log4k.Logger
import io.ktor.http.*
import io.ktor.server.routing.*

/**
 * Example REST handler demonstrating the usage of the AbstractRestHandler.
 *
 * This handler requires the Authentication plugin to be installed in your application.
 */
class ExampleHandler : AbstractRestHandler() {

    override val log = Logger.of(this::class)

    override fun String.uri(): String = "/api/v1$this"

    override fun Route.routes() {
        // Example 1: Simple GET endpoint
        GET("/hello") {
            // Access user information from context
            log.info("User ${user.username} accessed /hello")
            "Hello, ${user.username}!"
        }

        // Example 2: GET with permission check
        GET(
            path = "/admin",
            permissions = { ctx -> ctx.user.hasRole("ADMIN") }
        ) {
            log.info("Admin ${user.username} accessed /admin")
            "Welcome, admin ${user.username}!"
        }

        // Example 3: GET with path parameter
        GET("/users/{id}") {
            // Extract path parameter
            val userId = httpRequest.pathVariable("id").asString()
            log.info("Fetching user: $userId")

            mapOf(
                "id" to userId,
                "name" to "Example User",
                "email" to "user@example.com"
            )
        }

        // Example 4: GET with query parameters
        GET("/search") {
            // Extract query parameters
            val query = httpRequest.queryParam("q").asStringOrNull() ?: ""
            val limit = httpRequest.queryParam("limit").asIntOrNull() ?: 10

            log.info("Searching for: $query (limit: $limit)")

            mapOf(
                "query" to query,
                "limit" to limit,
                "results" to emptyList<String>()
            )
        }

        // Example 5: Using custom success code
        GET(
            path = "/auto-respond",
            onSuccessHttpCode = HttpStatusCode.Accepted
        ) {
            log.info("Auto-responding for ${user.username}")
            mapOf("status" to "success", "data" to "Hello!")
        }
    }
}

/**
 * Example of how to register the handler in your Ktor application.
 *
 * In your Application.module() function:
 * ```
 * import io.github.smyrgeorge.ktorlib.api.rest.auth.xRealName
 * import io.ktor.serialization.kotlinx.json.*
 * import io.ktor.server.application.*
 * import io.ktor.server.auth.*
 * import io.ktor.server.plugins.contentnegotiation.*
 * import io.ktor.server.routing.*
 *
 * fun Application.module() {
 *     // Install the ContentNegotiation plugin
 *     install(ContentNegotiation) {
 *         json()
 *     }
 *
 *     // Install Ktor's Authentication plugin with X-Real-Name provider
 *     install(Authentication) {
 *         xRealName {
 *             headerName = "x-real-name" // optional, this is the default
 *         }
 *     }
 *
 *     // Register your routes
 *     routing {
 *         // All routes will require authentication by default
 *         with(ExampleHandler()) {
 *             routes()
 *         }
 *     }
 * }
 * ```
 *
 * Alternative approach using the generic ktorlib provider:
 * ```
 * import io.github.smyrgeorge.ktorlib.api.rest.auth.ktorlib
 * import io.github.smyrgeorge.ktorlib.api.rest.auth.XRealNameAuthenticationExtractor
 *
 * fun Application.module() {
 *     install(ContentNegotiation) {
 *         json()
 *     }
 *
 *     // Use the generic ktorlib provider for maximum flexibility
 *     install(Authentication) {
 *         ktorlib {
 *             extractor = XRealNameAuthenticationExtractor(headerName = "x-real-name")
 *         }
 *     }
 *
 *     routing {
 *         with(ExampleHandler()) {
 *             routes()
 *         }
 *     }
 * }
 * ```
 *
 * You can also use multiple authentication providers:
 * ```
 * fun Application.module() {
 *     install(Authentication) {
 *         // Approach 1: Using convenience method
 *         xRealName("internal") {
 *             headerName = "x-real-name"
 *         }
 *
 *         // Approach 2: Using generic provider
 *         ktorlib("dev") {
 *             extractor = XRealNameAuthenticationExtractor(headerName = "x-dev-user")
 *         }
 *
 *         // Future: You can easily add more authentication extractors
 *         // ktorlib("bearer") {
 *         //     extractor = BearerAuthenticationExtractor { token ->
 *         //         // JWT validation logic here
 *         //         UserToken(...)
 *         //     }
 *         // }
 *     }
 *
 *     routing {
 *         // Routes without authentication
 *         get("/health") {
 *             call.respondText("OK")
 *         }
 *
 *         // Routes with authentication using the "internal" provider
 *         authenticate("internal") {
 *             with(ExampleHandler()) {
 *                 routes()
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * Creating a custom AuthenticationExtractor:
 * ```
 * class CustomAuthExtractor : AuthenticationExtractor {
 *     override suspend fun extract(call: ApplicationCall): UserToken? {
 *         // Your custom authentication logic here
 *         val token = call.request.headers["X-Custom-Auth"] ?: return null
 *         return UserToken(
 *             uuid = "extracted-from-token",
 *             username = "custom-user",
 *             roles = setOf("USER")
 *         )
 *     }
 * }
 *
 * // Then use it:
 * install(Authentication) {
 *     ktorlib {
 *         extractor = CustomAuthExtractor()
 *     }
 * }
 * ```
 */
