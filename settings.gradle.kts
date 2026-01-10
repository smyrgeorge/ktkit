rootProject.name = "ktkit"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    includeBuild("build-logic")
}

include("dokka")
include("ktkit")
include("ktkit-jvm-spring-webclient")
include("ktkit-jvm-ktor-client")
include("ktkit-sqlx4k")
include("ktkit-sqlx4k-pgmq")
include("example")
