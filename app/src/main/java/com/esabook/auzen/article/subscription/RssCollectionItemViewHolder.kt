package com.esabook.auzen.article.subscription

import android.view.ViewGroup
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.esabook.auzen.R
import com.esabook.auzen.data.db.entity.RssEntity
import com.esabook.auzen.databinding.RssViewHolderBinding
import com.esabook.auzen.extentions.layoutInflater
import com.esabook.auzen.extentions.loadImageWithGlide

class RssCollectionItemViewHolder(
    parent: ViewGroup,
    private val v: RssViewHolderBinding = RssViewHolderBinding.inflate(
        parent.layoutInflater(),
        parent,
        false
    )
) : RecyclerView.ViewHolder(v.root) {

    fun getBinding() = v

    fun setData(data: RssEntity) {

        v.title.text = data.title

        val autoSyncImg = if (data.muteAutoSync) R.drawable.ic_round_sync_disabled else R.drawable.ic_round_sync
        v.btMute.setImageResource(autoSyncImg)

        val favicon = data.favicon ?: "https://www.google.com/s2/favicons?domain=${data.link}&sz=96"
        v.thumbnail.loadImageWithGlide(favicon)

        v.tvCount.text = itemView.resources.getString(
            R.string.rss_count_available_or_unread,
            data.totalEntry,
            data.totalEntryUnread
        ).parseAsHtml()

        v.root.setOnClickListener {
            v.gButton.isVisible = v.gButton.isVisible.not()
        }

    }
}