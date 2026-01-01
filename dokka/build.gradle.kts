plugins {
    id("io.github.smyrgeorge.ktkit.dokka")
}

dependencies {
//    dokka(project(":ktkit"))
}

dokka {
    moduleName.set(rootProject.name)
}
