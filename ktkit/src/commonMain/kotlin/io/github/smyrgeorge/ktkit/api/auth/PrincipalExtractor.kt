package io.github.smyrgeorge.ktkit.api.auth

import io.github.smyrgeorge.ktkit.context.Principal
import io.github.smyrgeorge.ktkit.service.Component
import io.ktor.server.application.ApplicationCall

/**
 * Defines a contract for extracting a [Principal] from an incoming [ApplicationCall].
 *
 * Implementations of this interface are responsible for interpreting the HTTP request
 * to authenticate and extract any relevant user credentials or metadata. The extracted
 * result can either be a valid user token or null, wrapped in a [Result] to account for
 * potential errors during the extraction process.
 *
 * This interface is typically used in conjunction with authentication providers
 * to enable custom logic for principal extraction.
 *
 * @see Principal
 * @see ApplicationCall
 */
interface PrincipalExtractor : Component {
    /**
     * Extracts a [Principal] from the given [ApplicationCall].
     *
     * This method is responsible for parsing the incoming HTTP request represented by
     * the [ApplicationCall] to extract authentication information. The result of this
     * operation is a [Principal], if the extraction is successful, or null if no valid
     * token could be resolved. Any errors encountered during the process are captured
     * within the [Result] wrapper.
     *
     * @param call The [ApplicationCall] representing the incoming HTTP request from
     * which the [Principal] will be extracted.
     * @return A [Result] containing a [Principal] if extraction is successful,
     * null if no token could be resolved, or an exception if an error occurs.
     */
    fun extract(call: ApplicationCall): Result<Principal?>

    /**
     * Extracts a [Principal] from the given header string.
     *
     * This method is responsible for parsing the specified HTTP header to extract
     * authentication information. The result of this operation is encapsulated
     * in a [Result] which may contain a [Principal] if extraction is successful,
     * or an error if the process encounters any issue.
     *
     * @param header The HTTP header string to be parsed for extracting the [Principal].
     * @return A [Result] containing a [Principal] if extraction succeeds, or an error if extraction fails.
     */
    fun extract(header: String): Result<Principal>

    /**
     * Provides the name of the implementing class or a unique identifier if the class name
     * cannot be resolved.
     *
     * The default implementation attempts to resolve the simple name of the implementing class.
     * If the class name is unavailable (e.g., due to obfuscation or reflection limitations),
     * a fallback string incorporating the object's hash code is returned, ensuring uniqueness.
     *
     * @return The name of the implementing class or a fallback identifier in the format
     * "extractor-${hashCode()}".
     */
    fun name(): String = this::class.simpleName ?: "extractor-${hashCode()}"
}
