package io.github.smyrgeorge.ktorlib.error.system

import io.github.smyrgeorge.ktorlib.error.ErrorSpec
import io.github.smyrgeorge.ktorlib.util.DomainResult
import io.github.smyrgeorge.sqlx4k.arrow.impl.extensions.DbResult
import kotlinx.serialization.Serializable

@Serializable
data class DatabaseError(
    val code: String,
    override val message: String,
    override val httpStatus: ErrorSpec.HttpStatus = ErrorSpec.HttpStatus.INTERNAL_SERVER_ERROR,
) : SystemError {
    companion object {
        fun <T> DbResult<T>.toDomainResult(): DomainResult<T> =
            mapLeft { DatabaseError(it.code.name, it.message ?: "Unknown error") }
    }
}
