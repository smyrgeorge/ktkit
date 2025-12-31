package io.github.smyrgeorge.ktorlib.api.rest.impl

import arrow.core.NonEmptySet
import io.github.smyrgeorge.ktorlib.api.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktorlib.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktorlib.api.rest.HttpContext
import io.github.smyrgeorge.ktorlib.context.UserToken

/**
 * Abstract base class for handling REST endpoints that are associated with real-name-based authentication.
 * This handler extends [AbstractRestHandler] and incorporates additional configurations specific to real-name
 * scenarios, including user authentication and role-based access control.
 *
 * @constructor Initializes the handler with optional parameters for user authentication, role constraints,
 * and custom permission evaluations.
 *
 * @param defaultUser An optional [UserToken] representing the default user context to apply in requests when no
 * specific user context is provided.
 * @param hasRole An optional string specifying a single role required to access the handler.
 * Requests by users without the specified role will be denied.
 * @param hasAnyRole An optional non-empty set specifying multiple role names, where the user is required to
 * have at least one of the roles from the set to gain access.
 * @param hasAllRoles An optional non-empty set specifying multiple role names, where the user is required to
 * have all specified roles from the set to gain access.
 * @param permissions A lambda function with the [HttpContext] receiver allowing custom conditions to define
 * whether a request should be authorized. Defaults to always allowing requests.
 */
abstract class XRealNameRestHandler(
    defaultUser: UserToken? = null,
    hasRole: String? = null,
    hasAnyRole: NonEmptySet<String>? = null,
    hasAllRoles: NonEmptySet<String>? = null,
    permissions: HttpContext.() -> Boolean = { true },
) : AbstractRestHandler(defaultUser, hasRole, hasAnyRole, hasAllRoles, permissions, XRealNamePrincipalExtractor)

