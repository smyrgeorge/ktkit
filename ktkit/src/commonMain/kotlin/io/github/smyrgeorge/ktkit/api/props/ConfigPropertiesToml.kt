package io.github.smyrgeorge.ktkit.api.props

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.writers.TomlWriter
import io.github.smyrgeorge.ktkit.util.FS
import io.github.smyrgeorge.ktkit.util.getEnv
import io.github.smyrgeorge.ktkit.util.plus
import io.github.smyrgeorge.ktkit.util.readEntireFileFromDisk
import io.github.smyrgeorge.ktkit.util.readEntireFileFromResources
import io.github.smyrgeorge.ktkit.util.toPath
import kotlinx.io.files.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * Utility object for loading and deserializing configuration properties from TOML files.
 *
 * Provides a method for reading TOML files and deserializing their content into strongly typed objects
 * using Kotlin serialization.
 */
object ConfigPropertiesToml {
    inline fun <reified T : @Serializable Any> load(): T {
        val base = readEntireFileFromResources(Path("application.toml"))

        return when {
            FS.exists("application.toml".toPath()) ->
                parse(base, readEntireFileFromDisk("application.toml".toPath()))

            FS.exists("config/application.toml".toPath()) ->
                parse(base, readEntireFileFromDisk("config/application.toml".toPath()))

            FS.exists("application.local.toml".toPath()) ->
                parse(base, readEntireFileFromDisk("application.local.toml".toPath()))

            FS.exists("config/application.local.toml".toPath()) ->
                parse(base, readEntireFileFromDisk("config/application.local.toml".toPath()))

            else -> parse(base)
        }
    }

    /**
     * Loads and deserializes a file's contents into an object of type `T`.
     *
     * The file is read as UTF-8, and its contents are deserialized using the provided `deserializer`
     * and `TOML` format.
     *
     * @param path The path to the file that will be read and deserialized.
     * @return An instance of type `T` created from the file's contents.
     */
    inline fun <reified T : @Serializable Any> loadFromFileSystem(path: String): T = loadFromFileSystem(Path(path))

    /**
     * Loads and deserializes a file's contents into an object of type `T`.
     *
     * The file is read as UTF-8, and its contents are deserialized using the provided `deserializer`
     * and `TOML` format.
     *
     * @param path The path to the file that will be read and deserialized.
     * @return An instance of type `T` created from the file's contents.
     */
    inline fun <reified T : @Serializable Any> loadFromFileSystem(path: Path): T {
        val file = readEntireFileFromDisk(path)
        return parse(file)
    }

    /**
     * Loads and deserializes a resource file into an object of type `T` using the provided deserialization strategy.
     *
     * @param T The type of object to deserialize the resource file into.
     * @param path The path to the resource file to be loaded and deserialized.
     * @return An instance of type `T` created from the deserialized contents of the resource file.
     */
    inline fun <reified T : @Serializable Any> loadFromResources(path: String): T = loadFromResources(Path(path))

    /**
     * Loads and deserializes a resource file into an object of type `T` using the provided deserialization strategy.
     *
     * @param T The type of object to deserialize the resource file into.
     * @param path The path to the resource file to be loaded and deserialized.
     * @return An instance of type `T` created from the deserialized contents of the resource file.
     */
    inline fun <reified T : @Serializable Any> loadFromResources(path: Path): T {
        val file = readEntireFileFromResources(path)
        return parse(file)
    }

    /**
     * Parses the given file content into an object of type `T` using the provided deserialization strategy.
     *
     * This function resolves environment variable placeholders in the content before deserializing it
     * using the TOML format.
     *
     * @param content The file content as a string, which may include environment variable placeholders.
     * @return An instance of type `T` created from the deserialized content.
     */
    inline fun <reified T : @Serializable Any> parse(content: String): T =
        Toml.decodeFromString(resolveEnvironmentVariables(content))

