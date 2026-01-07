package io.github.smyrgeorge.ktkit.sqlx4k.pgmq

import arrow.core.left
import io.github.smyrgeorge.ktkit.api.auth.impl.UserToken
import io.github.smyrgeorge.ktkit.api.auth.impl.XRealNamePrincipalExtractor
import io.github.smyrgeorge.ktkit.api.auth.impl.XRealNamePrincipalExtractor.toXRealName
import io.github.smyrgeorge.ktkit.api.event.EventContext
import io.github.smyrgeorge.ktkit.context.ExecContext
import io.github.smyrgeorge.ktkit.context.Principal
import io.github.smyrgeorge.ktkit.context.Principal.Companion.cast
import io.github.smyrgeorge.ktkit.error.impl.Unauthorized
import io.github.smyrgeorge.ktkit.service.Component
import io.github.smyrgeorge.ktkit.util.EitherThrowable
import io.github.smyrgeorge.ktkit.util.TRACE_PARENT_HEADER
import io.github.smyrgeorge.ktkit.util.extractOpenTelemetryHeader
import io.github.smyrgeorge.ktkit.util.launch
import io.github.smyrgeorge.ktkit.util.retryCatching
import io.github.smyrgeorge.ktkit.util.toEither
import io.github.smyrgeorge.ktkit.util.toOpenTelemetryHeader
import io.github.smyrgeorge.log4k.Logger
import io.github.smyrgeorge.log4k.Tracer
import io.github.smyrgeorge.log4k.TracingContext
import io.github.smyrgeorge.log4k.TracingContext.Companion.span
import io.github.smyrgeorge.log4k.TracingEvent.Span
import io.github.smyrgeorge.log4k.impl.OpenTelemetryAttributes
import io.github.smyrgeorge.sqlx4k.QueryExecutor
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.Message
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.PgmqClient
import io.github.smyrgeorge.sqlx4k.postgres.pgmq.PgmqConsumer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi

