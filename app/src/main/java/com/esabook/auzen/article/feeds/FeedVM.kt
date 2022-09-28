package com.esabook.auzen.article.feeds

import android.util.SparseArray
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class FeedVM : ViewModel() {


    var checkedFilter: SparseArray<FeedFilter> = SparseArray()

    val onquery: SearchView.OnQueryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            queryFlow.tryEmit(query)
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            queryFlow.tryEmit(newText)
            return true
        }
    }

    val queryFlow: MutableStateFlow<String?> = MutableStateFlow(null)

    var totalItemFlowTitle: MutableStateFlow<String?> = MutableStateFlow("Semua")
}