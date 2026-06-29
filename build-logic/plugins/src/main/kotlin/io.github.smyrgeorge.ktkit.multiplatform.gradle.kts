@file:OptIn(ExperimentalWasmDsl::class)

import io.github.smyrgeorge.ktkit.multiplatform.Utils
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
}

kotlin {
    val availableTargets = mapOf(
        "iosArm64" to { iosArm64() },
        "androidNativeArm64" to { androidNativeArm64() },
        "androidNativeX64" to { androidNativeX64() },
        "macosArm64" to { macosArm64() },
        "linuxArm64" to { linuxArm64() },
        "linuxX64" to { linuxX64() },
        "mingwX64" to { mingwX64() },
        "jvm" to {
            jvm {
                compilerOptions {
                    freeCompilerArgs.set(listOf("-Xjsr305=strict"))
                    jvmTarget.set(JvmTarget.JVM_21)
                }
            }
        },
        "js" to {
            js {
                browser()
                nodejs()
            }
        },
        "wasmJs" to {
            wasmJs {
                browser()
                nodejs()
            }
        },
        "wasmWasi" to {
            wasmWasi {
                nodejs()
            }
        },
    )

    Utils.targets.forEach {
        println("Enabling target $it")
        availableTargets[it]?.invoke()
    }

    applyDefaultHierarchyTemplate()
}
