package com.esabook.auzen.article.subscription

import androidx.lifecycle.ViewModel
import com.esabook.auzen.App
import com.esabook.auzen.data.db.entity.RssEntity
import com.esabook.auzen.extentions.collectLatest2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RssCollectionVM : ViewModel() {
    val rssAdapter = RssCollectionItemAdapter()
    val rssEntries: Flow<List<RssEntity>> = App.db.rssDao().getAll()

    suspend fun fillAdapter() {
        rssEntries.collectLatest2 {
            rssAdapter.submitList(it)
        }
    }

    suspend fun invalidateTotalArticle() {
        withContext(Dispatchers.IO) {
            rssEntries.collectLatest2 { rssEntry ->
                rssEntry.forEach { rss ->
                    val bool = App.db.articleDao().getIsUnreadStatusByRssGuid(rss.guid)
                    val total = bool.size
                    val totalUnread = bool.filter { it }.size

                    if (total == rss.totalEntry && totalUnread == rss.totalEntryUnread)
                        return@forEach

                    val rssEntity = rss.copy(totalEntryUnread = totalUnread, totalEntry = total)
                    App.db.rssDao().update(rssEntity)
                }
            }
            fillAdapter()
        }
    }
}