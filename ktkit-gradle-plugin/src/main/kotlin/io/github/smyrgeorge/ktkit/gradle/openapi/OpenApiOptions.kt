package io.github.smyrgeorge.ktkit.gradle.openapi

import org.gradle.api.provider.Property

/** Options of the ktkit OpenAPI compiler plugin (`ktkit { openApi { } }`). */
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
