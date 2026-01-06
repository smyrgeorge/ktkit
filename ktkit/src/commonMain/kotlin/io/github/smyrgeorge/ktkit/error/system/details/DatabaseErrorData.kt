package io.github.smyrgeorge.ktkit.error.system.details

import kotlinx.serialization.Serializable

@Serializable
class DatabaseErrorData(
    val code: String
) : SystemErrorData
