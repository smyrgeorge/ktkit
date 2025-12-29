package io.github.smyrgeorge.ktorlib.api.rest

import arrow.core.Either
import arrow.core.NonEmptySet
import io.github.smyrgeorge.ktorlib.context.ExecutionContext
import io.github.smyrgeorge.ktorlib.context.UserToken
import io.github.smyrgeorge.ktorlib.error.ApiError
import io.github.smyrgeorge.ktorlib.error.Error
import io.github.smyrgeorge.ktorlib.error.InternalError
import io.github.smyrgeorge.ktorlib.error.types.ForbiddenImpl
import io.github.smyrgeorge.ktorlib.error.types.UnauthorizedImpl
import io.github.smyrgeorge.ktorlib.error.types.UnknownError
import io.github.smyrgeorge.ktorlib.service.AbstractComponent
import io.github.smyrgeorge.ktorlib.util.spanName
import io.github.smyrgeorge.ktorlib.util.spanTags
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.Tracer
import io.github.smyrgeorge.log4k.TracingContext
import io.github.smyrgeorge.log4k.TracingEvent.Span
import io.github.smyrgeorge.log4k.impl.OpenTelemetry
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
import kotlin.uuid.ExperimentalUuidApi

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
    private val permissions: HttpContext.() -> Boolean = { true }
) : AbstractComponent {
    val log: Logger = Logger.of(this::class)
    val trace: Tracer = Tracer.of(this::class)

    private val hasAnyRole: Array<String>? = hasAnyRole?.toTypedArray()
    private val hasAllRoles: Array<String>? = hasAllRoles?.toTypedArray()

    /**
     * Helper method used to build the base path.
     * Implement this to define your route prefix (e.g., "/api/v1").
     */
    abstract fun String.uri(): String

    abstract fun Route.routes()

    /**
     * Handles the current application call within a tracing context by creating a handler span
     * and executing the provided function within that span.
     *
     * @param f A lambda function to be executed within the tracing context. It takes a `Span`
     *          as a parameter and performs tracing-related logic.
     */
    private inline fun ApplicationCall.handleWithTracing(
        f: TracingContext.(Span) -> Unit
    ) {
        // Create the logging-context.
        val tracing = TracingContext.builder()
//            .with(parent) // TODO: create the remote span.
            .with(trace)
            .build()

        // Create the handler span.
        runCatching { tracing.span(spanName(), spanTags()) { tracing.f(this) } }
    }

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
     * @param handler The function to execute
     */
    private suspend inline fun <T> handle(
        call: ApplicationCall,
        defaultUser: UserToken? = null,
        permissions: HttpContext.() -> Boolean,
        onSuccessHttpStatusCode: HttpStatusCode,
        crossinline handler: suspend context(ExecutionContext, TracingContext) HttpContext.() -> T
    ): Unit = call.handleWithTracing { span ->
        try {
            // Get the authenticated user from the call (set by Ktor's Authentication plugin)
            val user = call.principal<UserToken>()
                ?: defaultUser
                ?: this@AbstractRestHandler.defaultUser
                ?: UnauthorizedImpl("User is not authenticated").ex()

            // Create the execution-context for the request.
            val httpContext = HttpContext(user, call)
            val executionContext = ExecutionContext.fromHttp(span.context.spanId, user, httpContext, this)

            // Role-based authorization.
            hasRole?.let { role -> user.requireRole(role) }
            hasAnyRole?.let { anyRole -> user.requireAnyRole(*anyRole) }
            hasAllRoles?.let { allRoles -> user.requireAllRoles(*allRoles) }

            // Add user tags to the span.
            span.tags.apply {
                @OptIn(ExperimentalUuidApi::class)
                put(OpenTelemetry.USER_ID, user.uuid)
                put(OpenTelemetry.USER_NAME, user.username)
            }

            // Check for permissions.
            val hasAccess = this@AbstractRestHandler.permissions(httpContext) && permissions(httpContext)
            if (!hasAccess) ForbiddenImpl("User does not have the required permissions to access uri='${httpContext.uri()}'").ex()

            // Load the execution context into the coroutine context.
            val result = withContext(executionContext) {
                // Execute the handler.
                context(executionContext) { httpContext.handler() }
            }

            when (result) {
                is Result<*> -> result
                    .onFailure { throw it }
                    .onSuccess { value -> respond(span, call, onSuccessHttpStatusCode, value) }

                is Either<*, *> -> result.fold(
                    ifLeft = { error ->
                        when (error) {
                            is Error -> throw error.toThrowable()
                            is InternalError -> throw error
                            else -> throw IllegalStateException("Unexpected error type: $error")
                        }
                    },
                    ifRight = { value -> respond(span, call, onSuccessHttpStatusCode, value) }
                )

                else -> respond(span, call, onSuccessHttpStatusCode, result)
            }
        } catch (e: Throwable) {
            respond(span, call, e)
            throw e
        }
    }

    /**
     * Responds to an HTTP request based on the type of the provided result.
     *
     * Supports the following result types:
     * - Flow<*>: Responds with a filtered Flow, excluding null values.
     * - Unit: Responds with the provided success code, typically indicating success without a body.
     * - Any other type: Responds with the provided success code and the result serialized as the response body.
     *
     * @param span The tracing span for the operation.
     * @param call The Ktor ApplicationCall.
     * @param status The HTTP status code indicating a successful response.
     * @param result The response body or stream to return to the client.
     */
    private suspend inline fun respond(
        span: Span,
        call: ApplicationCall,
        status: HttpStatusCode,
        result: Any?
    ) {
        // Set the HTTP status code and tags on the span.
        span.tags[OpenTelemetry.HTTP_RESPONSE_STATUS_CODE] = status.value
        when (result) {
            is Flow<*> -> call.respond(status, result.filterNotNull())
            is Unit -> call.respond(status)
            else -> call.respond(status, result ?: Unit)
        }
    }

    /**
     * Handles the response process for a given HTTP call by generating an appropriate API error
     * based on the provided throwable and tracing span. Logs server errors and sends the
     * generated API error as a response with the corresponding HTTP status code.
     *
     * @param span The tracing span associated with the current context to assist with observability.
     * @param call The application call representing the HTTP request and response context.
     * @param cause The throwable that triggered the error response handling.
     */
    private suspend fun respond(
        span: Span,
        call: ApplicationCall,
        cause: Throwable,
    ) {
        val error: Error = if (cause is InternalError) cause.error
        else UnknownError(cause.message ?: "An unknown error occurred")

        // Only log server errors (5xx)
        if (error.http.code >= 500) {
            log.error(cause) { cause.message ?: "null" }
        }

        // Set the HTTP status code and tags on the span.
        span.tags[OpenTelemetry.HTTP_REQUEST_METHOD] = error.http.code

        val res = ApiError(
            code = error.type,
            requestId = span.context.spanId,
            details = ApiError.Details(
                type = error.type,
                message = error.message,
                http = error.http
            )
        )

        val status = HttpStatusCode.fromValue(error.http.code)
        call.respond(status, res)
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
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend context(ExecutionContext, TracingContext) HttpContext.() -> T
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
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.Created,
        handler: suspend context(ExecutionContext, TracingContext) HttpContext.() -> T
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
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend context(ExecutionContext, TracingContext) HttpContext.() -> T
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
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend context(ExecutionContext, TracingContext) HttpContext.() -> T
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
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend context(ExecutionContext, TracingContext) HttpContext.() -> T
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
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend context(ExecutionContext, TracingContext) HttpContext.() -> T
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
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend context(ExecutionContext, TracingContext) HttpContext.() -> T
    ) {
        options(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }
}
