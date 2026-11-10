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

    // `x.y = 1` under `[a]` means a = { x = { y = 1 } }.
    // Before: merging (even with an empty override) lost the `x.` prefix -> "Missing required property <x>".
    @Test
    fun parseMergeKeepsALoneDottedKey() {
        val base = """
            [a]
            x.y = 1
        """.trimIndent()

        val config: DottedConfig = ConfigPropertiesToml.parse(base, "")

        assertEquals(DottedConfig(DottedSection(x = DottedLeaf(1))), config)
    }

    // base `x.y = 1, z.y = 2` + override `z.y = 3` -> x.y = 1 (kept), z.y = 3 (overridden).
    @Test
    fun parseMergeOverridesADottedKeyAndKeepsItsSibling() {
        val base = """
            [a]
            x.y = 1
            z.y = 2
        """.trimIndent()

        val override = """
            [a]
            z.y = 3
        """.trimIndent()

        val config: DottedConfig = ConfigPropertiesToml.parse(base, override)

        assertEquals(DottedConfig(DottedSection(x = DottedLeaf(1), z = DottedLeaf(3))), config)
    }

    // Decoding into a Map (not a class) still works after a merge:
    // {name, env = local} + {env = prod, region} -> {name, env = prod, region}.
    @Test
    fun parseMergesIntoATopLevelMap() {
        val base = """
            name = "base-app"
            env = "local"
        """.trimIndent()

        val override = """
            env = "prod"
            region = "eu"
        """.trimIndent()

        val config: Map<String, String> = ConfigPropertiesToml.parse(base, override)

        assertEquals(mapOf("name" to "base-app", "env" to "prod", "region" to "eu"), config)
    }

    // base `[labels] k1, k2` + override with an empty `[labels]` -> labels stay {k1, k2}.
    // The empty table must not leave anything behind in the merged tree.
    @Test
    fun parseMergeKeepsAMapSectionWhenTheOverrideTableIsEmpty() {
        val base = """
            [labels]
            k1 = "a"
            k2 = "b"
        """.trimIndent()

        val override = """
            [labels]
        """.trimIndent()

        val config: LabelsConfig = ConfigPropertiesToml.parse(base, override)

        assertEquals(LabelsConfig(mapOf("k1" to "a", "k2" to "b")), config)
    }

    // base with an empty `[labels]` + override `[labels] k1` -> labels = {k1}.
    @Test
    fun parseMergeFillsAMapSectionThatIsEmptyInTheBase() {
        val base = """
            [labels]
        """.trimIndent()

        val override = """
            [labels]
            k1 = "a"
        """.trimIndent()

        val config: LabelsConfig = ConfigPropertiesToml.parse(base, override)

        assertEquals(LabelsConfig(mapOf("k1" to "a")), config)
    }
}
