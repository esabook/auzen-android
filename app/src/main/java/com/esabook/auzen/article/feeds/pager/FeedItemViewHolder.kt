package com.esabook.auzen.article.feeds.pager

import android.view.ViewGroup
import androidx.core.text.parseAsHtml
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.esabook.auzen.App
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.databinding.FeedViewHolderBinding
import com.esabook.auzen.extentions.*
import kotlinx.coroutines.*

class FeedItemViewHolder(
    parent: ViewGroup,
    private val v: FeedViewHolderBinding = FeedViewHolderBinding.inflate(
        parent.layoutInflater(),
        parent,
        false
    )
) : RecyclerView.ViewHolder(v.root) {

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
                v.tvTitle.alpha = alpha
                v.tvDescription.alpha = alpha
                v.tvSource.alpha = alpha
                v.tvPublishDate.alpha = alpha

                v.tvSource.text = data.sourceTitle
                val title = data.title?.parseAsHtml()?.removeDebris()
                v.tvTitle.text = title
                var desc = data.description?.parseAsHtml()?.removeDebris()
                if (title.isNullOrBlank().not()) desc = desc?.replaceFirst(title ?: "", "")
                if ((desc?.length ?: 0) < 20) desc = null
                v.tvDescription.text = desc

                v.tvPublishDate.text = data.pubDate?.toDate()?.relativeLocalizeDateIndo() ?: ""

                if (data.enclosure != null) {
                    v.ivThumbnail.loadImageWithGlide(data.enclosure,
                        onFail = {
                            loadThumbnail(data)

                        }
                    )
                } else {
                    loadThumbnail(data)
                }



                data.sourceLink?.let {
                    val favicon = NewsParserUtils.getFaviconUrl(it)
                    v.ivFavicon.loadImageWithGlide(favicon)
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
        Glide.with(v.ivFavicon).clear(v.ivFavicon)
        Glide.with(v.ivThumbnail).clear(v.ivThumbnail)
    }
}