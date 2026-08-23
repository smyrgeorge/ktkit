plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ktkit)
}

kotlin {
    jvm()
    macosArm64 { binaries { executable() } }
    linuxArm64 { binaries { executable() } }
    linuxX64 { binaries { executable() } }
    mingwX64 { binaries { executable() } }
}

ktkit {
    sqlx4k {
        extensions(Pgmq)
        driver = PostgreSQL
        generatedCodePackage = "io.github.smyrgeorge.ktkit.example.generated"
    }
    jar {
        mainClass = "io.github.smyrgeorge.ktkit.example.ExampleApplicationKt"
    }
}
