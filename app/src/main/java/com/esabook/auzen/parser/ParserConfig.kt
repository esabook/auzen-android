package com.esabook.auzen.parser

import com.esabook.auzen.data.db.entity.ParserDictEntity
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import java.util.regex.Pattern

object ParserConfig {
    const val parserRemoteUrl =
        "https://raw.githubusercontent.com/esabook/auzen-android/main/config/parserDict.json"

    var unlikelyCandidate = ""
    var byLine = ""
    var positiveCandidate = ""

    val parserDicts = mutableListOf<ParserDictEntity>()

    private fun getParserDict() =
        parserDicts //+ ParserDictEntity("news.detik.com", "div.detail__body-text")

    fun findParserDictOrNull(link: String): ParserDictEntity? {
        if (getParserDict().isEmpty())
            return null

        val host = link.toHttpUrlOrNull()?.host
            ?: return null

        Timber.d(host)
        return getParserDict().firstOrNull {
            if (it.domainMatcher.isBlank())
                return@firstOrNull false

            try {
                //eg. "^*news.tv".toRegex().toPattern().matcher(host).find()
                Pattern.compile(it.domainMatcher, Pattern.CASE_INSENSITIVE)
                    .matcher(host)
                    .find()

            } catch (e: Exception) {
                Timber.e(e)
                false
            }

        }?.also {
            Timber.d("domainMatcher" + it.domainMatcher)

        }
    }
}