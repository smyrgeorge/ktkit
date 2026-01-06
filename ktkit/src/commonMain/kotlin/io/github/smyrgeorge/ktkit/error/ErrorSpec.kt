package io.github.smyrgeorge.ktkit.error

import io.github.smyrgeorge.ktkit.error.system.UnknownError

/**
 * Represents a specification for errors used within the system.
 *
 * This interface provides a structured format to define error-related details,
 * including categorization (`kind`), human-readable message (`message`),
 * and associated HTTP status (`httpStatus`). It also offers utility methods
 * to handle errors as throwable exceptions.
 *
 * @property message A human-readable description of the error.
 * @property httpStatus The HTTP status code associated with this error.
 */
interface ErrorSpec {
    val message: String
    val httpStatus: HttpStatus

    /**
     * Converts this error specification into a detailed representation of type [ErrorSpecData].
     *
     * This method provides a structured way to convert an error, along with its associated message
     * and HTTP status, into a format that implements the [ErrorSpecData] interface, enabling
     * additional handling, display, or serialization.
     *
     * @return An instance of [ErrorSpecData] representing the details of this error.
     */
    fun toErrorSpecData(): ErrorSpecData

    /**
     * Throws this error as an [io.github.smyrgeorge.ktkit.error.RuntimeError] exception.
     *
     * @param throwable Optional cause of the error
     * @throws io.github.smyrgeorge.ktkit.error.RuntimeError Always throws
     */
    fun raise(throwable: Throwable? = null): Nothing =
        throw RuntimeError(this, message, throwable)

    /**
     * Converts this error to an [RuntimeError] throwable without throwing it.
     *
     * @param throwable Optional cause of the error
     * @return InternalError wrapping this error
     */
    fun toThrowable(throwable: Throwable? = null): RuntimeError =
        RuntimeError(this, message, throwable)

    /**
     * HTTP status codes supported by the error system.
     *
     * @property code HTTP status code number
     * @property phrase Human-readable description of the status
     */
    enum class HttpStatus(
        val code: Int,
        val phrase: String
    ) {
        // --- 4xx Client Error ---
        BAD_REQUEST(400, "Bad Request"),
        UNAUTHORIZED(401, "Unauthorized"),
        PAYMENT_REQUIRED(402, "Payment Required"),
        FORBIDDEN(403, "Forbidden"),
        NOT_FOUND(404, "Not Found"),
        METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
        NOT_ACCEPTABLE(406, "Not Acceptable"),
        PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
        REQUEST_TIMEOUT(408, "Request Timeout"),
        CONFLICT(409, "Conflict"),
        GONE(410, "Gone"),
        LENGTH_REQUIRED(411, "Length Required"),
        PRECONDITION_FAILED(412, "Precondition Failed"),
        PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
        URI_TOO_LONG(414, "URI Too Long"),
        UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
        RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable"),
        EXPECTATION_FAILED(417, "Expectation Failed"),
        IM_A_TEAPOT(418, "I'm a teapot"),
        MISDIRECTED_REQUEST(421, "Misdirected Request"),
        UNPROCESSABLE_ENTITY(422, "Unprocessable Entity"),
        LOCKED(423, "Locked"),
        FAILED_DEPENDENCY(424, "Failed Dependency"),
        TOO_EARLY(425, "Too Early"),
        UPGRADE_REQUIRED(426, "Upgrade Required"),
        PRECONDITION_REQUIRED(428, "Precondition Required"),
        TOO_MANY_REQUESTS(429, "Too Many Requests"),
        REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),
        UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons"),

        // --- 5xx Server Error ---
        INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
        NOT_IMPLEMENTED(501, "Not Implemented"),
        BAD_GATEWAY(502, "Bad Gateway"),
        SERVICE_UNAVAILABLE(503, "Service Unavailable"),
        GATEWAY_TIMEOUT(504, "Gateway Timeout"),
        HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported"),
        VARIANT_ALSO_NEGOTIATES(506, "Variant Also Negotiates"),
        INSUFFICIENT_STORAGE(507, "Insufficient Storage"),
        LOOP_DETECTED(508, "Loop Detected"),
        NOT_EXTENDED(510, "Not Extended"),
        NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required"),
    }

    companion object {
        fun fromThrowable(throwable: Throwable): ErrorSpec =
            UnknownError(throwable.message ?: "An unknown error occurred")
    }
}
