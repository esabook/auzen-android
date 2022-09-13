package com.esabook.auzen.extentions

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs


val longMonthNamesIndo = arrayOf(
    "Januari",
    "Februari",
    "Maret",
    "April",
    "Mei",
    "Juni",
    "Juli",
    "Agustus",
    "September",
    "Oktober",
    "November",
    "Desember"
)
val shortMonthNamesIndo = arrayOf(
    "Jan",
    "Feb",
    "Mar",
    "Apr",
    "Mei",
    "Jun",
    "Jul",
    "Agu",
    "Sep",
    "Okt",
    "Nov",
    "Des"
)
val weekDaysINDLong = arrayOf("Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
val weekDaysINDShort = arrayOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")

fun getSimpleDateFormatIndo(pattern: String): SimpleDateFormat {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    sdf.isLenient = true

    val dfs = sdf.dateFormatSymbols
    dfs.months = longMonthNamesIndo
    dfs.shortMonths = shortMonthNamesIndo
    dfs.weekdays = weekDaysINDLong
    dfs.shortWeekdays = weekDaysINDShort
    sdf.dateFormatSymbols = dfs
    return sdf
}

/**
 * 1. dibawah 1 jam, tampilin menit (contoh: 59 menit yang lalu)
 * 2. dibawah <24 jam, tampilin per jam (contoh 2 jam yang lalu). Kalau 1 jam 29 menit itu 1 jam, 1 jam 30 menit itu 2 jam
 * 3. dibawah 1 minggu, hari (contoh kemarin, 2 hari yang lalu, 5 hari yang lalu)
 * 4. diatas 1 minggu, tanggal (contoh 11 Mei)
 * 5. beda tahun, DD MMM YYYY (contoh 11 Mei 2020)
 */

//fun Date.relativeLocalizeDateIndo(): String {
//    val milliseconds = time - System.currentTimeMillis()
//    val positiveValue = abs(milliseconds)
//    val now = Date().time
//
//    val prePostSuffix = if (time > now) "lagi" else "yang lalu"
//    return when {
//        this.year != Date().year -> getSimpleDateFormatIndo("dd MMM yyyy").format(this)
//        positiveValue > DateUtils.WEEK_IN_MILLIS -> getSimpleDateFormatIndo("dd MMMM").format(this)
//        positiveValue > DateUtils.DAY_IN_MILLIS -> "${TimeUnit.MILLISECONDS.toDays(positiveValue)} hari $prePostSuffix"
//        positiveValue > DateUtils.HOUR_IN_MILLIS -> "${
//            abs(
//                (TimeUnit.MILLISECONDS.toMinutes(
//                    positiveValue
//                ) / 90).toInt()
//            ) + 1
//        } jam $prePostSuffix"
//        positiveValue > DateUtils.MINUTE_IN_MILLIS -> "${
//            TimeUnit.MILLISECONDS.toMinutes(
//                positiveValue
//            )
//        } menit $prePostSuffix"
//        else -> "Baru saja"
//    }
//}

/**
 * hari ini
 * kemarin
 * Juma't, 9 September 2022
 */
fun Date.relativeLocalizeDate2DayIndo(): String {
    val milliseconds = time - System.currentTimeMillis()
    val positiveValue = abs(milliseconds)
    val now = Date().time

    val prePostSuffix = if (time > now) "lagi" else "yang lalu"
    return when {
        positiveValue > DateUtils.WEEK_IN_MILLIS -> this.toStringWithPattern("EEEE, dd MMM yyyy")
        positiveValue > DateUtils.DAY_IN_MILLIS -> "${TimeUnit.MILLISECONDS.toDays(positiveValue)} hari $prePostSuffix"
        else -> "Hari ini"
    }
}

