package com.oldguy.markup

/**
 * Thrown for all sorts of errors related to parsing XML files.
 */
class ParseException(
    message: String,
    val line: Int = 0,
    val column: Int = 0
): Exception("line $line:column $column, message: $message") {
}