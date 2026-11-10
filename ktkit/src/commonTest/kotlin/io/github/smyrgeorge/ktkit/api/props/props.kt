package io.github.smyrgeorge.ktkit.api.props

import kotlinx.serialization.Serializable

@Serializable
data class SimpleConfig(
    val name: String,
    val port: Int,
    val enabled: Boolean
)

@Serializable
data class NestedConfig(
    val server: ServerConfig,
    val database: DatabaseConfig
)

@Serializable
data class ServerConfig(
    val host: String,
    val port: Int
)

@Serializable
data class DatabaseConfig(
    val url: String,
    val maxConnections: Int
)

@Serializable
data class DottedConfig(
    val a: DottedSection
)

@Serializable
data class DottedSection(
    val x: DottedLeaf,
    val z: DottedLeaf? = null
)

@Serializable
data class DottedLeaf(
    val y: Int
)

@Serializable
data class LabelsConfig(
    val labels: Map<String, String>
)

@Serializable
data class ConfigWithOptional(
    val name: String,
    val description: String = "default"
)
