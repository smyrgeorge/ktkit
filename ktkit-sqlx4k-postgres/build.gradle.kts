plugins {
    id("io.github.smyrgeorge.ktkit.multiplatform.jvm")
    id("io.github.smyrgeorge.ktkit.dokka")
    id("io.github.smyrgeorge.ktkit.publish")
}

kotlin {
    sourceSets {
        all {
            languageSettings.enableLanguageFeature("ContextParameters")
        }
        configureEach {
            languageSettings.progressiveMode = true
        }
        jvmMain {
            dependencies {
                api(project(":ktkit"))
                api(project(":ktkit-sqlx4k"))
                api(libs.sqlx4k.arrow)
                api(libs.sqlx4k.postgres)
                implementation(libs.r2dbc.postgresql)
            }
        }
    }
}
