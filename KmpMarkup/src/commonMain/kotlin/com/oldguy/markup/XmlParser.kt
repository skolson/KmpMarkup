package com.oldguy.markup

import com.oldguy.common.io.TextBuffer
import com.oldguy.markup.model.*

class XmlParser(val textBuffer: TextBuffer)
{
    enum class Event {
        StartDocument,
        EndDocument,
        StartTag,
        StartTagEnd,
        EndTagStart,
        EmptyTag,
        Characters,
        CommentStart,
        CommentEnd,
        CDataStart,
        CDataEnd,
        ProcessingInstruction,
        ProcessingInstructionEnd,
        Declaration,
        DocType,
        CharacterEscape
    }

    private lateinit var declaration: Declaration
    private var nodeStack: MutableList<Node> = mutableListOf()
    private val root: Node? get() = if (nodeStack.isEmpty()) null else nodeStack[0]
    private val active: Node? get() = nodeStack.lastOrNull()
    private val parent: Node? get() = nodeStack.getOrNull(nodeStack.size - 2)

    private var lastToken: TextBuffer.Token? = null

    /**
     * Set this true to have the parse function to call its event lambda. False if not.
     */
    var pullParser = true

    /**
     * Set this true if the parser should retain the instances found in it's DOM. False if not.
     * Default is false.
     */
    var domParser = false

    suspend fun parse(
        event: (Event, Model) -> Boolean
    ) {
        val document = Document()
        event(Event.StartDocument, document)
        while (!textBuffer.isEndOfFile) {
            val token = textBuffer.token()
            var event: Event? = null
            var model: Model? = null
            when (separators[token.leadingSeparator]) {
                Event.StartDocument -> { }
                Event.EndDocument -> { }
                Event.StartTag -> {
                    val node = Node(token.value)
                    event = Event.StartTag
                    val attrs = parseAttributes()
                    node.attributes.attributes.putAll(attrs.attributes)
                    nodeStack.add(node)
                    if (domParser) parent?.children?.add(node)
                    model = node
                }
                Event.StartTagEnd -> {
                    active?.let {
                        it.text = token.value
                    } ?: throw ParseException(
                        "Error parsing data for a node. No current node found",
                        token.line,
                        token.position
                    )
                }
                Event.EndTagStart -> {
                    active?.let {
                        if (token.value != it.name)
                            throw ParseException(
                                "Node end tag name does not match start tag name: ${token.value} != ${it.name}",
                                textBuffer.lineCount,
                                textBuffer.linePosition
                            )
                        nodeStack.removeLast()
                    } ?: throw ParseException(
                        "Error parsing end tag for name ${token.value}. No current node found",
                        token.line,
                        token.position
                    )
                    event = Event.EndTagStart
                    model = active
                }
                Event.EmptyTag -> TODO()
                Event.Characters -> TODO()
                Event.CommentStart -> {
                    event = Event.CommentStart
                    val comment = Comment(
                        textBuffer.nextUntil(listOf(Comment.stop)).first
                    )
                    if (domParser) {
                        document.comments.add(comment)
                    }
                    model = comment
                }
                Event.CommentEnd -> { }
                Event.CDataStart -> {
                    val cdata = textBuffer.nextUntil(listOf(CData.stop)).first
                    active?.let {
                        it.text += cdata
                    } ?: throw ParseException(
                        "Error parsing CDATA. No current node found",
                        textBuffer.lineCount,
                        textBuffer.linePosition
                    )
                }
                Event.CDataEnd -> {  }
                Event.ProcessingInstruction -> {
                    event = Event.ProcessingInstruction
                    val pi  = parseProcessingInstruction(token)
                    if (domParser)
                        document.prolog.add(pi)
                    model = pi
                }
                Event.ProcessingInstructionEnd -> {}
                Event.Declaration -> { }
                Event.DocType -> TODO()
                Event.CharacterEscape -> { }
                null -> {
                    throw ParseException(
                        "Invalid token found: ${token.value}",
                        token.line,
                        token.position
                    )
                }
            }
            event?.let {
                if (pullParser) {
                    if (model is Declaration)
                        event(Event.Declaration, model)
                    else
                        event(it, model!!)
                }
            }
        }
        event(Event.EndDocument, document)
    }

