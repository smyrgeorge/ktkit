package io.github.smyrgeorge.ktkit.gradle.sqlx4k

import com.google.devtools.ksp.gradle.KspExtension
import io.github.smyrgeorge.ktkit.gradle.BuildConfig
import io.github.smyrgeorge.ktkit.gradle.KtkitDependencies
import io.github.smyrgeorge.ktkit.gradle.KtkitExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Wires the sqlx4k module into a project when `ktkit { sqlx4k { } }` is used:
 * KSP plugin + sqlx4k code generator on the configured source sets, generated-sources wiring,
 * and (optionally) the ktkit/sqlx4k library dependencies. See [KtkitExtension.sqlx4k].
 */
internal object Sqlx4k {

    private const val GROUP = "io.github.smyrgeorge"
    private const val KSP_PLUGIN_ID = "com.google.devtools.ksp"
    private const val KMP_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"
    private const val COMMON_MAIN = "commonMain"
    private const val METADATA_KSP_TASK = "kspCommonMainKotlinMetadata"
    private const val GENERATED_DIR = "generated/ksp/metadata/commonMain/kotlin"

    /**
     * The KSP configuration processing a source set: `commonMain` is processed by the metadata
     * compilation, `main` is the plain-JVM project case, and any other `<target>Main` source set
     * by the target's compilation. (KSP wires per-target outputs into the compilation itself;
     * only the commonMain output needs the manual source-dir and task-ordering wiring.)
     */
    internal fun kspConfigurationName(sourceSet: String): String = when {
        sourceSet == COMMON_MAIN -> "kspCommonMainMetadata"
        sourceSet == "main" -> "ksp"
        sourceSet.endsWith("Main") ->
            "ksp" + sourceSet.removeSuffix("Main").replaceFirstChar { it.uppercaseChar() }

        else -> error(
            "ktkit { sqlx4k { } }: unsupported source set '$sourceSet' — " +
                    "expected 'commonMain', 'main' (plain JVM) or a '<target>Main' source set."
        )
    }

    fun apply(project: Project, ktkit: KtkitExtension) {
        val options = ktkit.sqlx4k

        // The KSP wiring derives from the source sets, so they must be final by now — i.e.
        // configured in the first sqlx4k { } block. A later re-assignment fails loudly.
        options.sourceSets.finalizeValue()
        val sourceSets = options.sourceSets.get()
        require(sourceSets.isNotEmpty()) { "ktkit { sqlx4k { } }: 'sourceSets' must not be empty." }
        val kspConfigurations = sourceSets.map { kspConfigurationName(it) }.toSet()

        project.pluginManager.apply(KSP_PLUGIN_ID)

        // Register the code generator on each source set's KSP configuration as soon as KSP
        // creates it. Added eagerly (not withDependencies) — KSP decides whether to create the
        // task from the configuration's declared dependencies.
        project.configurations.matching { it.name in kspConfigurations }.configureEach { configuration ->
            configuration.dependencies.add(
                project.dependencies.create("$GROUP:sqlx4k-codegen:${BuildConfig.SQLX4K_VERSION}")
            )
        }

        // The KSP arguments are read after the build script is fully evaluated, so later
        // re-configuration of the options is still picked up.
        project.afterEvaluate {
            require(options.outputPackage.isPresent) {
                "ktkit { sqlx4k { } }: 'outputPackage' must be set."
            }
            require(options.driver.isPresent) {
                "ktkit { sqlx4k { } }: 'driver' must be set (PostgreSQL, MySQL, SQLite, SQLiteCipher)."
            }
            if (Sqlx4kExtension.Pgmq in options.enabledExtensions.get()) {
                require(options.driver.get() == Driver.PostgreSQL) {
                    "ktkit { sqlx4k { } }: the Pgmq extension requires the PostgreSQL driver " +
                            "(PGMQ runs on Postgres) — the configured driver is ${options.driver.get()}."
                }
            }
            // Catch typos and layout mismatches (e.g. 'commonMain' on a plain JVM project, or a
            // target that does not exist) instead of silently generating nothing.
            kspConfigurations.forEach { name ->
                requireNotNull(project.configurations.findByName(name)) {
                    "ktkit { sqlx4k { } }: no KSP configuration '$name' exists in this project — " +
                            "check the 'sourceSets' option (${sourceSets.joinToString()})."
                }
            }
            project.extensions.configure(KspExtension::class.java) { ksp ->
                ksp.arg("dialect", options.driver.get().dialect)
                ksp.arg("output-package", options.outputPackage.get())
                options.extraArgs.forEach { (key, value) -> ksp.arg(key, value) }
            }
        }

        if (COMMON_MAIN in sourceSets) {
            project.pluginManager.withPlugin(KMP_PLUGIN_ID) {
                // The generated sources become part of commonMain, and every other KSP task must
                // wait for the codegen (the per-target KSP tasks also process the generated
                // commonMain sources; their compile tasks are ordered transitively through them).
                project.extensions.configure(KotlinMultiplatformExtension::class.java) { kotlin ->
                    kotlin.sourceSets.named(COMMON_MAIN) { commonMain ->
                        commonMain.kotlin.srcDir(project.layout.buildDirectory.dir(GENERATED_DIR))
                    }
                }
                project.tasks
                    .matching { it.name.startsWith("ksp") && it.name != METADATA_KSP_TASK }
                    .configureEach { it.dependsOn(METADATA_KSP_TASK) }
            }
        }

        project.afterEvaluate {
            if (!ktkit.addDependencies.get()) return@afterEvaluate
            val driver = options.driver.get()
            KtkitDependencies.add(project, "ktkit-sqlx4k", BuildConfig.VERSION)
            KtkitDependencies.add(project, driver.artifact, BuildConfig.SQLX4K_VERSION)
            if (Sqlx4kExtension.Pgmq in options.enabledExtensions.get()) {
                KtkitDependencies.add(project, "ktkit-sqlx4k-pgmq", BuildConfig.VERSION)
            }
            // The r2dbc-based integration (health checks, migrations) is JVM-only.
            if (driver == Driver.PostgreSQL && project.configurations.findByName("jvmMainImplementation") != null) {
                KtkitDependencies.add(project, "ktkit-sqlx4k-postgres", BuildConfig.VERSION, "jvmMainImplementation")
            }
        }
    }
}
