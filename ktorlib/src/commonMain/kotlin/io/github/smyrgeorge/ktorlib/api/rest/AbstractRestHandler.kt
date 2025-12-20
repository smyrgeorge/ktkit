package io.github.smyrgeorge.ktorlib.api.rest

import io.github.smyrgeorge.ktorlib.domain.Context
import io.github.smyrgeorge.ktorlib.domain.UserToken
import io.github.smyrgeorge.ktorlib.error.types.CannotParseAuthorizationHeader
import io.github.smyrgeorge.ktorlib.error.types.ForbiddenImpl
import io.github.smyrgeorge.ktorlib.util.AbstractComponent
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

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
    private val permissions: (ctx: Context) -> Boolean = { true }
) : AbstractComponent {

    /**
     * Helper method used to build the base path.
     * Implement this to define your route prefix (e.g., "/api/v1").
     */
    abstract fun String.uri(): String

    /**
     * Used to construct the routes for a specific handler.
     * Implement this to define your Ktor routes using the routing DSL.
     *
     * Example:
     * ```
     * override fun Route.routes() {
     *     get("/users".uri()) {
     *         handle(call) {
     *             of { ctx ->
     *                 // Your logic here
     *                 listOf(user1, user2)
     *             }
     *         }
     *     }
     * }
     * ```
     */
    abstract fun Route.routes()

    /**
     * Executes a given suspendable function within a context, evaluating permissions before execution.
     *
     * @param permissions A lambda function to check whether the current context has the required permissions.
     *                    It defaults to a function that always returns true, granting automatic permission.
     * @param f A suspend function representing the action to be executed within the given context.
     * @return The result of executing the function. Returns the function's result if permissions are granted,
     *         or raises a Forbidden error if permission is denied.
     */
    open suspend fun <T> of(
        permissions: (ctx: Context) -> Boolean = { true },
        f: suspend (ctx: Context) -> T
    ): T = of(ctx(), permissions, f)

    /**
     * Executes a given function within a specific context and handles permissions and response building.
     *
     * @param ctx The context in which the function is executed. It is used to evaluate permissions
     *            and stored in the coroutine context to access request information.
     * @param permissions A lambda function that checks whether the current context has the necessary permissions.
     *                    Defaults to always returning true.
     * @param f A suspendable function to be executed within the given context.
     * @return The result of the function execution.
     * @throws ForbiddenImpl if permissions are not granted.
     */
    open suspend fun <T> of(
        ctx: Context,
        permissions: (ctx: Context) -> Boolean = { true },
        f: suspend (ctx: Context) -> T
    ): T {
        // Check that user has access to the corresponding resources.
        val hasAccess = this.permissions(ctx) && permissions(ctx)
        if (!hasAccess) ForbiddenImpl("User does not have access to uri='${ctx.req.uri()}'.").ex()

        // Call the f and wait for the result.
        // Store the [Context] inside the coroutine context.
        val result: T = withContext(ctx) { f(ctx) }

        // Clears the [Context] here (ensures no leftovers).
        ctx.clear()

        return result
    }

    /**
     * Handles a request by executing a suspendable function within a constructed context.
     *
     * This is a convenience method that combines context creation and function execution.
     * Use this when you need to respond to a Ktor request with proper context management.
     *
     * @param call The Ktor ApplicationCall
     * @param user The user token to use for the context
     * @param attributes Optional attributes to include in the context
     * @param f The suspendable function to execute, which should return the response
     *
     * Example:
     * ```
     * get("/endpoint") {
     *     handle(call, userToken) {
     *         of { ctx ->
     *             // Your logic here
     *             "response"
     *         }
     *     }
     * }
     * ```
     */
    suspend fun <T> handle(
        call: ApplicationCall,
        user: UserToken,
        attributes: Map<String, Any> = emptyMap(),
        f: suspend () -> T
    ): T = withContext(call.toCtx(user, attributes)) { f() }

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
     * @param attributes Optional attributes
     * @param f The function to execute
     */
    suspend fun <T> handleAndRespond(
        call: ApplicationCall,
        user: UserToken,
        attributes: Map<String, Any> = emptyMap(),
        f: suspend () -> T
    ) {
        when (val result = handle(call, user, attributes, f)) {
            is Flow<*> -> call.respond(result.filterNotNull())
            is Unit -> call.respond(HttpStatusCode.OK)
            else -> call.respond(HttpStatusCode.OK, result as Any)
        }
    }

    /**
     * Converts an ApplicationCall to a Context.
     *
     * @param user The user token
     * @param attributes Optional attributes map
     * @return Context created from the ApplicationCall
     */
    fun ApplicationCall.toCtx(
        user: UserToken,
        attributes: Map<String, Any> = emptyMap()
    ): Context = Context.of(call = this, user = user, attributes = attributes)

    /**
     * Parses a user token from a JSON header string.
     *
     * @param header The JSON header value (UserToken serialized as JSON)
     * @return Decoded UserToken
     * @throws CannotParseAuthorizationHeader if parsing fails
     */
    private fun userTokenOf(header: String): UserToken {
        return try {
            Json.decodeFromString<UserToken>(header)
        } catch (e: Exception) {
            CannotParseAuthorizationHeader(
                "Cannot parse (x-impersonated-user) header. Original error: ${e.message}"
            ).ex(e)
        }
    }
}
