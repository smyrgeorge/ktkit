package io.github.smyrgeorge.ktkit.api.auth.impl

import io.github.smyrgeorge.ktkit.api.auth.PrincipalExtractor
import io.github.smyrgeorge.ktkit.api.error.impl.Unauthorized
import io.github.smyrgeorge.ktkit.util.default
import io.ktor.server.application.ApplicationCall
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

/**
 * Implementation of the [PrincipalExtractor] interface for extracting a [UserToken] from
 * an HTTP request's `x-real-name` header.
 *
 * This extractor decodes a base64-encoded JSON string from the `x-real-name` header,
 * deserializes the string into a [UserToken] object, and returns it as a [Result].
 * If the header is missing or the decoding/deserialization fails, the method captures
 * the error and returns it within the [Result].
 *
 * @constructor Initializes the extractor with a predefined header name (`x-real-name`)
 * and a JSON serializer configured to ignore unknown keys during deserialization.
 *
 * @see PrincipalExtractor
 * @see UserToken
 */
object XRealNamePrincipalExtractor : PrincipalExtractor {
    const val HEADER_NAME: String = "x-real-name"

    override fun extract(call: ApplicationCall): Result<UserToken?> {
        return runCatching {
            val header = call.request.headers[HEADER_NAME] ?: return@runCatching null
            extract(header).getOrThrow()
        }
    }

    override fun extract(header: String): Result<UserToken> {
        return runCatching {
            try {
                val j = Base64.decode(header).decodeToString()
                json.decodeFromString<UserToken>(j)
            } catch (e: Exception) {
                Unauthorized("Cannot extract $HEADER_NAME header: ${e.message}").raise(e)
            }
        }
    }

    private val json: Json = Json { default() }
    fun UserToken.toXRealName(): String {
        return Base64.encode(json.encodeToString(this).toByteArray())
    }
}
