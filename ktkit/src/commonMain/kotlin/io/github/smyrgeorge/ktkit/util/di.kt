package io.github.smyrgeorge.ktkit.util

import org.koin.core.KoinApplication
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import kotlin.reflect.KClass

fun <T : Any> KoinApplication.get(
    clazz: KClass<T>,
    qualifier: Qualifier? = null,
    parameters: ParametersDefinition? = null
): T = koin.get(clazz, qualifier, parameters)

inline fun <reified T : Any> KoinApplication.get(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T = koin.get(qualifier, parameters)

inline fun <reified T : Any> KoinApplication.getAll(): List<T> = koin.getAll()
