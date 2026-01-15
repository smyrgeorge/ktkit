package io.github.smyrgeorge.ktkit.sqlx4k

import kotlin.reflect.KClass

actual fun KClass<*>.sealedSubclasses(): List<KClass<*>> = sealedSubclasses
