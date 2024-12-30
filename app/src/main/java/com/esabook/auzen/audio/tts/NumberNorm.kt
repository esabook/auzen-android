package com.esabook.auzen.audio.tts


// Borrowed from https://rosettacode.org/wiki/Spelling_of_ordinal_numbers
object NumberNorm {
    private val ordinalMap: MutableMap<String?, String> = HashMap()

    init {
        ordinalMap["one"] = "first"
        ordinalMap["two"] = "second"
        ordinalMap["three"] = "third"
        ordinalMap["five"] = "fifth"
        ordinalMap["eight"] = "eighth"
        ordinalMap["nine"] = "ninth"
        ordinalMap["twelve"] = "twelfth"
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
            } else if (lastWithDash.endsWith("y")) {
                lastWithDash.substring(0, lastWithDash.length - 1) + "ieth"
            } else {
                lastWithDash + "th"
            }
            replace = lastSplit[0] + "-" + lastReplace
        } else {
            replace = if (ordinalMap.containsKey(last)) {
                ordinalMap[last]
            } else if (last.endsWith("y")) {
                last.substring(0, last.length - 1) + "ieth"
            } else {
                last + "th"
            }
        }
        split[split.size - 1] = replace
        return java.lang.String.join(" ", *split)
    }

    private val nums = arrayOf(
        "zero",
        "one",
        "two",
        "three",
        "four",
        "five",
        "six",
        "seven",
        "eight",
        "nine",
        "ten",
        "eleven",
        "twelve",
        "thirteen",
        "fourteen",
        "fifteen",
        "sixteen",
        "seventeen",
        "eighteen",
        "nineteen"
    )

    private val tens = arrayOf(
        "zero",
        "ten",
        "twenty",
        "thirty",
        "forty",
        "fifty",
        "sixty",
        "seventy",
        "eighty",
        "ninety"
    )

    fun numToString(n: Long): String {
        return numToStringHelper(n)
    }

    private fun numToStringHelper(n: Long): String {
        if (n < 0) {
            return "negative " + numToStringHelper(-n)
        }
        val index = n.toInt()
        if (n <= 19) {
            return nums[index]
        }
        if (n <= 99) {
            return tens[index / 10] + (if (n % 10 > 0) "-" + numToStringHelper(n % 10) else "")
        }
        var label: String? = null
        var factor: Long = 0
        if (n <= 999) {
            label = "hundred"
            factor = 100
        } else if (n <= 999999) {
            label = "thousand"
            factor = 1000
        } else if (n <= 999999999) {
            label = "million"
            factor = 1000000
        } else if (n <= 999999999999L) {
            label = "billion"
            factor = 1000000000
        } else if (n <= 999999999999999L) {
            label = "trillion"
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