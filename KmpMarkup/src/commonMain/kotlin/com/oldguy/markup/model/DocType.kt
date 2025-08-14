package com.oldguy.markup.model

data class DocType(
    val name: String,
    val type: Type = Type.System,
    val systemId: String = "",
    val publicId: String = ""
) {
    enum class Type { System, Public, NData}
}