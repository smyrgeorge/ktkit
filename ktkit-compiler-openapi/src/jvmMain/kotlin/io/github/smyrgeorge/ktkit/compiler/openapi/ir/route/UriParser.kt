@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.smyrgeorge.ktkit.compiler.openapi.ir.route

import io.github.smyrgeorge.ktkit.compiler.openapi.ir.calleeName
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.extensionReceiverParam
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.unwrapCasts
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/** Statically evaluates a handler's `String.uri()` implementation into a [UriTemplate]. */
object UriParser {

    /**
     * Evaluates a `String.uri()` implementation, supporting string constants, templates and
     * `plus` chains over the extension receiver. Returns `null` for anything dynamic
     * (e.g. a prefix read from configuration).
     */
    fun parse(fn: IrSimpleFunction): UriTemplate? {
        val receiver = fn.extensionReceiverParam() ?: return null
        val expression: IrExpression = when (val body = fn.body) {
            is IrExpressionBody -> body.expression
            is IrBlockBody -> (body.statements.singleOrNull() as? IrReturn)?.value ?: return null
            else -> return null
        }
        val parts = mutableListOf<UriTemplate.Part>()
        fun eval(e0: IrExpression): Boolean {
            when (val e = e0.unwrapCasts()) {
                is IrConst -> parts += UriTemplate.Part.Lit((e.value as? String) ?: return false)
                is IrGetValue -> if (e.symbol == receiver.symbol) parts += UriTemplate.Part.Hole else return false
                is IrStringConcatenation -> e.arguments.forEach { if (!eval(it)) return false }
                is IrReturn -> return eval(e.value)
                is IrCall -> {
                    if (e.calleeName() != "plus") return false
                    val params = e.symbol.owner.parameters
                    val receiverParam = params.firstOrNull { it.kind != IrParameterKind.Regular } ?: return false
                    val argParam = params.firstOrNull { it.kind == IrParameterKind.Regular } ?: return false
                    val left = e.arguments[receiverParam] ?: return false
                    val right = e.arguments[argParam] ?: return false
                    if (!eval(left)) return false
                    if (!eval(right)) return false
                }

                else -> return false
            }
            return true
        }
        return if (eval(expression)) UriTemplate(parts) else null
    }
}
