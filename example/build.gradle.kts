import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("io.github.smyrgeorge.ktkit.multiplatform.binaries")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalContextParameters")
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
                implementation(project(":ktkit"))
                implementation(libs.arrow.core)
                implementation(libs.sqlx4k.arrow)
                implementation(libs.sqlx4k.postgres)
            }
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }
    }
}

tasks.named<Jar>("jvmJar") {
    archiveFileName.set("example.jar")

    manifest {
        attributes(
            "Main-Class" to "io.github.smyrgeorge.ktkit.example.MainKt"
        )
    }

    // Include dependencies in your JAR (similar to what Shadow does)
    from(configurations.named("jvmRuntimeClasspath").map { config ->
        config.map { if (it.isDirectory) it else zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

ksp {
    arg("output-package", "io.github.smyrgeorge.ktkit.example.generated")
}

//dependencies {
//    add("kspJvm", libs.sqlx4k.codegen)
//    add("kspMacosArm64", libs.sqlx4k.codegen)
//}

dependencies {
    add("kspCommonMainMetadata", libs.sqlx4k.codegen)
}

targetsOf(project).forEach {
    project.tasks.getByName("compileKotlin$it") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

tasks.withType<KotlinCompilationTask<*>> {
    dependsOn("kspCommonMainKotlinMetadata")
}

fun targetsOf(project: Project): List<String> {
    val os = DefaultNativePlatform.getCurrentOperatingSystem()
    val arch = DefaultNativePlatform.getCurrentArchitecture()

    val osString = when {
        os.isLinux -> "Linux"
        os.isMacOsX -> "Macos"
        os.isWindows -> "Mingw"
        else -> throw GradleException("Unsupported operating system: $os")
    }
    val archString = when {
        arch.isArm64 -> "Arm64"
        arch.isAmd64 -> "X64"
        else -> throw GradleException("Unsupported architecture: $arch")
    }
    val defaultTarget = "$osString$archString"
    return (project.properties["targets"] as? String)?.let {
        when (it) {
            "all" -> listOf(
                "MacosArm64",
                "MacosX64",
                "LinuxArm64",
                "LinuxX64",
                "MingwX64"
            )

            else -> it.split(",").map { t -> t.trim().capitalized() }
        }
    } ?: listOf(defaultTarget) // Default for local development.
}
