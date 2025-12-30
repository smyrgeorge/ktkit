package io.github.smyrgeorge.ktorlib.context

import arrow.core.raise.context.Raise
import io.github.smyrgeorge.ktorlib.api.event.EventContext
import io.github.smyrgeorge.ktorlib.api.rest.HttpContext
import io.github.smyrgeorge.log4k.TracingContext
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Request context containing user information, request metadata, and utilities for accessing request data.
 *
 * Implements [CoroutineContext.Element] to allow passing context through coroutines.
 *
 * @property reqId Unique request identifier
 * @property reqTs Request timestamp
 * @property user User token containing authentication/authorization information
 * @property attributes Custom attributes map for storing request-scoped data
 * @property tracingContext TracingContext for logging purposes
 * @property httpContext Optional HttpContext for accessing request data
 * @property eventContext Optional EventContext for accessing event data
 */
class ExecutionContext(
    val reqId: String,
    val reqTs: Instant = Clock.System.now(),
    val user: UserToken,
    val attributes: Map<String, Any> = emptyMap(),
    val tracingContext: TracingContext,
    httpContext: HttpContext? = null,
    eventContext: EventContext? = null,
) : Raise<Throwable>, TracingContext by tracingContext, CoroutineContext.Element {
    private val _http: HttpContext? = httpContext
    private val _event: EventContext? = eventContext

    val httpContext: HttpContext get() = _http ?: error("HttpContext is null.")
    val eventContext: EventContext get() = _event ?: error("EventContext is null.")

    override fun raise(r: Throwable): Nothing = throw r
    override val key: CoroutineContext.Key<ExecutionContext> get() = ExecutionContext
    override fun toString() = "ExecutionContext(reqId='$reqId', reqTs=$reqTs, user=$user, attributes=$attributes)"

    companion object : CoroutineContext.Key<ExecutionContext> {
        /**
         * Creates a Context from an ApplicationCall with a user token.
         *
         * @param user The user token
         * @param httpContext The HttpContext
         * @param attributes Custom attributes map
         * @param tracingContext TracingContext for logging purposes
         */
        fun fromHttp(
            user: UserToken,
            httpContext: HttpContext,
            tracingContext: TracingContext,
            attributes: Map<String, Any> = emptyMap(),
        ): ExecutionContext = ExecutionContext(
            user = user,
            reqId = tracingContext.current().id,
            attributes = attributes,
            tracingContext = tracingContext,
            httpContext = httpContext,
        )

        /**
         * Creates an [ExecutionContext] by extracting relevant information from the given event.
         *
         * @param user The user authentication token associated with the request.
         * @param eventContext The event context that contains event-specific details.
         * @param tracingContext Tracing context for distributed tracing.
         * @param attributes Additional attributes or metadata to include in the execution context, default is an empty map.
         * @return A new [ExecutionContext] instance populated with the provided inputs.
         */
        fun fromEvent(
            user: UserToken,
            eventContext: EventContext,
            tracingContext: TracingContext,
            attributes: Map<String, Any> = emptyMap(),
        ): ExecutionContext = ExecutionContext(
            user = user,
            reqId = tracingContext.current().id,
            attributes = attributes,
            tracingContext = tracingContext,
            eventContext = eventContext,
        )
    }
}
