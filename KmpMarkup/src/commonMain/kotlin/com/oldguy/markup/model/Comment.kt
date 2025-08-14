package com.oldguy.markup.model

data class Comment(
    val text: String
): Model("Comment") {

    companion object {
        val start = "<!--"
        val stop = "--!>"
    }
}