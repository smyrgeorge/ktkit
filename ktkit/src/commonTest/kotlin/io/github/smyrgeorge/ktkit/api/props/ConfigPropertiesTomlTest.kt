package io.github.smyrgeorge.ktkit.api.props

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigPropertiesTomlTest {
    @Test
    fun parseSimpleToml() {
        val toml = """
            name = "test-app"
            port = 8080
            enabled = true
        """.trimIndent()

        val config: SimpleConfig = ConfigPropertiesToml.parse(toml)

        assertEquals("test-app", config.name)
        assertEquals(8080, config.port)
        assertEquals(true, config.enabled)
    }

    @Test
    fun parseNestedToml() {
        val toml = """
            [server]
            host = "localhost"
            port = 8080

            [database]
            url = "postgresql://localhost:5432/db"
            maxConnections = 10
        """.trimIndent()

        val config: NestedConfig = ConfigPropertiesToml.parse(toml)

        assertEquals("localhost", config.server.host)
        assertEquals(8080, config.server.port)
        assertEquals("postgresql://localhost:5432/db", config.database.url)
        assertEquals(10, config.database.maxConnections)
    }

    @Test
    fun parseTomlWithOptionalFieldMissing() {
        val toml = """
            name = "test-app"
        """.trimIndent()

        val config: ConfigWithOptional = ConfigPropertiesToml.parse(toml)

        assertEquals("test-app", config.name)
        assertEquals("default", config.description)
    }

    @Test
    fun parseTomlWithOptionalFieldPresent() {
        val toml = """
            name = "test-app"
            description = "custom description"
        """.trimIndent()

        val config: ConfigWithOptional = ConfigPropertiesToml.parse(toml)

        assertEquals("test-app", config.name)
        assertEquals("custom description", config.description)
    }

    @Test
    fun parseMergesBaseWithOverride() {
        val base = """
            name = "base-app"
            port = 8080
            enabled = false
        """.trimIndent()

        val override = """
            name = "override-app"
            enabled = true
        """.trimIndent()

        val config: SimpleConfig = ConfigPropertiesToml.parse(base, override)

        assertEquals("override-app", config.name)
        assertEquals(8080, config.port)
        assertEquals(true, config.enabled)
    }

    @Test
    fun parseMergesNestedConfigs() {
        val base = """
            [server]
            host = "localhost"
            port = 8080

            [database]
            url = "postgresql://localhost:5432/db"
            maxConnections = 10
        """.trimIndent()

        val override = """
            [server]
            host = "0.0.0.0"
            port = 9090

            [database]
            maxConnections = 50
        """.trimIndent()

        val config: NestedConfig = ConfigPropertiesToml.parse(base, override)

        assertEquals("0.0.0.0", config.server.host)
        assertEquals(9090, config.server.port)
        assertEquals("postgresql://localhost:5432/db", config.database.url)
        assertEquals(50, config.database.maxConnections)
    }

    @Test
    fun parseMergesPartialNestedOverride() {
        val base = """
            [server]
            host = "localhost"
            port = 8080

            [database]
            url = "postgresql://localhost:5432/db"
            maxConnections = 10
        """.trimIndent()

        val override = """
            [database]
            maxConnections = 100
        """.trimIndent()

        val config: NestedConfig = ConfigPropertiesToml.parse(base, override)

        assertEquals("localhost", config.server.host)
        assertEquals(8080, config.server.port)
        assertEquals("postgresql://localhost:5432/db", config.database.url)
        assertEquals(100, config.database.maxConnections)
    }
}
