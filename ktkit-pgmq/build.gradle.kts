plugins {
    id("io.github.smyrgeorge.ktkit.multiplatform")
//    id("io.github.smyrgeorge.ktkit.publish")
//    id("io.github.smyrgeorge.ktkit.dokka")
}

kotlin {
    sourceSets {
        all {
            languageSettings.enableLanguageFeature("ContextParameters")
        }
        configureEach {
            languageSettings.progressiveMode = true
        }
        commonMain {
            dependencies {
                api(project(":ktkit"))
                api(libs.sqlx4k.postgres.pgmq)
            }
        }
    }
}
