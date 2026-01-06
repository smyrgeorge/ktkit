package io.github.smyrgeorge.ktkit.api.props

import com.akuleshov7.ktoml.Toml
import io.github.smyrgeorge.ktkit.util.readEntireFileUtf8
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable

/**
 * Utility object for loading and deserializing configuration properties from TOML files.
 *
 * Provides a method for reading TOML files and deserializing their content into strongly typed objects
 * using Kotlin serialization.
 */
object ConfigPropertiesToml {
    inline fun <reified T : @Serializable Any> loadFromFile(
        deserializer: DeserializationStrategy<T>,
        path: String
    ): T {
        val file = readEntireFileUtf8(path)
        return Toml.decodeFromString(deserializer, file)
    }
}