    private suspend fun parseProcessingInstruction(token: TextBuffer.Token): ProcessingInstruction {
        val target = token.value
        return if (token.value.lowercase() == ProcessingInstruction.xml) {
            val attrs = parseAttributes()
            Declaration.parse(attrs)
        } else {
            ProcessingInstruction(
                target,
            textBuffer.nextUntil(listOf(ProcessingInstruction.stop)).first
            )
        }
    }

    /**
     * Examine a string for all valid XML character escape sequences, and replace each escape sequence
     * with the appropriate character.
     * @param value string to process
     * @return string with all escape sequences replaced
     */
    fun stringEscapes(value: String): String {
        var result = value
        generalEscapes.keys.forEach {
            if (value.indexOf(it) >= 0) {
                result = result.replace(it, generalEscapes[it]!!)
            }
        }
        var index = result.indexOf(escapePrefix)
        while (index >= 0) {
            val endIndex = result.indexOf(';', index)
            if (endIndex < 0)
                throw ParseException(
                    "Invalid escape sequence found in string: $result",
                    1,
                    index
                )
            val escaped = result.substring(index, endIndex + 1)
            val code = result.substring(index + escapePrefix.length, endIndex).toIntOrNull()
            if (code == null)
                throw ParseException(
                    "Invalid escape sequence found in string: $result",
                    1,
                    index
                )
            result = result.replace(escaped, Char(code).toString())
            index = result.indexOf(escapePrefix)
        }
        return result
    }

    suspend fun parseAttributes(): Attributes {
        return Attributes().apply {
            while (!textBuffer.isEndOfFile) {
                val nameToken = textBuffer.token()
                if (nameToken.leadingSeparator.isNotEmpty() ||
                    nameToken.value.isEmpty()) {
                    lastToken = nameToken
                    break
                }
                val name = nameToken.value
                val valueToken = textBuffer.token()
                val value = if (valueToken.leadingSeparator == "=")
                    valueToken.value
                else {
                    lastToken = valueToken
                    break
                }
                attributes[name] = Attribute(name, value)
            }
        }
    }
    companion object {
        /**
         * Most of this info from https://www.w3.org/TR/xml
         */

        private const val escapePrefix = "&#"
        private val separators = mapOf<String, Event>(
            Node.start to Event.StartTag,
            Node.stop to Event.StartTagEnd,
            Node.endStart to Event.EndTagStart,
            Node.selfClosing to Event.EmptyTag,
            ProcessingInstruction.start to Event.ProcessingInstruction,
            ProcessingInstruction.stop to Event.ProcessingInstructionEnd,
            Comment.start to Event.CommentStart,
            Comment.stop to Event.CommentEnd,
            CData.start to Event.CDataStart,
            CData.stop to Event.CDataEnd,
            "<!DOCTYPE" to Event.DocType,
            "<!ENTITY" to Event.CharacterEscape,
        )

        private val generalEscapes = mapOf<String, String>(
            "&lt;" to "<",
            "&gt;" to ">",
            "&quot;" to "\"",
            "&apos;" to "'",
            "&amp;" to "&"
        )

        private val nameStartCharacter = listOf<IntRange>(
            'a'.code..'z'.code,
            'A'.code..'Z'.code,
            '_'.code..'_'.code,
            ':'.code..':'.code,
            0x00C0..0x00D6,
            0x00D8..0x00F6,
            0x00F8..0x02FF,
            0x0370.. 0x37D,
            0x37F .. 0x1FFF,
            0x200C .. 0x200D,
            0x2070 .. 0x218F,
            0x2C00 .. 0x2FEF,
            0x3001 .. 0xD7FF,
            0xF900 .. 0xFDCF,
            0xFDF0 .. 0xFFFD,
            0x10000 .. 0xEFFFF
        )
        private val nameCharacter =
            nameStartCharacter +
            listOf<IntRange>(
                '0'.code..'9'.code,
                '-'.code..'-'.code,
                '.'.code..'.'.code,
                0xB7 .. 0xB7,
                0x0300 .. 0x036F,
                0x203F .. 0x2040
            )

        private fun testCharacter(c: Char, valid: List<IntRange>): Boolean {
            for (range in valid) {
                if (c.code in range)
                    return true
            }
            return false
        }
    }
}