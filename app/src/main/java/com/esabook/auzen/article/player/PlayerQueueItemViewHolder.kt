package com.esabook.auzen.article.player

import android.view.ViewGroup
import androidx.core.text.parseAsHtml
import com.bumptech.glide.Glide
import com.esabook.auzen.App
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.databinding.PlayerQueueItemViewHolderBinding
import com.esabook.auzen.extentions.*
import com.esabook.auzen.ui.ViewHolder
import kotlinx.coroutines.*

class PlayerQueueItemViewHolder(parent: ViewGroup) : ViewHolder<PlayerQueueItemViewHolderBinding>(
    parent,
    PlayerQueueItemViewHolderBinding::inflate
) {
    companion object {
        private val mainScope by lazy { MainScope().plus(CoroutineName("PlayerQueueItemViewHolder")) }
    }

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
                    loadThumbnail(data)
                }
            )
        }



        data.sourceLink?.let {
            val favicon = "https://www.google.com/s2/favicons?domain=${it}&sz=96"
            v.ivFavicon.loadImageWithGlide(favicon)
        }

    }

    var thumbnailJob: Job? = null
    private fun loadThumbnail(data: ArticleEntity?) {
        if (data?.link == null) {
            notifyRecycled()
            return
        }
        thumbnailJob = mainScope.launch {
            withContext(Dispatchers.IO) {
                delay(500)
                val ogArticle = OgParser.getOgEntity(data.link)
                val newData = data.copy(
                    description = data.description ?: ogArticle?.description,
                    enclosure = ogArticle?.image,
                    sourceTitle = ogArticle?.siteName
                )

                App.db.articleDao().update(newData)

                if (newData.enclosure != null && newData.enclosure != data.enclosure)
                    setData(newData)
            }
        }
    }

    fun notifyRecycled() {
        thumbnailJob?.cancel()
        thumbnailJob = null
        binding.run {
            Glide.with(ivFavicon).clear(ivFavicon)
            Glide.with(ivThumbnail).clear(ivThumbnail)
        }

    }

}