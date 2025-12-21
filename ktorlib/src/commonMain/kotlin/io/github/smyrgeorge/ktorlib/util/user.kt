@file:OptIn(ExperimentalUuidApi::class)

package io.github.smyrgeorge.ktorlib.util

import io.github.smyrgeorge.ktorlib.context.UserToken
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a pre-defined user token for the system user.
 *
 * This variable is used to identify the system's automated processes or
 * actions within an application, where a real user's credentials are not
 * applicable. The system user is characterized by a predefined UUID and
 * basic user details such as username, email, and name.
 *
 * The `SYSTEM_USER` variable provides a consistent and traceable identity
 * for system-initiated actions, enabling easier debugging and accountability
 * in the system.
 */
var SYSTEM_USER: UserToken =
    UserToken(
        uuid = Uuid.parse("00000000-0000-0000-0000-000000000000"),
        username = "System",
        email = "system@internal.user",
        name = "System User",
        firstName = "System",
        lastName = "Internal"
    )

/**
 * A predefined instance of `UserToken` representing an anonymous user in the system.
 *
 * This object can be used to identify or handle requests from unauthenticated users.
 * The `ANONYMOUS_USER` instance is initialized with default values such as:
 * - A universally unique identifier (UUID) specific to anonymous users.
 * - A generic username ("Anonymous").
 * - An email address set to "anonymous@internal.user".
 * - A user name and first/last names reflecting the anonymous role.
 *
 * This instance does not have any roles or custom attributes assigned.
 */
var ANONYMOUS_USER: UserToken =
    UserToken(
        uuid = Uuid.parse("00000000-0000-0000-0000-000000000001"),
        username = "Anonymous",
        email = "anonymous@internal.user",
        name = "Anonymous User",
        firstName = "Anonymous",
        lastName = "Internal"
    )
