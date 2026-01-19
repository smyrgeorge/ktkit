package io.github.smyrgeorge.ktkit.util

import io.github.smyrgeorge.ktkit.api.error.ErrorSpecData
import io.github.smyrgeorge.ktkit.api.error.impl.details.DatabaseErrorData
import io.github.smyrgeorge.ktkit.api.error.impl.details.EmptyErrorData
import io.github.smyrgeorge.ktkit.api.error.impl.details.MissingParameterErrorData
import io.github.smyrgeorge.ktkit.api.error.impl.details.UnsupportedEnumValueErrorData
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

fun JsonBuilder.defaultWithErrors() {
    default()
    serializersModule = defaultSerializersModule
}

val defaultSerializersModule = SerializersModule {
    polymorphic(ErrorSpecData::class) {
        subclass(DatabaseErrorData::class)
        subclass(EmptyErrorData::class)
        subclass(MissingParameterErrorData::class)
        subclass(UnsupportedEnumValueErrorData::class)
    }
}

fun JsonBuilder.default() {
    classDiscriminator = "@type"
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = false
    prettyPrint = false
    coerceInputValues = false
    explicitNulls = false
    serializersModule = defaultSerializersModule
}
