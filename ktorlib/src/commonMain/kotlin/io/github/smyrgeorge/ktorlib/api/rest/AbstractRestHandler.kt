package io.github.smyrgeorge.ktorlib.api.rest

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.NonEmptySet
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
import io.ktor.server.routing.head
import io.ktor.server.routing.options
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext

/**
 * Abstract base class for creating REST API endpoints with built-in request handling and security.
 *
 * This class provides helper methods for defining routes with integrated request processing,
 * user authentication, authorization checks, and response handling.
 *
 * @param defaultUser A default user token to use for request handling if no authenticated user is present.
 * @param hasRole A specific role required for authorization.
 * @param hasAnyRole A set of roles, of which at least one is required for authorization.
 * @param hasAllRoles A set of roles, all of which are required for authorization.
 * @param permissions Additional permission checks to be enforced during request processing.
 */
@Suppress("FunctionName", "unused")
abstract class AbstractRestHandler(
    private val defaultUser: UserToken? = null,
    private val hasRole: String? = null,
    hasAnyRole: NonEmptySet<String>? = null,
    hasAllRoles: NonEmptySet<String>? = null,
    private val permissions: HttpRequest.() -> Boolean = { true }
) : AbstractComponent {
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
     * @param defaultUser The default user token to use if none is provided in the request
     * @param permissions Optional permission check function
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses
     * @param f The function to execute
     */
    private suspend inline fun <T> handle(
        call: ApplicationCall,
        defaultUser: UserToken? = null,
        permissions: HttpRequest.() -> Boolean,
        onSuccessHttpStatusCode: HttpStatusCode,
        crossinline f: suspend HttpRequest.() -> T
    ) {
        // Get the authenticated user from the call (set by Ktor's Authentication plugin)
        val user = call.principal<UserToken>()
            ?: defaultUser
            ?: this.defaultUser
            ?: UnauthorizedImpl("User is not authenticated").ex()

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
            is Result<*> -> result
                .onSuccess { value -> respond(call, onSuccessHttpStatusCode, value) }
                .onFailure { throw it }

            is Either<*, *> -> result.fold(
                ifLeft = { throw (it as? Throwable ?: IllegalStateException(it.toString())) },
                ifRight = { value -> respond(call, onSuccessHttpStatusCode, value) }
            )

            else -> respond(call, onSuccessHttpStatusCode, result)
        }

        // Clears the [Context] here (ensures no leftovers).
        context.clear()
    }

    /**
     * Responds to an HTTP request based on the type of the provided result.
     *
     * Supports the following result types:
     * - Flow<*>: Responds with a filtered Flow, excluding null values.
     * - Unit: Responds with the provided success code, typically indicating success without a body.
     * - Any other type: Responds with the provided success code and the result serialized as the response body.
     *
     * @param call The Ktor ApplicationCall.
     * @param onSuccessHttpStatusCode The HTTP status code indicating a successful response.
     * @param result The response body or stream to return to the client.
     */
    private suspend inline fun <T> respond(call: ApplicationCall, onSuccessHttpStatusCode: HttpStatusCode, result: T) {
        when (result) {
            is Flow<*> -> call.respond(onSuccessHttpStatusCode, result.filterNotNull())
            is Unit -> call.respond(onSuccessHttpStatusCode)
            else -> call.respond(onSuccessHttpStatusCode, result as Any)
        }
    }

    /**
     * Defines a GET route with integrated request handling.
     *
     * @param path The route path
     * @param defaultUser The default user token to use if none is provided in the request
     * @param permissions Optional permission check function
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses (default: 200 OK)
     * @param handler The function to execute
     */
    fun <T> Route.GET(
        path: String,
        defaultUser: UserToken? = null,
        permissions: HttpRequest.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend HttpRequest.() -> T
    ) {
        get(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }

    /**
     * Defines a POST route with integrated request handling.
     *
     * @param path The route path
     * @param defaultUser The default user token to use if none is provided in the request
     * @param permissions Optional permission check function
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses (default: 201 Created)
     * @param handler The function to execute
     */
    fun <T> Route.POST(
        path: String,
        defaultUser: UserToken? = null,
        permissions: HttpRequest.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.Created,
        handler: suspend HttpRequest.() -> T
    ) {
        post(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }

    /**
     * Defines a PUT route with integrated request handling.
     *
     * @param path The route path
     * @param defaultUser The default user token to use if none is provided in the request
     * @param permissions Optional permission check function
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses (default: 200 OK)
     * @param handler The function to execute
     */
    fun <T> Route.PUT(
        path: String,
        defaultUser: UserToken? = null,
        permissions: HttpRequest.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend HttpRequest.() -> T
    ) {
        put(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }

    /**
     * Defines a PATCH route with integrated request handling.
     *
     * @param path The route path
     * @param defaultUser The default user token to use if none is provided in the request
     * @param permissions Optional permission check function
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses (default: 200 OK)
     * @param handler The function to execute
     */
    fun <T> Route.PATCH(
        path: String,
        defaultUser: UserToken? = null,
        permissions: HttpRequest.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend HttpRequest.() -> T
    ) {
        patch(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }

    /**
     * Defines a DELETE route with integrated request handling.
     *
     * @param path The route path
     * @param defaultUser The default user token to use if none is provided in the request
     * @param permissions Optional permission check function
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses (default: 200 OK)
     * @param handler The function to execute
     */
    fun <T> Route.DELETE(
        path: String,
        defaultUser: UserToken? = null,
        permissions: HttpRequest.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend HttpRequest.() -> T
    ) {
        delete(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }

    /**
     * Defines an HEAD route with integrated request handling.
     *
     * @param path The route path.
     * @param defaultUser The default user token to use if none is provided in the request. Defaults to null.
     * @param permissions Optional permission check function. Defaults to a function that always returns true.
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses. Defaults to HttpStatusCode.OK.
     * @param handler The function to execute for handling the request.
     */
    fun <T> Route.HEAD(
        path: String,
        defaultUser: UserToken? = null,
        permissions: HttpRequest.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend HttpRequest.() -> T
    ) {
        head(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }

    /**
     * Defines an OPTIONS route with integrated request handling.
     *
     * @param path The route path.
     * @param defaultUser The default user token to use if none is provided in the request. Defaults to null.
     * @param permissions Optional permission check function. Defaults to a function that always returns true.
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses. Defaults to HttpStatusCode.OK.
     * @param handler The function to execute for handling the request.
     */
    fun <T> Route.OPTIONS(
        path: String,
        defaultUser: UserToken? = null,
        permissions: HttpRequest.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend HttpRequest.() -> T
    ) {
        options(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }
}
