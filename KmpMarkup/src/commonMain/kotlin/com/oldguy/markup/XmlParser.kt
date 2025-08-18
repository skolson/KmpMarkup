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
    private val level get() = nodeStack.size

    var document = Document()
        private set

    val root: Node? get() = if (nodeStack.isEmpty()) null else nodeStack[0]
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

    /**
     * Configure TextBuffer for XML parsing. Then parse the source, calling the event lambda for each
     * XML token type encountered
     */
    suspend fun parse(
        event: (Event, Model) -> Boolean
    ) {

        textBuffer.apply {
            tokenSeparators = separators.keys.toList()
            escapedQuote = ""
            escapedSingleQuote = ""
            quoteType = TextBuffer.QuoteType.Either
        }
        document = Document()
        event(Event.StartDocument, document)
        var saveSeparators = emptyList<String>()
        var whitespace = false
        var legalNextSeparators = emptyList<String>()
        var model: Model? = null
        var capturingText = false
        while (!textBuffer.isEndOfFile) {
            val token = lastToken ?: textBuffer.token()
            lastToken = null
            if (legalNextSeparators.isNotEmpty() && !legalNextSeparators.contains(token.separator))
                throw ParseException(
                    "Invalid token separator found: ${token.separator}",
                    token.line,
                    token.position
                )
            val eventType: Event? = separators[token.separator]
                ?: throw ParseException(
                    "Illegal separator (bug): ${token.separator}",
                    token.line,
                    token.position
                )
            when (separators[token.separator]) {
                Event.StartDocument -> {
                    model = document
                }
                Event.EndDocument -> {
                    model = document
                }
                Event.StartTag -> {
                    capturingText = false
                    if (!token.value.isBlank())
                        throw ParseException(
                            "Invalid characters found before node start tag: ${token.value}",
                            textBuffer.lineCount,
                            textBuffer.linePosition
                        )
                    val name = textBuffer.token(true)
                    validateName(name.value)
                    Node(name.value).apply {
                        if (name.separator == Node.stop) {
                            lastToken = name
                        } else {
                            val attrs = parseAttributes(listOf(Node.stop, Node.selfClosing))
                            attributes.attributes.putAll(attrs.attributes)
                        }
                        nodeStack.add(this)
                        level = level
                        if (domParser) {
                            if (document.root == null) document.root = root
                            parent?.children?.add(this)
                        }
                        model = this
                    }
                    legalNextSeparators = listOf(Node.stop, Node.endStart, Node.selfClosing)
                }
                Event.StartTagEnd -> {
                    capturingText = true
                    legalNextSeparators = listOf(Node.start, Comment.start, Node.endStart)
                    textBuffer.tokenValueQuotedString = false
                }
                Event.EndTagStart -> {
                    if (capturingText)
                        addTextToNode(token)
                    active?.let {
                        if (it.text.isNotBlank())
                            it.text = stringEscapes(it.text)
                    }
                    val endNameToken = textBuffer.token()
                    capturingText = false
                    if (endNameToken.separator == Node.stop) {
                        val name = endNameToken.value
                        active?.let {
                            if (name != it.name)
                                throw ParseException(
                                    "Node end tag name does not match start tag name: $name != ${it.name}",
                                    textBuffer.lineCount,
                                    textBuffer.linePosition
                                )
                            if (nodeStack.last().name != name)
                                throw ParseException(
                                    "nodestack name: ${nodeStack.last().name} must match $name"
                                )
                        } ?: throw ParseException(
                            "Error parsing end tag for name ${token.value}. No current node found",
                            token.line,
                            token.position
                        )
                    } else
                        throw ParseException(
                            "Illegal separator while parsing node End tag: ${endNameToken.separator}",
                            endNameToken.line,
                            endNameToken.position
                        )
                    model = active
                    nodeStack.removeLast()
                }
                Event.EmptyTag -> {
                    capturingText = false
                    model = active
                    nodeStack.removeLast()
                    legalNextSeparators = listOf(Node.start, Node.endStart, Comment.start)
                }
                Event.CommentStart -> {
                    if (capturingText && !token.value.isBlank())
                        addTextToNode(token)
                    textBuffer.apply {
                        saveSeparators = tokenSeparators
                        whitespace = retainWhitespace
                        retainWhitespace = true
                        tokenValueQuotedString = false
                        tokenSeparators = listOf(Comment.stop)
                    }
                    model = Comment("")
                    legalNextSeparators = listOf(Comment.stop)
                }
                Event.CommentEnd -> {
                    textBuffer.tokenSeparators = saveSeparators
                    textBuffer.retainWhitespace = whitespace
                    model = Comment(token.value)
                    legalNextSeparators = emptyList()
                }
                Event.CDataStart -> {
                    model = CData("")
                    legalNextSeparators = listOf(CData.stop)
                    textBuffer.tokenValueQuotedString = false
                }
                Event.CDataEnd -> {
                    model = CData(token.value)
                    active?.let {
                        it.cData = model
                    } ?: throw ParseException(
                        "Error parsing CDATA. No current node found",
                        textBuffer.lineCount,
                        textBuffer.linePosition
                    )
                }
                Event.ProcessingInstruction -> {
                    val token = textBuffer.token(true)
                    val target = token.value
                    if (target.isEmpty())
                        throw ParseException(
                            "Error parsing processing instruction. Processing instruction has no target",
                            textBuffer.lineCount,
                            textBuffer.linePosition
                        )
                    model = if (target.lowercase() == ProcessingInstruction.xml) {
                        val attrs = parseAttributes(listOf(ProcessingInstruction.stop))
                        Declaration.parse(attrs)
                    } else
                        ProcessingInstruction(target, "")
                    legalNextSeparators = listOf(ProcessingInstruction.stop)
                }
                Event.ProcessingInstructionEnd -> {
                    if (!(model as ProcessingInstruction).isXml) {
                        if (token.value.isEmpty())
                            throw ParseException(
                                "Error parsing processing instruction. Processing instruction has target but no text",
                                textBuffer.lineCount,
                                textBuffer.linePosition
                            )
                        model = ProcessingInstruction(model.target, token.value)
                    }
                    if (domParser)
                        document.prolog.add(model)
                    legalNextSeparators = emptyList()
                }
                Event.Declaration -> { }
                Event.DocType -> TODO()
                Event.CharacterEscape -> { }
                null -> {
                    // Should never happen, see logic before "when"
                    throw ParseException(
                        "Invalid token found: ${token.value}",
                        token.line,
                        token.position
                    )
                }
            }
            eventType?.let {
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

    private fun addTextToNode(token: TextBuffer.Token) {
        active?.let {
            it.text += token.value
        } ?: throw ParseException(
            "Error parsing data for a node. No current node found",
            token.line,
            token.position
        )
    }

    private fun validateName(name: String) {
        name.apply {
            if (isEmpty())
                throw ParseException(
                    "Invalid empty name",
                    textBuffer.lineCount,
                    textBuffer.linePosition
                )
            if (nameStartCharacter.count { name[0].code in it  } == 0)
                throw ParseException(
                    "Invalid name first character: '${this[0]}, name: $this",
                    textBuffer.lineCount,
                    textBuffer.linePosition
                )
            substring(1).forEach { char ->
                if (nameCharacter.count { it.contains(char.code) } == 0)
                    throw ParseException(
                        "Invalid name character: '$char', in name: $this",
                        textBuffer.lineCount,
                        textBuffer.linePosition
                    )
            }
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
        return result.trim()
    }

    /**
     * Parse attributes until a Node end tag separator is found. Set the lastToken,
     * typically the PI.stop separator, so it can be processed normally
     */
    suspend fun parseAttributes(endingSeparators: List<String>): Attributes {
        val equalChar = "="
        val save: List<String>
        textBuffer.apply {
            tokenValueQuotedString = true
            save = tokenSeparators
            tokenSeparators = listOf(equalChar) + endingSeparators
            return Attributes().apply {
                while (!isEndOfFile ) {
                    val nameToken = token()
                    if (endingSeparators.contains(nameToken.separator)) {
                        lastToken = nameToken
                        tokenSeparators = save
                        return this
                    }
                    val valueToken = token()
                    parse(nameToken, valueToken)
                }
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