package io.github.smyrgeorge.ktkit.openapi.compiler.ir

import io.github.smyrgeorge.ktkit.openapi.compiler.utils.KtkitNames
import io.github.smyrgeorge.ktkit.openapi.compiler.utils.MetadataStore
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Analyzes every concrete `AbstractRestHandler` subclass of the module (via [HandlerAnalyzer]) and
 * generates its `openApiSpec()` override (via [OpenApiSpecSynthesizer]), returning the handler's
 * OpenAPI fragment as a string constant.
 *
 * The extension is a no-op for modules that don't contain REST handlers (or don't have the ktkit
 * runtime on their compile classpath), so it is safe to attach to every Kotlin compilation.
 */
class OpenApiIrGenerationExtension(
    private val configuration: CompilerConfiguration,
    private val store: MetadataStore,
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val messageCollector = configuration[CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE]
        val analyzer = HandlerAnalyzer(messageCollector, store)

        // Analyze first, mutate after: never change declaration lists mid-traversal.
        val generated = mutableListOf<Triple<IrClass, IrSimpleFunction, String>>()
        moduleFragment.files.forEach { file ->
            collectClasses(file).forEach { irClass ->
                if (!irClass.isConcreteRestHandler()) return@forEach
                if (irClass.isOpenApiIgnored()) return@forEach
                if (OpenApiSpecSynthesizer.hasHandWrittenOverride(irClass)) return@forEach
                // An older ktkit runtime without openApiSpec(): nothing to override — no-op.
                val base = OpenApiSpecSynthesizer.baseOpenApiSpecOf(irClass) ?: return@forEach
                val fragment = analyzer.analyze(irClass, file) ?: return@forEach
                generated += Triple(irClass, base, fragment)
            }
        }
        generated.forEach { (irClass, base, fragment) ->
            OpenApiSpecSynthesizer.addOverride(pluginContext, irClass, base, fragment)
        }
    }

    private fun collectClasses(file: IrFile): List<IrClass> {
        val classes = mutableListOf<IrClass>()
        file.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)
            override fun visitClass(declaration: IrClass) {
                classes += declaration
                declaration.acceptChildrenVoid(this)
            }
        })
        return classes
    }

    private fun IrClass.isConcreteRestHandler(): Boolean =
        (kind == ClassKind.CLASS || kind == ClassKind.OBJECT) &&
                modality != Modality.ABSTRACT &&
                modality != Modality.SEALED &&
                allSuperClasses().any { it.fqNameWhenAvailable == KtkitNames.ABSTRACT_REST_HANDLER }

    private fun IrClass.isOpenApiIgnored(): Boolean =
        hasAnnotation(KtkitNames.OPEN_API_IGNORE) ||
                allSuperClasses().any { it.hasAnnotation(KtkitNames.OPEN_API_IGNORE) }
}
