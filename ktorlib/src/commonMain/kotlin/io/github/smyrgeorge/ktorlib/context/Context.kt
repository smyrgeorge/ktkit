package io.github.smyrgeorge.ktorlib.context

import io.github.smyrgeorge.ktorlib.api.rest.Request
import io.github.smyrgeorge.ktorlib.error.types.MissingParameter
import io.github.smyrgeorge.ktorlib.error.types.UnsupportedEnumValue
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.*
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
 * @property call Optional Ktor ApplicationCall for accessing request data
 */
data class Context(
    val reqId: String,
    val reqTs: Instant = Clock.System.now(),
    val user: UserToken,
    val attributes: Map<String, Any> = emptyMap(),
    private var call: ApplicationCall? = null,
) : CoroutineContext.Element {

    val httpRequest: Request = Request(call)

    /**
     * Clears the [Context] from possible left-overs.
     * Should be called manually after request processing.
     */
    fun clear() {
        call = null
        httpRequest.call = null
    }

    companion object : CoroutineContext.Key<Context> {

        /**
         * Creates a Context from an ApplicationCall with a user token.
         *
         * @param user The user token
         * @param call The Ktor ApplicationCall
         * @param reqId Request ID (defaults to a generated UUID-like string)
         * @param attributes Custom attributes map
         */
        fun of(
            user: UserToken,
            call: ApplicationCall,
            reqId: String = generateRequestId(),
            attributes: Map<String, Any> = emptyMap(),
        ): Context = Context(
            user = user,
            reqId = reqId,
            call = call,
            attributes = attributes
        )

        /**
         * Creates a Context without an ApplicationCall (for background tasks, etc.).
         *
         * @param user The user token
         * @param reqId Request ID (defaults to a generated UUID-like string)
         * @param attributes Custom attributes map
         */
        fun of(
            user: UserToken,
            reqId: String = generateRequestId(),
            attributes: Map<String, Any> = emptyMap(),
        ): Context = Context(
            user = user,
            reqId = reqId,
            attributes = attributes
        )

        /**
         * Generates a simple request ID.
         * Can be overridden with custom implementation.
         */
        private fun generateRequestId(): String = generateNonce()
    }

    override val key: CoroutineContext.Key<Context>
        get() = Context
}
