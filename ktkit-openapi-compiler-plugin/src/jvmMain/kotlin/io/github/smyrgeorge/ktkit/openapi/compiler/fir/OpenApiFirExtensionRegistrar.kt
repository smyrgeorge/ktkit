package io.github.smyrgeorge.ktkit.openapi.compiler.fir

import io.github.smyrgeorge.ktkit.openapi.compiler.utils.MetadataStore
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class OpenApiFirExtensionRegistrar(
    private val store: MetadataStore,
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirAdditionalCheckersExtension.Factory { session -> OpenApiCheckersExtension(session, store) }
    }
}
