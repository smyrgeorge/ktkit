package io.github.smyrgeorge.ktorlib.api.auth

import io.github.smyrgeorge.ktorlib.context.UserToken
import io.ktor.server.application.ApplicationCall

/**
 * Defines a contract for extracting a [UserToken] from an incoming [ApplicationCall].
 *
 * Implementations of this interface are responsible for interpreting the HTTP request
 * to authenticate and extract any relevant user credentials or metadata. The extracted
 * result can either be a valid user token or null, wrapped in a [Result] to account for
 * potential errors during the extraction process.
 *
 * This interface is typically used in conjunction with authentication providers
 * to enable custom logic for principal extraction.
 *
 * @see UserToken
 * @see ApplicationCall
 */
interface PrincipalExtractor {
    /**
     * Extracts a [UserToken] from the given [ApplicationCall].
     *
     * This method is responsible for parsing the incoming HTTP request represented by
     * the [ApplicationCall] to extract authentication information. The result of this
     * operation is a [UserToken], if the extraction is successful, or null if no valid
     * token could be resolved. Any errors encountered during the process are captured
     * within the [Result] wrapper.
     *
     * @param call The [ApplicationCall] representing the incoming HTTP request from
     * which the [UserToken] will be extracted.
     * @return A [Result] containing a [UserToken] if extraction is successful,
     * null if no token could be resolved, or an exception if an error occurs.
     */
    suspend fun extract(call: ApplicationCall): Result<UserToken?>

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
