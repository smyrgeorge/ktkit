plugins {
    id("io.github.smyrgeorge.ktkit.multiplatform.binaries")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.log4k)
    alias(libs.plugins.ktkit)
}

kotlin {
    sourceSets {
        configureEach {
            languageSettings.progressiveMode = true
        }
        commonMain {
            dependencies {
                implementation(libs.log4k.context)
                implementation(libs.sqlx4k.postgres)
                implementation(project(":ktkit"))
                implementation(project(":ktkit-sqlx4k"))
                implementation(project(":ktkit-sqlx4k-pgmq"))
            }
            // Config if your code is under the commonMain module.
            kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
        }
        jvmMain {
            dependencies {
                implementation(project(":ktkit-sqlx4k-postgres"))
            }
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", libs.sqlx4k.codegen)
}

ksp {
    arg("dialect", "postgresql")
    arg("output-package", "io.github.smyrgeorge.ktkit.example.generated")
}

// Config if your code is under the commonMain module.
tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
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
