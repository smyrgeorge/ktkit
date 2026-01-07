package io.github.smyrgeorge.ktkit.api.props

import com.akuleshov7.ktoml.Toml
import io.github.smyrgeorge.ktkit.util.readEntireFileFromDisk
import io.github.smyrgeorge.ktkit.util.readEntireFileFromResources
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
    inline fun <reified T : @Serializable Any> loadFromFileSystem(
        deserializer: DeserializationStrategy<T>,
        path: String
    ): T = loadFromFileSystem(deserializer, Path(path))

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
    inline fun <reified T : @Serializable Any> loadFromFileSystem(
        deserializer: DeserializationStrategy<T>,
        path: Path
    ): T {
        val file = readEntireFileFromDisk(path)
        return loadFile(deserializer, file)
    }

    /**
     * Loads and deserializes a resource file into an object of type `T` using the provided deserialization strategy.
     *
     * @param T The type of object to deserialize the resource file into.
     * @param deserializer The deserialization strategy to use for interpreting the contents of the resource file.
     * @param path The path to the resource file to be loaded and deserialized.
     * @return An instance of type `T` created from the deserialized contents of the resource file.
     */
    inline fun <reified T : @Serializable Any> loadFromResources(
        deserializer: DeserializationStrategy<T>,
        path: String
    ): T = loadFromResources(deserializer, Path(path))

    /**
     * Loads and deserializes a resource file into an object of type `T` using the provided deserialization strategy.
     *
     * @param T The type of object to deserialize the resource file into.
     * @param deserializer The deserialization strategy to use for interpreting the contents of the resource file.
     * @param path The path to the resource file to be loaded and deserialized.
     * @return An instance of type `T` created from the deserialized contents of the resource file.
     */
    inline fun <reified T : @Serializable Any> loadFromResources(
        deserializer: DeserializationStrategy<T>,
        path: Path
    ): T {
        val file = readEntireFileFromResources(path)
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
