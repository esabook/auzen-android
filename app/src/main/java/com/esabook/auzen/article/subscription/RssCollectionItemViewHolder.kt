package com.esabook.auzen.article.subscription

import android.view.ViewGroup
import androidx.core.text.parseAsHtml
import androidx.core.view.isVisible
import com.esabook.auzen.R
import com.esabook.auzen.data.db.entity.RssEntity
import com.esabook.auzen.databinding.RssViewHolderBinding
import com.esabook.auzen.extentions.loadImageWithGlide
import com.esabook.auzen.ui.ViewHolder

class RssCollectionItemViewHolder(parent: ViewGroup) :
    ViewHolder<RssViewHolderBinding>(parent, RssViewHolderBinding::inflate) {

    fun getBinding() = binding

    fun setData(data: RssEntity) {
        val v = binding
        v.title.text = data.title

        val autoSyncImg =
            if (data.muteAutoSync) R.drawable.ic_round_sync_disabled else R.drawable.ic_round_sync
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