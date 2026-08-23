plugins {
    id("io.github.smyrgeorge.ktkit.multiplatform.jvm")
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
                // Provided by the Kotlin compiler at runtime — must NOT be bundled.
                compileOnly(libs.kotlin.compiler.embeddable)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.compiler.embeddable)
                implementation(project(":ktkit"))
            }
        }
    }
}
