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
        driver = PostgreSQL
        outputPackage = "io.github.smyrgeorge.ktkit.example.generated"
        extensions(Pgmq)
    }
    jar {
        mainClass = "io.github.smyrgeorge.ktkit.example.ExampleApplicationKt"
    }
}
