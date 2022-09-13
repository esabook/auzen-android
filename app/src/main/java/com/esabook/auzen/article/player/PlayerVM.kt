package com.esabook.auzen.article.player

import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esabook.auzen.App
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.extentions.collectLatest2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerVM : ViewModel() {
    val onQuery: SearchView.OnQueryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            searchQueryAction.trySend(query ?: "")
            return false
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            searchQueryAction.trySend(newText ?: "")
            return false
        }
    }
    val itemAdapter by lazy { PlayerQueueAdapter() }

    val feeds = App.db.articleQueueDao().getAll()

    private var feedsFilterQuery: String = ""
    val searchQueryAction = Channel<String>().also {
        viewModelScope.launch(Dispatchers.IO) {
            it.consumeAsFlow().debounce(500).collectLatest2 { s ->
                if (s.isBlank()) {
                    feeds.collectLatest2 { submitList(it) }
                    return@collectLatest2
                }

                feedsFilterQuery = s
                feeds.collectLatest2 {
                    val list = it.filter { a -> a.title?.contains(s, true) ?: false }
                    submitList(list)
                }
            }
        }
    }

    private suspend fun submitList(list: List<ArticleEntity>) {
        withContext(Dispatchers.Main) {
            itemAdapter.submitList(list)
        }
    }
}