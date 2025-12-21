package io.github.smyrgeorge.ktorlib.api.rest

import io.github.smyrgeorge.ktorlib.context.Context
import io.github.smyrgeorge.ktorlib.context.UserToken
import io.github.smyrgeorge.ktorlib.error.types.ForbiddenImpl
import io.github.smyrgeorge.ktorlib.error.types.UnauthorizedImpl
import io.github.smyrgeorge.ktorlib.util.AbstractComponent
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext

/**
 * Abstract base class for defining RESTful API handlers with integrated route handling,
 * permission checks, and response management.
 *
 * This class provides a structured way to define API routes using Ktor's routing mechanism,
 * with built-in support for handling permissions, different HTTP methods, and response types.
 *
 * Subclasses need to implement the `uri` method for building base paths and the `routes`
 * method for defining specific API routes.
 *
 * @property permissions Default permission check function that can be overridden in each
 * route handler. Ensures that the API request is authorized based on custom logic.
 */
@Suppress("FunctionName", "unused", "CanBePrimaryConstructorProperty")
abstract class AbstractRestHandler(
    hasRole: String? = null,
    hasAnyRole: List<String>? = null,
    hasAllRoles: List<String>? = null,
    private val permissions: HttpRequest.() -> Boolean = { true }
) : AbstractComponent {

    private val hasRole: String? = hasRole
    private val hasAnyRole: Array<String>? = hasAnyRole?.toTypedArray()
    private val hasAllRoles: Array<String>? = hasAllRoles?.toTypedArray()

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
        permissions: HttpRequest.() -> Boolean,
        successCode: HttpStatusCode,
        crossinline f: suspend HttpRequest.() -> T
    ) {
        // Get the authenticated user from the call (set by Ktor's Authentication plugin)
        val user = call.principal<UserToken>() ?: UnauthorizedImpl("User is not authenticated").ex()

        // Role based authorization.
        hasRole?.let { role -> user.requireRole(role) }
        hasAnyRole?.let { anyRole -> user.requireAnyRole(*anyRole) }
        hasAllRoles?.let { allRoles -> user.requireAllRoles(*allRoles) }

        // Create the context for the request.
        val context = Context.of(call = call, user = user)
        val httpRequest = context.httpRequest

        // Check that user has access to the corresponding resources.
        val hasAccess = this.permissions(httpRequest) && permissions(httpRequest)
        if (!hasAccess) ForbiddenImpl("User does not have the required permissions to access uri='${httpRequest.uri()}'").ex()

        val result = withContext(context) { httpRequest.f() }
        when (result) {
            // TODO: handle Result<*>
            // TODO: handle Either<*, *>
            is Flow<*> -> call.respond(successCode, result.filterNotNull())
            is Unit -> call.respond(successCode)
            else -> call.respond(successCode, result as Any)
        }

        // Clears the [Context] here (ensures no leftovers).
        context.clear()
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
        permissions: HttpRequest.() -> Boolean = { true },
        onSuccessHttpCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend HttpRequest.() -> T
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
        permissions: HttpRequest.() -> Boolean = { true },
        onSuccessHttpCode: HttpStatusCode = HttpStatusCode.Created,
        handler: suspend HttpRequest.() -> T
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
        permissions: HttpRequest.() -> Boolean = { true },
        onSuccessHttpCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend HttpRequest.() -> T
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
        permissions: HttpRequest.() -> Boolean = { true },
        onSuccessHttpCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend HttpRequest.() -> T
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
        permissions: HttpRequest.() -> Boolean = { true },
        onSuccessHttpCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend HttpRequest.() -> T
    ) {
        delete(path.uri()) {
            handle(call, permissions, onSuccessHttpCode, handler)
        }
    }
}
