@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema

import io.github.smyrgeorge.ktkit.compiler.openapi.ir.annotationArgument
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.constString
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.isNullableType
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.arr
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.obj
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode.Companion.str
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.simpleArguments
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.typeArgumentOrNull
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.typeOrNull
import io.github.smyrgeorge.ktkit.compiler.openapi.utils.KtkitNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties

/**
 * Generates OpenAPI 3.1 (JSON Schema) schemas from Kotlin IR types, mirroring how the ktkit
 * runtime serializes values with kotlinx.serialization:
 * - `@Serializable` classes become named component schemas referenced with `$ref`.
 * - Property names respect `@SerialName`, `@Transient` properties are omitted.
 * - Properties inherited from `@Serializable` superclasses are included (base-class first).
 * - Generic classes get one component per instantiation (e.g. `PageOfTestDto`), with type
 *   parameters substituted by the concrete type arguments.
 * - Sealed hierarchies become `oneOf` over the concrete leaves with an `@type` discriminator
 *   (ktkit's `classDiscriminator`).
 * - Nullable types allow `null`; properties with default values are not `required`.
 *
 * All named schemas end up in [components]; one generator instance is used per handler class so
 * the collected components land in that handler's fragment.
 */
class SchemaGenerator(private val warn: (String) -> Unit) {

    /** Named component schemas collected while generating (component key → schema). */
    val components = LinkedHashMap<String, JsonNode.Obj>()

    /** Instantiation identity (fq name + rendered type arguments) → component key. */
    private val keyByInstantiation = mutableMapOf<String, String>()

    /**
     * A type argument bound to a class type parameter, together with the bindings that were in
     * scope where the argument was written (so `Page<T>` inside another generic class resolves).
     */
    private class Binding(val type: IrType?, val context: Map<IrTypeParameterSymbol, Binding>) {
        val nullable: Boolean get() = type?.isNullableType() == true
    }

    fun schemaFor(type: IrType?): JsonNode.Obj = schemaFor(type, emptyMap())

    /** An enum schema listing the Kotlin entry names — what `Var.asEnum()` (enumValueOf) accepts. */
    fun enumParamSchema(cls: IrClass): JsonNode.Obj = obj(
        "type" to str("string"),
        "enum" to arr(cls.declarations.filterIsInstance<IrEnumEntry>().map { str(it.name.asString()) }),
    )

    /**
     * Registers ktkit's standard `ApiError` schema as a component and returns a `$ref` to it.
     * The descriptions follow RFC 9457 (Problem Details for HTTP APIs) — keep them in sync with
     * the `@OpenApiInfo` annotations on `io.github.smyrgeorge.ktkit.api.rest.ApiError`.
     */
    fun apiErrorRef(): JsonNode.Obj {
        if (API_ERROR_KEY !in components) {
            components[API_ERROR_KEY] = obj(
                "title" to str(API_ERROR_KEY),
                "description" to str("A problem details object (RFC 9457) — the error payload of all error responses."),
                "type" to str("object"),
                "properties" to obj(
                    "type" to obj(
                        "type" to arr(str("string"), str("null")),
                        "description" to str("A URI reference that identifies the problem type (\"about:blank\" when absent)."),
                    ),
                    "title" to obj(
                        "type" to str("string"),
                        "description" to str("A short, human-readable summary of the problem type."),
                    ),
                    "status" to obj(
                        "type" to str("integer"), "format" to str("int32"),
                        "description" to str("The HTTP status code generated by the origin server for this occurrence of the problem."),
                    ),
                    "detail" to obj(
                        "type" to str("string"),
                        "description" to str("A human-readable explanation specific to this occurrence of the problem."),
                    ),
                    "requestId" to obj(
                        "type" to arr(str("string"), str("null")),
                        "description" to str("The id of the request that produced the problem (extension member)."),
                    ),
                    "data" to obj(
                        "description" to str("Additional problem-specific data (extension member)."),
                    ),
                ),
                "required" to arr(str("title"), str("status"), str("detail")),
            )
        }
        return ref(API_ERROR_KEY)
    }

