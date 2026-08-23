plugins {
    id("io.github.smyrgeorge.ktkit.multiplatform")
    alias(libs.plugins.kotlin.serialization)
    id("io.github.smyrgeorge.ktkit.dokka")
    id("io.github.smyrgeorge.ktkit.publish")
}

kotlin {
    sourceSets {
        configureEach {
            languageSettings.progressiveMode = true
        }
        commonMain {
            dependencies {
                api(project(":ktkit"))
                api(libs.sqlx4k)
                api(libs.sqlx4k.arrow)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.kotlin.reflect)
            }
        }
    }
}
