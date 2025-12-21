package io.github.smyrgeorge.ktorlib.example

import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.log4k.Logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route

class UserRestHandler(
    private val userService: UserService  // Injected via constructor
) : AbstractRestHandler() {

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
            permissions = { user.hasRole("ADMIN") }
        ) {
            log.info("Admin ${user.username} accessed /admin")
            "Welcome, admin ${user.username}!"
        }

        // Example 3: GET with path parameter - using service
        GET("/users/{id}") {
            val userId = pathVariable("id").asString()
            log.info("Fetching user: $userId")

            // Business logic delegated to service
            userService.getUserById(userId)
        }

        // Example 4: GET with query parameters - using service
        GET("/search") {
            val query = queryParam("q").asStringOrNull() ?: ""
            val limit = queryParam("limit").asIntOrNull() ?: 10

            log.info("Searching for: $query (limit: $limit)")

            // Business logic delegated to service
            mapOf(
                "query" to query,
                "limit" to limit,
                "results" to userService.searchUsers(query, limit)
            )
        }

        // Example 5: Using custom success code
        GET(
            path = "/auto-respond",
            onSuccessHttpStatusCode = HttpStatusCode.Accepted
        ) {
            log.info("Auto-responding for ${user.username}")
            mapOf("status" to "success", "data" to "Hello!")
        }
    }
}
