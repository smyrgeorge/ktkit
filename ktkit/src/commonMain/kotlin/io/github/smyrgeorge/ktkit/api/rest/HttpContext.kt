@file:Suppress("unused")

package io.github.smyrgeorge.ktkit.api.rest

import io.github.smyrgeorge.ktkit.context.UserToken
import io.github.smyrgeorge.ktkit.error.system.MissingParameter
import io.github.smyrgeorge.ktkit.error.system.UnsupportedEnumValue
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.receive
import io.ktor.server.request.uri

/**
 * Represents an HTTP request, encapsulating user authentication information and
 * providing methods for retrieving various HTTP request parameters and headers.
 *
 * @property user The authenticated user's token containing user data and permissions
 * @property call The application call associated with this request
 */
class HttpContext(
    val user: UserToken,
    val call: ApplicationCall,
) {
    val request: ApplicationRequest get() = call.request

    /**
     * Represents a variable with a type, name, and an optional value. The variable can be used
     * to retrieve its value in various formats such as String, Long, Int, Float, Double, Boolean,
     * or as an enum value.
     *
     * @constructor Creates an instance of Var with a specified type, name, and optional value.
     * @param type The type of the variable. It can be one of the values from [Type].
     * @param name The name of the variable.
     * @param value The optional value of the variable.
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
            } catch (_: Exception) {
                UnsupportedEnumValue(T::class.simpleName ?: "Unknown", this).ex()
            }

        inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
            try {
                enumValueOf<T>(this)
            } catch (_: Exception) {
                null
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
        enum class Type {
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
    fun pathVariable(name: String): Var = Var(Var.Type.PATH_VARIABLE, name, call.parameters[name])

    /**
     * Gets a query parameter by name.
     */
    fun queryParam(name: String): Var = Var(Var.Type.QUERY_PARAM, name, request.queryParameters[name])

    /**
     * Gets all query parameters with the given name.
     */
    fun queryParams(name: String): List<String> = request.queryParameters.getAll(name) ?: emptyList()

    /**
     * Gets a header by name.
     */
    fun header(name: String): Var = Var(Var.Type.HEADER, name, request.headers[name])

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
    suspend inline fun <reified T : Any> body(): T = call.receive()
}