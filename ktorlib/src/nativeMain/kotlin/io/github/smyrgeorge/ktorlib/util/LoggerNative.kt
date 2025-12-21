package io.github.smyrgeorge.ktorlib.util

import io.github.smyrgeorge.log4k.Level
import io.github.smyrgeorge.log4k.Logger
import io.ktor.util.logging.LogLevel
import io.ktor.util.logging.Logger as KtorLogger

@Suppress("OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE")
class LoggerNative(name: String) : KtorLogger {
    val log: Logger = Logger.of(name)
    override val level: LogLevel
        get() = when (log.level) {
            Level.OFF -> LogLevel.TRACE
            Level.TRACE -> LogLevel.TRACE
            Level.DEBUG -> LogLevel.DEBUG
            Level.INFO -> LogLevel.INFO
            Level.WARN -> LogLevel.WARN
            Level.ERROR -> LogLevel.ERROR
        }

    override inline fun trace(message: String) = log.trace(message)
    override inline fun trace(message: String, cause: Throwable) = log.trace(message, cause)
    override inline fun debug(message: String) = log.debug(message)
    override inline fun debug(message: String, cause: Throwable) = log.debug(message, cause)
    override inline fun info(message: String) = log.info(message)
    override inline fun info(message: String, cause: Throwable) = log.info(message, cause)
    override inline fun warn(message: String) = log.warn(message)
    override inline fun warn(message: String, cause: Throwable) = log.warn(message, cause)
    override inline fun error(message: String) = log.error(message)
    override inline fun error(message: String, cause: Throwable) = log.error(message, cause)
}