    private fun schemaFor(type: IrType?, bindings: Map<IrTypeParameterSymbol, Binding>): JsonNode.Obj {
        if (type == null) return obj()
        // A type parameter: resolve it through the bindings of the enclosing instantiation.
        val classifier = (type as? IrSimpleType)?.classifier
        if (classifier is IrTypeParameterSymbol) {
            val binding = bindings[classifier] ?: return obj() // unbound (star projection, raw use)
            val schema = schemaFor(binding.type, binding.context)
            return if (type.isNullableType() && !binding.nullable) nullable(schema) else schema
        }
        val schema = nonNullSchemaFor(type, bindings)
        return if (type.isNullableType()) nullable(schema) else schema
    }

    private fun nonNullSchemaFor(type: IrType, bindings: Map<IrTypeParameterSymbol, Binding>): JsonNode.Obj {
        val cls = type.classOrNull?.owner ?: return obj() // star projections, error types, ...
        val fq = cls.fqNameWhenAvailable?.asString() ?: return obj()

        primitiveSchema(fq)?.let { return it }

        return when {
            fq in ARRAY_FQS -> Schemas.arrayOf(schemaFor(type.typeArgumentOrNull(0), bindings))

            fq in MAP_FQS -> obj(
                "type" to str("object"),
                "additionalProperties" to schemaFor(type.typeArgumentOrNull(1), bindings),
            )

            cls.kind == ClassKind.ENUM_CLASS -> enumSchema(cls)

            cls.isValue -> {
                // Inline (value) classes serialize as their underlying value.
                if (hasCustomSerializer(cls)) {
                    warn("value class '$fq' uses a custom serializer; its schema cannot be derived.")
                    obj()
                } else {
                    val underlying = cls.primaryConstructor?.parameters
                        ?.firstOrNull { it.kind == IrParameterKind.Regular }?.type
                    schemaFor(underlying, bindings)
                }
            }

            else -> namedSchema(cls, type as? IrSimpleType, bindings)
        }
    }

    private fun namedSchema(
        cls: IrClass,
        type: IrSimpleType?,
        bindings: Map<IrTypeParameterSymbol, Binding>,
    ): JsonNode.Obj {
        val fq = cls.fqNameWhenAvailable?.asString() ?: return obj()

        if (!cls.hasAnnotation(KtkitNames.SERIALIZABLE)) {
            warn("type '$fq' is not @Serializable; its schema cannot be derived.")
            return obj()
        }
        // @Serializable(with = CustomSerializer::class) — the wire format is unknown to the plugin.
        if (hasCustomSerializer(cls)) {
            warn("type '$fq' uses a custom serializer; its schema cannot be derived.")
            return obj()
        }

        // Bindings of this instantiation: class type parameter → concrete type argument.
        val ownBindings = cls.typeParameters.mapIndexed { i, tp ->
            tp.symbol to Binding(type?.typeArgumentOrNull(i), bindings)
        }.toMap()

        val argsSuffix = argsSuffix(cls, ownBindings)
        val instantiation = "$fq$argsSuffix"
        keyByInstantiation[instantiation]?.let { return ref(it) }
        val key = uniqueKey(cls, fq, argsSuffix)
        keyByInstantiation[instantiation] = key
        // Reserve the slot before building: keeps deterministic ordering and breaks recursion.
        components[key] = obj()
        val schema = when {
            cls.modality == Modality.SEALED -> sealedSchema(cls, fq)
            cls.kind == ClassKind.OBJECT -> Schemas.objectType()
            else -> objectSchema(cls, ownBindings)
        }
        // Swagger UI's OpenAPI 3.1 renderer displays schema names from the `title` keyword only
        // (the component key behind a $ref is not shown, everything reads as "object" without it).
        val titled = obj("title" to str(key))
        infoOf(cls)?.let { titled["description"] = str(it) }
        schema.entries.forEach { (k, v) -> titled[k] = v }
        components[key] = titled
        return ref(key)
    }

