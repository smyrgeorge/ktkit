package io.github.smyrgeorge.ktkit.compiler.openapi.fir

import io.github.smyrgeorge.ktkit.compiler.openapi.utils.MetadataStore
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall

class OpenApiCheckersExtension(
    session: FirSession,
    store: MetadataStore,
) : FirAdditionalCheckersExtension(session) {

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers: Set<FirExpressionChecker<FirFunctionCall>> = setOf(
            MetadataCollector(store, MppCheckerKind.Common),
            MetadataCollector(store, MppCheckerKind.Platform),
        )
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val propertyCheckers: Set<FirPropertyChecker> = setOf(
            InfoCollector(store, MppCheckerKind.Common),
            InfoCollector(store, MppCheckerKind.Platform),
        )
    }
}
