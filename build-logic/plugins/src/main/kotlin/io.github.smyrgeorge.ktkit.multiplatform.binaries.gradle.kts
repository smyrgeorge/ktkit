import io.github.smyrgeorge.ktkit.multiplatform.Utils

plugins {
    kotlin("multiplatform")
}

val enabledTargets = Utils.targetsOf(project)

kotlin {
    val availableTargets = mapOf(
        "iosArm64" to { iosArm64 { binaries { executable() } } },
        "androidNativeArm64" to { androidNativeArm64 { binaries { executable() } } },
        "androidNativeX64" to { androidNativeX64 { binaries { executable() } } },
        "macosArm64" to { macosArm64 { binaries { executable() } } },
        "linuxArm64" to { linuxArm64 { binaries { executable() } } },
        "linuxX64" to { linuxX64 { binaries { executable() } } },
        "mingwX64" to { mingwX64 { binaries { executable() } } },
        "jvm" to { jvm() },
    )

    enabledTargets.forEach {
        println("Enabling target $it")
        availableTargets[it]?.invoke()
    }

    applyDefaultHierarchyTemplate()
}
