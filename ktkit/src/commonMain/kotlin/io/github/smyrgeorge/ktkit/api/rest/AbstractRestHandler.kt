package io.github.smyrgeorge.ktkit.api.rest

import arrow.core.Either
import arrow.core.NonEmptySet
import io.github.smyrgeorge.ktkit.api.auth.PrincipalExtractor
import io.github.smyrgeorge.ktkit.context.ExecContext
import io.github.smyrgeorge.ktkit.context.Principal
import io.github.smyrgeorge.ktkit.error.ErrorSpec
import io.github.smyrgeorge.ktkit.error.RuntimeError
import io.github.smyrgeorge.ktkit.error.impl.Forbidden
import io.github.smyrgeorge.ktkit.error.impl.Unauthorized
import io.github.smyrgeorge.ktkit.error.impl.UnknownError
import io.github.smyrgeorge.ktkit.error.impl.details.EmptyErrorData
import io.github.smyrgeorge.ktkit.service.AbstractComponent
import io.github.smyrgeorge.ktkit.util.camelCaseToKebabCase
import io.github.smyrgeorge.ktkit.util.extractOpenTelemetryHeader
import io.github.smyrgeorge.ktkit.util.spanName
import io.github.smyrgeorge.ktkit.util.spanTags
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.Tracer
import io.github.smyrgeorge.log4k.TracingContext
import io.github.smyrgeorge.log4k.TracingContext.Companion.span
import io.github.smyrgeorge.log4k.TracingEvent.Span
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
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi

/**
 * Abstract base class for creating REST API endpoints with built-in request handling and security.
 *
 * This class provides helper methods for defining routes with integrated request processing,
 * user authentication, authorization checks, and response handling.
 *
 * @param defaultUser A default principal to use for request handling if no authenticated principal is present.
 * @param hasRole A specific role required for authorization.
 * @param hasAnyRole A set of roles, of which at least one is required for authorization.
 * @param hasAllRoles A set of roles, all of which are required for authorization.
 * @param permissions Additional permission checks to be enforced during request processing.
 * @param principalExtractor Custom principal extractor for user authentication.
 */
