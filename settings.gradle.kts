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
include("ktkit-gradle-plugin")
include("ktkit-openapi-compiler-plugin")
include("ktkit-sqlx4k")
include("ktkit-sqlx4k-pgmq")
include("ktkit-sqlx4k-postgres")
include("example")