    private fun sealedSchema(cls: IrClass, fq: String): JsonNode.Obj {
        val leaves = sealedLeaves(cls)
        if (leaves.isEmpty()) {
            warn("sealed type '$fq' has no visible subclasses; its schema cannot be derived.")
            return obj()
        }
        val oneOf = JsonNode.Arr()
        val mapping = JsonNode.Obj()
        leaves.forEach { leaf ->
            val leafRef = namedSchema(leaf, leaf.defaultType, emptyMap())
            oneOf.add(leafRef)
            val refPath = (leafRef[$$"$ref"] as? JsonNode.Str)?.value
            val serialName = serialNameOf(leaf) ?: leaf.fqNameWhenAvailable?.asString()
            if (refPath != null && serialName != null) mapping[serialName] = str(refPath)
            // kotlinx adds the "@type" discriminator when a leaf is serialized polymorphically;
            // declare it as an optional property on the leaf component (absent on direct use).
            addDiscriminatorProperty(refPath)
        }
        return obj(
            "oneOf" to oneOf,
            // ktkit configures kotlinx.serialization with classDiscriminator = "@type".
            "discriminator" to obj("propertyName" to str("@type"), "mapping" to mapping),
        )
    }

    /** The concrete (non-sealed) leaves of a sealed hierarchy, nested sealed levels flattened. */
    private fun sealedLeaves(cls: IrClass): List<IrClass> =
        cls.sealedSubclasses.map { it.owner }.flatMap { sub ->
            if (sub.modality == Modality.SEALED) sealedLeaves(sub) else listOf(sub)
        }

    private fun addDiscriminatorProperty(refPath: String?) {
        val key = refPath?.substringAfterLast('/') ?: return
        val component = components[key] ?: return
        if ((component["type"] as? JsonNode.Str)?.value != "object") return
        val properties = component["properties"] as? JsonNode.Obj
            ?: JsonNode.Obj().also { component["properties"] = it }
        if (properties["@type"] == null) properties["@type"] = Schemas.string()
    }

    private fun objectSchema(cls: IrClass, bindings: Map<IrTypeParameterSymbol, Binding>): JsonNode.Obj {
        // The @Serializable superclass chain with the bindings of each level, subclass first.
        val chain = mutableListOf<Pair<IrClass, Map<IrTypeParameterSymbol, Binding>>>()
        var current: IrClass? = cls
        var currentBindings = bindings
        while (current != null) {
            chain += current to currentBindings
            val superType = current.superTypes
                .firstOrNull { it.classOrNull?.owner?.kind == ClassKind.CLASS } as? IrSimpleType
            val superClass = superType?.classOrNull?.owner
            if (superClass == null || superClass.fqNameWhenAvailable?.asString() == "kotlin.Any") break
            if (!superClass.hasAnnotation(KtkitNames.SERIALIZABLE)) break
            val outer = currentBindings
            currentBindings = superClass.typeParameters.mapIndexed { i, tp ->
                tp.symbol to Binding(superType.typeArgumentOrNull(i), outer)
            }.toMap()
            current = superClass
        }

        val properties = JsonNode.Obj()
        val requiredByName = LinkedHashMap<String, Boolean>()
        // kotlinx.serialization writes superclass properties first.
        chain.asReversed().forEach { (c, b) ->
            val ctorParams = c.primaryConstructor?.parameters
                ?.filter { it.kind == IrParameterKind.Regular }
                ?.associate { it.name.asString() to (it.defaultValue != null) }
                ?: emptyMap()

            c.properties.forEach { property ->
                if (property.isFakeOverride) return@forEach // processed at its declaring class
                val backingField = property.backingField ?: return@forEach // getter-only: not serialized
                if (property.isDelegated) return@forEach
                if (property.hasAnnotation(KtkitNames.TRANSIENT) || backingField.hasAnnotation(KtkitNames.TRANSIENT)) return@forEach

                val name = serialNameOf(property) ?: serialNameOf(backingField) ?: property.name.asString()
                val type = backingField.type
                val propertySchema =
                    if (hasCustomSerializer(property) || hasCustomSerializer(backingField)) {
                        warn("property '$name' of '${c.fqNameWhenAvailable}' uses a custom serializer; its schema cannot be derived.")
                        obj()
                    } else {
                        schemaFor(type, b)
                    }
                // A description beside a $ref is valid in OpenAPI 3.1 (JSON Schema 2020-12).
                (infoOf(property) ?: infoOf(backingField))?.let { propertySchema["description"] = str(it) }
                properties[name] = propertySchema

                val propName = property.name.asString()
                val hasDefault = ctorParams[propName] == true ||
                        (propName !in ctorParams && backingField.initializer != null)
                requiredByName[name] = !isNullable(type, b) && !hasDefault
            }
        }

        val out = Schemas.objectType()
        if (properties.isNotEmpty()) out["properties"] = properties
        val required = requiredByName.filterValues { it }.keys
        if (required.isNotEmpty()) out["required"] = arr(required.map { str(it) })
        return out
    }

