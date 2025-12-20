plugins {
    id("io.github.smyrgeorge.ktorlib.multiplatform")
//    id("io.github.smyrgeorge.ktorlib.publish")
//    id("io.github.smyrgeorge.ktorlib.dokka")
}

kotlin {
    sourceSets {
        configureEach {
            languageSettings.progressiveMode = true
        }
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.core)
            }
        }
    }
}
