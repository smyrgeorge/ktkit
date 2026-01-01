package io.github.smyrgeorge.ktkit.config

import io.github.smyrgeorge.ktkit.util.readEntireFileUtf8
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.properties.Properties

object ConfigProperties {
    inline fun <reified T : @Serializable Any> loadFromFile(
        deserializer: DeserializationStrategy<T>,
        path: String
    ): T {
        val file = readEntireFileUtf8(path)
        val map = parseToStringMap(file)
        return loadFromStringMap(deserializer, map)
    }

    inline fun <reified T : @Serializable Any> loadFromStringMap(
        deserializer: DeserializationStrategy<T>,
        map: Map<String, String>
    ): T = decodeFromStringMap(deserializer, map)

    fun parseToStringMap(file: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        file.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith('#') && !it.startsWith('!') }
            .forEach { line ->
                // Check that the line is a valid property line
                require(linePropertyRegex.matches(line)) { "Invalid property line: $line" }
                val parts = line.split(splitLinePropertyRegex, limit = 2)
                val key = parts[0].trim()
                val value = parts[1].trim()
                result[key] = value
            }

        return result
    }

    inline fun <reified T : @Serializable Any> decodeFromStringMap(
        deserializer: DeserializationStrategy<T>,
        map: Map<String, String>
    ): T {
        @OptIn(ExperimentalSerializationApi::class)
        return Properties.decodeFromStringMap(deserializer, map)
    }

    private val linePropertyRegex = Regex("^([^=:]+)[=:](.*)$")
    private val splitLinePropertyRegex = Regex("[=:]")
}
