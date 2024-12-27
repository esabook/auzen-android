package com.esabook.auzen.article.feeds

import android.util.SparseArray
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class FeedVM : ViewModel() {


    var checkedFilter: SparseArray<FeedFilter> = SparseArray()

    val onquery: SearchView.OnQueryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean {
            queryFlow.postValue(query)
            return true
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            queryFlow.postValue(newText)
            return true
        }
    }

    val queryFlow: MutableLiveData<String?> = MutableLiveData(null)

    var totalItemFlowTitle: MutableStateFlow<String?> = MutableStateFlow("Semua")
}