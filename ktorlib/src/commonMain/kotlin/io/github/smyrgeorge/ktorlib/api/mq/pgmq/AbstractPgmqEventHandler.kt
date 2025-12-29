package io.github.smyrgeorge.ktorlib.api.mq.pgmq

import arrow.core.Either
import io.github.smyrgeorge.ktorlib.api.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktorlib.context.ExecutionContext
import io.github.smyrgeorge.ktorlib.context.UserToken
import io.github.smyrgeorge.ktorlib.error.Error
import io.github.smyrgeorge.ktorlib.error.InternalError
import io.github.smyrgeorge.ktorlib.error.types.UnauthorizedImpl
import io.github.smyrgeorge.ktorlib.service.AbstractComponent
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.Tracer
import io.github.smyrgeorge.log4k.TracingContext
import io.github.smyrgeorge.log4k.TracingEvent.Span
import io.github.smyrgeorge.log4k.impl.OpenTelemetry
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.Message
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.Metrics
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.PgMqClient
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.PgMqConsumer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi

abstract class AbstractPgmqEventHandler(
    private val pgmq: PgMq,
    private val queue: PgMqClient.Queue,
    private val options: PgMqConsumer.Options = DEFAULT_OPTIONS,
    private val userHeaderName: String = XRealNamePrincipalExtractor.HEADER_NAME,
    private val defaultUser: UserToken? = null,
) : AbstractComponent {
    val log = Logger.of(this::class)
    val trace: Tracer = Tracer.of(this::class)

    private val consumer = PgMqConsumer(
        pgmq = pgmq.client,
        options = options.copy(queue = queue.name, autoStart = false),
        onMessage = ::handle,
        onFaiToRead = ::onFaiToRead,
        onFailToProcess = ::onFailToProcess,
        onFaiToAck = ::onFailToAck,
        onFaiToNack = ::onFailToNack,
    )

    private val principal: XRealNamePrincipalExtractor = XRealNamePrincipalExtractor()

    init {
        if (options.autoStart) start()
    }

    fun start() {
        log.info { "Starting consumer for queue: $queue" }
        // Create the queue if it doesn't exist.
        runBlocking { pgmq.client.create(queue) }.getOrThrow()
        // Start the consumer.
        consumer.start()
    }

    fun stop(): Unit = consumer.stop()

    context(db: QueryExecutor)
    suspend fun send(
        message: String,
        headers: Map<String, String> = emptyMap(),
        delay: Duration = 0.seconds
    ): Result<Long> = pgmq.client.send(options.queue, message, headers, delay)

    context(db: QueryExecutor)
    suspend fun send(
        messages: List<String>,
        headers: Map<String, String> = emptyMap(),
        delay: Duration = 0.seconds
    ): Result<List<Long>> = pgmq.client.send(options.queue, messages, headers, delay)

    suspend fun metrics(): Result<Metrics> = pgmq.client.metrics(options.queue)

    private inline fun handleWithTracing(
        f: TracingContext.(Span) -> Unit
    ) {
        // Create the logging-context.
        val tracing = TracingContext.builder()
//            .with(parent) // TODO: create the remote span.
            .with(trace)
            .build()

        // Create the handler span.
        runCatching { tracing.span("CHANGE", emptyMap()) { tracing.f(this) } }
    }

    private suspend fun handle(
        message: Message
    ) = handleWithTracing { span ->
        // Find the header that contains user information.
        val user = message.headers[userHeaderName]
            ?.let { principal.extract(it) } // Convert header to [UserToken]
            ?: defaultUser
            ?: UnauthorizedImpl("Request does not contain user data.").ex()

        val eventContext = EventContext(user)
        val executionContext = ExecutionContext.fromEvent(span.context.spanId, user, eventContext, this)

        // Add user tags to the span.
        span.tags.apply {
            @OptIn(ExperimentalUuidApi::class)
            put(OpenTelemetry.USER_ID, user.uuid)
            put(OpenTelemetry.USER_NAME, user.username)
        }

        // Load the execution context into the coroutine context.
        val result: Any = withContext(executionContext) {
            // Execute the handler.
            context(executionContext) { eventContext.handler(message) }
        }

        when (result) {
            is Result<*> if result.isFailure -> throw result.exceptionOrNull()!!
            is Either<*, *> if result.isLeft() -> {
                when (val error = result.leftOrNull() ?: throw IllegalStateException("Unexpected null error")) {
                    is Error -> throw error.toThrowable()
                    is InternalError -> throw error
                    else -> throw IllegalStateException("Unexpected error type: $error")
                }
            }
        }
    }

    context(_: ExecutionContext, _: TracingContext)
    abstract suspend fun <T : Any> EventContext.handler(message: Message): T

    open suspend fun onFaiToRead(e: Throwable) {
        log.warn { "Failed to read from the queue: ${e.message}" }
    }

    open suspend fun onFailToProcess(e: Throwable) {
        log.warn { "Failed to process message: ${e.message}" }
    }

    open suspend fun onFailToAck(e: Throwable) {
        log.warn { "Failed to ack message: ${e.message}" }
    }

    open suspend fun onFailToNack(e: Throwable) {
        log.warn { "Failed to nack message: ${e.message}" }
    }

    companion object {
        private val DEFAULT_OPTIONS = PgMqConsumer.Options(
            queue = "DEFAULT",
            prefetch = 250,
            vt = 5.seconds,
            autoStart = false,
            enableNotifyInsert = false,
            queueMinPullDelay = 50.milliseconds,
            queueMaxPullDelay = 2.seconds,
            messageRetryDelayStep = 500.milliseconds,
            messageMaxRetryDelay = 60.seconds,
        )
    }
}