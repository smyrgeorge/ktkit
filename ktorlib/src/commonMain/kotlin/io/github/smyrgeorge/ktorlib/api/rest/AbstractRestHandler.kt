package io.github.smyrgeorge.ktorlib.api.rest

import io.github.smyrgeorge.ktorlib.api.rest.auth.AuthenticationExtractor
import io.github.smyrgeorge.ktorlib.domain.Context
import io.github.smyrgeorge.ktorlib.error.types.ForbiddenImpl
import io.github.smyrgeorge.ktorlib.error.types.UnauthorizedImpl
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
 * - Pluggable authentication extraction
 * - Permission-based access control
 * - Request context management
 * - Structured error handling
 *
 * @property authenticationExtractor Strategy for extracting user authentication from requests
 * @property permissions Lambda function to check user permissions for all applied endpoints
 */
@Suppress("FunctionName")
abstract class AbstractRestHandler(
    private val authenticationExtractor: AuthenticationExtractor,
    private val permissions: (ctx: Context) -> Boolean = { true }
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
     * @param permissions Optional permission check function
     * @param successCode The HTTP status code to use for successful responses
     * @param f The function to execute
     */
    private suspend inline fun <T> handle(
        call: ApplicationCall,
        permissions: (ctx: Context) -> Boolean,
        successCode: HttpStatusCode = HttpStatusCode.OK,
        crossinline f: suspend Context.() -> T
    ) {
        // Extract user authentication from the request
        val user = authenticationExtractor.extract(call)
            ?: UnauthorizedImpl("Authentication required").ex()

        val context = Context.of(call = call, user = user)

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

    /**
     * Defines a GET route with integrated request handling.
     *
     * @param path The route path
     * @param permissions Optional permission check function
     * @param onSuccessHttpCode The HTTP status code to use for successful responses (default: 200 OK)
     * @param handler The function to execute
     */
    fun <T> Route.GET(
        path: String,
        permissions: (ctx: Context) -> Boolean = { true },
        onSuccessHttpCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend Context.() -> T
    ) {
        get(path.uri()) {
            handle(call, permissions, onSuccessHttpCode, handler)
        }
    }

    /**
     * Defines a POST route with integrated request handling.
     *
     * @param path The route path
     * @param permissions Optional permission check function
     * @param onSuccessHttpCode The HTTP status code to use for successful responses (default: 201 Created)
     * @param handler The function to execute
     */
    fun <T> Route.POST(
        path: String,
        permissions: (ctx: Context) -> Boolean = { true },
        onSuccessHttpCode: HttpStatusCode = HttpStatusCode.Created,
        handler: suspend Context.() -> T
    ) {
        post(path.uri()) {
            handle(call, permissions, onSuccessHttpCode, handler)
        }
    }

    /**
     * Defines a PUT route with integrated request handling.
     *
     * @param path The route path
     * @param permissions Optional permission check function
     * @param onSuccessHttpCode The HTTP status code to use for successful responses (default: 200 OK)
     * @param handler The function to execute
     */
    fun <T> Route.PUT(
        path: String,
        permissions: (ctx: Context) -> Boolean = { true },
        onSuccessHttpCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend Context.() -> T
    ) {
        put(path.uri()) {
            handle(call, permissions, onSuccessHttpCode, handler)
        }
    }

    /**
     * Defines a PATCH route with integrated request handling.
     *
     * @param path The route path
     * @param permissions Optional permission check function
     * @param onSuccessHttpCode The HTTP status code to use for successful responses (default: 200 OK)
     * @param handler The function to execute
     */
    fun <T> Route.PATCH(
        path: String,
        permissions: (ctx: Context) -> Boolean = { true },
        onSuccessHttpCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend Context.() -> T
    ) {
        patch(path.uri()) {
            handle(call, permissions, onSuccessHttpCode, handler)
        }
    }

    /**
     * Defines a DELETE route with integrated request handling.
     *
     * @param path The route path
     * @param permissions Optional permission check function
     * @param onSuccessHttpCode The HTTP status code to use for successful responses (default: 200 OK)
     * @param handler The function to execute
     */
    fun <T> Route.DELETE(
        path: String,
        permissions: (ctx: Context) -> Boolean = { true },
        onSuccessHttpCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend Context.() -> T
    ) {
        delete(path.uri()) {
            handle(call, permissions, onSuccessHttpCode, handler)
        }
    }
}
