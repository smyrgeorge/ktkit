package io.github.smyrgeorge.ktkit.sqlx4k

import io.github.smyrgeorge.sqlx4k.ResultSet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Account(val fullName: String, val age: Int = 3, val nickName: String? = null)

@Serializable
internal sealed interface Change {
    @Serializable
    @SerialName("added")
    data class Added(val id: Int) : Change

    @Serializable
    @SerialName("removed")
    data class Removed(val id: Int) : Change
}

internal fun column(value: String): ResultSet.Row.Column =
    ResultSet.Row.Column(ordinal = 0, name = "data", type = "jsonb", value = value)
