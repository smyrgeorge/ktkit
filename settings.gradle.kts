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
include("ktkit-ktor-httpclient")
include("ktkit-spring-webclient")
include("ktkit-sqlx4k")
include("ktkit-sqlx4k-pgmq")
include("example")
include("example-mcp")
