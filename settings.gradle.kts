rootProject.name = "ktorlib"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    includeBuild("build-logic")
}

//include("dokka")
include("ktorlib")
include("example")