    /** Nullability of [type], resolving type parameters through [bindings]. */
    private fun isNullable(type: IrType, bindings: Map<IrTypeParameterSymbol, Binding>): Boolean {
        if (type.isNullableType()) return true
        val classifier = (type as? IrSimpleType)?.classifier as? IrTypeParameterSymbol ?: return false
        val binding = bindings[classifier] ?: return false
        return binding.type?.let { isNullable(it, binding.context) } ?: false
    }

    private fun enumSchema(cls: IrClass): JsonNode.Obj {
        val entries = cls.declarations.filterIsInstance<IrEnumEntry>()
            .map { serialNameOf(it) ?: it.name.asString() }
        return obj("type" to str("string"), "enum" to arr(entries.map { str(it) }))
    }

    private fun serialNameOf(annotated: IrAnnotationContainer): String? =
        annotated.annotationArgument(KtkitNames.SERIAL_NAME)?.constString()

    /** The `@OpenApiInfo` description of a class or property, or `null`. */
    private fun infoOf(annotated: IrAnnotationContainer): String? =
        annotated.annotationArgument(KtkitNames.OPEN_API_INFO)?.constString()?.ifEmpty { null }

    /** `@Serializable(with = CustomSerializer::class)` — a present first argument is the custom serializer. */
    private fun hasCustomSerializer(annotated: IrAnnotationContainer): Boolean =
        annotated.annotationArgument(KtkitNames.SERIALIZABLE) != null

    /** Component-key suffix rendering the type arguments of a generic instantiation, e.g. `OfTestDto`. */
    private fun argsSuffix(cls: IrClass, bindings: Map<IrTypeParameterSymbol, Binding>): String {
        if (cls.typeParameters.isEmpty()) return ""
        val names = cls.typeParameters.map { tp ->
            val binding = bindings[tp.symbol]
            argName(binding?.type, binding?.context ?: emptyMap())
        }
        return "Of" + names.joinToString("And")
    }

    private fun argName(type: IrType?, bindings: Map<IrTypeParameterSymbol, Binding>): String {
        if (type == null) return "Any"
        val classifier = (type as? IrSimpleType)?.classifier
        if (classifier is IrTypeParameterSymbol) {
            val binding = bindings[classifier] ?: return "Any"
            return argName(binding.type, binding.context) + (if (type.isNullableType()) "OrNull" else "")
        }
        val base = type.classOrNull?.owner?.name?.asString() ?: "Any"
        val inner = type.simpleArguments.mapNotNull { it.typeOrNull() }.map { argName(it, bindings) }
        val innerSuffix = if (inner.isEmpty()) "" else "Of" + inner.joinToString("And")
        return base + innerSuffix + (if (type.isNullableType()) "OrNull" else "")
    }

