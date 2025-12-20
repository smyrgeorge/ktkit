package io.github.smyrgeorge.ktorlib.domain

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
 * @property call Optional Ktor ApplicationCall for accessing request data
 * @property attributes Custom attributes map for storing request-scoped data
 */
data class Context(
    val reqId: String,
    val reqTs: Instant = Clock.System.now(),
    val user: UserToken,
    private var call: ApplicationCall? = null,
    val attributes: Map<String, Any> = emptyMap(),
) : CoroutineContext.Element {

    val req = Request(call)

    /**
     * Clears the [Context] from possible left-overs.
     * Should be called manually after request processing.
     */
    fun clear() {
        call = null
    }

    /**
     * Wrapper class for accessing request data from Ktor's ApplicationCall.
     */
    class Request(
        private val call: ApplicationCall?
    ) {
        /**
         * Represents a request variable (path param, query param, or header) with type conversion utilities.
         */
        class Var(
            private val type: Type,
            private val name: String,
            private val value: String?
        ) {
            fun asString(): String = value ?: MissingParameter(type.name, name).ex()
            fun asStringOrNull(): String? = value

            fun asLong(): Long = asString().toLong()
            fun asLongOrNull(): Long? = value?.toLongOrNull()

            fun asInt(): Int = asString().toInt()
            fun asIntOrNull(): Int? = value?.toIntOrNull()

            fun asFloat(): Float = asString().toFloat()
            fun asFloatOrNull(): Float? = value?.toFloatOrNull()

            fun asDouble(): Double = asString().toDouble()
            fun asDoubleOrNull(): Double? = value?.toDoubleOrNull()

            fun asBoolean(): Boolean = asString().toBoolean()
            fun asBooleanOrNull(): Boolean? = value?.toBooleanStrictOrNull()

            inline fun <reified T : Enum<T>> asEnum(): T = asString().toEnum<T>()
            inline fun <reified T : Enum<T>> asEnumOrNull(): T? = asStringOrNull()?.toEnumOrNull<T>()

            inline fun <reified T : Enum<T>> String.toEnum(): T =
                try {
                    enumValueOf<T>(this)
                } catch (e: Exception) {
                    UnsupportedEnumValue(T::class.simpleName ?: "Unknown", this).ex()
                }

            inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
                try {
                    enumValueOf<T>(this)
                } catch (e: Exception) {
                    null
                }

            enum class Type {
                HEADER,
                PATH_VARIABLE,
                QUERY_PARAM
            }
        }

        /**
         * Gets the request URI.
         */
        fun uri(): String = httpCall().request.uri

        /**
         * Gets a path parameter by name.
         */
        fun pathVariable(name: String): Var =
            Var(Var.Type.PATH_VARIABLE, name, httpCall().parameters[name])

        /**
         * Gets a query parameter by name.
         */
        fun queryParam(name: String): Var =
            Var(Var.Type.QUERY_PARAM, name, httpCall().request.queryParameters[name])

        /**
         * Gets all query parameters with the given name.
         */
        fun queryParams(name: String): List<String> =
            httpCall().request.queryParameters.getAll(name) ?: emptyList()

        /**
         * Gets a header by name.
         */
        fun header(name: String): Var =
            Var(Var.Type.HEADER, name, httpCall().request.headers[name])

        /**
         * Gets all headers with the given name.
         */
        fun headers(name: String): List<String> =
            httpCall().request.headers.getAll(name) ?: emptyList()

        /**
         * Gets the underlying ApplicationCall.
         * @throws IllegalStateException if the ApplicationCall is null
         */
        fun httpCall(): ApplicationCall = call ?: error("ApplicationCall is null.")
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
