package com.esabook.auzen.extentions

import android.text.Spanned

fun String.removeNewLine(char: String = " ") = this.replace("[\n\r]".toRegex(), char)

fun Spanned.removeDebris(): String {
    return toString()
        .replace('\n', Char(32))
        .replace(Char(160), Char(32))
        .replace(Char(65532), Char(32))
        .trim()
}