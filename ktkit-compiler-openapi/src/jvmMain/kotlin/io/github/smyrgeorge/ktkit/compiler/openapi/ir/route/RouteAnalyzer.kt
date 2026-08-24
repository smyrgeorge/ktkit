@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.smyrgeorge.ktkit.compiler.openapi.ir.route

import io.github.smyrgeorge.ktkit.compiler.openapi.ir.calleeName
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.calleeParentClassFq
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.classFq
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.constString
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.dispatchReceiverExpression
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.regularArgument
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.reportWarning
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.JsonNode
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.SchemaGenerator
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.schema.Schemas
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.simpleArguments
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.typeOrNull
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.unwrapCasts
import io.github.smyrgeorge.ktkit.compiler.openapi.utils.KtkitNames
import io.github.smyrgeorge.ktkit.compiler.openapi.utils.Metadata
import io.github.smyrgeorge.ktkit.compiler.openapi.utils.MetadataStore
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class RouteAnalyzer(
    private val messageCollector: MessageCollector,
    private val store: MetadataStore,
    private val irClass: IrClass,
    private val file: IrFile,
    private val template: UriTemplate?,
    anonymous: Boolean,
    private val schemas: SchemaGenerator,
) {
    /** An analyzed route: the full path, the lower-case HTTP method, and the operation object. */
    class Route(val path: String, val verb: String, val operation: JsonNode.Obj)

    private val operations = OperationBuilder(schemas, anonymous, irClass.name.asString())

    /** The shared error responses referenced by the analyzed operations (component name → status code). */
    val errorResponses: Map<String, Int> get() = operations.usedErrorResponses

    /** Returns the analyzed route, or `null` when the route is skipped (dynamic path, `@OpenApiIgnore`). */
    fun analyze(groupPrefix: String, call: IrCall): Route? {
        val verb = call.calleeName()

        val metadata = metadataOf(call)
        if (metadata.ignore) return null

        val rawPath = call.regularArgument("path")?.constString()
        if (rawPath == null) {
            messageCollector.reportWarning(
                file = file,
                offset = call.startOffset,
                message = "non-constant path for $verb route in ${irClass.name}; route skipped."
            )
            return null
        }

        val fullPath = fullPath(groupPrefix, rawPath)
        val pathParams = pathParams(fullPath)
        val successCode = successCode(verb, fullPath, call)
        val (responseType, streaming) = unwrapResponseType(call)

        // Handler lambda: parameters, request body, raised errors.
        val scan = (call.regularArgument("handler")?.unwrapCasts() as? IrFunctionExpression)
            ?.function?.body?.let { scanHandlerLambda(it) }
            ?: HandlerLambdaScan()

        // pathVariable() names that are not part of the final route path cannot be documented.
        scan.params.values.filter { it.location == "path" && it.name !in pathParams }.forEach { orphan ->
            messageCollector.reportWarning(
                file = file,
                offset = call.startOffset,
                message = "pathVariable(\"${orphan.name}\") is not part of path '$fullPath'; parameter skipped."
            )
            scan.params.remove("path:${orphan.name}")
        }

        return Route(
            path = fullPath,
            verb = verb.lowercase(),
            operation = operations.build(
                verb = verb,
                fullPath = fullPath,
                pathParams = pathParams,
                successCode = successCode,
                responseType = responseType,
                streaming = streaming,
                scan = scan,
                metadata = metadata,
                defaultTag = defaultTag(),
            ),
        )
    }

    /**
     * The metadata of [call] — the `@OpenApi(...)`/`@OpenApiIgnore` annotations collected by the
     * FIR phase ([io.github.smyrgeorge.ktkit.compiler.openapi.fir.MetadataCollector]) — or
     * [Metadata.EMPTY] when the call is not annotated.
     */
    private fun metadataOf(call: IrCall): Metadata {
        val entry = store.get(file.fileEntry.name, call.startOffset, call.endOffset) ?: return Metadata.EMPTY
        entry.warning?.let { messageCollector.reportWarning(file, call.startOffset, it) }
        return entry.metadata
    }

    /**
     * Scans a route's handler lambda for its inputs and errors:
     * - `pathVariable`/`queryParam`/`header` calls (also through local `val`s) and their
     *   `asInt()`/`asBooleanOrNull()`/... conversions — the conversion determines the parameter's
     *   schema, and a non-`OrNull` conversion marks it required.
     * - `queryParams`/`headers` (multi-value) calls — string-array parameters.
     * - `body<T>()` calls — the request body type.
     * - Constructor calls of ktkit's built-in error types — documented as error responses.
     */
    /** A recognized parameter-source call: its location, name and `@OpenApiInfo` description. */
    private class Source(val location: String, val name: String, val info: String?)

    private fun scanHandlerLambda(body: IrBody): HandlerLambdaScan {
        val scan = HandlerLambdaScan()
        val varSources = mutableMapOf<IrValueSymbol, Source>()

        body.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

            override fun visitVariable(declaration: IrVariable) {
                declaration.acceptChildrenVoid(this)
                val initializer = declaration.initializer?.unwrapCasts()
                if (initializer is IrCall) sourceOf(initializer)?.let { varSources[declaration.symbol] = it }
            }

            override fun visitCall(expression: IrCall) {
                expression.acceptChildrenVoid(this)
                handle(expression)
            }

            override fun visitConstructorCall(expression: IrConstructorCall) {
                expression.acceptChildrenVoid(this)
                val fq = expression.type.classFq()?.asString()
                KtkitNames.ERROR_STATUS_BY_FQ[fq]?.let { scan.errorCodes += it }
            }

            private fun handle(call: IrCall) {
                when (call.calleeParentClassFq()) {
                    KtkitNames.HTTP_CONTEXT -> when (call.calleeName()) {
                        "pathVariable", "queryParam", "header" -> sourceOf(call)?.let { record(it, null, null) }
                        "queryParams" -> nameArg(call)?.let {
                            record(Source("query", it, infoOf(call)), Schemas.stringArray(), false)
                        }

                        "headers" -> nameArg(call)?.let {
                            record(Source("header", it, infoOf(call)), Schemas.stringArray(), false)
                        }

                        "body" -> scan.bodyType = call.typeArguments.getOrNull(0) ?: scan.bodyType
                    }

                    KtkitNames.HTTP_CONTEXT_VAR -> {
                        val name = call.calleeName()
                        if (!name.startsWith("as")) return
                        val source = receiverSource(call) ?: return
                        val required = !name.endsWith("OrNull")
                        val schema = conversionSchema(name.removeSuffix("OrNull"), call) ?: return
                        record(source, schema, required)
                    }

                    else -> Unit
                }
            }

            private fun receiverSource(call: IrCall): Source? =
                when (val receiver = call.dispatchReceiverExpression()?.unwrapCasts()) {
                    is IrCall -> sourceOf(receiver)
                    is IrGetValue -> varSources[receiver.symbol]
                    else -> null
                }

            private fun sourceOf(call: IrCall): Source? {
                if (call.calleeParentClassFq() != KtkitNames.HTTP_CONTEXT) return null
                val location = when (call.calleeName()) {
                    "pathVariable" -> "path"
                    "queryParam" -> "query"
                    "header" -> "header"
                    else -> return null
                }
                val name = nameArg(call) ?: return null
                return Source(location, name, infoOf(call))
            }

            /** The `@OpenApiInfo` description collected by the FIR phase for this call, or `null`. */
            private fun infoOf(call: IrCall): String? =
                store.getInfo(file.fileEntry.name, call.startOffset, call.endOffset)

            private fun nameArg(call: IrCall): String? = call.regularArgument("name")?.constString()

            private fun conversionSchema(conversion: String, call: IrCall): JsonNode.Obj? =
                Schemas.fromConversion(conversion)
                // asEnum uses enumValueOf, which matches Kotlin entry names — not @SerialName values.
                    ?: call.takeIf { conversion == "asEnum" }
                        ?.typeArguments?.getOrNull(0)?.classOrNull?.owner
                        ?.takeIf { it.kind == ClassKind.ENUM_CLASS }
                        ?.let { schemas.enumParamSchema(it) }

            private fun record(source: Source, schema: JsonNode.Obj?, required: Boolean?) {
                val key = "${source.location}:${source.name}"
                val existing = scan.params[key]
                if (existing == null) {
                    scan.params[key] = ParamInfo(
                        location = source.location,
                        name = source.name,
                        schema = schema ?: Schemas.string(),
                        required = required ?: false,
                        description = source.info,
                    )
                } else {
                    schema?.let { existing.schema = it }
                    required?.let { existing.required = existing.required || it }
                    source.info?.let { existing.description = it }
                }
            }
        })
        return scan
    }

    /** Enclosing route("...") groups wrap the handler-mounted path (prefix + uri(rawPath)). */
    private fun fullPath(groupPrefix: String, rawPath: String): String {
        var fullPath = groupPrefix + (template?.apply(rawPath) ?: rawPath)
        if (!fullPath.startsWith("/")) fullPath = "/$fullPath"
        // OpenAPI has no optional path parameters — normalize Ktor's `{id?}` to `{id}`.
        return fullPath.replace(Regex("\\{([^}/]+)\\?}"), "{$1}")
    }

    /** The `{param}` names of the final route path (Ktor tailcards excluded). */
    private fun pathParams(fullPath: String): List<String> =
        Regex("\\{([^}/]+)}").findAll(fullPath)
            .map { it.groupValues[1] }
            .filter { !it.startsWith("...") }
            .toList()

    private fun successCode(verb: String, fullPath: String, call: IrCall): Int {
        val defaultStatus = KtkitNames.VERBS.getValue(verb)
        val statusArg = call.regularArgument("onSuccessHttpStatusCode") ?: return defaultStatus
        return HttpStatusCodes.resolve(statusArg) ?: run {
            messageCollector.reportWarning(
                file = file,
                offset = call.startOffset,
                message = "could not resolve onSuccessHttpStatusCode of '$verb $fullPath'; assuming $defaultStatus."
            )
            defaultStatus
        }
    }

    /** The route's response type, unwrapping one Either/Result compatibility layer and Flow streaming. */
    private fun unwrapResponseType(call: IrCall): Pair<IrType?, Boolean> {
        var responseType: IrType? = call.typeArguments.getOrNull(0)
        var streaming = false
        unwrap@ while (true) {
            val current = responseType ?: break@unwrap
            when (current.classFq()?.asString()) {
                "arrow.core.Either" -> responseType = current.simpleArguments.getOrNull(1)?.typeOrNull()
                "kotlin.Result" -> responseType = current.simpleArguments.getOrNull(0)?.typeOrNull()
                "kotlinx.coroutines.flow.Flow" -> {
                    streaming = true
                    responseType = current.simpleArguments.getOrNull(0)?.typeOrNull()
                }

                else -> break@unwrap
            }
        }
        return responseType to streaming
    }

    private fun defaultTag(): String {
        val simple = irClass.name.asString()
        return simple.removeSuffix("RestHandler").removeSuffix("Handler").ifEmpty { simple }
    }
}
