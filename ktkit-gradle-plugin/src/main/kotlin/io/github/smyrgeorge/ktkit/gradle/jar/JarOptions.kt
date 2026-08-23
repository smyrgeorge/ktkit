package io.github.smyrgeorge.ktkit.gradle.jar

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Property

/** Options of the jar module (`ktkit { jar { } }`). */
public abstract class JarOptions {
    // The DuplicatesStrategy values, exposed directly in the DSL scope: `duplicatesStrategy = EXCLUDE`.
    public val INCLUDE: DuplicatesStrategy = DuplicatesStrategy.INCLUDE
    public val EXCLUDE: DuplicatesStrategy = DuplicatesStrategy.EXCLUDE
    public val WARN: DuplicatesStrategy = DuplicatesStrategy.WARN
    public val FAIL: DuplicatesStrategy = DuplicatesStrategy.FAIL
    public val INHERIT: DuplicatesStrategy = DuplicatesStrategy.INHERIT

    /** The file name of the produced jar. Defaults to `<project-name>.jar`. */
    public abstract val archiveFileName: Property<String>

    /**
     * The fully qualified main class of the application, written as the `Main-Class` manifest
     * attribute (e.g. `com.example.MainKt` for a top-level `main` in `Main.kt`). Required.
     */
    public abstract val mainClass: Property<String>

    /**
     * How duplicate entries (e.g. the same resource contributed by several dependencies) are
     * handled while assembling the jar. Defaults to [DuplicatesStrategy.EXCLUDE] (first wins).
     */
    public abstract val duplicatesStrategy: Property<DuplicatesStrategy>

    init {
        duplicatesStrategy.convention(DuplicatesStrategy.EXCLUDE)
    }
}
