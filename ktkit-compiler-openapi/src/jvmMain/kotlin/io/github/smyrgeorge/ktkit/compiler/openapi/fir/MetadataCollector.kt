package io.github.smyrgeorge.ktkit.compiler.openapi.fir

import io.github.smyrgeorge.ktkit.compiler.openapi.utils.KtkitNames
import io.github.smyrgeorge.ktkit.compiler.openapi.utils.Metadata
import io.github.smyrgeorge.ktkit.compiler.openapi.utils.MetadataStore
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression

class MetadataCollector(
    private val store: MetadataStore,
    kind: MppCheckerKind,
) : FirExpressionChecker<FirFunctionCall>(kind) {

    private class UnsupportedValue(message: String) : Exception(message)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val session = context.session
        val openApi = expression.annotations.getAnnotationByClassId(KtkitNames.OPEN_API_ANNOTATION, session)
        val ignored = expression.annotations
            .getAnnotationByClassId(KtkitNames.OPEN_API_IGNORE_ANNOTATION, session) != null
        if (openApi == null && !ignored) return

        val filePath = context.containingFile?.path ?: return
        val source = expression.source ?: return

        val entry = if (openApi == null) {
            MetadataStore.Entry(Metadata.EMPTY.copy(ignore = true))
        } else {
            try {
                MetadataStore.Entry(evaluate(openApi).copy(ignore = ignored))
            } catch (e: UnsupportedValue) {
                MetadataStore.Entry(
                    metadata = Metadata.EMPTY.copy(ignore = ignored),
                    warning = "could not read the @OpenApi(...) annotation (${e.message}); the annotation is ignored.",
                )
            }
        }
        store.put(filePath, source.startOffset, source.endOffset, entry)
    }

    // --- Annotation evaluation -------------------------------------------------------------

    private fun evaluate(annotation: FirAnnotation): Metadata {
        val args = annotation.namedArguments()
        return Metadata(
            summary = args.str("summary"),
            description = args.str("description"),
            tags = args.strings("tags"),
            deprecated = args.str("deprecated"),
        )
    }

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

    private fun Map<String, FirExpression>.strings(name: String): List<String> =
        this[name]?.elements(name)?.map { element ->
            ((element as? FirLiteralExpression)?.value as? String)
                ?: throw UnsupportedValue("'$name' contains a non-string element")
        } ?: emptyList()

    private fun FirExpression.elements(name: String): List<FirExpression> = when (this) {
        is FirVarargArgumentsExpression -> arguments.map { it.unwrap() }
        is FirCall -> argumentList.arguments.map { it.unwrap() }
        else -> throw UnsupportedValue("'$name' is not an array literal")
    }
}
