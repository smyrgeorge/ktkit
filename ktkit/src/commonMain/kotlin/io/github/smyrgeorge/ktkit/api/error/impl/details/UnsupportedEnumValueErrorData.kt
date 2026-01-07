package io.github.smyrgeorge.ktkit.api.error.impl.details

import kotlinx.serialization.Serializable

@Serializable
class UnsupportedEnumValueErrorData(
    val kind: String,
    val value: String,
) : SystemErrorData
