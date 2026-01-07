package io.github.smyrgeorge.ktkit.api.props

import com.akuleshov7.ktoml.Toml
import io.github.smyrgeorge.ktkit.util.readEntireFileUtf8
import kotlinx.io.files.Path
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
    inline fun <reified T : @Serializable Any> loadFrom(
        deserializer: DeserializationStrategy<T>,
        path: String
    ): T = loadFrom(deserializer, Path(path))

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
    inline fun <reified T : @Serializable Any> loadFrom(
        deserializer: DeserializationStrategy<T>,
        path: Path
    ): T {
        val file = readEntireFileUtf8(path)
        return loadFile(deserializer, file)
    }

    /**
     * Loads and deserializes a file's contents into an object of type `T`.
     *
     * The file is read as UTF-8, and its contents are deserialized using the provided `deserializer`
     * and `TOML` format.
     *
     * @param deserializer The deserialization strategy used to interpret the contents of the file.
     * @param content The contents of the file to be deserialized.
     * @return An instance of type `T` created from the file's contents.
     */
    inline fun <reified T : @Serializable Any> loadFile(
        deserializer: DeserializationStrategy<T>,
        content: String
    ): T = Toml.decodeFromString(deserializer, content)
}
