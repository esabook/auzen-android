package com.esabook.auzen.article.feeds.pager

import android.view.ViewGroup
import androidx.core.view.isVisible
import com.esabook.auzen.databinding.FeedHeaderViewHolderBinding
import com.esabook.auzen.ui.ViewHolder

class FeedItemHeaderViewHolder(parent: ViewGroup) :
    ViewHolder<FeedHeaderViewHolderBinding>(parent, FeedHeaderViewHolderBinding::inflate) {

    fun setData(label: String?) {
        binding.tvDateIndicator.text = label
        binding.root.isVisible = true
    }
}