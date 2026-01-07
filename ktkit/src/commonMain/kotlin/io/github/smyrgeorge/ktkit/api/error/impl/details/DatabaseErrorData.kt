package io.github.smyrgeorge.ktkit.api.error.impl.details

import kotlinx.serialization.Serializable

@Serializable
class DatabaseErrorData(
    val code: String
) : SystemErrorData
