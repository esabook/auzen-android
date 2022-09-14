package com.esabook.auzen.extentions

import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


val defaultDatePatterns = arrayOf(
    "EEE, d MMM yyyy HH:mm:ss z",
    "EEE, dd MMM yyyy HH:mm:ss z",
    "EEE, dd MMM yyyy HH:mm:ss 'GMT'",

    )

val dateFormat = SimpleDateFormat(defaultDatePatterns[0], Locale.ENGLISH).apply {
    isLenient = false
}


fun String.toDate(vararg patterns: String = defaultDatePatterns): Date? {
    for (p in patterns) {
        try {
            return dateFormat.apply { applyPattern(p) }.parse(this@toDate)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    return null
}

fun Date.toStringWithPattern(
    patterns: String = defaultDatePatterns[0],
    useIndo: Boolean = false
): String {
    return try {
        if (useIndo)
            getSimpleDateFormatIndo(patterns).format(this)
        else
            dateFormat.apply { applyPattern(patterns) }.format(this@toStringWithPattern)
    } catch (e: Exception) {
        Timber.e(e)
        toString()
    }
}