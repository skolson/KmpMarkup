package com.oldguy.markup.model

/**
 * If domParser is configured, track document content here.
 */
class Document: Model("Document") {
    val prolog = emptyList<ProcessingInstruction>().toMutableList()
    var root: Node? = null
    val comments = emptyList<Comment>().toMutableList()

    private var level = 0

    /**
     * If a DOM has been parsed, use this to traverse the nodes recursively. The action lambda will
     * be called for each node, followed by each child. The level parameter is the current depth.
     * @param action lambda to call for each node.
     */
    fun traverse(
        action: (level: Int, node: Node) -> Unit
    ) {
        level = 0
        root?.let { traverse(it, action) }
    }

    private fun traverse(
        node: Node,
        action: (level: Int, node: Node) -> Unit
    ) {
        action(level, node)
        level++
        node.children.forEach {
            traverse(it, action)
        }
    }
}