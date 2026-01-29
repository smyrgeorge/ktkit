package io.github.smyrgeorge.ktkit.api.auth.impl

import arrow.core.raise.context.Raise
import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.impl.GenericError
import io.github.smyrgeorge.ktkit.context.Principal
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a user authentication token containing user information and metadata.
 *
 * @property uuid Unique identifier for the user
 * @property username User's username
 * @property email User's email address (optional)
 * @property name User's full name (optional)
 * @property firstName User's first name (optional)
 * @property lastName User's last name (optional)
 * @property roles Set of role names assigned to the user
 * @property iat Issued at timestamp in milliseconds (optional)
 * @property exp Expiration timestamp in milliseconds (optional)
 * @property authTime Authentication timestamp in milliseconds (optional)
 * @property attributes Custom token claims/attributes as a JSON object
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class UserToken(
    val uuid: Uuid,
    override val username: String,
    val email: String? = null,
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    override val roles: Set<String> = emptySet(),
    val iat: Long? = null,
    val exp: Long? = null,
    val authTime: Long? = null,
    val attributes: Map<String, String> = emptyMap()
) : Principal {
    override val id: Uuid = uuid

    companion object {
        context(_: Raise<ErrorSpec>)
        inline fun <reified T : Principal> Principal.toUserToken(): T =
            this as? T ?: GenericError("Could not convert Principal to the desired type ${T::class.simpleName}").raise()

        context(_: Raise<ErrorSpec>)
        fun Principal.toUserToken(): UserToken =
            this as? UserToken ?: GenericError("Principal is not a UserToken").raise()

        internal val DEFAULT_SYSTEM_USER: UserToken =
            UserToken(
                uuid = Uuid.parse("00000000-0000-0000-0000-000000000000"),
                username = "system",
                email = "system@internal.user",
                name = "System User",
                firstName = "System",
                lastName = "Internal"
            )

        internal val DEFAULT_ANONYMOUS_USER: UserToken =
            UserToken(
                uuid = Uuid.parse("00000000-0000-0000-0000-000000000001"),
                username = "anonymous",
                email = "anonymous@internal.user",
                name = "Anonymous User",
                firstName = "Anonymous",
                lastName = "Internal"
            )
    }
}
