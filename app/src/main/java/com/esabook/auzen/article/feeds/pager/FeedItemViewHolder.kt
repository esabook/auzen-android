package com.esabook.auzen.article.feeds.pager

import android.view.ViewGroup
import androidx.core.text.parseAsHtml
import com.bumptech.glide.Glide
import com.esabook.auzen.App
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.databinding.FeedViewHolderBinding
import com.esabook.auzen.extentions.*
import com.esabook.auzen.ui.ViewHolder
import kotlinx.coroutines.*

class FeedItemViewHolder(parent: ViewGroup) :
    ViewHolder<FeedViewHolderBinding>(parent, FeedViewHolderBinding::inflate) {

    companion object {
        private val mainScope by lazy { MainScope().plus(CoroutineName("FeedItemViewHolder")) }
    }

    var job: Job? = null
    fun setData(data: ArticleEntity?) {
        if (data == null) {
            notifyRecycled()
            return
        }

        job = mainScope.launch {
            withContext(Dispatchers.Main) {
                val alpha = if (data.isUnread) 1F else .5F
                binding.run {
                    tvTitle.alpha = alpha
                    tvDescription.alpha = alpha
                    tvSource.alpha = alpha
                    tvPublishDate.alpha = alpha

                    tvSource.text = data.sourceTitle
                    val title = data.title?.parseAsHtml()?.removeDebris()
                    tvTitle.text = title
                    var desc = data.description?.parseAsHtml()?.removeDebris()
                    if ((desc?.length ?: 0) < 20) desc = null
                    tvDescription.text = desc

                    tvPublishDate.text = data.pubDate?.toDate()?.toStringWithPattern("HH:mm") ?: ""

                    if (data.enclosure != null) {
                        ivThumbnail.loadImageWithGlide(data.enclosure,
                            onFail = {
                                loadThumbnail(data)

                            }
                        )
                    } else {
                        loadThumbnail(data)
                    }



                    data.sourceLink?.let {
                        val favicon = NewsParserUtils.getFaviconUrl(it)
                        ivFavicon.loadImageWithGlide(favicon)
                    }
                }

            }
        }
    }


    var thumbnailJob: Job? = null
    private fun loadThumbnail(data: ArticleEntity?) {
        if (data?.link == null){
            notifyRecycled()
            return
        }
        thumbnailJob = mainScope.launch {
            withContext(Dispatchers.IO){
                delay(500)
                val ogArticle = OgParser.getOgEntity(data.link)
                val newData = data.copy(
                    description = data.description ?: ogArticle?.description,
                    enclosure = ogArticle?.image,
                    sourceTitle = ogArticle?.siteName)

                App.db.articleDao().update(newData)

                if (newData.enclosure != null && newData.enclosure != data.enclosure)
                    setData(newData)
            }
        }
    }

    fun notifyRecycled() {
        job?.cancel()
        job = null
        thumbnailJob?.cancel()
        thumbnailJob = null
        binding.run {
            Glide.with(ivFavicon).clear(ivFavicon)
            Glide.with(ivThumbnail).clear(ivThumbnail)
        }

    }
}