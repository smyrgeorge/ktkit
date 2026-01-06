package io.github.smyrgeorge.ktkit.context

import arrow.core.raise.context.Raise
import io.github.smyrgeorge.ktkit.api.event.EventContext
import io.github.smyrgeorge.ktkit.api.rest.HttpContext
import io.github.smyrgeorge.ktkit.error.ErrorSpec
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
 * @property principal The authenticated user's principal
 * @property attributes Custom attributes map for storing request-scoped data
 * @property tracing TracingContext for logging purposes
 * @property http Optional HttpContext for accessing request data
 * @property event Optional EventContext for accessing event data
 */
class ExecContext(
    val reqId: String,
    val reqTs: Instant = Clock.System.now(),
    val principal: Principal,
    val attributes: Map<String, Any> = emptyMap(),
    val tracing: TracingContext,
    http: HttpContext? = null,
    event: EventContext? = null,
) : Raise<ErrorSpec>, TracingContext by tracing, CoroutineContext.Element {
    private val _http: HttpContext? = http
    private val _event: EventContext? = event

    val http: HttpContext get() = _http ?: error("HttpContext is null.")
    val event: EventContext get() = _event ?: error("EventContext is null.")

    override fun raise(r: ErrorSpec): Nothing = throw r.toThrowable()
    override val key: CoroutineContext.Key<ExecContext> get() = ExecContext
    override fun toString() = "ExecutionContext(reqId='$reqId', reqTs=$reqTs, user=$principal, attributes=$attributes)"

    companion object : CoroutineContext.Key<ExecContext> {
        /**
         * Creates a Context from an ApplicationCall with a user principal.
         *
         * @param principal The user principal associated with the request.
         * @param http The HttpContext
         * @param attributes Custom attributes map
         * @param tracing TracingContext for logging purposes
         */
        fun fromHttp(
            principal: Principal,
            http: HttpContext,
            tracing: TracingContext,
            attributes: Map<String, Any> = emptyMap(),
        ): ExecContext = ExecContext(
            principal = principal,
            reqId = tracing.current().id,
            attributes = attributes,
            tracing = tracing,
            http = http,
        )

        /**
         * Creates an [ExecContext] by extracting relevant information from the given event.
         *
         * @param principal The user principal associated with the request.
         * @param event The event context that contains event-specific details.
         * @param tracing Tracing context for distributed tracing.
         * @param attributes Additional attributes or metadata to include in the execution context, default is an empty map.
         * @return A new [ExecContext] instance populated with the provided inputs.
         */
        fun fromEvent(
            principal: Principal,
            event: EventContext,
            tracing: TracingContext,
            attributes: Map<String, Any> = emptyMap(),
        ): ExecContext = ExecContext(
            principal = principal,
            reqId = tracing.current().id,
            attributes = attributes,
            tracing = tracing,
            event = event,
        )
    }
}
