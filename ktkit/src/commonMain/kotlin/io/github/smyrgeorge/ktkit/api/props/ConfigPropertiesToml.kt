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
    /**
     * Loads and deserializes a file's contents into an object of type `T`.
     *
     * The file is read as UTF-8, and its contents are deserialized using the provided `deserializer`
     * and `TOML` format.
     *
     * @param deserializer The deserialization strategy used to interpret the contents of the file.
     * @param path The path to the file that will be read and deserialized.
     * @return An instance of type `T` created from the file's contents.
     */
    inline fun <reified T : @Serializable Any> loadFromFile(
        deserializer: DeserializationStrategy<T>,
        path: String
    ): T {
        val file = readEntireFileUtf8(path)
        return Toml.decodeFromString(deserializer, file)
    }
}
