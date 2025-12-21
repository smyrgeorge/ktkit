@file:OptIn(ExperimentalUuidApi::class)

package io.github.smyrgeorge.ktorlib.util

import io.github.smyrgeorge.ktorlib.context.UserToken
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

var SYSTEM_USER: UserToken =
    UserToken(
        uuid = Uuid.parse("00000000-0000-0000-0000-000000000000"),
        username = "System",
        email = "system@internal.user",
        name = "System User",
        firstName = "System",
        lastName = "Internal"
    )

var ANONYMOUS_USER: UserToken =
    UserToken(
        uuid = Uuid.parse("00000000-0000-0000-0000-000000000000"),
        username = "Anonymous",
        email = "anonymous@internal.user",
        name = "Anonymous User",
        firstName = "Anonymous",
        lastName = "Internal"
    )