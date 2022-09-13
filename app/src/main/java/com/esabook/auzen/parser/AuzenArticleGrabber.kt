package com.esabook.auzen.parser

import net.dankito.readability4j.extended.processor.ArticleGrabberExtended
import net.dankito.readability4j.extended.util.RegExUtilExtended
import net.dankito.readability4j.model.ReadabilityOptions

class AuzenArticleGrabber(
    options: ReadabilityOptions = ReadabilityOptions(),
    regExExtended: RegExUtilExtended = AuzenRegexUtil()
) : ArticleGrabberExtended(options, regExExtended) {
}