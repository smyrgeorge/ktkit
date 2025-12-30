plugins {
    id("io.github.smyrgeorge.ktorlib.multiplatform")
//    id("io.github.smyrgeorge.ktorlib.publish")
//    id("io.github.smyrgeorge.ktorlib.dokka")
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
                api(project(":ktorlib"))
                api(libs.sqlx4k.postgres.pgmq)
            }
        }
    }
}
