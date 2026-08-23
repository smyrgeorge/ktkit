plugins {
    id("io.github.smyrgeorge.ktkit.multiplatform.jvm")
    alias(libs.plugins.kotlin.serialization)
    id("io.github.smyrgeorge.ktkit.dokka")
    id("io.github.smyrgeorge.ktkit.publish")
}

kotlin {
    sourceSets {
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
        jvmTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.reflect)
            }
        }
    }
}
