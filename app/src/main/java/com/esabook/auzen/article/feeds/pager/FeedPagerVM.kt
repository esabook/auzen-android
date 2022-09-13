package com.esabook.auzen.article.feeds.pager

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import com.esabook.auzen.App
import com.esabook.auzen.article.feeds.FeedFilterType
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.extentions.collectLatest2
import com.esabook.auzen.extentions.relativeLocalizeDate2DayIndo
import com.esabook.auzen.extentions.toDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class FeedPagerVM : ViewModel() {


    val itemAdapter by lazy { FeedItemAdapter() }

    var filterType: FeedFilterType = FeedFilterType.ALL
        set(value) {
            field = value
            _feeds = generateFeeds()

        }

    private fun pageSource() = when (filterType) {
        FeedFilterType.ALL -> App.db.articleDao().getAll()
        FeedFilterType.PLAYLIST -> App.db.articleQueueDao().getAllPaged()
        FeedFilterType.READ -> App.db.articleDao().loadAllWithUnread(false)
        FeedFilterType.UNREAD -> App.db.articleDao().loadAllWithUnread(true)
        FeedFilterType.DUMMY -> App.db.articleDao().getAll() //todo
    }

    private val pager = Pager(config = PagingConfig(pageSize = 20, enablePlaceholders = true)) {
        pageSource()
    }

    private fun generateFeeds() = pager.flow.map { data ->
        data.map { article -> FeedListItem.Item(article) }
            .insertSeparators { before: FeedListItem?, after: FeedListItem? ->
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

    private var _feeds = generateFeeds()
    val feeds: Flow<PagingData<FeedListItem>>
        get() = _feeds

    private var feedsFilterQuery: String = ""
    val searchQueryAction = Channel<String>().also {
        viewModelScope.launch(Dispatchers.IO) {
            it.consumeAsFlow().debounce(500).collectLatest2 { s ->
                if (s == feedsFilterQuery)
                    return@collectLatest2

                withContext(Dispatchers.IO) {
                    feedsFilterQuery = s
                    feeds.collectLatest2 {
                        adapterSubmitList(it)
                    }
                }
            }
        }
    }

    fun adapterSubmitList(aes: PagingData<FeedListItem>, lifecycle: Lifecycle? = null) =
        viewModelScope.launch {
            val filteredData = withContext(Dispatchers.IO) {
                if (feedsFilterQuery.isBlank())
                    aes
                else
                    aes.filter {
                        if (it is FeedListItem.Item)
                            it.articleEntity.title?.contains(feedsFilterQuery, true) == true
                        else
                            false
                    }
            }

            if (lifecycle == null)
                itemAdapter.submitData(filteredData)
            else
                itemAdapter.submitData(lifecycle, filteredData)

            Timber.v("==" + aes.hashCode())
        }

    fun invalidateItem(pos: Int, new: ArticleEntity, onFinish: Runnable) {
        viewModelScope.launch {
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