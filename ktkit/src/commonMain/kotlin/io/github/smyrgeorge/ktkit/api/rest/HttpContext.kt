package io.github.smyrgeorge.ktkit.api.rest

import io.github.smyrgeorge.ktkit.api.error.impl.MalformedParameter
import io.github.smyrgeorge.ktkit.api.error.impl.MalformedRequestBody
import io.github.smyrgeorge.ktkit.api.error.impl.MissingParameter
import io.github.smyrgeorge.ktkit.api.error.impl.UnsupportedEnumValue
import io.github.smyrgeorge.ktkit.context.Principal
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import kotlin.uuid.Uuid

/**
 * Represents an HTTP request, encapsulating user authentication information and
 * providing methods for retrieving various HTTP request parameters and headers.
 *
 * @property user The authenticated user's principal containing user data and permissions
 * @property call The application call associated with this request
 */
class HttpContext(
    val user: Principal,
    val call: ApplicationCall,
) {
    val request: ApplicationRequest get() = call.request

    /**
     * Represents a variable with a type, name, and an optional value. The variable can be used
     * to retrieve its value in various formats such as String, Long, Int, Float, Double, Boolean,
     * Uuid, or as an enum value.
     *
     * @constructor Creates an instance of Var with a specified type, name, and optional value.
     * @param kind The type of the variable. It can be one of the values from [Kind].
     * @param name The name of the variable.
     * @param value The optional value of the variable.
     */
    class Var(
        private val kind: Kind,
        private val name: String,
        private val value: String?
    ) {
        fun asString(): String = value ?: MissingParameter(kind.name, name).throwRuntimeError()
        fun asStringOrNull(): String? = value

        fun asLong(): Long = safe("Long") { it.toLongOrNull() }
        fun asLongOrNull(): Long? = safeOrNull("Long") { it.toLongOrNull() }

        fun asInt(): Int = safe("Int") { it.toIntOrNull() }
        fun asIntOrNull(): Int? = safeOrNull("Int") { it.toIntOrNull() }

        fun asFloat(): Float = safe("Float") { it.toFloatOrNull() }
        fun asFloatOrNull(): Float? = safeOrNull("Float") { it.toFloatOrNull() }

        fun asDouble(): Double = safe("Double") { it.toDoubleOrNull() }
        fun asDoubleOrNull(): Double? = safeOrNull("Double") { it.toDoubleOrNull() }

        fun asBoolean(): Boolean = safe("Boolean") { it.toBooleanStrictOrNull() }
        fun asBooleanOrNull(): Boolean? = safeOrNull("Boolean") { it.toBooleanStrictOrNull() }

        fun asUuid(): Uuid = safe("Uuid") { Uuid.parseOrNull(it) }
        fun asUuidOrNull(): Uuid? = safeOrNull("Uuid") { Uuid.parseOrNull(it) }

        inline fun <reified T : Enum<T>> asEnum(): T = asString().toEnum<T>()
        inline fun <reified T : Enum<T>> asEnumOrNull(): T? = asStringOrNull()?.toEnum<T>()

        inline fun <reified T : Enum<T>> String.toEnum(): T =
            try {
                enumValueOf<T>(this)
            } catch (e: Exception) {
                UnsupportedEnumValue(T::class.simpleName ?: "Unknown", this).throwRuntimeError(e)
            }

        inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
            try {
                enumValueOf<T>(this)
            } catch (_: Exception) {
                null
            }

        /**
         * Attempts to safely parse a value using the provided function.
         * Throws a runtime error if the value is missing or cannot be parsed.
         *
         * @param expected The name of the expected type, used in error reporting if parsing fails.
         * @param parse A function that attempts to parse the value, returning null if the value is invalid.
         * @return The parsed value if successful.
         */
        private fun <T : Any> safe(expected: String, parse: (String) -> T?): T =
            safeOrNull(expected, parse) ?: MissingParameter(kind.name, name).throwRuntimeError()

        /**
         * Attempts to parse the current value using the provided parsing function.
         * If parsing fails, returns null or throws a runtime error when the value is malformed.
         *
         * @param expected The name of the target type, used for error reporting when parsing fails.
         * @param parse A function that attempts to parse the value and returns null if the value is not valid.
         * @return The parsed value if successful, or null if the value does not exist or cannot be parsed.
         */
        private fun <T : Any> safeOrNull(expected: String, parse: (String) -> T?): T? =
            value?.let { raw ->
                parse(raw) ?: MalformedParameter(kind.name, name, expected, raw).throwRuntimeError()
            }

        /**
         * Represents the type of variable in an HTTP context.
         *
         * This enum class defines different categories for how a variable is used
         * in the context of HTTP requests and responses.
         *
         * Types:
         * - `HEADER`: Represents a variable that is used in the headers of an HTTP request.
         * - `PATH_VARIABLE`: Represents a variable that is part of the URI path in an HTTP request.
         * - `QUERY_PARAM`: Represents a variable that is used as a query parameter in an HTTP request.
         */
        enum class Kind {
            HEADER,
            PATH_VARIABLE,
            QUERY_PARAM
        }
    }

    /**
     * Gets the request URI.
     */
    fun uri(): String = call.request.uri

    /**
     * Gets a path parameter by name.
     */
    fun pathVariable(name: String): Var = Var(Var.Kind.PATH_VARIABLE, name, call.parameters[name])

    /**
     * Gets a query parameter by name.
     */
    fun queryParam(name: String): Var = Var(Var.Kind.QUERY_PARAM, name, request.queryParameters[name])

    /**
     * Gets all query parameters with the given name.
     */
    fun queryParams(name: String): List<String> = request.queryParameters.getAll(name) ?: emptyList()

    /**
     * Gets a header by name.
     */
    fun header(name: String): Var = Var(Var.Kind.HEADER, name, request.headers[name])

    /**
     * Gets all headers with the given name.
     */
    fun headers(name: String): List<String> = request.headers.getAll(name) ?: emptyList()

    /**
     * Receives the request body and deserializes it to the specified type.
     *
     * @param T The type to deserialize the body into
     * @return The deserialized body of type T
     */
    suspend inline fun <reified T : Any> body(): T =
        try {
            call.receive()
        } catch (e: Throwable) {
            MalformedRequestBody(e).throwRuntimeError(e)
        }
}
