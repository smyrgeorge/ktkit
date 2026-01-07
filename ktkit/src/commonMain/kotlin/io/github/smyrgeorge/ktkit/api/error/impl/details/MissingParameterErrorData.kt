package io.github.smyrgeorge.ktkit.api.error.impl.details

import kotlinx.serialization.Serializable

@Serializable
class MissingParameterErrorData(
    val kind: String,
    val name: String,
) : SystemErrorData
