package io.github.smyrgeorge.ktorlib.util

/**
 * Represents an abstract service within the application.
 *
 * This interface extends the capabilities of [AbstractComponent], allowing
 * implementing classes to inherit common utilities including logging, I/O operations,
 * retry mechanisms, and context extraction from the coroutines context.
 *
 * Use this interface to define services that conform to a unified structure with
 * shared functionality at the component level.
 */
interface AbstractService : AbstractComponent
