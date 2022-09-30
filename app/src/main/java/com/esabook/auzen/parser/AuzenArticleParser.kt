package com.esabook.auzen.parser

import net.dankito.readability4j.Article
import net.dankito.readability4j.extended.Readability4JExtended
import timber.log.Timber

class AuzenArticleParser(val link: String, val html: String) {
    fun parse(): Article? {
        return try {
            val regexUtil = AuzenRegexUtil()
            val readability4J = Readability4JExtended(
                link,
                html,
                regExUtil = regexUtil,
                preprocessor = AuzenPreprocessor(link, regexUtil),
                articleGrabber = AuzenArticleGrabber()
            )

            readability4J.parse()
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }


}