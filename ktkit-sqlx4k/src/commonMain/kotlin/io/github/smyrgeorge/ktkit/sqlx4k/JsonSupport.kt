@file:OptIn(InternalSerializationApi::class)

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
 * Utility object for providing JSON serialization and deserialization support,
 * including the creation of value encoders for mapping Kotlin classes to string
 * representations suitable for database interactions.
 *
 * This object simplifies the registration and usage of JSON-based encoders
 * by offering methods for generating value encoder registries for specified
 * Kotlin classes. It supports a customizable JSON configuration through the
 * kotlinx.serialization library.
 */
object JsonSupport {
    @OptIn(ExperimentalSerializationApi::class)
    private val defaultJson by lazy {
        Json {
            default()
            namingStrategy = JsonNamingStrategy.SnakeCase
        }
    }

    fun encoders(vararg types: KClass<*>): ValueEncoderRegistry =
        encoders(defaultJson, *types)

    fun encoders(json: Json, vararg types: KClass<*>): ValueEncoderRegistry =
        encoders(json, types.toSet())

    fun encoders(types: Set<KClass<*>>): ValueEncoderRegistry =
        encoders(defaultJson, types)

    fun encoders(json: Json, types: Set<KClass<*>>): ValueEncoderRegistry {
        val encoders = types.map { it to encoder(json, it) }
        return ValueEncoderRegistry().apply {
            encoders.forEach { (clazz, encoder) -> register(clazz, encoder) }
        }
    }

    fun <T : @Serializable Any> encoder(json: Json, clazz: KClass<T>): ValueEncoder<T> {
        return object : ValueEncoder<T> {
            override fun encode(value: T): String =
                json.encodeToString(clazz.serializer(), value)

            override fun decode(value: ResultSet.Row.Column): T =
                json.decodeFromString(clazz.serializer(), value.asString())
        }
    }
}
