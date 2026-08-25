package io.github.smyrgeorge.ktkit.compiler.openapi.fir

import io.github.smyrgeorge.ktkit.compiler.openapi.utils.KtkitNames
import io.github.smyrgeorge.ktkit.compiler.openapi.utils.MetadataStore
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

/** Unwraps the argument wrappers (named/spread) the frontend puts around call arguments. */
internal tailrec fun FirExpression.unwrapArgument(): FirExpression =
    if (this is FirWrappedArgumentExpression) expression.unwrapArgument() else this

/** The annotation's explicitly passed arguments by parameter name (defaults are simply absent). */
internal fun FirAnnotation.namedArguments(): Map<String, FirExpression> =
    argumentMapping.mapping.entries.associate { (name, value) -> name.asString() to value.unwrapArgument() }

/** The constant String value of this (possibly wrapped) expression, or `null`. */
internal fun FirExpression.constStringOrNull(): String? =
    (unwrapArgument() as? FirLiteralExpression)?.value as? String

/** The constant `description` argument of an `@OpenApiInfo(...)` annotation, or `null`. */
internal fun FirAnnotation.openApiInfoDescription(): String? =
    namedArguments()["description"]?.constStringOrNull()?.ifEmpty { null }

/** The first `pathVariable`/`queryParam`/... call inside [element] (depth-first), or `null`. */
internal fun findParamSourceCall(element: FirElement): FirFunctionCall? {
    var found: FirFunctionCall? = null
    element.accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (found != null) return
            if (element is FirFunctionCall &&
                element.calleeReference.name.asString() in KtkitNames.PARAM_SOURCES
            ) {
                found = element
                return
            }
            element.acceptChildren(this)
        }
    })
    return found
}

/**
 * Collects the `@OpenApiInfo` description of an annotated element into the store, keyed by the
 * source offsets of the `pathVariable`/`queryParam`/... call found inside [searchRoot] — the way
 * the IR phase looks it up (see [MetadataStore.getInfo]). A no-op when the annotation, its
 * description, the target call, or the source information is missing.
 */
context(context: CheckerContext)
internal fun MetadataStore.collectOpenApiInfo(annotations: List<FirAnnotation>, searchRoot: FirElement?) {
    val annotation = annotations.getAnnotationByClassId(KtkitNames.OPEN_API_INFO_ANNOTATION, context.session) ?: return
    val description = annotation.openApiInfoDescription() ?: return
    val target = searchRoot?.let { findParamSourceCall(it) } ?: return
    val filePath = context.containingFile?.path ?: return
    val source = target.source ?: return
    putInfo(filePath, source.startOffset, source.endOffset, description)
}
