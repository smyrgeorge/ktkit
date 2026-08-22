package io.github.smyrgeorge.ktkit.openapi.compiler.ir.route

import org.jetbrains.kotlin.ir.types.IrType

/** What a route's handler lambda revealed: parameters, the request body type, and raised error codes. */
class HandlerLambdaScan {
    /** Detected parameters, keyed `"<location>:<name>"`, in detection order. */
    val params = LinkedHashMap<String, ParamInfo>()

    /** The type argument of a `body<T>()` call, when present. */
    var bodyType: IrType? = null

    /** Status codes of ktkit error types constructed directly inside the lambda. */
    val errorCodes = sortedSetOf<Int>()
}
