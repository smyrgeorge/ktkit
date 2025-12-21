plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("multiplatform") {
            id = "io.github.smyrgeorge.ktorlib.multiplatform"
            implementationClass = "io.github.smyrgeorge.ktorlib.multiplatform.MultiplatformConventions"
        }
        create("multiplatform.binaries") {
            id = "io.github.smyrgeorge.ktorlib.multiplatform.binaries"
            implementationClass = "io.github.smyrgeorge.ktorlib.multiplatform.MultiplatformBinariesConventions"
        }
        create("publish") {
            id = "io.github.smyrgeorge.ktorlib.publish"
            implementationClass = "io.github.smyrgeorge.ktorlib.publish.PublishConventions"
        }
        create("dokka") {
            id = "io.github.smyrgeorge.ktorlib.dokka"
            implementationClass = "io.github.smyrgeorge.ktorlib.dokka.DokkaConventions"
        }
    }
}

dependencies {
    compileOnly(libs.gradle.kotlin.plugin)
    compileOnly(libs.gradle.publish.plugin)
    compileOnly(libs.gradle.dokka.plugin)
}