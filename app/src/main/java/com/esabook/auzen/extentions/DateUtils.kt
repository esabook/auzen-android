package com.esabook.auzen.extentions

import android.annotation.SuppressLint
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*


val defaultDatePatterns = arrayOf(
    "EEE, d MMM yyyy HH:mm:ss z",
    "EEE, dd MMM yyyy HH:mm:ss z",
    "EEE, dd MMM yyyy HH:mm:ss 'GMT'",

)

@SuppressLint("SimpleDateFormat")
fun String.toDate(vararg patterns: String = defaultDatePatterns): Date? {
    for (p in patterns) {
        try {
            val df = SimpleDateFormat(p)
            return df.parse(this)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    return null
}

@SuppressLint("SimpleDateFormat")
fun Date.toStringWithPattern(patterns: String = defaultDatePatterns[0]): String {
    return try {
        getSimpleDateFormatIndo(patterns).format(this)
    } catch (e: Exception) {
        Timber.e(e)
        toString()
    }
}