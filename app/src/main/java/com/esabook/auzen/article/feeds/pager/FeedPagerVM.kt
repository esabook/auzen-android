package com.esabook.auzen.article.feeds.pager

import android.util.SparseArray
import androidx.core.util.containsKey
import androidx.core.util.isEmpty
import androidx.core.util.keyIterator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import androidx.sqlite.db.SimpleSQLiteQuery
import com.esabook.auzen.App
import com.esabook.auzen.article.feeds.FeedFilter
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.extentions.Debouncer
import com.esabook.auzen.extentions.collectLatest2
import com.esabook.auzen.extentions.relativeLocalizeDate2DayIndo
import com.esabook.auzen.extentions.toDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class FeedPagerVM : ViewModel() {


    val filters: SparseArray<FeedFilter> = SparseArray()

    val itemAdapter by lazy { FeedItemAdapter() }

    private fun generateFeeds() = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false, jumpThreshold = 60)
    ) {

        val whereFilter = arrayListOf<String>()

        if (feedsFilterQuery.isNotBlank()) {
            whereFilter.add("title LIKE '%$feedsFilterQuery%'")
        }

        if (!guidsWhiteList.isNullOrEmpty()) {
            whereFilter.add(
                "rss_guid IN (${
                    guidsWhiteList!!.joinToString(
                        ", ",
                        prefix = "'",
                        postfix = "'"
                    )
                })"
            )
        }

        if (filters.containsKey(FeedFilter.PLAYLIST.ordinal)) {
            whereFilter.add("is_playlist_queue = TRUE")
        }

        if (filters.containsKey(FeedFilter.READ.ordinal)
            && !filters.containsKey(FeedFilter.UNREAD.ordinal)
        ) {
            whereFilter.add("is_unread = FALSE")
        }
        if (filters.containsKey(FeedFilter.UNREAD.ordinal)
            && !filters.containsKey(FeedFilter.READ.ordinal)
        ) {
            whereFilter.add("is_unread = TRUE")
        }

        var allQuery = "SELECT * FROM article ORDER by pub_date_timestamp DESC"
        if (whereFilter.isNotEmpty()) {
            allQuery = allQuery.replace(
                "ORDER",
                "WHERE ${whereFilter.filter(String::isNotBlank).joinToString(" AND ")} ORDER"
            )
        }

        Timber.d(allQuery)

        val query = SimpleSQLiteQuery(allQuery)
        App.db.articleDao().getAllByQuery(query)

    }.flow
        .map { data ->
            data.map { article ->
                FeedListItem.Item(article)

            }.insertSeparators { before: FeedListItem?, after: FeedListItem? ->
                return@insertSeparators withContext(Dispatchers.Default) {
                    if (before == null && after == null) {
                        // List is empty after fully loaded; return null to skip adding separator.
                        null
                    } else if (after == null) {
                        // Footer; return null here to skip adding a footer.
                        null
                    } else if (before == null && after is FeedListItem.Item) {
                        // Header
                        val dateLabel = after.articleEntity.pubDate?.toDate()
                            ?.relativeLocalizeDate2DayIndo()
                        FeedListItem.Separator(dateLabel)
                    } else if (before is FeedListItem.Item
                        && after is FeedListItem.Item
                    ) {
                        val beforeDate = before.articleEntity.pubDate?.toDate()
                            ?.relativeLocalizeDate2DayIndo()

                        val afterDate = after.articleEntity.pubDate?.toDate()
                            ?.relativeLocalizeDate2DayIndo()
                        // Between two items that start with different letters.
                        if (beforeDate != afterDate) {
                            FeedListItem.Separator(afterDate)
                        } else
                            null
                    } else {
                        // Between two items that start with the same letter.
                        null
                    }
                }
            }
        }.cachedIn(viewModelScope)

    private var feedsFilterQuery: String = ""

    val searchQueryAction = Debouncer(viewModelScope).create<String>(500) { s ->
        if (s == feedsFilterQuery)
            return@create

        feedsFilterQuery = s
        invalidateDataList()
    }

    var guidsWhiteList: List<String>? = null
        set(value) {
            if (field == value)
                return

            viewModelScope.launch(Dispatchers.IO) {
                field = value
                invalidateDataList()
            }
        }

    private val latestItemWatcherJob = Debouncer(viewModelScope).create(500) {
        generateFeeds().collectLatest2 {
            itemAdapter.submitData(it)
        }
    }

    fun invalidateDataList() {
        latestItemWatcherJob.invoke()
    }

    private fun isInFilter(article: ArticleEntity): Boolean {
        var isInFilter = filters.isEmpty()
        if (isInFilter)
            return true

        for (key in filters.keyIterator()) {
            if (isInFilter)
                return true

            isInFilter = when (key) {
                FeedFilter.PLAYLIST.ordinal -> article.isPlayListQueue
                FeedFilter.READ.ordinal -> article.isUnread.not()
                FeedFilter.UNREAD.ordinal -> article.isUnread
                else -> false
            }
        }

        Timber.d("isInFilter: ret = $isInFilter, playlist = ${article.isPlayListQueue}, unread = ${article.isUnread}")

        return isInFilter
    }

    private fun isInFilterGuid(article: ArticleEntity): Boolean {
        return guidsWhiteList.isNullOrEmpty() || guidsWhiteList?.contains(article.rssGuid) == true
    }

    private fun isInFilterQuery(article: ArticleEntity): Boolean {
        if (feedsFilterQuery.isBlank())
            return true

        return article.title?.contains(feedsFilterQuery, true) == true
    }

    fun invalidateItem(pos: Int, new: ArticleEntity, onFinish: Runnable) {
        viewModelScope.launch {
            if (isInFilter(new).not()) {
                invalidateDataList()
                onFinish.run()
                return@launch
            }

            withContext(Dispatchers.IO) {
                itemAdapter.snapshot()
                    .firstOrNull {
                        it is FeedListItem.Item && it.articleEntity.guid == new.guid
                    }?.apply {
                        if (this is FeedListItem.Item) {
                            articleEntity.isUnread = new.isUnread
                            articleEntity.isPlayListQueue = new.isPlayListQueue
                        }
                    }
            }

            itemAdapter.notifyItemChanged(pos, new)
            onFinish.run()
        }
    }

    fun delete(a: ArticleEntity) {
        App.db.launchIo {
            articleDao().delete(a)
        }
    }
}