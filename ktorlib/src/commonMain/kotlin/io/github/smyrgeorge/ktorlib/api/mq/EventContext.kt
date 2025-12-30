package io.github.smyrgeorge.ktorlib.api.mq

import io.github.smyrgeorge.ktorlib.context.UserToken

class EventContext(
    val user: UserToken,
    val headers: Map<String, String>
)
