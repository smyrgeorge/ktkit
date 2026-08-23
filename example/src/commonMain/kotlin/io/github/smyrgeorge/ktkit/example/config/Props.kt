package io.github.smyrgeorge.ktkit.example.config

import kotlinx.serialization.Serializable

@Serializable
data class Props(
    val server: Server,
    val database: Database,
) {
    @Serializable
    data class Server(
        val host: String,
        val port: Int,
    )

    @Serializable
    data class Database(
        val url: String,
        val username: String,
        val password: String,
        val maxConnections: Int,
    )
}