    private fun uniqueKey(cls: IrClass, fq: String, argsSuffix: String): String {
        val simple = (cls.classId?.relativeClassName?.asString() ?: cls.name.asString()) + argsSuffix
        if (simple !in components && simple != API_ERROR_KEY) return simple
        var candidate = fq + argsSuffix
        var counter = 2
        while (candidate in components || candidate == API_ERROR_KEY) candidate = "$fq${argsSuffix}_${counter++}"
        return candidate
    }

    private fun ref(key: String): JsonNode.Obj = obj($$"$ref" to str("#/components/schemas/$key"))

    /** Turns a schema into its nullable variant (OpenAPI 3.1 / JSON Schema style). */
    private fun nullable(schema: JsonNode.Obj): JsonNode.Obj {
        if (schema.isEmpty()) return schema // "any" already allows null
        val type = schema["type"]
        return when {
            type is JsonNode.Arr || schema["anyOf"] != null -> schema // already nullable
            type is JsonNode.Str && schema[$$"$ref"] == null -> {
                schema["type"] = arr(type, str("null"))
                // An enum list forbids everything not listed — null must be listed explicitly.
                (schema["enum"] as? JsonNode.Arr)?.add(JsonNode.Null)
                schema
            }

            else -> obj("anyOf" to arr(schema, obj("type" to str("null"))))
        }
    }

    //@formatter:off
    private fun primitiveSchema(fq: String): JsonNode.Obj? = when (fq) {
        "kotlin.String", "kotlin.Char", "kotlin.CharSequence" -> Schemas.string()
        "kotlin.Int", "kotlin.Short", "kotlin.Byte" -> Schemas.int32()
        "kotlin.Long" -> Schemas.int64()
        "kotlin.UInt", "kotlin.UShort", "kotlin.UByte", "kotlin.ULong" -> obj("type" to str("integer"), "minimum" to JsonNode.num(0))
        "kotlin.Float" -> Schemas.float()
        "kotlin.Double" -> Schemas.double()
        "kotlin.Boolean" -> Schemas.boolean()
        "kotlin.time.Instant", "kotlinx.datetime.Instant" -> obj("type" to str("string"), "format" to str("date-time"))
        "kotlin.uuid.Uuid" -> Schemas.uuid()
        "kotlinx.datetime.LocalDate" -> obj("type" to str("string"), "format" to str("date"))
        "kotlinx.datetime.LocalDateTime", "kotlinx.datetime.LocalTime" -> Schemas.string()
        "kotlin.time.Duration" -> Schemas.string()
        "kotlin.ByteArray", "kotlin.ShortArray", "kotlin.IntArray" -> Schemas.arrayOf(Schemas.int32())
        "kotlin.LongArray" -> Schemas.arrayOf(Schemas.int64())
        "kotlin.FloatArray", "kotlin.DoubleArray" -> Schemas.arrayOf(obj("type" to str("number")))
        "kotlin.BooleanArray" -> Schemas.arrayOf(Schemas.boolean())
        // kotlinx serializes CharArray as an array of one-character strings.
        "kotlin.CharArray" -> Schemas.arrayOf(Schemas.string())

        // Free-form values: nothing useful can be said about their schema.
        "kotlin.Any", "kotlin.Unit", "kotlin.Nothing",
        "kotlinx.serialization.json.JsonElement", "kotlinx.serialization.json.JsonObject",
        "kotlinx.serialization.json.JsonArray", "kotlinx.serialization.json.JsonPrimitive",
        "kotlinx.serialization.json.JsonNull" -> obj()
        else -> null
    }
    //@formatter:on

    companion object {
        private const val API_ERROR_KEY = "ApiError"

        private val ARRAY_FQS = setOf(
            "kotlin.collections.List",
            "kotlin.collections.MutableList",
            "kotlin.collections.Set",
            "kotlin.collections.MutableSet",
            "kotlin.collections.Collection",
            "kotlin.collections.MutableCollection",
            "kotlin.collections.Iterable",
            "kotlin.collections.MutableIterable",
            "kotlin.Array",
            "kotlin.sequences.Sequence",
        )

        private val MAP_FQS = setOf("kotlin.collections.Map", "kotlin.collections.MutableMap")
    }
}
