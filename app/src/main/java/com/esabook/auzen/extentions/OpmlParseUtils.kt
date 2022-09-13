package com.esabook.auzen.extentions

import com.esabook.auzen.App
import com.esabook.auzen.data.api.Api
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.data.db.entity.RssEntity
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import java.util.*

object OpmlParseUtils {
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getArticleEntries(link: String): SyndFeed {
        return withContext(Dispatchers.IO) {
            val feed: SyndFeed = SyndFeedInput().build(XmlReader(Api.inputStream(Api.okHttpClient, link)))
            feed
        }
    }


    suspend fun saveArticle(rssEntity: RssEntity, e: List<SyndEntry>) {
        withContext(Dispatchers.IO) {
            try {
                val rssGuid = rssEntity.guid
                val articles = e.map {
                    val pubDate = (it.publishedDate ?: it.updatedDate ?: Date())
                    val title = it.title?.removeNewLine()?.replace(" - ${it.source?.title}", "")
                    val thumbnail = it.enclosures?.getOrNull(0)?.url
                    val description = it.description?.value?.take(360)
                    val source = it.source?.link ?: it.link?.toHttpUrlOrNull()?.host?.substringAfter("www.")
                    ArticleEntity(
                        UUID.nameUUIDFromBytes(title?.toByteArray()).toString(),
                        rssGuid,
                        title,
                        it.link,
                        description,
                        it.author,
                        pubDate.toStringWithPattern(),
                        pubDate.time,
                        thumbnail,
                        source,
                        it.source?.title ?: source
                    )
                }
                App.db.articleDao().insertAll(*articles.toTypedArray())
            } catch (e: Exception) {
                Timber.e(e)
            }

        }
    }
}