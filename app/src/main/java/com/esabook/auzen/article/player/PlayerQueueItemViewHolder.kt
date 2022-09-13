package com.esabook.auzen.article.player

import android.view.ViewGroup
import androidx.core.text.parseAsHtml
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.databinding.PlayerQueueItemViewHolderBinding
import com.esabook.auzen.extentions.*

class PlayerQueueItemViewHolder(
    parent: ViewGroup,
    val v: PlayerQueueItemViewHolderBinding = PlayerQueueItemViewHolderBinding.inflate(
        parent.layoutInflater(),
        parent,
        false
    )
) : RecyclerView.ViewHolder(v.root) {

    fun setData(data: ArticleEntity) {
        itemView.postInvalidate()
        v.tvTitle.text = data.title?.removeNewLine()?.parseAsHtml()?.removeDebris()
        v.tvPublishDate.text = data.pubDate?.toDate()?.relativeLocalizeDateIndo() ?: ""
        v.tvSource.text = data.sourceTitle

        if (data.enclosure != null) {
            v.ivThumbnail.loadImageWithGlide(data.enclosure,
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