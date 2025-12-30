package io.github.smyrgeorge.ktorlib.api.mq.pgmq

import io.github.smyrgeorge.ktorlib.api.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktorlib.api.mq.EventContext
import io.github.smyrgeorge.ktorlib.context.ExecutionContext
import io.github.smyrgeorge.ktorlib.context.UserToken
import io.github.smyrgeorge.ktorlib.error.types.UnauthorizedImpl
import io.github.smyrgeorge.ktorlib.service.AbstractComponent
import io.github.smyrgeorge.ktorlib.util.TRACE_PARENT_HEADER
import io.github.smyrgeorge.ktorlib.util.extractOpenTelemetryTraceParent
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.Tracer
import io.github.smyrgeorge.log4k.TracingContext
import io.github.smyrgeorge.log4k.TracingContext.Companion.span
import io.github.smyrgeorge.log4k.TracingEvent.Span
import io.github.smyrgeorge.log4k.impl.CoroutinesTracingContext
import io.github.smyrgeorge.log4k.impl.OpenTelemetryAttributes
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
    private val spanName = "${this::class.simpleName}.handle"

    private val consumer = PgMqConsumer(
        pgmq = pgmq.client,
        options = options.copy(queue = queue.name, autoStart = false),
        onMessage = ::handle,
        onFaiToRead = ::onFailToRead,
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

    suspend fun metrics(): Result<Metrics> = pgmq.client.metrics(options.queue)

    private fun Message.spanTags(): Map<String, String> = mapOf(
//        "messaging.message.id" to msgId.toString(),
//        "messaging.destination.name" to options.queue,
        "messaging.system" to "pgmq",
    )

    private inline fun Message.trace(
        f: TracingContext.(Span) -> Unit
    ) {
        // Extract the parent span from the OpenTelemetry trace header.
        val parent = headers[TRACE_PARENT_HEADER]?.let { h ->
            extractOpenTelemetryTraceParent(h)?.let { trace.span(it.spanId, it.traceId) }
        }
        // Create the logging-context.
        val tracing = CoroutinesTracingContext(trace, parent)

        // Create the handler span.
        runCatching { tracing.span(spanName, spanTags()) { tracing.f(this) } }
    }

    private suspend fun handle(
        message: Message
    ) = message.trace { span ->
        // Find the header that contains user information.
        val user = message.headers[userHeaderName]
            ?.let { principal.extract(it) } // Convert header to [UserToken]
            ?: defaultUser
            ?: UnauthorizedImpl("Request does not contain user data.").ex()

        // Add user tags to the span.
        span.tags.apply {
            @OptIn(ExperimentalUuidApi::class)
            put(OpenTelemetryAttributes.USER_ID, user.uuid)
            put(OpenTelemetryAttributes.USER_NAME, user.username)
        }

        val eventContext = EventContext(user, message.headers)
        val executionContext = ExecutionContext.fromEvent(span.context.spanId, user, eventContext, this)

        val rc = message.readCt
        if (rc > 10) log.warn("Retry-count '$rc > 10' was reached on queue='${queue.name}'.")

        // Load the execution context into the coroutine context.
        withContext(executionContext) {
            // Execute the handler.
            context(executionContext) { eventContext.handler(message) }
        }
    }

    context(db: QueryExecutor)
    suspend fun send(
        message: String,
        headers: Map<String, String> = emptyMap(),
        delay: Duration = 0.seconds
    ): Result<Long> = pgmq.client.send(options.queue, message, headers, delay)

    context(db: QueryExecutor)
    suspend fun send(
        headers: Map<String, String> = emptyMap(),
        delay: Duration = 0.seconds,
        supplier: () -> String,
    ): Result<Long> = pgmq.client.send(options.queue, supplier(), headers, delay)

    context(_: ExecutionContext)
    abstract suspend fun EventContext.handler(message: Message)

    open suspend fun onFailToRead(e: Throwable) {
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
            vt = 30.seconds,
            autoStart = false,
            enableNotifyInsert = false,
            queueMinPullDelay = 50.milliseconds,
            queueMaxPullDelay = 2.seconds,
            messageRetryDelayStep = 500.milliseconds,
            messageMaxRetryDelay = 60.seconds,
        )
    }
}
