package io.github.smyrgeorge.ktkit.util

fun camelCaseToKebabCase(camelCase: String): String =
    camelCase.replace(Regex("(?<!^)(?=[A-Z])")) { "-${it.value}" }.lowercase()