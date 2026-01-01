package io.github.smyrgeorge.ktkit.api.event

import io.github.smyrgeorge.ktkit.context.UserToken

class EventContext(
    val user: UserToken,
    val headers: Map<String, String>
)
