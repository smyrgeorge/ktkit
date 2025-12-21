plugins {
    id("io.github.smyrgeorge.ktorlib.multiplatform")
}

kotlin {
    sourceSets {
        configureEach {
            languageSettings.progressiveMode = true
        }
        commonMain {
            dependencies {
                api(project(":ktorlib"))
                api(libs.koin.core)
            }
        }
    }
}
