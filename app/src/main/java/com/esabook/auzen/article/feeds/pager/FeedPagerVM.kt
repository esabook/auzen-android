package com.esabook.auzen.article.feeds.pager

import android.util.SparseArray
import androidx.core.util.isEmpty
import androidx.core.util.keyIterator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.esabook.auzen.App
import com.esabook.auzen.article.feeds.FeedFilter
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.extentions.collectLatest2
import com.esabook.auzen.extentions.relativeLocalizeDate2DayIndo
import com.esabook.auzen.extentions.toDate
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber

class FeedPagerVM : ViewModel() {


    val filters: SparseArray<FeedFilter> = SparseArray()

    val itemAdapter by lazy { FeedItemAdapter() }

    private fun generateFeeds() = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = true)
    ) {
        App.db.articleDao().getAll()
    }.flow
        .map { data ->
            data.filter {
                if (feedsFilterQuery.isBlank()
                    && guidsWhiteList.isNullOrEmpty()
                    && filters.isEmpty()
                )
                    return@filter true

                val isTitleInQuery = isInFilterQuery(it)
                if (isTitleInQuery.not())
                    return@filter false

                val isGuidInWhiteList = isInFilterGuid(it)
                if (isGuidInWhiteList.not())
                    return@filter false

                val isMatchWithFilter = isInFilter(it)
                if (isMatchWithFilter.not())
                    return@filter false

                return@filter true

            }.map { article ->
                FeedListItem.Item(article)

            }.insertSeparators { before: FeedListItem?, after: FeedListItem? ->
                return@insertSeparators if (before == null && after == null) {
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
        }.cachedIn(viewModelScope)

    val feeds: Flow<PagingData<FeedListItem>> = generateFeeds()

    private var feedsFilterQuery: String = ""

    val searchQueryAction = Channel<String>().also {
        viewModelScope.launch(Dispatchers.IO) {
            it.consumeAsFlow().debounce(500).collectLatest2 { s ->
                if (s == feedsFilterQuery)
                    return@collectLatest2

                withContext(Dispatchers.IO) {
                    feedsFilterQuery = s
                    invalidateDataList()
                }
            }
        }
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


    fun invalidateDataList() {
        viewModelScope.launch(Dispatchers.IO) {
            generateFeeds().take(1).collectLatest2 {
                adapterSubmitList(it)
            }
        }

    }

    private fun isInFilter(article: ArticleEntity): Boolean {
        var isInFilter = filters.isEmpty()
        if (isInFilter)
            return true

        for (key in filters.keyIterator()) {
            if (isInFilter)
                return true

            isInFilter = when (key) {
                FeedFilter.PLAYLIST.ordinal -> isInFilter || article.isPlayListQueue
                FeedFilter.READ.ordinal -> isInFilter || article.isUnread.not()
                FeedFilter.UNREAD.ordinal -> isInFilter || article.isUnread
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

    var filteringJob: Job? = null
    fun adapterSubmitList(aes: PagingData<FeedListItem>, lifecycle: Lifecycle? = null) {
        filteringJob?.cancel()
        filteringJob = viewModelScope.launch {
            try {
                if (lifecycle == null)
                    itemAdapter.submitData(aes)
                else
                    itemAdapter.submitData(lifecycle, aes)

                Timber.v("==" + aes.hashCode())
            } catch (e: Exception) {
                Timber.e(e)
            }

        }
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