package io.github.smyrgeorge.ktorlib.examples

import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.log4k.Logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.Route

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

