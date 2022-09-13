package com.esabook.auzen.extentions

import com.esabook.auzen.data.api.Api
import com.esabook.auzen.data.db.entity.OpenGraphEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import timber.log.Timber

object OgParser {
    private val DOC_SELECT_QUERY = "meta[property^=og:]"
    private val OPEN_GRAPH_KEY = "content"
    private val PROPERTY = "property"
    private val OG_IMAGE = "og:image"
    private val OG_DESCRIPTION = "og:description"
    private val OG_URL = "og:url"
    private val OG_TITLE = "og:title"
    private val OG_SITE_NAME = "og:site_name"
    private val OG_TYPE = "og:type"

    private val REFERRER = "http://www.google.com"
    private val TIMEOUT = 100000

    private val AGENTS = mutableListOf(
        "Mozilla",
        "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.181 Safari/537.36",
        "WhatsApp/2.19.81 A",
        "facebookexternalhit/1.1",
        "facebookcatalog/1.0"
    )


    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun getOgEntity(link: String?): OpenGraphEntity? =
        withContext(Dispatchers.IO) {
            if (link == null) {
                return@withContext null
            }

            return@withContext try {
                val openGraphEntity = OpenGraphEntity(
                    null, null,
                    null, null,
                    null, null
                )

                val docResponse = Api.response(url = link)

                val doc = Jsoup.parse(docResponse.body.byteStream(),
                    null,
                    link)

                val ogTags = doc.select(DOC_SELECT_QUERY)
                ogTags.forEachIndexed { index, _ ->
                    val tag = ogTags[index]
                    when (tag.attr(PROPERTY)) {
                        OG_IMAGE -> {
                            openGraphEntity.image = (tag.attr(OPEN_GRAPH_KEY))
                        }
                        OG_DESCRIPTION -> {
                            openGraphEntity.description = (tag.attr(OPEN_GRAPH_KEY))
                        }
                        OG_URL -> {
                            openGraphEntity.url = (tag.attr(OPEN_GRAPH_KEY))
                        }
                        OG_TITLE -> {
                            openGraphEntity.title = (tag.attr(OPEN_GRAPH_KEY))
                        }
                        OG_SITE_NAME -> {
                            openGraphEntity.siteName = (tag.attr(OPEN_GRAPH_KEY))
                        }
                        OG_TYPE -> {
                            openGraphEntity.type = (tag.attr(OPEN_GRAPH_KEY))
                        }
                    }
                }
                openGraphEntity

            } catch (e: Exception) {
                Timber.e(e)
                null
            }

        }
}