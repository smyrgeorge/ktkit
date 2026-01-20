plugins {
    id("io.github.smyrgeorge.ktkit.dokka")
}

dependencies {
    dokka(project(":ktkit"))
    dokka(project(":ktkit-ktor-httpclient"))
    dokka(project(":ktkit-spring-webclient"))
    dokka(project(":ktkit-sqlx4k"))
    dokka(project(":ktkit-sqlx4k-pgmq"))
    dokka(project(":ktkit-sqlx4k-postgres"))
}

dokka {
    moduleName.set(rootProject.name)
}
