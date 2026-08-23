rootProject.name = "ktkit"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }

    includeBuild("build-logic")
}

include("dokka")
include("ktkit")
include("ktkit-ktor-httpclient")
include("ktkit-gradle-plugin")
include("ktkit-compiler-openapi")
include("ktkit-sqlx4k")
include("ktkit-sqlx4k-pgmq")
include("ktkit-sqlx4k-postgres")
include("example")
