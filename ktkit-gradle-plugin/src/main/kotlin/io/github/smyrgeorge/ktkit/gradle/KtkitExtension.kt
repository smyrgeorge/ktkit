package io.github.smyrgeorge.ktkit.gradle

import io.github.smyrgeorge.ktkit.gradle.jar.Jar
import io.github.smyrgeorge.ktkit.gradle.jar.JarOptions
import io.github.smyrgeorge.ktkit.gradle.openapi.OpenApiOptions
import io.github.smyrgeorge.ktkit.gradle.sqlx4k.Sqlx4k
import io.github.smyrgeorge.ktkit.gradle.sqlx4k.Sqlx4kOptions
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * The `ktkit { }` build-script extension — the single place to configure a ktkit service:
 * the ktkit compiler plugins and the optional ktkit modules/integrations.
 *
 * ```kotlin
 * ktkit {
 *     openApi {
 *         enabled = true // default
 *     }
 *     sqlx4k {
 *         driver = PostgreSQL
 *         generatedCodePackage = "com.example.generated"
 *     }
 * }
 * ```
 *
 * Enabling a module wires everything it needs: required Gradle plugins, code generation, and
 * (unless [addDependencies] is disabled) the ktkit library dependencies at the matching versions.
 */
public abstract class KtkitExtension @Inject constructor(
    private val project: Project,
    objects: ObjectFactory,
) {

    /**
     * Whether enabling a module also adds its ktkit library dependencies (e.g. `ktkit-sqlx4k`
     * and the sqlx4k driver for [Sqlx4kOptions]) at the versions this plugin was built with.
     * Disable to manage the dependencies (and their versions) yourself. Defaults to true.
     */
    public abstract val addDependencies: Property<Boolean>

    init {
        addDependencies.convention(true)
    }

    /** Configuration of the ktkit OpenAPI compiler plugin. */
    public val openApi: OpenApiOptions = objects.newInstance(OpenApiOptions::class.java)

    /** Configures the ktkit OpenAPI compiler plugin. */
    public fun openApi(action: Action<OpenApiOptions>) {
        action.execute(openApi)
    }

    /** Configuration of the sqlx4k module ([sqlx4k] must be called for it to take effect). */
    public val sqlx4k: Sqlx4kOptions = objects.newInstance(Sqlx4kOptions::class.java)

    private var sqlx4kEnabled = false

    /**
     * Enables and configures the sqlx4k module (database access via sqlx4k with compile-time
     * query validation):
     * - applies the KSP Gradle plugin and registers the sqlx4k code generator for commonMain,
     * - passes the [Sqlx4kOptions.driver] / [Sqlx4kOptions.generatedCodePackage] KSP arguments,
     * - adds the generated sources to commonMain and orders the KSP tasks accordingly,
     * - unless [addDependencies] is false, adds `ktkit-sqlx4k` and the sqlx4k driver.
     */
    public fun sqlx4k(action: Action<Sqlx4kOptions>) {
        action.execute(sqlx4k)
        if (!sqlx4kEnabled) {
            sqlx4kEnabled = true
            Sqlx4k.apply(project, this)
        }
    }

    /** Configuration of the jar module ([jar] must be called for it to take effect). */
    public val jar: JarOptions = objects.newInstance(JarOptions::class.java)

    private var jarEnabled = false

    /**
     * Configures the `jvmJar` task as a runnable, self-contained ("fat") jar: the runtime
     * dependencies are bundled in and [JarOptions.mainClass] becomes the `Main-Class` manifest
     * attribute. Requires the Kotlin multiplatform jvm() target.
     */
    public fun jar(action: Action<JarOptions>) {
        action.execute(jar)
        if (!jarEnabled) {
            jarEnabled = true
            Jar.apply(project, this)
        }
    }
}
