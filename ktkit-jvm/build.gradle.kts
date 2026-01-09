plugins {
    id("io.github.smyrgeorge.ktkit.multiplatform.jvm")
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
        jvmMain {
            dependencies {
                api(project(":ktkit"))
                implementation(libs.spring.boot.starter.webflux)
            }
        }
    }
}
