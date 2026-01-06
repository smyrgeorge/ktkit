package io.github.smyrgeorge.ktkit.api.event

import io.github.smyrgeorge.ktkit.context.Principal

/**
 * Represents the context of an event that includes authentication and request metadata.
 *
 * @property principal The authenticated principal associated with the event.
 * @property headers A map of request headers, providing additional context or metadata about the event.
 */
class EventContext(
    val principal: Principal,
    val headers: Map<String, String>
)
