plugins {
    id("io.github.smyrgeorge.ktorlib.multiplatform.binaries")
}

kotlin {
    sourceSets {
        configureEach {
            languageSettings.progressiveMode = true
        }
        commonMain {
            dependencies {
                api(project(":ktorlib"))
            }
        }
    }
}

tasks.named<Jar>("jvmJar") {
    archiveFileName.set("examples.jar")

    manifest {
        attributes(
            "Main-Class" to "io.github.smyrgeorge.ktorlib.examples.MainKt"
        )
    }

    // Include dependencies in your JAR (similar to what Shadow does)
    from(configurations.named("jvmRuntimeClasspath").map { config ->
        config.map { if (it.isDirectory) it else zipTree(it) }
    })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
