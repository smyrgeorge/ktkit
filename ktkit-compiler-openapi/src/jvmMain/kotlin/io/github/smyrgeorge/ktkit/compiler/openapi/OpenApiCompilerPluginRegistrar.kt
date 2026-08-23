package io.github.smyrgeorge.ktkit.compiler.openapi

import io.github.smyrgeorge.ktkit.compiler.openapi.fir.OpenApiFirExtensionRegistrar
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.OpenApiIrGenerationExtension
import io.github.smyrgeorge.ktkit.compiler.openapi.utils.MetadataStore
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
class OpenApiCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = "io.github.smyrgeorge.ktkit.openapi"
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val store = MetadataStore()
        FirExtensionRegistrarAdapter.registerExtension(OpenApiFirExtensionRegistrar(store))
        IrGenerationExtension.registerExtension(OpenApiIrGenerationExtension(configuration, store))
    }
}