abstract class AbstractPgmqEventHandler(
    private val pgmq: Pgmq,
    private val queue: PgmqClient.Queue,
    private val options: PgmqConsumer.Options = DEFAULT_OPTIONS,
    private val defaultUser: Principal? = null,
) : Component {
    val log: Logger = Logger.of(this::class)
    val trace: Tracer = Tracer.of(this::class)

    private var started: Boolean = false
    private val sendSpanName = "${this::class.simpleName}.send"
    private val handleSpanName = "${this::class.simpleName}.handle"

    private val consumer = PgmqConsumer(
        pgmq = pgmq.client,
        options = options.copy(queue = queue.name, autoStart = false),
        onMessage = ::handle,
        onFailToRead = ::onFailToRead,
        onFailToProcess = ::onFailToProcess,
        onFailToAck = ::onFailToAck,
        onFailToNack = ::onFailToNack,
    )

    private val principal: XRealNamePrincipalExtractor = XRealNamePrincipalExtractor

    init {
        if (options.autoStart) start()
        launch {
            // Silently try to register a shutdown handler.
            retryCatching(times = 8) {
                // Register shutdown handler.
                app.onShutdown { stop() }
            }
        }
    }

    fun start() {
        log.info { "Starting consumer for queue: $queue" }
        // Create the queue if it doesn't exist.
        runBlocking { pgmq.client.create(queue) }.getOrThrow()
        // Start the consumer.
        consumer.start()
        started = true
    }

    fun stop() {
        log.info { "Stopping consumer for queue: $queue" }
        consumer.stop()
        started = false
    }

    private inline fun Message.trace(
        f: TracingContext.(Span.Local) -> Unit
    ) {
        // Extract the parent span from the OpenTelemetry trace header.
        val parent = headers[TRACE_PARENT_HEADER]?.let { h ->
            extractOpenTelemetryHeader(h)?.let { trace.span(it.spanId, it.traceId) }
        }
        // Create the logging-context.
        val tracing = TracingContext.builder().with(trace).with(parent).build()

        // Create the handler span.
        runCatching { tracing.span(handleSpanName, spanTags(app.name)) { tracing.f(this) } }
    }

    private suspend fun handle(
        message: Message
    ) = message.trace { span ->
        // Find the header that contains user information.
        val user = message.headers[XRealNamePrincipalExtractor.HEADER_NAME]
            ?.let { principal.extract(it).getOrNull() } // Convert header to [Principal]
            ?: defaultUser
            ?: Unauthorized("Request does not contain user data.").raise()

        // Add user tags to the span.
        span.tags.apply {
            @OptIn(ExperimentalUuidApi::class)
            put(OpenTelemetryAttributes.USER_ID, user.id)
            put(OpenTelemetryAttributes.USER_NAME, user.username)
        }

        val event = EventContext(user, message.headers)
        val exec = ExecContext.fromEvent(user, event, this)

        val rc = message.readCt
        if (rc > 10) log.warn { "Retry-count '$rc > 10' was reached on queue='${queue.name}'." }

        // Load the execution context into the coroutine context.
        withContext(exec) {
            // Execute the handler.
            context(exec) { event.handler(message) }
        }
    }

    context(ec: ExecContext)
    private fun defaultSendHeaders(): Map<String, String> {
        val span = ec.currentOrNull()
        return listOfNotNull(
            if (span != null) TRACE_PARENT_HEADER to span.toOpenTelemetryHeader() else null,
            XRealNamePrincipalExtractor.HEADER_NAME to ec.principal.cast<UserToken>().toXRealName(),
        ).toMap()
    }

    context(ec: ExecContext, db: QueryExecutor)
    suspend fun send(
        message: String,
        headers: Map<String, String> = emptyMap(),
        delay: Duration = 0.seconds
    ): EitherThrowable<Long> {
        return ec.span(sendSpanName) {
            if (!started) return IllegalStateException("Cannot send message, handler is not started.").left()
            val headers = defaultSendHeaders() + headers
            pgmq.client.send(options.queue, message, headers, delay).toEither()
        }
    }

    context(ec: ExecContext, db: QueryExecutor)
    suspend fun send(
        headers: Map<String, String> = emptyMap(),
        delay: Duration = 0.seconds,
        supplier: () -> String,
    ): EitherThrowable<Long> {
        return ec.span(sendSpanName) {
            if (!started) return IllegalStateException("Cannot send message, handler is not started.").left()
            val headers = defaultSendHeaders() + headers
            pgmq.client.send(options.queue, supplier(), headers, delay).toEither()
        }
    }

    context(_: ExecContext)
    abstract suspend fun EventContext.handler(message: Message)

    open suspend fun onFailToRead(e: Throwable) {
        with(ctx()) { log.warn { "Failed to read from the queue: ${e.message}" } }
    }

    open suspend fun onFailToProcess(e: Throwable) {
        with(ctx()) { log.warn { "Failed to process message: ${e.message}" } }
    }

    open suspend fun onFailToAck(e: Throwable) {
        with(ctx()) { log.warn { "Failed to ack message: ${e.message}" } }
    }

    open suspend fun onFailToNack(e: Throwable) {
        with(ctx()) { log.warn { "Failed to nack message: ${e.message}" } }
    }

    private fun Message.spanTags(serviceName: String): Map<String, String> = mapOf(
        OpenTelemetryAttributes.SERVICE_NAME to serviceName,
        OpenTelemetryAttributes.MESSAGING_SYSTEM to "pgmq",
        OpenTelemetryAttributes.MESSAGING_DESTINATION_NAME to options.queue,
        OpenTelemetryAttributes.MESSAGING_MESSAGE_ID to msgId.toString(),
    )

    companion object {
        private val DEFAULT_OPTIONS = PgmqConsumer.Options(
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
