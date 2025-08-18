package com.oldguy.markup.model

open class Node(
    val name: String,
    val attributes: Attributes = Attributes(),
    children: List<Node> = emptyList(),
    var text: String = "",
    var cData: CData? = null,
    var level: Int = 0
): Model("Node") {
    val attributesList get() = attributes.normals
    val namespacesList get() = attributes.namespaces
    val children = children.toMutableList()
    val isLeafNode get() = children.isEmpty()

    fun attribute(name: String) = attributes.attributes[name]
    fun namespace(name: String) = attributes.namespaces.firstOrNull { it.name == name }

    /**
     * Traverse DOM tree using left-wise recursion, starting at the current node.
     * Invoke each() for each node, with arguments
     * "parent" null if first node (typically root), else parent node
     * "node" current node
     */
    fun traverse(each: (parent: Node?, node: Node) -> Unit) {
        traverse(null, each)
    }

    fun traverse(parent: Node?, each: (parent: Node?, node: Node) -> Unit) {
        each(parent, this)
        children.forEach { it ->
            it.traverse(this, each)
        }
    }

    companion object {
        val start = "<"
        val stop = ">"
        val selfClosing = "/>"
        val endStart = "</"
    }
}