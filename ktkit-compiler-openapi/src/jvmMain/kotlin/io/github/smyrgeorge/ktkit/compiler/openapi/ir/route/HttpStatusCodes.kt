@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package io.github.smyrgeorge.ktkit.compiler.openapi.ir.route

import io.github.smyrgeorge.ktkit.compiler.openapi.ir.calleeName
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.calleeParentClassFq
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.classFq
import io.github.smyrgeorge.ktkit.compiler.openapi.ir.unwrapCasts
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.name.FqName

object HttpStatusCodes {
    private val HTTP_STATUS_CODE = FqName("io.ktor.http.HttpStatusCode")
    private val HTTP_STATUS_CODE_COMPANION = FqName("io.ktor.http.HttpStatusCode.Companion")

    /** Ktor's HttpStatusCode companion properties, mapped to their numeric codes. */
    val BY_NAME: Map<String, Int> = mapOf(
        "Continue" to 100,
        "SwitchingProtocols" to 101,
        "Processing" to 102,
        "OK" to 200,
        "Created" to 201,
        "Accepted" to 202,
        "NonAuthoritativeInformation" to 203,
        "NoContent" to 204,
        "ResetContent" to 205,
        "PartialContent" to 206,
        "MultiStatus" to 207,
        "MultipleChoices" to 300,
        "MovedPermanently" to 301,
        "Found" to 302,
        "SeeOther" to 303,
        "NotModified" to 304,
        "UseProxy" to 305,
        "SwitchProxy" to 306,
        "TemporaryRedirect" to 307,
        "PermanentRedirect" to 308,
        "BadRequest" to 400,
        "Unauthorized" to 401,
        "PaymentRequired" to 402,
        "Forbidden" to 403,
        "NotFound" to 404,
        "MethodNotAllowed" to 405,
        "NotAcceptable" to 406,
        "ProxyAuthenticationRequired" to 407,
        "RequestTimeout" to 408,
        "Conflict" to 409,
        "Gone" to 410,
        "LengthRequired" to 411,
        "PreconditionFailed" to 412,
        "PayloadTooLarge" to 413,
        "RequestURITooLong" to 414,
        "UnsupportedMediaType" to 415,
        "RequestedRangeNotSatisfiable" to 416,
        "ExpectationFailed" to 417,
        "UnprocessableEntity" to 422,
        "Locked" to 423,
        "FailedDependency" to 424,
        "TooEarly" to 425,
        "UpgradeRequired" to 426,
        "TooManyRequests" to 429,
        "RequestHeaderFieldTooLarge" to 431,
        "InternalServerError" to 500,
        "NotImplemented" to 501,
        "BadGateway" to 502,
        "ServiceUnavailable" to 503,
        "GatewayTimeout" to 504,
        "VersionNotSupported" to 505,
        "VariantAlsoNegotiates" to 506,
        "InsufficientStorage" to 507,
    )

    /** Default response descriptions by status code (used when no KDoc override is present). */
    private val PHRASE_BY_CODE: Map<Int, String> = BY_NAME.entries.associate { (name, code) ->
        code to name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
    }

    fun phraseOf(code: Int): String? = PHRASE_BY_CODE[code]

    /**
     * Statically resolves an `HttpStatusCode` expression: a companion property (`HttpStatusCode.Created`)
     * or a constructor call (`HttpStatusCode(422, "...")`). Returns `null` for anything dynamic.
     */
    fun resolve(expression: IrExpression): Int? = when (val e = expression.unwrapCasts()) {
        is IrCall ->
            if (e.calleeParentClassFq() == HTTP_STATUS_CODE_COMPANION) {
                val propertyName = e.symbol.owner.correspondingPropertySymbol?.owner?.name?.asString()
                    ?: e.calleeName().removePrefix("<get-").removeSuffix(">")
                BY_NAME[propertyName]
            } else null

        is IrConstructorCall ->
            if (e.type.classFq() == HTTP_STATUS_CODE) {
                (e.arguments.getOrNull(0) as? IrConst)?.value as? Int
            } else null

        else -> null
    }
}