    /**
     * Loads and deserializes an object of type `T` from two TOML configuration file paths.
     * The method resolves the input paths into `Path` representations, delegates file reading
     * and merging tasks, and deserializes the final result into an instance of type `T`.
     *
     * @param base The string path representing the base TOML file, which serves as the primary
     *             configuration source.
     * @param override The string path representing the override TOML file, whose values take
     *                 precedence over overlapping values in the base file during merging.
     * @return An instance of type `T` created by deserializing the merged TOML content.
     */
    inline fun <reified T : @Serializable Any> loadFromFileSystem(base: String, override: String): T =
        loadFromFileSystem(Path(base), Path(override))

    /**
     * Loads and deserializes an object of type `T` from two TOML configuration files.
     * The method reads the contents of both the `base` and `override` files, merges them,
     * and then deserializes the merged result into an object of type `T`.
     *
     * @param base The path to the base TOML file to be used as the primary configuration source.
     * @param override The path to the override TOML file, whose values will take precedence
     *                 over any overlapping values in the base file during merging.
     * @return An instance of type `T` created by deserializing the merged TOML content.
     */
    inline fun <reified T : @Serializable Any> loadFromFileSystem(base: Path, override: Path): T =
        parse(readEntireFileFromDisk(base), readEntireFileFromDisk(override))

    /**
     * Loads and merges two TOML configuration files from the specified resource paths into an object of type `T`.
     *
     * This function reads the contents of the `base` and `override` resource paths, merges them,
     * and deserializes the result into the specified type `T`. When merging, values from `override`
     * take precedence over conflicting values in `base`.
     *
     * @param base The resource path to the base TOML configuration file.
     * @param override The resource path to the override TOML configuration file.
     * @return An instance of type `T` created from the merged TOML configuration.
     * @throws IllegalStateException if the resource files are inaccessible or cannot be parsed.
     */
    inline fun <reified T : @Serializable Any> loadFromResources(base: String, override: String): T =
        loadFromResources(Path(base), Path(override))

    /**
     * Loads and merges two TOML configuration files from the specified resource paths into an object of type `T`.
     *
     * This function reads the contents of the `base` and `override` resource paths, merges them,
     * and deserializes the result into the specified type `T`. When merging, values from `override`
     * take precedence over conflicting values in `base`.
     *
     * @param base The resource path to the base TOML configuration file.
     * @param override The resource path to the override TOML configuration file.
     * @return An instance of type `T` created from the merged TOML configuration.
     * @throws IllegalStateException if the resource files are inaccessible or cannot be parsed.
     */
    inline fun <reified T : @Serializable Any> loadFromResources(base: Path, override: Path): T =
        parse(readEntireFileFromResources(base), readEntireFileFromResources(override))

    /**
     * Parses and merges two TOML file contents into an object of type `T`.
     *
     * The `base` and `override` files are parsed into TOML trees, merged, and then deserialized
     * into the specified type `T`. The merging ensures that values from `override` take precedence
     * over any conflicting values in `base`.
     *
     * @param base The base TOML file path to be used as the starting point.
     * @param override The override TOML file path whose values take precedence
     *                 during the merge process.
     * @return An instance of type `T` created from the merged TOML content.
     */
    inline fun <reified T : @Serializable Any> parse(base: String, override: String): T {
        val base: TomlFile = Toml.tomlParser.parseString(resolveEnvironmentVariables(base))
        val override: TomlFile = Toml.tomlParser.parseString(resolveEnvironmentVariables(override))
        val merged = TomlWriter(TomlOutputConfig()).writeToString(base + override)
        return Toml.decodeFromString(merged)
    }

    /**
     * Resolves environment variable placeholders within the given string.
     * This function identifies placeholders in the form of `${VAR_NAME}` and replaces
     * them with the respective values of the corresponding environment variables.
     * If a placeholder references a missing environment variable, the function throws an error.
     *
     * @param file The input string potentially containing placeholders for environment variables.
     * @return A string with the environment variable placeholders replaced by their respective values.
     * @throws IllegalStateException if an environment variable referenced in the input string is missing.
     */
    fun resolveEnvironmentVariables(file: String): String = envRegex.replace(file) { match ->
        val key = match.groupValues[1]
        getEnv(key) ?: error("Missing environment variable: $key")
    }

    private val envRegex = Regex("""\$\{([A-Za-z0-9_]+)}""")
}
