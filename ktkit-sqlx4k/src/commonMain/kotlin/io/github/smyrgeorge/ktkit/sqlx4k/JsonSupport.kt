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
 * A utility object that provides JSON serialization and deserialization support
 * for database-related operations using Kotlin Serialization.
 *
 * This object facilitates the creation of custom encoders and decoders, capable
 * of handling specific types and their subclasses. These encoders can be used for
 * translating between Kotlin objects and their database representations.
 *
 * Functionality:
 * - Configures a default JSON serialization format with a snake_case naming strategy.
 * - Creates preconfigured `ValueEncoderRegistry` instances for given types.
 * - Provides flexible methods for generating encoders for sealed and non-sealed classes.
 */
object JsonSupport {
    @OptIn(ExperimentalSerializationApi::class)
    private val defaultJson by lazy {
        Json {
            default()
            namingStrategy = JsonNamingStrategy.SnakeCase
        }
    }

    fun encoders(types: Set<KClass<*>>): ValueEncoderRegistry =
        encoders(defaultJson, types)

    fun encoders(types: Set<Pair<KClass<*>, Set<KClass<*>>>>): ValueEncoderRegistry =
        encoders(defaultJson, types)

    fun encoders(json: Json, types: Set<KClass<*>>): ValueEncoderRegistry =
        types
            .flatMap { encoder(json, it) }
            .let { encoders ->
                ValueEncoderRegistry().apply {
                    encoders.forEach { (clazz, encoder) -> register(clazz, encoder) }
                }
            }

    fun encoders(json: Json, types: Set<Pair<KClass<*>, Set<KClass<*>>>>): ValueEncoderRegistry =
        types
            .flatMap { (base, subclasses) -> encoder(json, base, subclasses) }
            .let { encoders ->
                ValueEncoderRegistry().apply {
                    encoders.forEach { (clazz, encoder) -> register(clazz, encoder) }
                }
            }

    fun <T : @Serializable Any> encoder(json: Json, clazz: KClass<T>): List<Pair<KClass<*>, ValueEncoder<T>>> =
        encoder(json, clazz, clazz.getSealedSubclasses().toSet())

    fun <T : @Serializable Any> encoder(
        json: Json,
        clazz: KClass<T>,
        subclasses: Set<KClass<*>>
    ): List<Pair<KClass<*>, ValueEncoder<T>>> {
        val subclasses = subclasses.map {
            it to object : ValueEncoder<T> {
                override fun encode(value: T): String =
                    json.encodeToString(clazz.serializer(), value)

                override fun decode(value: ResultSet.Row.Column): T =
                    json.decodeFromString(clazz.serializer(), value.asString())
            }
        }

        val main = object : ValueEncoder<T> {
            override fun encode(value: T): String =
                json.encodeToString(clazz.serializer(), value)

            override fun decode(value: ResultSet.Row.Column): T =
                json.decodeFromString(clazz.serializer(), value.asString())
        }

        return subclasses + (clazz to main)
    }
}
