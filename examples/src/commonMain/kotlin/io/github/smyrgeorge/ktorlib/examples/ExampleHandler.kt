package io.github.smyrgeorge.ktorlib.examples

import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktorlib.domain.UserToken
import io.github.smyrgeorge.log4k.Logger
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Example REST handler demonstrating how to use the AbstractRestHandler framework.
 *
 * This example shows:
 * 1. How to extend AbstractRestHandler
 * 2. How to define routes using Ktor's routing DSL
 * 3. How to use the `of()` method for permission-based execution
 * 4. How to handle requests with context
 * 5. How to implement user impersonation
 */
class ExampleHandler : AbstractRestHandler() {

    override val log = Logger.of(this::class)

    /**
     * Defines the base URI prefix for all routes in this handler.
     */
    override fun String.uri(): String = "/api/v1$this"

    /**
     * Defines the routes for this handler.
     */
    override fun Route.routes() {
        // Example 1: Simple GET endpoint
        get("/hello".uri()) {
            // Create a mock user for demonstration
            val user = createMockUser()

            // Handle the request with context
            val result = handle(call, user) {
                of { ctx ->
                    // Access user information from context
                    log.info("User ${ctx.user.username} accessed /hello")
                    "Hello, ${ctx.user.username}!"
                }
            }

            call.respond(HttpStatusCode.OK, result)
        }

        // Example 2: GET with permission check
        get("/admin".uri()) {
            val user = createMockUser()

            val result = handle(call, user) {
                // Only allow users with "ADMIN" role
                of(permissions = { ctx -> ctx.user.hasRole("ADMIN") }) { ctx ->
                    log.info("Admin ${ctx.user.username} accessed /admin")
                    "Welcome, admin ${ctx.user.username}!"
                }
            }

            call.respond(HttpStatusCode.OK, result)
        }

        // Example 3: GET with path parameter
        get("/users/{id}".uri()) {
            val user = createMockUser()

            val result = handle(call, user) {
                of { ctx ->
                    // Extract path parameter
                    val userId = ctx.req.pathVariable("id").asString()
                    log.info("Fetching user: $userId")

                    mapOf(
                        "id" to userId,
                        "name" to "Example User",
                        "email" to "user@example.com"
                    )
                }
            }

            call.respond(HttpStatusCode.OK, result)
        }

        // Example 4: GET with query parameters
        get("/search".uri()) {
            val user = createMockUser()

            val result = handle(call, user) {
                of { ctx ->
                    // Extract query parameters
                    val query = ctx.req.queryParam("q").asStringOrNull() ?: ""
                    val limit = ctx.req.queryParam("limit").asIntOrNull() ?: 10

                    log.info("Searching for: $query (limit: $limit)")

                    mapOf(
                        "query" to query,
                        "limit" to limit,
                        "results" to emptyList<String>()
                    )
                }
            }

            call.respond(HttpStatusCode.OK, result)
        }

        // Example 5: Using handleAndRespond for automatic response handling
        get("/auto-respond".uri()) {
            val user = createMockUser()

            handleAndRespond(call, user) {
                of { ctx ->
                    log.info("Auto-responding for ${ctx.user.username}")
                    mapOf("status" to "success", "data" to "Hello!")
                }
            }
        }
    }

    /**
     * Creates a mock user for demonstration purposes.
     * In a real application, this would come from authentication/authorization.
     */
    private fun createMockUser() = UserToken(
        uuid = "00000000-0000-0000-0000-000000000001",
        username = "demo-user",
        email = "demo@example.com",
        name = "Demo User",
        firstName = "Demo",
        lastName = "User",
        roles = setOf("USER", "ADMIN")
    )
}

/**
 * Example of how to register the handler in your Ktor application.
 *
 * In your Application.module() function:
 * ```
 * fun Application.module() {
 *     install(ContentNegotiation) {
 *         json()
 *     }
 *
 *     routing {
 *         val handler = ExampleHandler()
 *         with(handler) {
 *             routes()
 *         }
 *     }
 * }
 * ```
 */
