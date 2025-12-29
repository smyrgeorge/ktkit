package io.github.smyrgeorge.ktorlib.api.mq.pgmq

import io.github.smyrgeorge.ktorlib.context.UserToken

class EventContext(
    val user: UserToken
) {
}