package io.github.smyrgeorge.ktorlib.context

import io.github.smyrgeorge.ktorlib.error.types.ForbiddenImpl
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
 * @property jwt JWT token string
 * @property attributes Custom token claims/attributes as a JSON object
 */
@Serializable
@OptIn(ExperimentalUuidApi::class)
data class UserToken(
    val uuid: Uuid,
    val username: String,
    val email: String? = null,
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val roles: Set<String> = emptySet(),
    val iat: Long? = null,
    val exp: Long? = null,
    val authTime: Long? = null,
    val attributes: Map<String, String> = emptyMap()
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

    /**
     * Ensures that the user possesses the specified role. Throws an error if the role is not assigned to the user.
     *
     * @param role The name of the role required for the operation.
     * @throws ForbiddenImpl If the user does not have the specified role.
     */
    fun requireRole(role: String) {
        if (!hasRole(role)) ForbiddenImpl("User does not have authority $role").ex()
    }

    /**
     * Ensures that the user possesses at least one of the specified roles.
     * Throws an error if the user does not have any of the roles.
     *
     * @param roles The role names to check. One or more roles that the user must have at least one of.
     * @throws ForbiddenImpl If the user does not have any of the specified roles.
     */
    fun requireAnyRole(vararg roles: String) {
        if (!hasAnyRole(*roles)) ForbiddenImpl("User does not have authorities: ${roles.joinToString()}").ex()
    }

    /**
     * Ensures that the user possesses all of the specified roles.
     * Throws an error if the user does not have all the required roles.
     *
     * @param roles The role names that the user must have. One or more roles to check.
     * @throws ForbiddenImpl If the user does not have all of the specified roles.
     */
    fun requireAllRoles(vararg roles: String) {
        if (!hasAllRoles(*roles)) ForbiddenImpl("User does not have authorities: ${roles.joinToString()}").ex()
    }
}
