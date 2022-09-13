package com.esabook.auzen.article.feeds.pager

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.esabook.auzen.databinding.FeedHeaderViewHolderBinding
import com.esabook.auzen.extentions.layoutInflater

class FeedItemHeaderViewHolder(
    parent: ViewGroup,
    private val v: FeedHeaderViewHolderBinding = FeedHeaderViewHolderBinding.inflate(
        parent.layoutInflater(),
        parent,
        false
    )
) : RecyclerView.ViewHolder(v.root) {

    fun setData(label: String?) {
        v.tvDateIndicator.text = label
        v.root.isVisible = true
    }
}