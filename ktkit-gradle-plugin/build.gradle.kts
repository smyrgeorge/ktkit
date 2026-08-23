@file:Suppress("AvoidDuplicateDependencies")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    id("io.github.smyrgeorge.ktkit.publish")
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
    sourceSets {
        configureEach {
            languageSettings.progressiveMode = true
        }
    }
}

java {
    // Gradle plugins must run on the consumer's Gradle daemon JVM. The bundled log4k Gradle
    // plugin is built for Java 21, so that is the effective minimum of the ktkit stack anyway.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    // Provided by the consumer's Kotlin Gradle plugin at runtime — must NOT be bundled.
    compileOnly(libs.kotlin.gradle.plugin.api)
    compileOnly(libs.gradle.kotlin.plugin)

    // Plugins this plugin applies on the consumer's behalf — real dependencies (not compileOnly),
    // so applying `io.github.smyrgeorge.ktkit` puts them on the consumer's buildscript classpath.
    implementation(libs.gradle.kotlin.serialization.plugin)
    implementation(libs.gradle.ksp.plugin)
    implementation(libs.gradle.log4k.plugin)

    testImplementation(libs.kotlin.gradle.plugin.api)
    testImplementation(libs.gradle.kotlin.plugin)
    testImplementation(libs.kotlin.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("ktkit") {
            id = "io.github.smyrgeorge.ktkit"
            implementationClass = "io.github.smyrgeorge.ktkit.gradle.KtkitGradlePlugin"
            displayName = "ktkit Gradle plugin"
            description =
                "Wires the ktkit OpenAPI Kotlin compiler plugin (compile-time OpenAPI specification generation for REST handlers) onto every Kotlin compilation."
        }
    }
}

// Generate a BuildConfig carrying this module's version, so the plugin can request the matching
// ktkit-compiler-openapi artifact via SubpluginArtifact at consumer build time.
val generatedSourcesDir = layout.buildDirectory.dir("generated/ktkit/kotlin")
val generateBuildConfig = tasks.register("generateBuildConfig") {
    group = "build"
    description = "Generates BuildConfig.kt carrying this module's version for SubpluginArtifact resolution."
    val version = project.version.toString()
    val sqlx4kVersion = libs.versions.sqlx4k.get()
    val log4kVersion = libs.versions.log4k.get()
    val outputDir = generatedSourcesDir
    inputs.property("version", version)
    inputs.property("sqlx4kVersion", sqlx4kVersion)
    inputs.property("log4kVersion", log4kVersion)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("io/github/smyrgeorge/ktkit/gradle/BuildConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package io.github.smyrgeorge.ktkit.gradle

            internal object BuildConfig {
                const val VERSION: String = "$version"
                const val SQLX4K_VERSION: String = "$sqlx4kVersion"
                const val LOG4K_VERSION: String = "$log4kVersion"
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin.sourceSets.named("main") {
    kotlin.srcDir(generateBuildConfig)
}
