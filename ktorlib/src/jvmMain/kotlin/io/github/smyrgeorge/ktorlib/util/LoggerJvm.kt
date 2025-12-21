package io.github.smyrgeorge.ktorlib.util

import io.github.smyrgeorge.log4k.Level
import io.github.smyrgeorge.log4k.Logger
import org.slf4j.Marker
import io.ktor.util.logging.Logger as KtorLogger

@Suppress("OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE")
class LoggerJvm(name: String) : KtorLogger {
    val log: Logger = Logger.of(name)

    override fun getName(): String = log.name

    override inline fun isTraceEnabled(): Boolean = log.isEnabled(Level.TRACE)
    override inline fun trace(msg: String?) = log.trace(msg.orNull())
    override inline fun trace(format: String?, arg: Any?) = log.trace(format.orNull(), arg)
    override inline fun trace(format: String?, arg1: Any?, arg2: Any?) = log.trace(format.orNull(), arg1, arg2)
    override inline fun trace(format: String?, vararg arguments: Any?) = log.trace(format.orNull(), *arguments)
    override inline fun trace(msg: String?, t: Throwable?) = log.trace(msg.orNull(), t!!)
    override inline fun isTraceEnabled(marker: Marker?): Boolean = isTraceEnabled()
    override inline fun trace(marker: Marker?, msg: String?): Unit = markerNotSupported()
    override inline fun trace(marker: Marker?, format: String?, arg: Any?): Unit = markerNotSupported()
    override inline fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?): Unit = markerNotSupported()
    override inline fun trace(marker: Marker?, format: String?, vararg argArray: Any?): Unit = markerNotSupported()
    override inline fun trace(marker: Marker?, msg: String?, t: Throwable?) = markerNotSupported()

    override inline fun isDebugEnabled(): Boolean = log.isEnabled(Level.DEBUG)
    override inline fun debug(msg: String?) = log.debug(msg.orNull())
    override inline fun debug(format: String?, arg: Any?) = log.debug(format.orNull(), arg)
    override inline fun debug(format: String?, arg1: Any?, arg2: Any?) = log.debug(format.orNull(), arg1, arg2)
    override inline fun debug(format: String?, vararg arguments: Any?) = log.debug(format.orNull(), *arguments)
    override inline fun debug(msg: String?, t: Throwable?) = log.debug(msg.orNull(), t!!)
    override inline fun isDebugEnabled(marker: Marker?): Boolean = isDebugEnabled()
    override inline fun debug(marker: Marker?, msg: String?): Unit = markerNotSupported()
    override inline fun debug(marker: Marker?, format: String?, arg: Any?): Unit = markerNotSupported()
    override inline fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?): Unit = markerNotSupported()
    override inline fun debug(marker: Marker?, format: String?, vararg argArray: Any?): Unit = markerNotSupported()
    override inline fun debug(marker: Marker?, msg: String?, t: Throwable?) = markerNotSupported()

    override inline fun isInfoEnabled(): Boolean = log.isEnabled(Level.INFO)
    override inline fun info(msg: String?) = log.info(msg.orNull())
    override inline fun info(format: String?, arg: Any?) = log.info(format.orNull(), arg)
    override inline fun info(format: String?, arg1: Any?, arg2: Any?) = log.info(format.orNull(), arg1, arg2)
    override inline fun info(format: String?, vararg arguments: Any?) = log.info(format.orNull(), *arguments)
    override inline fun info(msg: String?, t: Throwable?) = log.info(msg.orNull(), t!!)
    override inline fun isInfoEnabled(marker: Marker?): Boolean = isInfoEnabled()
    override inline fun info(marker: Marker?, msg: String?): Unit = markerNotSupported()
    override inline fun info(marker: Marker?, format: String?, arg: Any?): Unit = markerNotSupported()
    override inline fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?): Unit = markerNotSupported()
    override inline fun info(marker: Marker?, format: String?, vararg argArray: Any?): Unit = markerNotSupported()
    override inline fun info(marker: Marker?, msg: String?, t: Throwable?) = markerNotSupported()

    override inline fun isWarnEnabled(): Boolean = log.isEnabled(Level.WARN)
    override inline fun warn(msg: String?) = log.warn(msg.orNull())
    override inline fun warn(format: String?, arg: Any?) = log.warn(format.orNull(), arg)
    override inline fun warn(format: String?, arg1: Any?, arg2: Any?) = log.warn(format.orNull(), arg1, arg2)
    override inline fun warn(format: String?, vararg arguments: Any?) = log.warn(format.orNull(), *arguments)
    override inline fun warn(msg: String?, t: Throwable?) = log.warn(msg.orNull(), t!!)
    override inline fun isWarnEnabled(marker: Marker?): Boolean = isWarnEnabled()
    override inline fun warn(marker: Marker?, msg: String?): Unit = markerNotSupported()
    override inline fun warn(marker: Marker?, format: String?, arg: Any?): Unit = markerNotSupported()
    override inline fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?): Unit = markerNotSupported()
    override inline fun warn(marker: Marker?, format: String?, vararg arguments: Any?): Unit = markerNotSupported()
    override inline fun warn(marker: Marker?, msg: String?, t: Throwable?) = markerNotSupported()

    override inline fun isErrorEnabled(): Boolean = log.isEnabled(Level.ERROR)
    override inline fun error(msg: String?) = log.error(msg.orNull())
    override inline fun error(format: String?, arg: Any?) = log.error(format.orNull(), arg)
    override inline fun error(format: String?, arg1: Any?, arg2: Any?) = log.error(format.orNull(), arg1, arg2)
    override inline fun error(format: String?, vararg arguments: Any?) = log.error(format.orNull(), *arguments)
    override inline fun error(msg: String?, t: Throwable?) = log.error(msg.orNull(), t!!)
    override inline fun isErrorEnabled(marker: Marker?): Boolean = isErrorEnabled()
    override inline fun error(marker: Marker?, msg: String?): Unit = markerNotSupported()
    override inline fun error(marker: Marker?, format: String?, arg: Any?): Unit = markerNotSupported()
    override inline fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?): Unit = markerNotSupported()
    override inline fun error(marker: Marker?, format: String?, vararg arguments: Any?): Unit = markerNotSupported()
    override inline fun error(marker: Marker?, msg: String?, t: Throwable?) = markerNotSupported()

    fun String?.orNull() = this ?: "null"
    fun markerNotSupported() = error("Marker not supported")
}
