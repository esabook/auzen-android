package com.esabook.auzen.parser

import net.dankito.readability4j.extended.util.RegExUtilExtended
import java.util.regex.Pattern

class AuzenRegexUtil: RegExUtilExtended() {
    companion object{
        const val unlikelyCandidate = UnlikelyCandidatesDefaultPattern.plus("|linksisip|paradetail")
        const val byLine = BylineDefaultPattern.plus("|read__credit|penulis")
        const val nextPage = NextLinkDefaultPattern.plus("|selanjutnya")
        const val positiveCandidate = PositiveDefaultPattern.plus("|read__time|cover-photo")
        const val keepImageCandidate = "|photo__wrap"
    }

    val unlikelyCandidataPattern = Pattern.compile(unlikelyCandidate, Pattern.CASE_INSENSITIVE)
    val byLinePattern = Pattern.compile(byLine, Pattern.CASE_INSENSITIVE)
    val positivePattern = Pattern.compile(positiveCandidate, Pattern.CASE_INSENSITIVE)
    val nextPagePattern = Pattern.compile(nextPage, Pattern.CASE_INSENSITIVE)
    val keepImagePattern = Pattern.compile(keepImageCandidate, Pattern.CASE_INSENSITIVE)

    override fun isUnlikelyCandidate(matchString: String): Boolean {
        return unlikelyCandidataPattern.matcher(matchString).find()
    }

    override fun isByline(matchString: String): Boolean {
        return byLinePattern.matcher(matchString).find()
    }

    override fun isPositive(matchString: String): Boolean {
        return positivePattern.matcher(matchString).find()
    }
//
//    override fun keepImage(matchString: String): Boolean {
//        return super.keepImage(matchString) || keepImagePattern.matcher(matchString).find()
//    }
}