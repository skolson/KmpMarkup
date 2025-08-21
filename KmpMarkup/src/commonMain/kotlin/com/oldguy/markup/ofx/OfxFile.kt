package com.oldguy.markup.ofx

import com.oldguy.common.io.TextBuffer
import com.oldguy.common.io.TextFile
import com.oldguy.common.io.charsets.Charset
import com.oldguy.common.io.charsets.Iso88591
import com.oldguy.common.io.charsets.Windows1252

class OfxFile(
    val path: String
) {
    var ofxCharset: Charset = Iso88591()
    val ofxHeaders = mutableMapOf<String, String>()

    var file: TextFile? = null
        private set

    suspend fun changeCharset(charset: Charset): TextFile {
        file?.close()
        return TextFile(path, charset).also { file = it }
    }

    suspend fun open(): TextBuffer {
        var headerLines = 0
        var tBuf: TextBuffer
        TextFile(path).apply {
            val headers = mutableListOf<String>().apply { addAll(ofxHeadersList) }
            tBuf = textBuffer
            while (true) {
                val line = readLine().trim()
                headerLines++
                if (line.isEmpty()) break
                if (line.startsWith('<'))
                    throw IllegalStateException("Invalid ofx file: < found before required empty header line")
                if (line.indexOf(':') < 0)
                    throw IllegalStateException("Invalid ofx header line: $line")
                val tokens = line.split(':')
                if (tokens.size != 2)
                    throw IllegalStateException("Invalid ofx header line: $line")
                ofxHeaders[tokens[0]] = tokens[1]
                if (headers.contains(tokens[0]))
                    headers.remove(tokens[0])
                else
                    throw IllegalStateException("Invalid ofx header name: ${tokens[0]}")
                when (tokens[0].uppercase()) {
                    "CHARSET" -> {
                        ofxCharset = when (tokens[1]) {
                            "1252" -> Windows1252()
                            "USASCII",
                            "UTF-8" -> Iso88591()
                            else ->
                                throw IllegalStateException("Unsupported ofx CHARSET name: ${tokens[1]}")
                        }
                    }
                    "DATA" -> {
                        when (tokens[1]) {
                            "OFXSGML" -> {}
                            else ->
                                throw IllegalStateException("Unsupported ofx DATA name: ${tokens[1]}")
                        }
                    }
                }
            }
            if (charset.name != defaultCharset.name) {
                close()
                changeCharset(charset).apply {
                    tBuf = textBuffer
                    repeat(headerLines) { tBuf.readLine() }
                }
            }
            val c = tBuf.next()
            if (c != '<')
                throw IllegalStateException("Invalid ofx file: missing < after headers")
            return tBuf
        }
    }

    suspend fun use(action: suspend (TextBuffer) -> Unit) {
        open()
        file?.let {
            action(it.textBuffer)
            it.close()
        }
    }
    companion object {
        val ofxHeadersList = listOf(
            "OFXHEADER",
            "DATA",
            "VERSION",
            "SECURITY",
            "ENCODING",
            "CHARSET",
            "COMPRESSION",
            "OLDFILEUID",
            "NEWFILEUID"
        )
        val defaultCharset: Charset = Iso88591()
    }
}