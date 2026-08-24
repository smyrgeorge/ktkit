package io.github.smyrgeorge.ktkit.compiler.openapi.fir

import io.github.smyrgeorge.ktkit.compiler.openapi.utils.KtkitNames
import io.github.smyrgeorge.ktkit.compiler.openapi.utils.MetadataStore
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId

class InfoCollector(
    private val store: MetadataStore,
    kind: MppCheckerKind,
) : FirPropertyChecker(kind) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirProperty) {
        val annotation = declaration.annotations
            .getAnnotationByClassId(KtkitNames.OPEN_API_INFO_ANNOTATION, context.session) ?: return
        val description = annotation.openApiInfoDescription() ?: return
        val target = declaration.initializer?.let { findParamSourceCall(it) } ?: return
        val filePath = context.containingFile?.path ?: return
        val source = target.source ?: return
        store.putInfo(filePath, source.startOffset, source.endOffset, description)
    }
}
