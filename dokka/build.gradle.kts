plugins {
    id("io.github.smyrgeorge.ktorlib.dokka")
}

dependencies {
//    dokka(project(":ktorlib"))
}

dokka {
    moduleName.set(rootProject.name)
}
