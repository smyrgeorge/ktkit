import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("io.github.smyrgeorge.ktkit.multiplatform.binaries")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
    }
    sourceSets {
        all {
            languageSettings.enableLanguageFeature("ContextParameters")
        }
        configureEach {
            languageSettings.progressiveMode = true
        }
        commonMain {
            dependencies {
                implementation(libs.sqlx4k.postgres)
                implementation(project(":ktkit"))
                implementation(project(":ktkit-sqlx4k"))
                implementation(project(":ktkit-sqlx4k-pgmq"))
            }
            // Config if your code is under the commonMain module.
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }
    }
}

tasks.named<Jar>("jvmJar") {
    archiveFileName.set("example.jar")

    manifest {
        attributes(
            "Main-Class" to "io.github.smyrgeorge.ktkit.example.ExampleApplicationKt"
        )
    }

    // Include dependencies in your JAR (similar to what Shadow does)
    from(configurations.named("jvmRuntimeClasspath").map { config ->
        config.map { if (it.isDirectory) it else zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

ksp {
    arg("dialect", "postgresql")
    arg("output-package", "io.github.smyrgeorge.ktkit.example.generated")
}

// Config if your code is under the commonMain module.
dependencies {
    add("kspCommonMainMetadata", libs.sqlx4k.codegen)
}

tasks.withType<KotlinCompilationTask<*>> {
    dependsOn("kspCommonMainKotlinMetadata")
}
