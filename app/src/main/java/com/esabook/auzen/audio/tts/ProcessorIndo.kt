package com.esabook.auzen.audio.tts

import android.content.Context
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern


class ProcessorIndo(context: Context) {
    init {
        SYMBOLS.add(PAD)
        SYMBOLS.add(SPECIAL)

        for (p in PUNCTUATION) {
            if ("" != p) {
                SYMBOLS.add(p)
            }
        }

        for (l in LETTERS) {
            if ("" != l) {
                SYMBOLS.add(l)
            }
        }

        for (validSymbol in VALID_SYMBOLS) {
            SYMBOLS.add("@$validSymbol")
        }

        SYMBOLS.add(EOS)

        for (i in SYMBOLS.indices) {
            SYMBOL_TO_ID[SYMBOLS[i]] = i
        }


    }


    private fun symbolsToSequence(symbols: String): List<Int?> {
        val sequence: MutableList<Int?> = ArrayList()

        for (i in symbols.indices) {
            val id = SYMBOL_TO_ID[symbols[i].toString()]
            if (id == null) {
                Timber.e("symbolsToSequence: id is not found for " + symbols[i])
            } else {
                sequence.add(id)
            }
        }

        return sequence
    }

    private fun alphabetToSequence(symbols: String?): List<Int?> {
        val sequence: MutableList<Int?> = ArrayList()
        if (symbols != null) {
            val aSym = symbols.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            for (s in aSym) {
                sequence.add(SYMBOL_TO_ID["@$s"])
            }
        }
        return sequence
    }

    private fun convertToAscii(text: String): String {
        val bytes = text.toByteArray(StandardCharsets.US_ASCII)
        return String(bytes)
    }

    private fun collapseWhitespace(text: String): String {
        return text.replace("\\s+".toRegex(), " ")
    }

    private fun expandAbbreviations(text: String): String {
        var text = text
        for ((key, value) in ABBREVIATIONS) {
            text = text.replace("\\b$key\\.".toRegex(), value)
        }
        return text
    }

    private fun removeCommasFromNumbers(text: String): String {
        var text = text
        val m: Matcher = COMMA_NUMBER_RE.matcher(text)
        while (m.find()) {
            val s = m.group().replace(",".toRegex(), "")
            text = text.replaceFirst(m.group().toRegex(), s)
        }
        return text
    }

    private fun expandPounds(text: String): String {
        var text = text
        val m: Matcher = POUNDS_RE.matcher(text)
        while (m.find()) {
            text = text.replaceFirst(m.group().toRegex(), m.group() + " pound")
        }
        return text
    }

    private fun expandDollarsOrRupiah(text: String): String {
        var text = text
        val m = DOLLARS_RE.matcher(text)
        val rp = RUPIAH_RE.matcher(text)
        while (m.find()) {
            var dollars = "0"
            var cents = "0"
            var spelling = ""
            val s = m.group().substring(1)
            val parts = s.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            if (!s.startsWith(".")) {
                dollars = parts[0]
            }
            if (!s.endsWith(".") && parts.size > 1) {
                cents = parts[1]
            }
            if ("0" != dollars) {
                spelling += parts[0] + " dollar "
            }
            if ("0" != cents && "00" != cents) {
                spelling += parts[1] + " sen "
            }
            text = text.replaceFirst(("\\" + m.group()).toRegex(), spelling)
        }

        while (rp.find()) {
            // todo
        }
        return text
    }

    private fun expandDecimals(text: String): String {
        var text = text
        val m: Matcher = DECIMAL_RE.matcher(text)
        while (m.find()) {
            val s = m.group().replace("\\.".toRegex(), " koma ")
            text = text.replaceFirst(m.group().toRegex(), s)
        }
        return text
    }

    private fun expandOrdinals(text: String): String {
        var text = text
        val m: Matcher = ORDINAL_RE.matcher(text)
        while (m.find()) {
            val s = m.group().substring(0, m.group().length - 2)
            val l = s.toLong()
            val spelling: String = NumberNormIndo.toOrdinal(l)
            text = text.replaceFirst(m.group().toRegex(), spelling)
        }
        return text
    }

    private fun expandCardinals(text: String): String {
        var text = text
        val m: Matcher = NUMBER_RE.matcher(text)
        while (m.find()) {
            val l = m.group().toLong()
            val spelling: String = NumberNormIndo.numToString(l)
            text = text.replaceFirst(m.group().toRegex(), spelling)
        }
        return text
    }

    private fun expandNumbers(text: String): String {
        var text = text
        text = removeCommasFromNumbers(text)
        text = expandPounds(text)
        text = expandDollars(text)
        text = expandDecimals(text)
        text = expandOrdinals(text)
        text = expandCardinals(text)
        return text
    }

    private fun cleanTextForIndo(text: String): String {
        var mText = text
        mText = convertToAscii(mText)
        mText = mText.lowercase(Locale.getDefault())
        mText = expandAbbreviations(mText)
        try {
            mText = expandNumbers(mText)
        } catch (e: Exception) {
            Timber.d(e, "Failed to convert numbers")
        }
        mText = collapseWhitespace(mText)
        Timber.d("text preprocessed: " + mText)
        return mText
    }

