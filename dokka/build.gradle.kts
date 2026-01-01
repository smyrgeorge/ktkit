plugins {
    id("io.github.smyrgeorge.ktkit.dokka")
}

dependencies {
    dokka(project(":ktkit"))
    dokka(project(":ktkit-pgmq"))
}

dokka {
    moduleName.set(rootProject.name)
}
