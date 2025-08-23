package com.oldguy.markup.model

/**
 * One XML element. Has a valid name. Has zero or more attributes. Has zero or more child elements.
 * May have a text value.
 * @property attributes is an Attributes instance, which has zero or more attributes and zero or
 * more namespace entries.
 * @property children is a list of child elements.
 * @property rawText is text value with whitespace characters retained
 * @property text is text value with leading and trailing whitespace characters removed
 * @property cData is non-null if element contains CDATA. Text will be empty in that case
 * @property level is depth of element in DOM tree, one-relative
 */
open class Node(
    val name: String,
    val attributes: Attributes = Attributes(),
    children: List<Node> = emptyList(),
    var rawText: String = "",
    var cData: CData? = null,
    var level: Int = 0
): Model("Node") {
    val attributesList get() = attributes.normals
    val namespacesList get() = attributes.namespaces
    val children = children.toMutableList()
    val isLeafNode get() = children.isEmpty()
    val text get() = rawText.trim()

    fun attribute(name: String) = attributes[name]

    fun child(name: String) = children.first { it.name == name }
    fun children(name: String) = children.filter { it.name == name }
    fun hasChild(name: String) = children.count { it.name == name }

    /**
     * Get namespace by local name.
     * @param localName local name of namespace, without prefix. or "xmlns" for XML namespace
     */
    fun namespace(localName: String) = attributes.namespace(localName)

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