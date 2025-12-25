package io.github.smyrgeorge.ktorlib.service

import io.github.smyrgeorge.ktorlib.util.AbstractComponent

/**
 * Represents an abstract service interface providing essential utilities to be implemented by specific services.
 *
 * This interface extends [AbstractComponent] to inherit common capabilities such as logging,
 * retry mechanisms, IO dispatcher support, and context extraction. It is designed to serve
 * as the foundation for service-level abstractions.
 *
 * Features include:
 * - Extension of [AbstractComponent] for shared component functionality.
 * - Support for structured `with` functions for executing code with contextual inputs.
 *
 * Companion Object:
 * - [with]: Inline and suspendable utility functions allowing scoped execution with varying numbers of contextual inputs (A, B, C).
 */
interface AbstractService : AbstractComponent {
    companion object {
        suspend inline fun <A, R> with(a: A, f: suspend context(A)() -> R): R = f(a)
        suspend inline fun <A, B, R> with(a: A, b: B, f: suspend context(A, B)() -> R): R = f(a, b)
        suspend inline fun <A, B, C, R> with(a: A, b: B, c: C, f: suspend context(A, B, C)() -> R): R = f(a, b, c)
    }
}