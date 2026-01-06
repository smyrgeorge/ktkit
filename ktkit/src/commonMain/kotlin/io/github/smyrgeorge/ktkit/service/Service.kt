package io.github.smyrgeorge.ktkit.service

/**
 * Represents a service abstraction that extends the functionality of [Component].
 *
 * Provides a contract for implementing services that inherit common utilities such as logging,
 * IO dispatcher operations, retry mechanisms, and context handling from [Component].
 *
 * Implementers of this interface are expected to define specific service-related functionalities
 * while leveraging the shared capabilities provided by the parent component abstraction.
 */
interface Service : Component
