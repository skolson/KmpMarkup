package com.oldguy.markup.model

import com.oldguy.common.io.Uri

open class Node(
    val name: String,
    val attributes: Attributes = Attributes(),
    children: List<Node> = emptyList(),
    var text: String = ""
): Model("Node") {
    val namespaces = emptyMap<String, Uri>().toMutableMap()
    val children = children.toMutableList()
    val isLeafNode get() = children.isEmpty()

    companion object {
        val start = "<"
        val stop = ">"
        val selfClosing = "/>"
        val endStart = "</"
    }
}