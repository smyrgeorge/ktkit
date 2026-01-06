package io.github.smyrgeorge.ktkit.error.system.details

import kotlinx.serialization.Serializable

@Serializable
class MissingParameterErrorData(
    val kind: String,
    val name: String,
) : SystemErrorData
