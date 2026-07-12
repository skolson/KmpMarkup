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

    /**
     * If this property is false, then standard text headers were found and parsed before SGML. If
     * true, then no text headers were found. OFX header values will be parsed from the OFX
     * ProcessingInstruction that is required since text headers are not present.
     */
    var noHeaders = false
        private set

    var file: TextFile? = null
        private set

    /**
     * Use this to close the TextFile and reopen from the beginning using the new Charset.  Note that
     * TextBuffer also has a changeCharset method, which just applies the new Charset for all subsequent
     * reads. Use the TextBuffer changeCharset function if closing and re-opening the TextFile
     * is not needed or desired.
     */
    suspend fun changeCharset(charset: Charset): TextFile {
        file?.close()
        return TextFile(path, charset).also { file = it }
    }

    suspend fun open(): TextBuffer {
        var headerLines = 0
        var tBuf: TextBuffer
        TextFile(path).apply {
            this@OfxFile.file = this
            val headers = mutableListOf<String>().apply { addAll(OfxParser.ofxHeadersList) }
            tBuf = textBuffer
            while (true) {
                val line = readLine().trim()
                headerLines++
                if (line.isEmpty()) break
                if (headerLines == 1 && line.startsWith("<?")) {
                    noHeaders = true
                    break
                }
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
                        ofxCharset = when (tokens[1].uppercase()) {
                            "WINDOWS-1252",
                            "CP1252",
                            "1252" -> Windows1252()
                            "ASCII",
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
            if (noHeaders) {
                rewind()
            } else {
                if (charset.name != defaultCharset.name) {
                    tBuf.changeCharset(charset)
                }
            }
            return tBuf
        }
    }

    suspend fun use(action: suspend (OfxFile, TextBuffer) -> Unit) {
        open()
        file?.let {
            action(this, it.textBuffer)
            it.close()
        }
    }
    companion object {
        val defaultCharset: Charset = Iso88591()
    }
}