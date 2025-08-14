package com.oldguy.markup.model

import com.oldguy.common.io.charsets.Charsets

open class ProcessingInstruction(
    val target: String,
    val content: String
): Model("ProcessingInstruction") {
    val isXml = target == xml

    companion object {
        val xml = "xml"
        val start = "<?"
        val stop = "?>"
    }
}
class Declaration(
    target: String,
    val version: String,
    val encoding: String?,
    val standalone: String?
): ProcessingInstruction(target, "")
{
    val isVersionValid = versions.contains(version)
    val isStandaloneValid: Boolean = standalone?.lowercase()?.let { it == yes || it == no } ?: true
    val isCharsetSupported: Boolean get() {
        return encoding?.let {
            try {
                Charsets.fromName(encoding)
                true
            } catch (_: Exception) {
                false
            }
        } ?: true
    }

    companion object {
        private val versions = listOf("1.0", "1.1")
        private val encodingTag = "encoding"
        private val standaloneTag = "standalone"
        private val yes = "yes"
        private val no = "no"
        private val versionTag = "version"

        fun parse(attributes: Attributes): Declaration {
            return Declaration(
                xml,
                attributes.attributes[versionTag]?.value ?: "",
                attributes.attributes[encodingTag]?.value ?: "",
                attributes.attributes[standaloneTag]?.value ?: ""
            )
        }
    }
}