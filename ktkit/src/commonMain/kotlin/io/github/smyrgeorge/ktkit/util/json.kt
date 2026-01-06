package io.github.smyrgeorge.ktkit.util

import io.github.smyrgeorge.ktkit.error.ErrorSpec
import io.github.smyrgeorge.ktkit.error.system.BadRequest
import io.github.smyrgeorge.ktkit.error.system.DatabaseError
import io.github.smyrgeorge.ktkit.error.system.Forbidden
import io.github.smyrgeorge.ktkit.error.system.InternalServerError
import io.github.smyrgeorge.ktkit.error.system.MissingParameter
import io.github.smyrgeorge.ktkit.error.system.NotFound
import io.github.smyrgeorge.ktkit.error.system.Unauthorized
import io.github.smyrgeorge.ktkit.error.system.UnknownError
import io.github.smyrgeorge.ktkit.error.system.UnsupportedEnumValue
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

internal fun JsonBuilder.default() {
    isLenient = true
    prettyPrint = false
    ignoreUnknownKeys = true
    explicitNulls = false
    serializersModule = defaultSerializersModule
}

internal val defaultSerializersModule = SerializersModule {
    polymorphic(ErrorSpec::class) {
        subclass(BadRequest::class)
        subclass(DatabaseError::class)
        subclass(Forbidden::class)
        subclass(InternalServerError::class)
        subclass(MissingParameter::class)
        subclass(NotFound::class)
        subclass(Unauthorized::class)
        subclass(UnknownError::class)
        subclass(UnsupportedEnumValue::class)
    }
}
