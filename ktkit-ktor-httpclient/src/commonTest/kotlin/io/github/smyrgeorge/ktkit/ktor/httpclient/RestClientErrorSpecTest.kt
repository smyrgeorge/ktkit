package io.github.smyrgeorge.ktkit.ktor.httpclient

import io.github.smyrgeorge.ktkit.api.error.ErrorSpec
import io.github.smyrgeorge.ktkit.api.error.ErrorSpecData
import io.github.smyrgeorge.ktkit.api.error.impl.details.EmptyErrorData
import io.github.smyrgeorge.ktkit.api.error.impl.details.MissingParameterErrorData
import io.github.smyrgeorge.ktkit.api.rest.ApiError
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RestClientErrorSpecTest {
    private fun apiError(status: Int = 409, data: ErrorSpecData? = null) =
        ApiError(
            type = null,
            title = "Conflict",
            status = status,
            detail = "already exists",
            requestId = "req-1",
            data = data,
        )

    /** The reported status is derived from the payload's own [ApiError.status] member. */
    @Test
    fun receiveErrorDerivesItsStatusFromThePayload() {
        val error = RestClientErrorSpec.RestClientReceiveError(apiError(status = 409))
        assertEquals(ErrorSpec.HttpStatus.CONFLICT, error.httpStatus)
        assertEquals(409, error.cause.status)
    }

    /** A payload status the enum does not model degrades to a 500 rather than throwing. */
    @Test
    fun anUnmodelledPayloadStatusFallsBackToInternalServerError() {
        assertEquals(
            ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR,
            RestClientErrorSpec.RestClientReceiveError(apiError(status = 599)).httpStatus,
        )
    }

    @Test
    fun receiveErrorMessageNamesThePayloadTypeAndDetail() {
        val error = RestClientErrorSpec.RestClientReceiveError(apiError())
        assertContains(error.message, "ApiError")
        assertContains(error.message, "already exists")
    }

    /** `data()` surfaces the payload's own data, and stands in [EmptyErrorData] when there is none. */
    @Test
    fun dataFallsBackToEmptyErrorData() {
        assertSame(EmptyErrorData, RestClientErrorSpec.RestClientReceiveError(apiError()).data())
    }

    @Test
    fun dataSurfacesThePayloadData() {
        val data = MissingParameterErrorData(kind = "query", name = "page")
        assertSame(data, RestClientErrorSpec.RestClientReceiveError(apiError(data = data)).data())
    }

    @Test
    fun requestErrorIsA500AndNamesTheCause() {
        val error = RestClientErrorSpec.RestClientRequestError(IllegalStateException("connection refused"))
        assertEquals(ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR, error.httpStatus)
        assertContains(error.message, "IllegalStateException")
        assertContains(error.message, "connection refused")
    }

    @Test
    fun deserializationErrorIsA500AndNamesTheCause() {
        val error = RestClientErrorSpec.RestClientDeserializationError(IllegalArgumentException("bad json"))
        assertEquals(ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR, error.httpStatus)
        assertContains(error.message, "IllegalArgumentException")
        assertContains(error.message, "bad json")
    }

    /**
     * Every member is a [RestClientErrorSpec], so a caller can handle the whole family in one `when`.
     * The interface is not sealed, so this needs an `else`; the branch list is still the proof that
     * all four are reachable through the one type.
     */
    @Test
    fun everyMemberIsReachableThroughTheInterface() {
        val all: List<RestClientErrorSpec> = listOf(
            RestClientErrorSpec.RestClientReceiveError(apiError()),
            RestClientErrorSpec.RestClientRequestError(RuntimeException("x")),
            RestClientErrorSpec.RestClientDeserializationError(RuntimeException("x")),
        )
        assertEquals(
            listOf(409, 500, 500),
            all.map { it.httpStatus.code },
        )
    }
}
