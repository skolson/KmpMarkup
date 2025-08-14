package com.oldguy.markup

import com.oldguy.common.io.ByteBuffer
import com.oldguy.common.io.File
import com.oldguy.common.io.RawFile
import com.oldguy.common.io.TextBuffer
import com.oldguy.common.io.TextFile
import com.oldguy.common.io.charsets.Charset
import com.oldguy.common.io.charsets.Utf16BE
import com.oldguy.common.io.charsets.Utf16LE
import com.oldguy.common.io.charsets.Utf32BE
import com.oldguy.common.io.charsets.Utf32LE
import com.oldguy.common.io.charsets.Utf8

/**
 * Represents a single XML file. Class is responsible for parsing any BOM in the beginning of the file
 * to determine encoding. It uses File and RawFile to do this. Once encoding is determined, it uses
 * TextFile and TextBuffer to parse the file. If no BOM is found, it assumes UTF-8. See
 * https://www.w3.org/TR/xml/#sec-guessing for specification
 */
class XmlFile(
    val path: String,
) {
    var charset: Charset = Utf8()
        private set

    private var bytesSkipped = 0
    val bomFound get() = bytesSkipped > 0

    var file: TextFile? = null
        private set

    /**
     * Change the encoding of the file, typically after parsing the XML header and finding an
     * encoding specified in the header. No BOM processing happens.
     */
    suspend fun changeCharset(charset: Charset): TextFile {
        file?.close()
        return TextFile(path, charset).also { file = it }
    }

    /**
     * Open with a specified charset.No BOM parsing is performed.
     */
    fun open(charset: Charset): TextBuffer {
        file = TextFile(path, charset).also { file = it }
        return file!!.textBuffer
    }

    /**
     * Open with no Charset. Parses any BOM in the beginning of the file to determine encoding.
     * If no BOM examine initial bytes to determine encoding per the specification. Produce a
     * TextBuffer with the correct charset for use by a parser.
     */
    suspend fun open(): TextBuffer {
        val raw = RawFile(File(path))
        val bomBuffer = ByteBuffer(4)
        val byteCount = raw.read(bomBuffer).toInt()
        if (byteCount < bomBuffer.capacity)
            throw ParseException("Could not read first 4 bytes from $path")
        val firstFour = bomBuffer.getBytes(4)
        bytesSkipped = 0
        charset = if (firstFour.sliceArray(0 until utf8Bom.size).contentEquals(utf8Bom)) {
            bytesSkipped = utf8Bom.size
            Utf8()
        } else if (firstFour.sliceArray(0 until utf16beBom.size).contentEquals(utf16beBom)) {
            bytesSkipped = utf16beBom.size
            Utf16BE()
        } else if (firstFour.sliceArray(0 until utf16leBom.size).contentEquals(utf16leBom)) {
            bytesSkipped = utf16leBom.size
            Utf16LE()
        } else if (firstFour.contentEquals(utf32beBom)) {
            bytesSkipped = utf32beBom.size
            Utf32BE()
        } else if (firstFour.contentEquals(utf32leBom)) {
            bytesSkipped = utf32leBom.size
            Utf32LE()
        } else if (firstFour.contentEquals(utf32beBytes)) {
            Utf32BE()
        } else if (firstFour.contentEquals(utf32leBytes)) {
            Utf32LE()
        } else if (firstFour.contentEquals(utf16beBytes)) {
            Utf16BE()
        } else if (firstFour.contentEquals(utf16leBytes)) {
            Utf16LE()
        } else if (firstFour.contentEquals(utf8Bytes)) {
            Utf8()
        } else
            Utf8()
        raw.close()
        return TextFile(path, charset).also {
            file = it
            if (bytesSkipped > 0)
                it.skip(bytesSkipped.toULong())
        }.textBuffer
    }

    companion object {
        val utf8Bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val utf16beBom = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        val utf16leBom = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        val utf32beBom = byteArrayOf(0x00.toByte(), 0x00.toByte(), 0xFE.toByte(), 0xFF.toByte())
        val utf32leBom = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00.toByte(), 0x00.toByte())

        val utf32beBytes = byteArrayOf(0.toByte(), 0.toByte(), 0.toByte(), 0x3C.toByte())
        val utf32leBytes = byteArrayOf(0x3C.toByte(), 0.toByte(), 0.toByte(), 0.toByte())
        val utf16beBytes = byteArrayOf(0.toByte(), 0x3C.toByte(), 0.toByte(), 0x3f.toByte())
        val utf16leBytes = byteArrayOf(0x3C.toByte(), 0.toByte(), 0x3f.toByte(), 0.toByte())
        val utf8Bytes = byteArrayOf(0x3C.toByte(), 0x3f.toByte(), 0x78.toByte(), 0x6d.toByte())
    }
}