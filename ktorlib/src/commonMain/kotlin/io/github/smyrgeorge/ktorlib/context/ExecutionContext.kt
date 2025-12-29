package io.github.smyrgeorge.ktorlib.context

import arrow.core.raise.context.Raise
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
 * @property tracingContext Optional TracingContext for logging purposes
 * @property httpContext Optional HttpContext for accessing request data
 */
class ExecutionContext(
    val reqId: String,
    val reqTs: Instant = Clock.System.now(),
    val user: UserToken,
    val attributes: Map<String, Any> = emptyMap(),
    tracingContext: TracingContext? = null,
    httpContext: HttpContext? = null,
) : CoroutineContext.Element, Raise<Throwable> {
    private val _tracingContext: TracingContext? = tracingContext
    private val _http: HttpContext? = httpContext

    val tracingContext = _tracingContext ?: error("TracingContext is null.")
    val httpContext: HttpContext = _http ?: error("HttpContext is null.")

    override fun raise(r: Throwable): Nothing = throw r
    override val key: CoroutineContext.Key<ExecutionContext> get() = ExecutionContext
    override fun toString() = "ExecutionContext(reqId='$reqId', reqTs=$reqTs, user=$user, attributes=$attributes)"

    companion object : CoroutineContext.Key<ExecutionContext> {
        /**
         * Creates a Context from an ApplicationCall with a user token.
         *
         * @param reqId Request ID
         * @param user The user token
         * @param httpContext The HttpContext
         * @param attributes Custom attributes map
         * @param tracingContext Optional TracingContext for logging purposes
         */
        fun fromHttp(
            reqId: String,
            user: UserToken,
            httpContext: HttpContext,
            tracingContext: TracingContext? = null,
            attributes: Map<String, Any> = emptyMap(),
        ): ExecutionContext = ExecutionContext(
            user = user,
            reqId = reqId,
            attributes = attributes,
            tracingContext = tracingContext,
            httpContext = httpContext,
        )
    }
}
