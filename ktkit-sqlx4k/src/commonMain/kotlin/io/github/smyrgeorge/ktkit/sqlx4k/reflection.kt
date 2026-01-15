package io.github.smyrgeorge.ktkit.sqlx4k

import kotlin.reflect.KClass

expect fun KClass<*>.sealedSubclasses(): List<KClass<*>>
