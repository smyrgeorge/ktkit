package io.github.smyrgeorge.ktorlib.context

import io.github.smyrgeorge.ktorlib.api.rest.HttpContext
import io.github.smyrgeorge.log4k.TracingContext
import io.ktor.server.application.ApplicationCall
import io.ktor.util.generateNonce
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
 * @property call Optional Ktor ApplicationCall for accessing request data
 */
class ExecutionContext(
    val reqId: String,
    val reqTs: Instant = Clock.System.now(),
    val user: UserToken,
    val attributes: Map<String, Any> = emptyMap(),
    tracingContext: TracingContext? = null,
    var call: ApplicationCall? = null,
) : CoroutineContext.Element {
    private val _tracingContext: TracingContext? = tracingContext
    private val _http: HttpContext? = call?.let { HttpContext(user, it) }

    val tracingContext = _tracingContext ?: error("TracingContext is null.")
    val http: HttpContext = _http ?: error("HttpContext is null.")

    override fun toString(): String {
        return "Context(attributes=$attributes, user=$user, reqTs=$reqTs, reqId='$reqId')"
    }

    companion object : CoroutineContext.Key<ExecutionContext> {

        /**
         * Creates a Context from an ApplicationCall with a user token.
         *
         * @param reqId Request ID
         * @param user The user token
         * @param call The Ktor ApplicationCall
         * @param attributes Custom attributes map
         * @param tracingContext Optional TracingContext for logging purposes
         */
        fun of(
            reqId: String,
            user: UserToken,
            call: ApplicationCall,
            attributes: Map<String, Any> = emptyMap(),
            tracingContext: TracingContext? = null
        ): ExecutionContext = ExecutionContext(
            user = user,
            reqId = reqId,
            call = call,
            attributes = attributes,
            tracingContext = tracingContext
        )

        /**
         * Creates a Context without an ApplicationCall (for background tasks, etc.).
         *
         * @param user The user token
         * @param reqId Request ID (defaults to a generated UUID-like string)
         * @param attributes Custom attributes map
         * @param tracingContext Optional TracingContext for logging purposes
         */
        fun of(
            user: UserToken,
            reqId: String = generateRequestId(),
            attributes: Map<String, Any> = emptyMap(),
            tracingContext: TracingContext? = null
        ): ExecutionContext = ExecutionContext(
            user = user,
            reqId = reqId,
            attributes = attributes,
            tracingContext = tracingContext
        )

        /**
         * Generates a simple request ID.
         * Can be overridden with custom implementation.
         */
        private fun generateRequestId(): String = generateNonce()
    }

    override val key: CoroutineContext.Key<ExecutionContext>
        get() = ExecutionContext
}
