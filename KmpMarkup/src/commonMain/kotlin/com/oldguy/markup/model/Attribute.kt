package com.oldguy.markup.model

import com.oldguy.common.io.TextBuffer
import com.oldguy.common.io.Uri
import com.oldguy.markup.ParseException

data class Attributes(
    val attributes: MutableMap<String, Attribute> = emptyMap<String, Attribute>().toMutableMap()
) {
    val namespaces get() = attributes.values.filter { it.isNamespace }
    val normals get() = attributes.values.filter { !it.isNamespace }

    fun parse(nameToken: TextBuffer.Token, valueToken: TextBuffer.Token): Attribute {
        val name = nameToken.value.trim()
        val value = valueToken.value
        if (nameToken.separator != "=")
            throw IllegalArgumentException("Invalid attribute name: ${nameToken.value}")
        if (!valueToken.quotesFound)
            throw IllegalArgumentException("Invalid attribute value - not delimited by quotes: ${valueToken.value}")
        if (attributes.containsKey(name))
            throw IllegalArgumentException("Duplicate attribute name: $name")
        attributes[name] = Attribute(name, value).apply {
            if (isNamespace) {
                try {
                    Uri(value)
                } catch (_: IllegalArgumentException) {
                    throw ParseException(
                        "Invalid namespace URI: $value, name: $name",
                        nameToken.line,
                        nameToken.position
                    )
                }
            }
        }
        return attributes[name]!!
    }
}
data class Attribute(
    val name: String,
    val value: String
) {
    val isNamespace get() = name.lowercase() == reservedPrefix || name.contains(namespaceSeparator)
    val namespace get() =
        if (name.contains(namespaceSeparator))
            name.substringBefore(namespaceSeparator, "")
        else if (name.lowercase() == reservedPrefix)
            reservedPrefix
        else
            ""
    val localName get() = name.substringAfter(namespaceSeparator, name)
    val uri get() = if (isNamespace) Uri(value) else null

    companion object {
        const val namespaceSeparator = ":"
        const val reservedPrefix = "xmlns:"
    }
}