plugins {
    id("io.github.smyrgeorge.ktkit.dokka")
}

dependencies {
    dokka(project(":ktkit"))
    dokka(project(":ktkit-pgmq"))
    dokka(project(":ktkit-sqlx4k"))
}

dokka {
    moduleName.set(rootProject.name)
}
