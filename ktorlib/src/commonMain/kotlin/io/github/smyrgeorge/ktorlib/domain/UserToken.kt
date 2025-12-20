package io.github.smyrgeorge.ktorlib.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

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
 * @property jwt JWT token string
 * @property attributes Custom token claims/attributes as a JSON object
 */
@Serializable
data class UserToken(
    val uuid: String,
    val username: String,
    val email: String? = null,
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val roles: Set<String> = emptySet(),
    val iat: Long? = null,
    val exp: Long? = null,
    val authTime: Long? = null,
    val jwt: String = "EMPTY_JWT_TOKEN",
    val attributes: JsonElement = JsonObject(emptyMap())
) {
    /**
     * Checks if the user has a specific role.
     *
     * @param role The role name to check
     * @return true if the user has the role, false otherwise
     */
    fun hasRole(role: String): Boolean = roles.contains(role)

    /**
     * Checks if the user has any of the specified roles.
     *
     * @param roles The role names to check
     * @return true if the user has at least one of the roles, false otherwise
     */
    fun hasAnyRole(vararg roles: String): Boolean = roles.any { this.roles.contains(it) }

    /**
     * Checks if the user has all of the specified roles.
     *
     * @param roles The role names to check
     * @return true if the user has all of the roles, false otherwise
     */
    fun hasAllRoles(vararg roles: String): Boolean = roles.all { this.roles.contains(it) }
}