    fun textToIds(text: String?): IntArray {
        var mText = text
        val sequence: MutableList<Int?> = ArrayList()
        while (!mText.isNullOrEmpty()) {
            val m: Matcher = CURLY_RE.matcher(mText)
            if (!m.find()) {
                sequence.addAll(symbolsToSequence(cleanTextForIndo(mText)))
                break
            }
            sequence.addAll(symbolsToSequence(cleanTextForIndo(m.group(1))))
            sequence.addAll(alphabetToSequence(m.group(2)))
            mText = m.group(3)
        }

        val size = sequence.size
        val tmp: Array<Int?> = sequence.toTypedArray()
        val ids = IntArray(size)
        for (i in 0 until size) {
            ids[i] = tmp[i]!!
        }
        return ids
    }

    companion object {

        private val VALID_SYMBOLS = arrayOf(
            "AA",
            "AA0",
            "AA1",
            "AA2",
            "AE",
            "AE0",
            "AE1",
            "AE2",
            "AH",
            "AH0",
            "AH1",
            "AH2",
            "AO",
            "AO0",
            "AO1",
            "AO2",
            "AW",
            "AW0",
            "AW1",
            "AW2",
            "AY",
            "AY0",
            "AY1",
            "AY2",
            "B",
            "CH",
            "D",
            "DH",
            "EH",
            "EH0",
            "EH1",
            "EH2",
            "ER",
            "ER0",
            "ER1",
            "ER2",
            "EY",
            "EY0",
            "EY1",
            "EY2",
            "F",
            "G",
            "HH",
            "IH",
            "IH0",
            "IH1",
            "IH2",
            "IY",
            "IY0",
            "IY1",
            "IY2",
            "JH",
            "K",
            "L",
            "M",
            "N",
            "NG",
            "OW",
            "OW0",
            "OW1",
            "OW2",
            "OY",
            "OY0",
            "OY1",
            "OY2",
            "P",
            "R",
            "S",
            "SH",
            "T",
            "TH",
            "UH",
            "UH0",
            "UH1",
            "UH2",
            "UW",
            "UW0",
            "UW1",
            "UW2",
            "V",
            "W",
            "Y",
            "Z",
            "ZH"
        )
        val ID_ALPHABET_MAPPING = mapOf(
            "a" to "a",
            "b" to "bé",
            "c" to "cé",
            "d" to "dé",
            "e" to "é",
            "f" to "èf",
            "g" to "gé",
            "h" to "ha",
            "i" to "i",
            "j" to "jé",
            "k" to "ka",
            "l" to "èl",
            "m" to "èm",
            "n" to "èn",
            "o" to "o",
            "p" to "pé",
            "q" to "ki",
            "r" to "èr",
            "s" to "ès",
            "t" to "té",
            "u" to "u",
            "v" to "vé",
            "w" to "wé",
            "x" to "èks",
            "y" to "yé",
            "z" to "zèt",
        )

        val ID_PHONETIC_MAPPING = mapOf(
            "sy" to "ʃ",
            "ny" to "ɲ",
            "ng" to "ŋ",
            "dj" to "dʒ",
            "'" to "ʔ",
            "c" to "tʃ",
            "é" to "e",
            "è" to "ɛ",
            "ê" to "ə",
            "g" to "ɡ",
            "I" to "ɪ",
            "j" to "dʒ",
            "ô" to "ɔ",
            "q" to "k",
            "U" to "ʊ",
            "v" to "f",
            "x" to "ks",
            "y" to "j",
        )

        private val CURLY_RE: Pattern = Pattern.compile("(.*?)\\{(.+?)\\}(.*)")
        private val COMMA_NUMBER_RE: Pattern = Pattern.compile("([0-9][0-9\\,]+[0-9])")
        private val DECIMAL_RE: Pattern = Pattern.compile("([0-9]+\\.[0-9]+)")
        private val POUNDS_RE: Pattern = Pattern.compile("£([0-9\\,]*[0-9]+)")
        private val DOLLARS_RE: Pattern = Pattern.compile("\\$([0-9.\\,]*[0-9]+)")
        private val RUPIAH_RE: Pattern = Pattern.compile("Rp([0-9.\\,]*[0-9]+)")
        private val ORDINAL_RE: Pattern = Pattern.compile("[0-9]+(st|nd|rd|th)")
        private val NUMBER_RE: Pattern = Pattern.compile("[0-9]+")

        private const val PAD = "_"
        private const val EOS = "~"
        private const val SPECIAL = "-"

        private val PUNCTUATION = "!'(),.:;? ".split("".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        private val LETTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".split("".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()

        private val SYMBOLS: MutableList<String> = ArrayList()
        private val ABBREVIATIONS = mapOf(
            "rp" to "rupiah",
            "mrs" to "misess",
            "mr" to "mister",
            "dr" to "doctor",
            "st" to "saint",
            "co" to "company",
            "jr" to "junior",
            "maj" to "major",
            "gen" to "general",
            "drs" to "doctors",
            "rev" to "reverend",
            "lt" to "lieutenant",
            "hon" to "honorable",
            "sgt" to "sergeant",
            "capt" to "captain",
            "esq" to "esquire",
            "ltd" to "limited",
            "col" to "colonel",
            "ft" to "fort",
        )
        private val SYMBOL_TO_ID: MutableMap<String, Int> = HashMap()
        private val ID_PHONEME = mutableMapOf<String, String>()
    }
}