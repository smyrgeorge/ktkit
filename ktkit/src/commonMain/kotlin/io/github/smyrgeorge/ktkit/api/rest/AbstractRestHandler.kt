package io.github.smyrgeorge.ktkit.api.rest

import arrow.core.Either
import arrow.core.NonEmptySet
import io.github.smyrgeorge.ktkit.api.auth.PrincipalExtractor
import io.github.smyrgeorge.ktkit.context.ExecutionContext
import io.github.smyrgeorge.ktkit.context.UserToken
import io.github.smyrgeorge.ktkit.error.ErrorSpec
import io.github.smyrgeorge.ktkit.error.RuntimeError
import io.github.smyrgeorge.ktkit.error.system.Forbidden
import io.github.smyrgeorge.ktkit.error.system.Unauthorized
import io.github.smyrgeorge.ktkit.error.system.UnknownError
import io.github.smyrgeorge.ktkit.service.AbstractComponent
import io.github.smyrgeorge.ktkit.util.extractOpenTelemetryHeader
import io.github.smyrgeorge.ktkit.util.spanName
import io.github.smyrgeorge.ktkit.util.spanTags
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.Tracer
import io.github.smyrgeorge.log4k.TracingContext
import io.github.smyrgeorge.log4k.TracingContext.Companion.span
import io.github.smyrgeorge.log4k.TracingEvent.Span
import io.github.smyrgeorge.log4k.impl.CoroutinesTracingContext
import io.github.smyrgeorge.log4k.impl.OpenTelemetryAttributes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
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
 * @param principalExtractor Custom principal extractor for user authentication.
 */
@Suppress("FunctionName", "unused")
abstract class AbstractRestHandler(
    private val defaultUser: UserToken? = null,
    private val hasRole: String? = null,
    hasAnyRole: NonEmptySet<String>? = null,
    hasAllRoles: NonEmptySet<String>? = null,
    private val permissions: HttpContext.() -> Boolean = { true },
    private val principalExtractor: PrincipalExtractor? = null
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
     * @param f A lambda function to be executed within the tracing context. It takes a `Span.Local`
     *          as a parameter and performs tracing-related logic.
     */
    private inline fun ApplicationCall.trace(
        f: TracingContext.(Span.Local) -> Unit
    ) {
        // Extract the parent span from the OpenTelemetry trace header.
        val parent = extractOpenTelemetryHeader()?.let { trace.span(it.spanId, it.traceId) }
        // Create the logging-context.
        val tracing = CoroutinesTracingContext(trace, parent)
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
        crossinline handler: suspend context(ExecutionContext) HttpContext.() -> T
    ): Unit = call.trace { span ->
        try {
            // Get the authenticated user from the call (set by Ktor's Authentication plugin)
            val user = principalExtractor?.extract(call)?.getOrThrow()
                ?: defaultUser
                ?: this@AbstractRestHandler.defaultUser
                ?: Unauthorized("User is not authenticated").ex()

            // Add user tags to the span.
            span.tags.apply {
                @OptIn(ExperimentalUuidApi::class)
                put(OpenTelemetryAttributes.USER_ID, user.uuid)
                put(OpenTelemetryAttributes.USER_NAME, user.username)
            }

            // Create the execution-context for the request.
            val httpContext = HttpContext(user, call)
            val executionContext = ExecutionContext.fromHttp(user, httpContext, this)

            // Role-based authorization.
            hasRole?.let { role -> user.requireRole(role) }
            hasAnyRole?.let { anyRole -> user.requireAnyRole(*anyRole) }
            hasAllRoles?.let { allRoles -> user.requireAllRoles(*allRoles) }

            // Check for permissions.
            val hasAccess = this@AbstractRestHandler.permissions(httpContext) && permissions(httpContext)
            if (!hasAccess) Forbidden("User does not have the required permissions to access uri='${httpContext.uri()}'").ex()

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
                            is ErrorSpec -> throw error.toThrowable()
                            is Throwable -> throw error
                            else -> throw UnknownError("Unexpected error type: $error").toThrowable()
                        }
                    },
                    ifRight = { value -> respond(span, call, onSuccessHttpStatusCode, value) }
                )

                else -> respond(span, call, onSuccessHttpStatusCode, result)
            }
        } catch (e: Throwable) {
            respond(span, call, e)
            // Forcefully close the span if an error occurred.
            span.exception(e, false)
            span.end(e)
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
        span.tags[OpenTelemetryAttributes.HTTP_RESPONSE_STATUS_CODE] = status.value
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
        val error: ErrorSpec = if (cause is RuntimeError) cause.error
        else UnknownError(cause.message ?: "An unknown error occurred")

        // Only log server errors (5xx)
        if (error.httpStatus.code >= 500) {
            log.error(cause) { cause.message ?: "null" }
        }

        // Set the HTTP status code and tags on the span.
        span.tags[OpenTelemetryAttributes.HTTP_REQUEST_METHOD] = error.httpStatus.code

        val res = ApiError(
            type = error::class.simpleName ?: "AnonymousError",
            status = error.httpStatus.code,
            requestId = span.context.spanId,
            detail = error.message,
            error = error
        )

        val status = HttpStatusCode.fromValue(error.httpStatus.code)
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
        handler: suspend context(ExecutionContext) HttpContext.() -> T
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
        handler: suspend context(ExecutionContext) HttpContext.() -> T
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
        handler: suspend context(ExecutionContext) HttpContext.() -> T
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
        handler: suspend context(ExecutionContext) HttpContext.() -> T
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
        handler: suspend context(ExecutionContext) HttpContext.() -> T
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
        handler: suspend context(ExecutionContext) HttpContext.() -> T
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
        handler: suspend context(ExecutionContext) HttpContext.() -> T
    ) {
        options(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }
}
