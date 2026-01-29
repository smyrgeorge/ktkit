package io.github.smyrgeorge.ktkit.context

import io.github.smyrgeorge.ktkit.api.error.impl.Forbidden
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
interface Principal {
    val id: Uuid
    val username: String
    val roles: Set<String>

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
    fun hasAnyRole(roles: Set<String>): Boolean = roles.any { this.roles.contains(it) }

    /**
     * Checks if the user has all the specified roles.
     *
     * @param roles The role names to check
     * @return true if the user has all the roles, false otherwise
     */
    fun hasAllRoles(roles: Set<String>): Boolean = roles.all { this.roles.contains(it) }

    /**
     * Ensures that the user possesses the specified role. Throws an error if the role is not assigned to the user.
     *
     * @param role The name of the role required for the operation.
     * @throws Forbidden If the user does not have the specified role.
     */
    fun requireRole(role: String) {
        if (!hasRole(role)) Forbidden("User does not have authority $role").raise()
    }

    /**
     * Ensures that the user possesses at least one of the specified roles.
     * Throws an error if the user does not have any of the roles.
     *
     * @param roles The role names to check. One or more roles that the user must have at least one of.
     * @throws Forbidden If the user does not have any of the specified roles.
     */
    fun requireAnyRole(roles: Set<String>) {
        if (!hasAnyRole(roles)) Forbidden("User does not have authorities: ${roles.joinToString()}").raise()
    }

    /**
     * Ensures that the user possesses all the specified roles.
     * Throws an error if the user does not have all the required roles.
     *
     * @param roles The role names that the user must have. One or more roles to check.
     * @throws Forbidden If the user does not have all the specified roles.
     */
    fun requireAllRoles(roles: Set<String>) {
        if (!hasAllRoles(roles)) Forbidden("User does not have authorities: ${roles.joinToString()}").raise()
    }
}
