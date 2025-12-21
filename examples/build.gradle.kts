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
                api("io.insert-koin:koin-core:3.5.3")
            }
        }
    }
}
