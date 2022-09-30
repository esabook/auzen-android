package com.esabook.auzen.parser

import net.dankito.readability4j.extended.util.RegExUtilExtended
import java.util.regex.Pattern

class AuzenRegexUtil: RegExUtilExtended() {
    companion object {
        const val unlikelyCandidate =
            UnlikelyCandidatesDefaultPattern.plus("|linksisip|paradetail|donasi$|paradetail")
        const val byLine = BylineDefaultPattern.plus("|read__credit|penulis|")
        const val positiveCandidate =
            PositiveDefaultPattern.plus("|read__time|cover-photo|pagination__box|")
    }

    val unlikelyCandidataPattern = Pattern.compile(
        unlikelyCandidate.plus(ParserConfig.unlikelyCandidate),
        Pattern.CASE_INSENSITIVE
    )
    val byLinePattern = Pattern.compile(byLine.plus(ParserConfig.byLine), Pattern.CASE_INSENSITIVE)
    val positivePattern = Pattern.compile(
        positiveCandidate.plus(ParserConfig.positiveCandidate),
        Pattern.CASE_INSENSITIVE
    )

    override fun isUnlikelyCandidate(matchString: String): Boolean {
        return unlikelyCandidataPattern.matcher(matchString).find()
    }

    override fun isByline(matchString: String): Boolean {
        return byLinePattern.matcher(matchString).find()
    }

    override fun isPositive(matchString: String): Boolean {
        return positivePattern.matcher(matchString).find()
    }
}