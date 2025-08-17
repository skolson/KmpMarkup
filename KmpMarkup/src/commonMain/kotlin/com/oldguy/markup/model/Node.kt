package com.oldguy.markup.model

open class Node(
    val name: String,
    val attributes: Attributes = Attributes(),
    children: List<Node> = emptyList(),
    var text: String = "",
    var cData: CData? = null
): Model("Node") {
    val attributesList = attributes.normals
    val namespacesList = attributes.namespaces
    val children = children.toMutableList()
    val isLeafNode get() = children.isEmpty()

    companion object {
        val start = "<"
        val stop = ">"
        val selfClosing = "/>"
        val endStart = "</"
    }
}