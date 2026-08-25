@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.smyrgeorge.ktkit.compiler.openapi.ir

import io.github.smyrgeorge.ktkit.compiler.openapi.utils.KtkitNames
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.makeNullable

/**
 * Synthesizes the `openApiSpec()` override of a handler class: replaces the frontend's fake
 * override with a real function returning the analyzed OpenAPI fragment as a string constant.
 */
object OpenApiSpecSynthesizer {

    /** Whether [irClass] declares its own `openApiSpec()` (same shape: no value parameters). */
    fun hasHandWrittenOverride(irClass: IrClass): Boolean =
        irClass.declarations.any {
            it is IrSimpleFunction && !it.isFakeOverride && it.isOpenApiSpecShape()
        }

    /** The `openApiSpec()` declaration of the AbstractRestHandler ancestor, or `null` on an older runtime. */
    fun baseOpenApiSpecOf(irClass: IrClass): IrSimpleFunction? =
        irClass.findSuperClass(KtkitNames.ABSTRACT_REST_HANDLER)
            ?.declarations
            ?.filterIsInstance<IrSimpleFunction>()
            ?.firstOrNull { it.isOpenApiSpecShape() }

    /** Adds `override fun openApiSpec(): String? = "<fragment>"` to [irClass]. */
    fun addOverride(
        pluginContext: IrPluginContext,
        irClass: IrClass,
        base: IrSimpleFunction,
        fragment: String,
    ) {
        // Drop the frontend's fake override so the class ends up with a single declaration.
        irClass.declarations.removeAll { declaration ->
            declaration is IrSimpleFunction && declaration.isFakeOverride && declaration.isOpenApiSpecShape()
        }
        val fn = irClass.addFunction(
            name = KtkitNames.OPEN_API_SPEC,
            returnType = pluginContext.irBuiltIns.stringType.makeNullable(),
            modality = Modality.OPEN,
            visibility = DescriptorVisibilities.PUBLIC,
        )
        fn.overriddenSymbols = listOf(base.symbol)
        fn.body = DeclarationIrBuilder(pluginContext, fn.symbol).irBlockBody {
            +irReturn(irString(fragment))
        }
    }

    private fun IrSimpleFunction.isOpenApiSpecShape(): Boolean =
        name.asString() == KtkitNames.OPEN_API_SPEC &&
                parameters.all { it.kind == IrParameterKind.DispatchReceiver }
}
