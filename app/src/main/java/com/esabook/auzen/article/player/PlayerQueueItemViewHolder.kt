package com.esabook.auzen.article.player

import android.view.ViewGroup
import androidx.core.text.parseAsHtml
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.databinding.PlayerQueueItemViewHolderBinding
import com.esabook.auzen.extentions.*
import com.esabook.auzen.ui.ViewHolder

class PlayerQueueItemViewHolder(parent: ViewGroup) : ViewHolder<PlayerQueueItemViewHolderBinding>(
    parent,
    PlayerQueueItemViewHolderBinding::inflate
) {

    fun setData(data: ArticleEntity) {
        itemView.postInvalidate()
        val v = binding
        v.tvTitle.text = data.title
            ?.removeNewLine()
            ?.parseAsHtml()
            ?.removeDebris()

        v.tvPublishDate.text = data.pubDate
            ?.toDate()
            ?.toStringWithPattern("EEEE, dd MMM yyyy '|' HH:mm", true)
            ?: ""

        v.tvSource.text = data.sourceTitle

        if (data.enclosure != null) {
            v.ivThumbnail.loadImageWithGlide(
                data.enclosure,
                onFail = {
                    isGone = true
                },
                onSuccess = {
                    this.isVisible = true
                }
            )
        } else {
            v.ivThumbnail.isGone = true
        }



        data.sourceLink?.let {
            val favicon = "https://www.google.com/s2/favicons?domain=${it}&sz=96"
            v.ivFavicon.loadImageWithGlide(favicon)
        }

    }
}