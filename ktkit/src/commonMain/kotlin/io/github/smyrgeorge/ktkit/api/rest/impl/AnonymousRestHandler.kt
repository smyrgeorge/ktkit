package io.github.smyrgeorge.ktkit.api.rest.impl

import io.github.smyrgeorge.ktkit.api.rest.AbstractRestHandler
import io.github.smyrgeorge.ktkit.util.ANONYMOUS_USER

/**
 * An abstract class representing a REST handler that operates with anonymous user context.
 * This class extends `AbstractRestHandler` and is pre-configured to use an anonymous user
 * as the default user. It serves as a base class for creating REST endpoints where
 * authenticated users are not required or where an anonymous context is enough.
 *
 * Classes inheriting from `AnonymousRestHandler` can implement specific behavior
 * for handling requests in anonymous contexts.
 */
abstract class AnonymousRestHandler: AbstractRestHandler(defaultUser = ANONYMOUS_USER)
