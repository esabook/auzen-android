package com.esabook.auzen.parser

import net.dankito.readability4j.processor.Preprocessor
import net.dankito.readability4j.util.RegExUtil
import org.jsoup.nodes.Document

class AuzenPreprocessor(val link: String, regEx: RegExUtil) : Preprocessor(regEx) {
    override fun prepareDocument(document: Document) {
        super.prepareDocument(document)

        ParserConfig.findParserDictOrNull(link)?.let {
            val content = document.select(it.xpathContentMatcher)
            document.body().apply {
                children().remove()
                append(content.outerHtml())
            }
        }
    }
}