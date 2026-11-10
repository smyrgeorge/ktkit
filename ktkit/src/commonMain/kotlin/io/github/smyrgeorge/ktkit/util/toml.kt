package io.github.smyrgeorge.ktkit.util

import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlNode
import com.akuleshov7.ktoml.tree.nodes.TomlStubEmptyNode

operator fun TomlFile.plus(other: TomlFile): TomlFile = merge(this, other).let { this }
private fun merge(base: TomlNode, override: TomlNode) {
    val baseChildren = base.children
    val overrideChildren = override.children

    for (overrideChild in overrideChildren) {
        // The parser's placeholder for an empty table carries nothing to merge, and the Map decoder
        // only tolerates one in first position.
        if (overrideChild is TomlStubEmptyNode) continue
        val matchingBase = baseChildren.find { it.name == overrideChild.name }
        if (matchingBase != null) {
            if (matchingBase.children.isNotEmpty() && overrideChild.children.isNotEmpty()) {
                // Both have children (tables), merge recursively
                merge(matchingBase, overrideChild)
            } else {
                // Replace base with override (key-value pairs or leaf nodes)
                baseChildren[baseChildren.indexOf(matchingBase)] = overrideChild
                // Decoders find a node's siblings through its parent, so the moved node must point at its new one.
                overrideChild.parent = base
            }
        } else {
            // Add new node from override
            baseChildren.add(overrideChild)
            overrideChild.parent = base
        }
    }
}
