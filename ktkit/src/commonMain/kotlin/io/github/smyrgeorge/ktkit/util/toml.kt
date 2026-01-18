package io.github.smyrgeorge.ktkit.util

import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlNode

operator fun TomlFile.plus(other: TomlFile): TomlFile = merge(this, other).let { this }
private fun merge(base: TomlNode, override: TomlNode) {
    val baseChildren = base.children
    val overrideChildren = override.children

    for (overrideChild in overrideChildren) {
        val matchingBase = baseChildren.find { it.name == overrideChild.name }
        if (matchingBase != null) {
            if (matchingBase.children.isNotEmpty() && overrideChild.children.isNotEmpty()) {
                // Both have children (tables), merge recursively
                merge(matchingBase, overrideChild)
            } else {
                // Replace base with override (key-value pairs or leaf nodes)
                baseChildren[baseChildren.indexOf(matchingBase)] = overrideChild
            }
        } else {
            // Add new node from override
            baseChildren.add(overrideChild)
        }
    }
}
