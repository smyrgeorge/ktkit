package io.github.smyrgeorge.ktkit.example.test

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class TestDto(
    val id: Int,
    val createdAt: Instant,
    val createdBy: Uuid,
    val updatedAt: Instant,
    val updatedBy: Uuid,
    val test: String,
    val data: Test.Data
)
