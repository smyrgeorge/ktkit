package io.github.smyrgeorge.ktorlib.example.test

import arrow.core.left
import io.github.smyrgeorge.ktorlib.context.ExecutionContext
import io.github.smyrgeorge.ktorlib.service.AbstractDatabaseService
import io.github.smyrgeorge.ktorlib.util.EitherThrowable
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.Transaction

class TestService(
    override val db: Driver,
    private val testRepository: TestRepository
) : AbstractDatabaseService {
    val log = Logger.of(this::class)

    context(_: ExecutionContext, _: Transaction)
    suspend fun findAll(): EitherThrowable<List<Test>> {
        log.info { "Fetching all tests" }
//        test().bind()
//        return either {
//            testRepository.findAll().bind()
//        }
//        test2().bind()

        return testRepository.findAll()
    }

    fun test(): EitherThrowable<Unit> {
        return IllegalStateException("An error occurred").left()
    }

//    fun test2(): Either<String, Unit> {
//        return "test".left()
//    }

    companion object {
//        context(raise: Raise<Throwable>)
//        @RaiseDSL fun <E, A> Either<E, A>.bind(): A {
//            return when (this) {
//                is Either.Left -> raise.raise(RuntimeException(value.toString()))
//                is Either.Right -> value
//            }
//        }
    }
}

