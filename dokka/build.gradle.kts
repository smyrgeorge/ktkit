plugins {
    id("io.github.smyrgeorge.ktkit.dokka")
}

dependencies {
    dokka(project(":ktkit"))
    dokka(project(":ktkit-jvm"))
    dokka(project(":ktkit-sqlx4k"))
    dokka(project(":ktkit-sqlx4k-pgmq"))
}

dokka {
    moduleName.set(rootProject.name)
}
