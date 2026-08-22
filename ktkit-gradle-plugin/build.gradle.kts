import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    id("io.github.smyrgeorge.ktkit.publish")
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
    sourceSets {
        configureEach {
            languageSettings.progressiveMode = true
        }
    }
}

java {
    // Gradle plugins must run on the consumer's Gradle daemon JVM — target the oldest JVM
    // supported by Gradle 9 (17), even though this build itself runs on a newer toolchain.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Provided by the consumer's Kotlin Gradle plugin at runtime — must NOT be bundled.
    compileOnly(libs.kotlin.gradle.plugin.api)

    testImplementation(libs.kotlin.gradle.plugin.api)
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
// ktkit-openapi-compiler-plugin artifact via SubpluginArtifact at consumer build time.
val generatedSourcesDir = layout.buildDirectory.dir("generated/ktkit/kotlin")
val generateBuildConfig = tasks.register("generateBuildConfig") {
    group = "build"
    description = "Generates BuildConfig.kt carrying this module's version for SubpluginArtifact resolution."
    val version = project.version.toString()
    val outputDir = generatedSourcesDir
    inputs.property("version", version)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("io/github/smyrgeorge/ktkit/gradle/BuildConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package io.github.smyrgeorge.ktkit.gradle

            internal object BuildConfig {
                const val VERSION: String = "$version"
            }
            """.trimIndent() + "\n"
        )
    }
}

kotlin.sourceSets.named("main") {
    kotlin.srcDir(generateBuildConfig)
}
