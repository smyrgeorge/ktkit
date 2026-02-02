plugins {
    id("io.github.smyrgeorge.ktkit.multiplatform")
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
        commonMain {
            dependencies {
                implementation(project(":ktkit"))
                api(libs.ktor.client.core)
                api(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}
