package io.github.smyrgeorge.ktkit.api.error.impl.details

import kotlinx.serialization.Serializable

@Serializable
class MalformedParameterErrorData(
    val kind: String,
    val name: String,
    val expected: String,
    val value: String,
) : SystemErrorData
