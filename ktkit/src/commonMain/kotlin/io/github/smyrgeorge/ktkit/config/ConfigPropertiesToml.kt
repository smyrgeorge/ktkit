package io.github.smyrgeorge.ktkit.config

import com.akuleshov7.ktoml.Toml
import io.github.smyrgeorge.ktkit.util.readEntireFileUtf8
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable

object ConfigPropertiesToml {
    inline fun <reified T : @Serializable Any> loadFromFile(
        deserializer: DeserializationStrategy<T>,
        path: String
    ): T {
        val file = readEntireFileUtf8(path)
        return Toml.decodeFromString(deserializer, file)
    }
}
