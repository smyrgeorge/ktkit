package io.github.smyrgeorge.ktkit.api.event

import io.github.smyrgeorge.ktkit.context.UserToken

/**
 * Represents the context of an event that includes authentication and request metadata.
 *
 * @property user The authenticated user's token containing identity and metadata information.
 * @property headers A map of request headers, providing additional context or metadata about the event.
 */
class EventContext(
    val user: UserToken,
    val headers: Map<String, String>
)
