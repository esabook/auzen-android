package com.esabook.auzen.extentions

import androidx.core.net.toUri
import androidx.core.text.parseAsHtml
import com.esabook.auzen.data.api.Api.response
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.parser.AuzenArticleGrabber
import com.esabook.auzen.parser.AuzenRegexUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Article
import net.dankito.readability4j.extended.Readability4JExtended
import timber.log.Timber
import java.util.*

object NewsParserUtils {

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getArticle(link: String): Article? = withContext(Dispatchers.IO) {
        try {
            val content = response(url = link).body.string()
            val readability4J = Readability4JExtended(link, content, regExUtil = AuzenRegexUtil(), articleGrabber = AuzenArticleGrabber())

            readability4J.parse()
        } catch (e: Exception) {
            Timber.e(e)
            null
        }

    }

    suspend fun Article.toSpeakable(): List<String> = withContext(Dispatchers.IO) {
        contentWithUtf8Encoding
            ?.parseAsHtml()
            .toString()
            .split("\n", "\r", "\r\n")
            .filterNot { it.isBlank() }
    }

    suspend fun Article.toArticleEntity(): ArticleEntity = withContext(Dispatchers.IO){
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

    fun getFaviconUrl(link: String) =  "https://www.google.com/s2/favicons?domain=${link}&sz=96"
}