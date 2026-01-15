@file:OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)

package io.github.smyrgeorge.ktkit.sqlx4k

import io.github.smyrgeorge.ktkit.util.default
import io.github.smyrgeorge.sqlx4k.ResultSet
import io.github.smyrgeorge.sqlx4k.ValueEncoder
import io.github.smyrgeorge.sqlx4k.ValueEncoderRegistry
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * Utility object for JSON serialization and deserialization support with configurable strategies.
 *
 * The primary goals of this object include:
 * - Providing a default configured instance of `Json` with common settings suitable for most use cases.
 * - Registering value encoders for handling serialization and deserialization of given types.
 * - Supporting `@Serializable` Kotlin classes for JSON transformation through the `ValueEncoder` interface.
 *
 * The default JSON configuration includes:
 * - Snake-case naming strategy for property names.
 * - Ignoring unknown keys during deserialization.
 * - Omitting defaults and explicit nulls while encoding.
 *
 * Functions:
 * - `encoders`: Creates a `ValueEncoderRegistry` by generating encoders for a set of classes.
 * - `encoders`: Generates encoders for a specific class and its optional sealed subclasses.
 * - `encoder`: Constructs a single `ValueEncoder` for serialization and deserialization of a specific class.
 */
object JsonSupport {
    private val defaultJson by lazy {
        Json {
            default()
            namingStrategy = JsonNamingStrategy.SnakeCase
        }
    }

    /**
     * Generates and registers encoders for a set of types, including their sealed subclasses, with a given JSON serializer.
     *
     * @param types A set of Kotlin classes for which encoders are generated. The classes may include sealed classes.
     * @param json The JSON configuration to be used for serialization. Defaults to `defaultJson`.
     * @return A [ValueEncoderRegistry] populated with the generated encoders for the provided types and their subclasses.
     */
    fun encoders(types: Set<KClass<*>>, json: Json = defaultJson): ValueEncoderRegistry =
        types
            .flatMap { encoders(it, it.sealedSubclasses().toSet(), json) }
            .let { encoders ->
                ValueEncoderRegistry().apply {
                    encoders.forEach { (clazz, encoder) -> register(clazz, encoder) }
                }
            }

    /**
     * Generates encoders for a given class and its optional subclasses using the specified JSON configuration.
     *
     * @param clazz The main Kotlin class for which an encoder is generated. This class must be serializable.
     * @param subclasses A set of subclasses of the specified class for which the same encoder is generated. Defaults to an empty set.
     * @param json The JSON configuration to be used for serialization and deserialization. Defaults to `defaultJson`.
     * @return A list of pairs, where each pair consists of a [KClass] and its corresponding [ValueEncoder].
     */
    fun <T : @Serializable Any> encoders(
        clazz: KClass<T>,
        subclasses: Set<KClass<*>> = emptySet(),
        json: Json = defaultJson
    ): List<Pair<KClass<*>, ValueEncoder<T>>> {
        val main = encoder(clazz, json)
        val subclasses = subclasses.map { subclass -> subclass to main }
        return subclasses + (clazz to main)
    }

    /**
     * Creates a [ValueEncoder] for a specified Kotlin class with optional JSON configuration.
     *
     * @param clazz The Kotlin class for which the encoder will be created. The class must be serializable.
     * @param json The JSON configuration used for encoding and decoding. Defaults to `defaultJson`.
     * @return A [ValueEncoder] capable of serializing and deserializing instances of the specified class.
     */
    fun <T : @Serializable Any> encoder(clazz: KClass<T>, json: Json = defaultJson): ValueEncoder<T> =
        object : ValueEncoder<T> {
            override fun encode(value: T): String =
                json.encodeToString(clazz.serializer(), value)

            override fun decode(value: ResultSet.Row.Column): T =
                json.decodeFromString(clazz.serializer(), value.asString())
        }
}
