package com.oldguy.markup.model

class CData(
    val text: String
): Model("CData") {

    companion object {
        val start = "<![CDATA["
        val stop = "]]>"
    }
}