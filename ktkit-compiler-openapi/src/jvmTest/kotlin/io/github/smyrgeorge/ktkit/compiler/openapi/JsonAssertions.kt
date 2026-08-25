package io.github.smyrgeorge.ktkit.compiler.openapi

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Navigation helpers for asserting on generated OpenAPI fragments. */

fun JsonObject.obj(key: String): JsonObject =
    (this[key] ?: throw AssertionError("missing key '$key' in $keys")).jsonObject

fun JsonObject.arr(key: String): JsonArray =
    (this[key] ?: throw AssertionError("missing key '$key' in $keys")).jsonArray

fun JsonObject.str(key: String): String =
    (this[key] ?: throw AssertionError("missing key '$key' in $keys")).jsonPrimitive.content

fun JsonObject.strings(key: String): List<String> = arr(key).map { it.jsonPrimitive.content }

/** The operation object of `<verb> <path>` in a fragment. */
fun JsonObject.operation(verb: String, path: String): JsonObject = obj("paths").obj(path).obj(verb)

/** The `name -> parameter` map of an operation. */
fun JsonObject.parametersByName(): Map<String, JsonObject> =
    (this["parameters"]?.jsonArray ?: JsonArray(emptyList()))
        .map { it.jsonObject }
        .associateBy { it.str("name") }

fun JsonObject.refOf(key: String): String = obj(key).str($$"$ref")
