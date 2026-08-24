package io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema

import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.obj
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.str

object Schemas {
    fun string(): JsonNode.Obj = obj("type" to str("string"))
    fun stringArray(): JsonNode.Obj = arrayOf(string())
    fun int32(): JsonNode.Obj = obj("type" to str("integer"), "format" to str("int32"))
    fun int64(): JsonNode.Obj = obj("type" to str("integer"), "format" to str("int64"))
    fun float(): JsonNode.Obj = obj("type" to str("number"), "format" to str("float"))
    fun double(): JsonNode.Obj = obj("type" to str("number"), "format" to str("double"))
    fun boolean(): JsonNode.Obj = obj("type" to str("boolean"))

    /** An array schema of [items]. */
    fun arrayOf(items: JsonNode.Obj): JsonNode.Obj = obj("type" to str("array"), "items" to items)

    /** The schema produced by an `HttpContext.Var` conversion (`asInt`, ...), or `null` for enum/unknown ones. */
    fun fromConversion(conversion: String): JsonNode.Obj? = when (conversion) {
        "asString" -> string()
        "asInt" -> int32()
        "asLong" -> int64()
        "asFloat" -> float()
        "asDouble" -> double()
        "asBoolean" -> boolean()
        else -> null
    }
}
