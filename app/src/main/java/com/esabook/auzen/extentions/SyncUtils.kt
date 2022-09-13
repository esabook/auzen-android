package com.esabook.auzen.extentions

import com.esabook.auzen.data.db.entity.RssEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber

suspend fun List<RssEntity>.doSync(onFinish: Runnable? = null){
    try {
        withContext (Dispatchers.IO) {
            forEach { r ->
                if (isActive) {
                    r.link?.let { link ->
                        OpmlParseUtils.getArticleEntries(link).let { s ->
                            if (isActive) {
                                OpmlParseUtils.saveArticle(r, s.entries)
                            }
                        }

                    }
                }

            }
            onFinish?.run()
        }
    } catch (e: Exception) {
        Timber.e(e)
        onFinish?.run()
    }

}