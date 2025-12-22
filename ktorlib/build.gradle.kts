plugins {
    id("io.github.smyrgeorge.ktorlib.multiplatform")
    alias(libs.plugins.kotlin.serialization)
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
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.serialization.properties)
                api(libs.ktor.server.core)
                api(libs.ktor.server.auth)
                api(libs.ktor.server.content.negotiation)
                api(libs.ktor.server.status.pages)
                api(libs.ktor.serialization.kotlinx.json)
                api(libs.log4k)
                api(libs.koin.core)
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
