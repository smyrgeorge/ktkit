@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.smyrgeorge.ktkit.compiler.openapi.ir

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.name.FqName

/**
 * All superclasses/superinterfaces of this class (excluding itself), breadth-first, deduplicated.
 * Works across module boundaries (lazy IR classes expose their supertypes).
 */
fun IrClass.allSuperClasses(): List<IrClass> {
    val out = mutableListOf<IrClass>()
    val seen = mutableSetOf<IrClass>()
    var frontier = listOf(this)
    while (frontier.isNotEmpty()) {
        frontier = frontier
            .flatMap { c -> c.superTypes.mapNotNull { it.classOrNull?.owner } }
            .filter { seen.add(it) }
        out += frontier
    }
    return out
}

/** The [fq] ancestor of this class (see [allSuperClasses]), or `null`. */
fun IrClass.findSuperClass(fq: FqName): IrClass? =
    allSuperClasses().firstOrNull { it.fqNameWhenAvailable == fq }

/** Whether [fq] is among this class's ancestors (see [allSuperClasses]). */
fun IrClass.hasSuperClass(fq: FqName): Boolean = findSuperClass(fq) != null

/**
 * The first non-fake-override function matching [predicate], searching this class chain in order.
 * Only functions with a body in the current module can be analyzed, so callers typically require one.
 */
fun List<IrClass>.findDeclaredFunction(predicate: (IrSimpleFunction) -> Boolean): IrSimpleFunction? =
    firstNotNullOfOrNull { cls ->
        cls.declarations.filterIsInstance<IrSimpleFunction>().firstOrNull { !it.isFakeOverride && predicate(it) }
    }

/**
 * The callee resolved through fake overrides to its original declaration. Calls to inherited
 * members reference the fake-override symbol of the dispatch receiver's class, not the declaring
 * class — resolving is required to match callees by their declaring class.
 */
fun IrCall.resolvedCallee(): IrSimpleFunction {
    var fn = symbol.owner
    val seen = mutableSetOf<IrSimpleFunction>()
    while (fn.isFakeOverride && seen.add(fn)) {
        fn = fn.overriddenSymbols.firstOrNull()?.owner ?: break
    }
    return fn
}

/** The class declaring the callee of this call (fake overrides resolved), or `null` for top-level callees. */
fun IrCall.calleeParentClassFq(): FqName? =
    (resolvedCallee().parent as? IrClass)?.fqNameWhenAvailable

fun IrCall.calleeName(): String = symbol.owner.name.asString()

/** The argument passed for the regular value parameter named [name], or `null` when defaulted. */
fun IrCall.regularArgument(name: String): IrExpression? {
    val param = symbol.owner.parameters
        .firstOrNull { it.kind == IrParameterKind.Regular && it.name.asString() == name }
        ?: return null
    return arguments[param]
}

fun IrCall.dispatchReceiverExpression(): IrExpression? {
    val param = symbol.owner.parameters.firstOrNull { it.kind == IrParameterKind.DispatchReceiver } ?: return null
    return arguments[param]
}

fun IrSimpleFunction.extensionReceiverParam(): IrValueParameter? =
    parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }

/** Unwraps implicit casts/coercions inserted by the frontend. */
tailrec fun IrExpression.unwrapCasts(): IrExpression =
    if (this is IrTypeOperatorCall) argument.unwrapCasts() else this

/**
 * The `(receiver, argument)` operands of a binary `plus` call, or `null` for any other call.
 * The two-parameter guard intentionally rejects `plus` overloads with extra parameters.
 */
fun IrCall.plusOperands(): Pair<IrExpression, IrExpression>? {
    if (calleeName() != "plus") return null
    val params = symbol.owner.parameters
    if (params.size != 2) return null
    val receiverParam = params.firstOrNull { it.kind != IrParameterKind.Regular } ?: return null
    val argParam = params.firstOrNull { it.kind == IrParameterKind.Regular } ?: return null
    val receiver = arguments[receiverParam] ?: return null
    val argument = arguments[argParam] ?: return null
    return receiver to argument
}

/**
 * Statically evaluates this expression as a String, supporting constants, string templates of
 * constants and `String.plus` chains. Returns `null` for anything dynamic.
 */
fun IrExpression.constString(): String? = when (val e = unwrapCasts()) {
    is IrConst -> e.value as? String
    is IrStringConcatenation -> {
        val parts = e.arguments.map { it.constString() ?: return null }
        parts.joinToString("")
    }

    is IrCall -> {
        val (receiver, argument) = e.plusOperands() ?: return null
        val left = receiver.constString() ?: return null
        val right = argument.constString() ?: return null
        left + right
    }

    else -> null
}

/**
 * The [index]-th argument of this declaration's [fq] annotation, or `null` — the annotation is
 * absent, or the parameter is defaulted (a defaulted parameter yields a null argument slot).
 */
fun IrAnnotationContainer.annotationArgument(fq: FqName, index: Int = 0): IrExpression? =
    getAnnotation(fq)?.arguments?.getOrNull(index)

val IrType.simpleArguments: List<IrTypeArgument>
    get() = (this as? IrSimpleType)?.arguments ?: emptyList()

fun IrTypeArgument.typeOrNull(): IrType? = (this as? IrTypeProjection)?.type

/** The [index]-th type argument as a type, or `null` (missing argument or star projection). */
fun IrType.typeArgumentOrNull(index: Int): IrType? = simpleArguments.getOrNull(index)?.typeOrNull()

fun IrType.isNullableType(): Boolean =
    (this as? IrSimpleType)?.nullability == SimpleTypeNullability.MARKED_NULLABLE

fun IrType.classFq(): FqName? = classOrNull?.owner?.fqNameWhenAvailable

/** Reports a warning anchored at the given source offset. */
fun MessageCollector.reportWarning(file: IrFile?, offset: Int, message: String) {
    val location = file?.fileEntry?.takeIf { offset >= 0 }?.let { entry ->
        CompilerMessageLocation.create(
            entry.name,
            entry.getLineNumber(offset) + 1,
            entry.getColumnNumber(offset) + 1,
            null
        )
    }
    report(CompilerMessageSeverity.WARNING, "ktkit-openapi: $message", location)
}
