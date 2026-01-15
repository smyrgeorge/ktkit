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

    fun encoders(json: Json, types: Set<KClass<*>>): ValueEncoderRegistry =
        types
            .flatMap { encoders(json, it) }
            .let { encoders ->
                ValueEncoderRegistry().apply {
                    encoders.forEach { (clazz, encoder) -> register(clazz, encoder) }
                }
            }

    fun <T : @Serializable Any> encoders(json: Json, clazz: KClass<T>): List<Pair<KClass<*>, ValueEncoder<T>>> =
        encoders(json, clazz, clazz.getSealedSubclasses().toSet())

    fun <T : @Serializable Any> encoders(
        json: Json,
        clazz: KClass<T>,
        subclasses: Set<KClass<*>>
    ): List<Pair<KClass<*>, ValueEncoder<T>>> {
        val main = encoder(json, clazz)

        val subclasses = subclasses.map { subclass ->
            subclass to object : ValueEncoder<T> {
                override fun encode(value: T): String =
                    json.encodeToString(clazz.serializer(), value)

                override fun decode(value: ResultSet.Row.Column): T =
                    json.decodeFromString(clazz.serializer(), value.asString())
            }
        }

        return subclasses + (clazz to main)
    }

    fun <T : @Serializable Any> encoder(json: Json, clazz: KClass<T>): ValueEncoder<T> =
        object : ValueEncoder<T> {
            override fun encode(value: T): String =
                json.encodeToString(clazz.serializer(), value)

            override fun decode(value: ResultSet.Row.Column): T =
                json.decodeFromString(clazz.serializer(), value.asString())
        }
}
