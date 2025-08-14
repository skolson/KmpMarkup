package com.oldguy.markup.model

import com.oldguy.common.io.TextBuffer

data class Attributes(
    val attributes: MutableMap<String, Attribute> = emptyMap<String, Attribute>().toMutableMap()
) {
    fun parse(nameToken: TextBuffer.Token, valueToken: TextBuffer.Token) {
        val name = nameToken.value
        val value = valueToken.value
        if (nameToken.leadingSeparator.isNotEmpty() ||
            valueToken.leadingSeparator != "=")
            throw IllegalArgumentException("Invalid attribute name: ${nameToken.value} or value: ${valueToken.value} or separator: ${valueToken.leadingSeparator}")
        if (attributes.containsKey(name))
            throw IllegalArgumentException("Duplicate attribute name: $name")
        attributes[name] = Attribute(name, value)
    }
}
data class Attribute(
    val name: String,
    val value: String
) {
}