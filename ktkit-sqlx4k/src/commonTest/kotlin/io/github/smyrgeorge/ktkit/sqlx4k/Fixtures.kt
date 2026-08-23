package io.github.smyrgeorge.ktkit.sqlx4k

import io.github.smyrgeorge.sqlx4k.ResultSet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Sample(val fullName: String, val age: Int = 3, val nickName: String? = null)

@Serializable
sealed interface Event {
    @Serializable
    @SerialName("created")
    data class Created(val id: Int) : Event

    @Serializable
    @SerialName("deleted")
    data class Deleted(val id: Int) : Event
}

internal fun column(value: String): ResultSet.Row.Column =
    ResultSet.Row.Column(ordinal = 0, name = "data", type = "jsonb", value = value)
