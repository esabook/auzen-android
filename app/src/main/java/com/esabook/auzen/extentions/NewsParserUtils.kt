package com.esabook.auzen.extentions

import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import com.esabook.auzen.data.api.Api.response
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.parser.AuzenArticleParser
import com.esabook.auzen.parser.GoogleNewsArticleDecoder
import com.esabook.auzen.parser.ParserConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Article
import timber.log.Timber
import java.nio.charset.Charset
import java.util.Date
import java.util.UUID

object NewsParserUtils {

    private fun String?.isGoogleUrl(): Boolean {
        return this?.contains(".google.") == true
    }

    suspend fun getArticle(link: String): Article? = withContext(Dispatchers.IO) {
        try {
            val id = link.length
            var allPageLink = appendUrlForAllPage(link)
            var response = response(url = allPageLink)
            val redirectedLink = response.request.url.toString()


            if (redirectedLink.isGoogleUrl()) {
                val peekBody = response.peekBody(Long.MAX_VALUE).byteString()
                val startKey = peekBody.indexOf("data-n-au".toByteArray(), 0)

                if (startKey > 0) {
                    Timber.d("start parsing with data-n-au")

                    val endKey = peekBody.indexOf("\" ".toByteArray(), startKey)
                    val url = peekBody.substring(startKey, endKey)
                        .string(Charset.defaultCharset())
                        .removePrefix("data-n-au=\"")
                    if (url.isNotBlank() && (url != redirectedLink || url != link)) {
                        Timber.d("%s url: %s", id, url)
                        return@withContext getArticle(url)
                    }

                } else {
                    Timber.d("start parsing with GoogleNewsArticleDecoder url: %s", link)
                    val newsDecoded = GoogleNewsArticleDecoder.decodeGoogleNewsUrl(link)
                    if (newsDecoded["status"] == true) {
                        val url = newsDecoded["decoded_url"] as String

                        if (url.isNotBlank() && (url != redirectedLink || url != link)) {
                            Timber.d("%s url: %s", id, url)
                            return@withContext getArticle(url)
                        }
                    } else {
                        Timber.e(Gson().toJson(newsDecoded))
                    }
                }

            }

            if (link != redirectedLink) {
                allPageLink = appendUrlForAllPage(redirectedLink)
                response = response(url = allPageLink)
            }

            Timber.d("%s url: %s", id, redirectedLink)
            if (redirectedLink.isGoogleUrl())
                return@withContext null

            val content = response.body.string()
            getArticle(redirectedLink, content)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }

    }


    private fun appendUrlForAllPage(link: String): String {
        val ret = StringBuilder(link)
        ParserConfig.findParserDictOrNull(link)?.let {
            if (it.allPageParam.isNotBlank()) {
                ret.append(
                    if (link.contains("?"))
                        it.allPageParam
                    else
                        """?${it.allPageParam}"""
                )
            }
        }
        return ret.toString()
    }

    private suspend fun getArticle(link: String, dom: String): Article? =
        withContext(Dispatchers.IO) {
        try {
            AuzenArticleParser(link, dom).parse()
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    suspend fun Article.toSpeakable(): List<String> = withContext(Dispatchers.IO) {
        StringBuilder()
            .append("Judul: ").append(title).append("\n")
            .toString()
            .plus(content?.parseAsHtml())
            .split("\n", "\r", "\r\n")
            .filterNot { it.isBlank() }
    }

    suspend fun Article.toArticleEntity(): ArticleEntity = withContext(Dispatchers.IO) {
        val description = excerpt?.take(360) ?: textContent?.take(360)
        val date = Date()
        val articleEntity = ArticleEntity(
            UUID.nameUUIDFromBytes(title?.toByteArray()).toString(),
            "External",
            title,
            uri,
            description,
            byline,
            date.toStringWithPattern(),
            date.time,
            null,
            uri.toUri().host,
            uri.toUri().host
        )
        articleEntity
    }

    fun getFaviconUrl(link: String) = "https://www.google.com/s2/favicons?domain=${link}&sz=96"
}