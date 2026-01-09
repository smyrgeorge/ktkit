plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("multiplatform") {
            id = "io.github.smyrgeorge.ktkit.multiplatform"
            implementationClass = "io.github.smyrgeorge.ktkit.multiplatform.MultiplatformConventions"
        }
        create("multiplatform.binaries") {
            id = "io.github.smyrgeorge.ktkit.multiplatform.binaries"
            implementationClass = "io.github.smyrgeorge.ktkit.multiplatform.MultiplatformBinariesConventions"
        }
        create("multiplatform.jvm") {
            id = "io.github.smyrgeorge.ktkit.multiplatform.jvm"
            implementationClass = "io.github.smyrgeorge.ktkit.multiplatform.MultiplatformJvmConventions"
        }
        create("publish") {
            id = "io.github.smyrgeorge.ktkit.publish"
            implementationClass = "io.github.smyrgeorge.ktkit.publish.PublishConventions"
        }
        create("dokka") {
            id = "io.github.smyrgeorge.ktkit.dokka"
            implementationClass = "io.github.smyrgeorge.ktkit.dokka.DokkaConventions"
        }
    }
}

dependencies {
    compileOnly(libs.gradle.kotlin.plugin)
    compileOnly(libs.gradle.publish.plugin)
    compileOnly(libs.gradle.dokka.plugin)
}