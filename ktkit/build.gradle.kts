plugins {
    id("io.github.smyrgeorge.ktkit.multiplatform")
    alias(libs.plugins.kotlin.serialization)
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
                api(libs.log4k)
                api(libs.koin.core)
                api(libs.arrow.core)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.core)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.serialization.properties)
                api(libs.ktoml.core)
                api(libs.ktor.server.core)
                api(libs.ktor.server.content.negotiation)
                api(libs.ktor.serialization.kotlinx.json)
                api(libs.sqlx4k)
                api(libs.sqlx4k.arrow)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.log4k.slf4j)
                implementation(libs.ktor.server.netty)
            }
        }
        nativeMain {
            dependencies {
                implementation(libs.ktor.server.cio)
            }
        }
    }
}
