package io.github.smyrgeorge.ktkit.compiler.openapi.fir

import io.github.smyrgeorge.ktkit.compiler.openapi.utils.KtkitNames
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

/** The constant `description` argument of an `@OpenApiInfo(...)` annotation, or `null`. */
internal fun FirAnnotation.openApiInfoDescription(): String? {
    val argument = argumentMapping.mapping.entries
        .firstOrNull { (name, _) -> name.asString() == "description" }?.value ?: return null
    val unwrapped = if (argument is FirWrappedArgumentExpression) argument.expression else argument
    return ((unwrapped as? FirLiteralExpression)?.value as? String)?.ifEmpty { null }
}

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
