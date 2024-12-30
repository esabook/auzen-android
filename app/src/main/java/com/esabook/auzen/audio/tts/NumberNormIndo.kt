package com.esabook.auzen.audio.tts


// Borrowed from https://rosettacode.org/wiki/Spelling_of_ordinal_numbers
object NumberNormIndo {
    private val ordinalMap: MutableMap<String?, String> = HashMap()

    init {
        ordinalMap["one"] = "pertama"
    }

    fun toOrdinal(n: Long): String {
        val spelling = numToString(n)
        val split: Array<String?> = spelling.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val last = split[split.size - 1]
        val replace: String?
        if (last!!.contains("-")) {
            val lastSplit = last.split("-".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val lastWithDash = lastSplit[1]
            val lastReplace = if (ordinalMap.containsKey(lastWithDash)) {
                ordinalMap[lastWithDash]
            } else {
                "ke $lastWithDash"
            }
            replace = lastSplit[0] + "-" + lastReplace
        } else {
            replace = if (ordinalMap.containsKey(last)) {
                ordinalMap[last]
            } else {
                "ke $last"
            }
        }
        split[split.size - 1] = replace
        return java.lang.String.join(" ", *split)
    }

    private val nums = arrayOf(
        "nol",
        "satu",
        "dua",
        "tiga",
        "empat",
        "lima",
        "enam",
        "tujuh",
        "delapan",
        "sembilan",
        "sepuluh",
        "sebelas"
    )

    private val tens = arrayOf(
        "nol",
        "sepuluh",
        "dua puluh",
        "tiga puluh",
        "empat puluh",
        "lima puluh",
        "enam puluh",
        "tujuh puluh",
        "delapan puluh",
        "sembilan puluh"
    )

    fun numToString(n: Long): String {
        return numToStringHelper(n)
    }

    private fun numToStringHelper(n: Long): String {
        if (n < 0) {
            return "negatif " + numToStringHelper(-n)
        }

        val index = n.toInt()

        if (n <= 19) {
            if (n > 11)
                return nums[index] + "belas"

            return nums[index]
        }

        if (n <= 99) {
            return tens[index / 10] + (if (n % 10 > 0) "-" + numToStringHelper(n % 10) else "")
        }

        var label: String? = null
        var factor: Long = 0
        if (n <= 999) {
            label = "ratus"
            factor = 100
        } else if (n <= 999999) {
            label = "ribu"
            factor = 1000
        } else if (n <= 999999999) {
            label = "juta"
            factor = 1000000
        } else if (n <= 999999999999L) {
            label = "miliyar"
            factor = 1000000000
        } else if (n <= 999999999999999L) {
            label = "triliun"
            factor = 1000000000000L
        } else if (n <= 999999999999999999L) {
            label = "quadrillion"
            factor = 1000000000000000L
        } else {
            label = "quintillion"
            factor = 1000000000000000000L
        }
        return numToStringHelper(n / factor) + " " + label + (if (n % factor > 0) " " + numToStringHelper(
            n % factor
        ) else "")
    }
}