package io.github.smyrgeorge.ktkit.gradle

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * The `ktkit { }` build-script extension: one nested block per ktkit compiler plugin.
 *
 * ```kotlin
 * ktkit {
 *     openApi {
 *         enabled = true // default
 *     }
 * }
 * ```
 */
public abstract class KtkitExtension @Inject constructor(objects: ObjectFactory) {

    /** Configuration of the ktkit OpenAPI compiler plugin. */
    public val openApi: OpenApiOptions = objects.newInstance(OpenApiOptions::class.java)

    /** Configures the ktkit OpenAPI compiler plugin. */
    public fun openApi(action: Action<OpenApiOptions>) {
        action.execute(openApi)
    }

    /** Options of the ktkit OpenAPI compiler plugin. */
    public abstract class OpenApiOptions {
        /**
         * Whether to attach the OpenAPI compiler plugin to the project's Kotlin compilations
         * (generating the `openApiSpec()` overrides of the REST handlers). Defaults to true.
         */
        public abstract val enabled: Property<Boolean>

        init {
            enabled.convention(true)
        }
    }
}