@Suppress("FunctionName", "unused")
abstract class AbstractRestHandler(
    private val defaultUser: Principal? = null,
    private val hasRole: String? = null,
    private val hasAnyRole: NonEmptySet<String>? = null,
    private val hasAllRoles: NonEmptySet<String>? = null,
    private val permissions: HttpContext.() -> Boolean = { true },
    private val principalExtractor: PrincipalExtractor? = null
) : AbstractComponent {
    val log: Logger = Logger.of(this::class)
    val trace: Tracer = Tracer.of(this::class)

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
        val tracing = TracingContext.builder().with(trace).with(parent).build()
        // Create the handler span.
        runCatching { tracing.span(spanName(), spanTags()) { tracing.f(this) } }
            .onFailure { error -> log.error(error) { "Unexpected error while handling request: ${error.message}" } }
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
     * @param defaultUser The default principal to use if none is provided in the request
     * @param permissions Optional permission check function
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses
     * @param handler The function to execute
     */
    private suspend inline fun <T> handle(
        call: ApplicationCall,
        defaultUser: Principal? = null,
        permissions: HttpContext.() -> Boolean,
        onSuccessHttpStatusCode: HttpStatusCode,
        crossinline handler: suspend context(ExecContext) HttpContext.() -> T
    ): Unit = call.trace { span ->
        try {
            // Extrat the principal from the request.
            val user = principalExtractor?.extract(call)?.getOrThrow()
                ?: defaultUser
                ?: this@AbstractRestHandler.defaultUser
                ?: Unauthorized("User is not authenticated").raise()

            // Add user tags to the span.
            span.tags.apply {
                @OptIn(ExperimentalUuidApi::class)
                put(OpenTelemetryAttributes.USER_ID, user.id)
                put(OpenTelemetryAttributes.USER_NAME, user.username)
            }

            // Create the execution-context for the request.
            val http = HttpContext(user, call)
            val exec = ExecContext.fromHttp(user, http, this)

            // Role-based authorization.
            hasRole?.let { role -> user.requireRole(role) }
            hasAnyRole?.let { anyRole -> user.requireAnyRole(anyRole) }
            hasAllRoles?.let { allRoles -> user.requireAllRoles(allRoles) }

            // Check for permissions.
            val hasAccess = this@AbstractRestHandler.permissions(http) && permissions(http)
            if (!hasAccess) Forbidden("User does not have the required permissions to access uri='${http.uri()}'").raise()

            // Load the execution context into the coroutine context.
            val result = withContext(exec) {
                // Execute the handler.
                context(exec) { http.handler() }
            }

            when (result) {
                is Result<*> -> result
                    .onFailure { error -> respond(span, call, error) }
                    .onSuccess { value -> respond(span, call, onSuccessHttpStatusCode, value) }

                is Either<*, *> -> result.fold(
                    ifLeft = { left ->
                        val error = when (left) {
                            is ErrorSpec -> left.toThrowable()
                            is Throwable -> left
                            else -> UnknownError("Unexpected error type: $left").toThrowable()
                        }
                        respond(span, call, error)
                    },
                    ifRight = { value -> respond(span, call, onSuccessHttpStatusCode, value) }
                )

                else -> respond(span, call, onSuccessHttpStatusCode, result)
            }
        } catch (error: Throwable) {
            respond(span, call, error)
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
        span: Span.Local,
        call: ApplicationCall,
        status: HttpStatusCode,
        result: Any?
    ) {
        // Set the HTTP status code and tags on the span.
        span.tags[OpenTelemetryAttributes.HTTP_RESPONSE_STATUS_CODE] = status.value
        when (result) {
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
     * @param error The throwable that triggered the error response handling.
     */
    private suspend fun respond(
        span: Span.Local,
        call: ApplicationCall,
        error: Throwable,
    ) {
        val cause: ErrorSpec = if (error is RuntimeError) error.error
        else UnknownError(error.message ?: "An unknown error occurred")

        // Only log server errors (5xx)
        if (cause.httpStatus.code >= 500) {
            log.error(span, error) { error.message ?: "null" }
        }

        // Set the HTTP status code and tags on the span.
        span.tags[OpenTelemetryAttributes.HTTP_REQUEST_METHOD] = cause.httpStatus.code

        // Forcefully close the span if an error occurred.
        span.exception(error)
        span.end(error)

        val data = cause.toErrorSpecData()
        val title = cause::class.simpleName ?: error("Could not determine error class name.")
        val res = ApiError(
            type =
                if (app.conf.includeTypePropertyInApiError)
                    "${app.conf.errorTypeHost}/${camelCaseToKebabCase(title)}"
                else null,
            title = title,
            status = cause.httpStatus.code,
            requestId = span.context.spanId,
            detail = cause.message,
            data = if (data is EmptyErrorData) null else data
        )

        val status = HttpStatusCode.fromValue(cause.httpStatus.code)
        call.respond(status, res)
    }

    /**
     * Defines a GET route with integrated request handling.
     *
     * @param path The route path
     * @param defaultUser The default principal to use if none is provided in the request
     * @param permissions Optional permission check function
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses (default: 200 OK)
     * @param handler The function to execute
     */
    fun <T> Route.GET(
        path: String,
        defaultUser: Principal? = null,
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend context(ExecContext) HttpContext.() -> T
    ) {
        get(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }

    /**
     * Defines a POST route with integrated request handling.
     *
     * @param path The route path
     * @param defaultUser The default principal to use if none is provided in the request
     * @param permissions Optional permission check function
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses (default: 201 Created)
     * @param handler The function to execute
     */
    fun <T> Route.POST(
        path: String,
        defaultUser: Principal? = null,
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.Created,
        handler: suspend context(ExecContext) HttpContext.() -> T
    ) {
        post(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }

    /**
     * Defines a PUT route with integrated request handling.
     *
     * @param path The route path
     * @param defaultUser The default principal to use if none is provided in the request
     * @param permissions Optional permission check function
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses (default: 200 OK)
     * @param handler The function to execute
     */
    fun <T> Route.PUT(
        path: String,
        defaultUser: Principal? = null,
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend context(ExecContext) HttpContext.() -> T
    ) {
        put(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }

    /**
     * Defines a PATCH route with integrated request handling.
     *
     * @param path The route path
     * @param defaultUser The default principal to use if none is provided in the request
     * @param permissions Optional permission check function
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses (default: 200 OK)
     * @param handler The function to execute
     */
    fun <T> Route.PATCH(
        path: String,
        defaultUser: Principal? = null,
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend context(ExecContext) HttpContext.() -> T
    ) {
        patch(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }

    /**
     * Defines a DELETE route with integrated request handling.
     *
     * @param path The route path
     * @param defaultUser The default principal to use if none is provided in the request
     * @param permissions Optional permission check function
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses (default: 200 OK)
     * @param handler The function to execute
     */
    fun <T> Route.DELETE(
        path: String,
        defaultUser: Principal? = null,
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend context(ExecContext) HttpContext.() -> T
    ) {
        delete(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }

    /**
     * Defines an HEAD route with integrated request handling.
     *
     * @param path The route path.
     * @param defaultUser The default principal to use if none is provided in the request. Defaults to null.
     * @param permissions Optional permission check function. Defaults to a function that always returns true.
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses. Defaults to HttpStatusCode.OK.
     * @param handler The function to execute for handling the request.
     */
    fun <T> Route.HEAD(
        path: String,
        defaultUser: Principal? = null,
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend context(ExecContext) HttpContext.() -> T
    ) {
        head(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }

    /**
     * Defines an OPTIONS route with integrated request handling.
     *
     * @param path The route path.
     * @param defaultUser The default principal to use if none is provided in the request. Defaults to null.
     * @param permissions Optional permission check function. Defaults to a function that always returns true.
     * @param onSuccessHttpStatusCode The HTTP status code to use for successful responses. Defaults to HttpStatusCode.OK.
     * @param handler The function to execute for handling the request.
     */
    fun <T> Route.OPTIONS(
        path: String,
        defaultUser: Principal? = null,
        permissions: HttpContext.() -> Boolean = { true },
        onSuccessHttpStatusCode: HttpStatusCode = HttpStatusCode.OK,
        handler: suspend context(ExecContext) HttpContext.() -> T
    ) {
        options(path.uri()) {
            handle(call, defaultUser, permissions, onSuccessHttpStatusCode, handler)
        }
    }
}
