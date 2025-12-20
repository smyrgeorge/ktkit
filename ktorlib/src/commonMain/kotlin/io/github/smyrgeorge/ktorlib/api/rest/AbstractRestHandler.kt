package io.github.smyrgeorge.ktorlib.api.rest

import io.github.smyrgeorge.ktorlib.domain.Context
import io.github.smyrgeorge.ktorlib.domain.UserToken
import io.github.smyrgeorge.ktorlib.error.types.ForbiddenImpl
import io.github.smyrgeorge.ktorlib.util.AbstractComponent
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext

/**
 * Abstract base class for REST handlers in Ktor.
 *
 * Provides utilities for:
 * - Permission-based access control
 * - Request context management
 * - Structured error handling
 *
 * @property permissions Lambda function to check user permissions for all applied endpoints
 */
abstract class AbstractRestHandler(
    // Checks for user permissions to all applied endpoints.
    val permissions: (ctx: Context) -> Boolean = { true }
) : AbstractComponent {

    /**
     * Helper method used to build the base path.
     * Implement this to define your route prefix (e.g., "/api/v1").
     */
    abstract fun String.uri(): String

    abstract fun Route.routes()

    /**
     * Handles a request and automatically responds based on the result type.
     *
     * Supports:
     * - Flow<*> responses (responds with chunked transfer)
     * - Regular objects (responds with JSON)
     * - Unit (responds with 200 OK)
     *
     * @param call The Ktor ApplicationCall
     * @param user The user token
     * @param permissions Optional permission check function
     * @param successCode The HTTP status code to use for successful responses
     * @param attributes Optional attributes
     * @param f The function to execute
     */
    suspend inline fun <T> handle(
        call: ApplicationCall,
        user: UserToken,
        permissions: (ctx: Context) -> Boolean = { true },
        successCode: HttpStatusCode = HttpStatusCode.OK,
        attributes: Map<String, Any> = emptyMap(),
        crossinline f: suspend Context.() -> T
    ) {
        val context = Context.of(call = call, user = user, attributes = attributes)

        // Check that user has access to the corresponding resources.
        val hasAccess = this.permissions(context) && permissions(context)
        if (!hasAccess) ForbiddenImpl("User does not have access to uri='${context.req.uri()}'.").ex()

        // Clears the [Context] here (ensures no leftovers).
        context.clear()

        val result = withContext(context) { f(context) }
        when (result) {
            is Flow<*> -> call.respond(successCode, result.filterNotNull())
            is Unit -> call.respond(successCode)
            else -> call.respond(successCode, result as Any)
        }
    }
}
