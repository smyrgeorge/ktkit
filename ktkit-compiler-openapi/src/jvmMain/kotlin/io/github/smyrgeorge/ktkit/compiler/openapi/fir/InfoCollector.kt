package io.github.smyrgeorge.ktkit.compiler.openapi.fir

import io.github.smyrgeorge.ktkit.compiler.openapi.utils.MetadataStore
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty

class InfoCollector(
    private val store: MetadataStore,
    kind: MppCheckerKind,
) : FirPropertyChecker(kind) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        store.collectOpenApiInfo(declaration.annotations, declaration.initializer)
    }
}
