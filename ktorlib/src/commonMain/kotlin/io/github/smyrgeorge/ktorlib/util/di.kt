package io.github.smyrgeorge.ktorlib.util

import org.koin.core.KoinApplication
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import kotlin.reflect.KClass

fun <T : AbstractComponent> KoinApplication.get(
    clazz: KClass<T>,
    qualifier: Qualifier? = null,
    parameters: ParametersDefinition? = null
): T = koin.get(clazz, qualifier, parameters)

inline fun <reified T : AbstractComponent> KoinApplication.get(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): T = koin.get(qualifier, parameters)

inline fun <reified T : AbstractComponent> KoinApplication.getAll(): List<T> = koin.getAll()

