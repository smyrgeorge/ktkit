package io.github.smyrgeorge.ktorlib.examples

import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktorlib.domain.UserToken
import io.github.smyrgeorge.log4k.Logger
import io.ktor.http.*
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
            handle(call, user) {
                // Access user information from context
                log.info("User ${user.username} accessed /hello")
                "Hello, ${user.username}!"
            }
        }

        // Example 2: GET with permission check
        get("/admin".uri()) {
            val user = createMockUser()

            // Only allow users with "ADMIN" role
            handle(call, user, permissions = { ctx -> ctx.user.hasRole("ADMIN") }) {
                log.info("Admin ${user.username} accessed /admin")
                "Welcome, admin ${user.username}!"
            }
        }

        // Example 3: GET with path parameter
        get("/users/{id}".uri()) {
            val user = createMockUser()
            handle(call, user) {
                // Extract path parameter
                val userId = req.pathVariable("id").asString()
                log.info("Fetching user: $userId")

                mapOf(
                    "id" to userId,
                    "name" to "Example User",
                    "email" to "user@example.com"
                )
            }
        }

        // Example 4: GET with query parameters
        get("/search".uri()) {
            val user = createMockUser()

            handle(call, user) {
                // Extract query parameters
                val query = req.queryParam("q").asStringOrNull() ?: ""
                val limit = req.queryParam("limit").asIntOrNull() ?: 10

                log.info("Searching for: $query (limit: $limit)")

                mapOf(
                    "query" to query,
                    "limit" to limit,
                    "results" to emptyList<String>()
                )
            }
        }

        // Example 5: Using handleAndRespond for automatic response handling
        get("/auto-respond".uri()) {
            val user = createMockUser()
            handle(call, user, successCode = HttpStatusCode.Accepted) {
                log.info("Auto-responding for ${user.username}")
                mapOf("status" to "success", "data" to "Hello!")
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
