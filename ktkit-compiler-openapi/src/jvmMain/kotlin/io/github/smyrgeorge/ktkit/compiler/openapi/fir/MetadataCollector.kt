package io.github.smyrgeorge.ktkit.compiler.openapi.fir

import io.github.smyrgeorge.ktkit.compiler.openapi.utils.KtkitNames
import io.github.smyrgeorge.ktkit.compiler.openapi.utils.Metadata
import io.github.smyrgeorge.ktkit.compiler.openapi.utils.MetadataStore
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import java.util.concurrent.ConcurrentHashMap

class MetadataCollector(
    private val store: MetadataStore,
    kind: MppCheckerKind,
) : FirExpressionChecker<FirFunctionCall>(kind) {

    private class UnsupportedValue(message: String) : Exception(message)

    /** The source text of each visited file, as the compiler sees it (checkers may run in parallel). */
    private val fileTextCache = ConcurrentHashMap<String, String>()

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val annotation = expression.annotations
            .getAnnotationByClassId(KtkitNames.OPEN_API_ANNOTATION, context.session)
        // Route calls without the annotation are still scanned for a KDoc block.
        if (annotation == null && expression.calleeReference.name.asString() !in KtkitNames.VERBS) return

        val filePath = context.containingFile?.path ?: return
        val source = expression.source ?: return

        val entry = if (annotation != null) {
            try {
                MetadataStore.Entry(evaluate(annotation, context.session))
            } catch (e: UnsupportedValue) {
                MetadataStore.Entry(
                    metadata = kdocMetadata(filePath, source),
                    warning = "could not read the @OpenApi(...) annotation (${e.message}); falling back to the KDoc.",
                )
            }
        } else {
            val metadata = kdocMetadata(filePath, source)
            if (metadata == Metadata.EMPTY) return // nothing to hand over — the IR phase defaults to EMPTY
            MetadataStore.Entry(metadata)
        }
        store.put(filePath, source.startOffset, source.endOffset, entry)
    }

    /** The KDoc metadata of the call at [source], parsed from the compiler's own file text. */
    private fun kdocMetadata(filePath: String, source: KtSourceElement): Metadata {
        val fileText = fileTextCache.getOrPut(filePath) {
            source.treeStructure.toString(source.treeStructure.root).toString()
        }
        return KDocParser.parse(KDocParser.extract(fileText, source.startOffset))
    }

    // --- Annotation evaluation -------------------------------------------------------------

    private fun evaluate(annotation: FirAnnotation, session: FirSession): Metadata {
        val args = annotation.namedArguments()
        return Metadata(
            summary = args.str("summary"),
            description = args.str("description"),
            tags = args.strings("tags"),
            deprecated = args.str("deprecated"),
            operationId = args.str("operationId"),
            ignore = args.bool("ignore"),
            securityNone = args.bool("securityNone"),
            params = args.nested("params", KtkitNames.OPEN_API_PARAM, session).map { toParam(it) },
            responses = args.nested("responses", KtkitNames.OPEN_API_RESPONSE, session).map { toResponse(it) },
            bodyDescription = args.str("body"),
        )
    }

    private fun toParam(args: Map<String, FirExpression>): Metadata.Param {
        val location = (args.str("location") ?: "query").lowercase()
        if (location !in PARAM_LOCATIONS) throw UnsupportedValue("unsupported OpenApiParam location '$location'")
        return Metadata.Param(
            location = location,
            name = args.str("name") ?: throw UnsupportedValue("OpenApiParam requires a name"),
            type = args.str("type"),
            description = args.str("description"),
        )
    }

    private fun toResponse(args: Map<String, FirExpression>): Metadata.Response =
        Metadata.Response(
            code = args.int("code") ?: throw UnsupportedValue("OpenApiResponse requires a code"),
            description = args.str("description") ?: "",
        )

    /** The annotation's explicitly passed arguments by parameter name (defaults are simply absent). */
    private fun FirAnnotation.namedArguments(): Map<String, FirExpression> =
        argumentMapping.mapping.entries.associate { (name, value) -> name.asString() to value.unwrap() }

    private fun FirExpression.unwrap(): FirExpression =
        if (this is FirWrappedArgumentExpression) expression.unwrap() else this

    private fun Map<String, FirExpression>.str(name: String): String? = when (val e = this[name]) {
        null -> null
        is FirLiteralExpression -> (e.value as? String)?.ifEmpty { null }
            ?: throw UnsupportedValue("'$name' is not a string constant")

        else -> throw UnsupportedValue("'$name' is not a constant")
    }

    private fun Map<String, FirExpression>.int(name: String): Int? = when (val e = this[name]) {
        null -> null
        is FirLiteralExpression -> (e.value as? Number)?.toInt()
            ?: throw UnsupportedValue("'$name' is not an integer constant")

        else -> throw UnsupportedValue("'$name' is not a constant")
    }

    private fun Map<String, FirExpression>.bool(name: String): Boolean = when (val e = this[name]) {
        null -> false
        is FirLiteralExpression -> (e.value as? Boolean)
            ?: throw UnsupportedValue("'$name' is not a boolean constant")

        else -> throw UnsupportedValue("'$name' is not a constant")
    }

    private fun Map<String, FirExpression>.strings(name: String): List<String> =
        this[name]?.elements(name)?.map { element ->
            ((element as? FirLiteralExpression)?.value as? String)
                ?: throw UnsupportedValue("'$name' contains a non-string element")
        } ?: emptyList()

    /** The nested `OpenApiParam(...)`/`OpenApiResponse(...)` entries of an array argument. */
    private fun Map<String, FirExpression>.nested(
        name: String,
        expected: String,
        session: FirSession,
    ): List<Map<String, FirExpression>> =
        this[name]?.elements(name)?.map { element ->
            when (element) {
                is FirAnnotation -> {
                    val short = element.toAnnotationClassIdSafe(session)?.shortClassName?.asString()
                    if (short != expected) throw UnsupportedValue("expected $expected(...) inside '$name', found '$short'")
                    element.namedArguments()
                }

                is FirFunctionCall -> {
                    val callee = element.calleeReference.name.asString()
                    if (callee != expected) throw UnsupportedValue("expected $expected(...) inside '$name', found '$callee'")
                    val arguments = element.argumentList as? FirResolvedArgumentList
                        ?: throw UnsupportedValue("unresolved arguments of $expected(...)")
                    arguments.mapping.entries.associate { (expr, param) -> param.name.asString() to expr.unwrap() }
                }

                else -> throw UnsupportedValue("expected $expected(...) inside '$name'")
            }
        } ?: emptyList()

    private fun FirExpression.elements(name: String): List<FirExpression> = when (this) {
        is FirVarargArgumentsExpression -> arguments.map { it.unwrap() }
        is FirCall -> argumentList.arguments.map { it.unwrap() }
        else -> throw UnsupportedValue("'$name' is not an array literal")
    }

    private companion object {
        val PARAM_LOCATIONS = setOf("path", "query", "header")
    }
}
