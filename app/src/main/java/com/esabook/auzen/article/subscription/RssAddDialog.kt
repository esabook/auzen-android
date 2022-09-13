package com.esabook.auzen.article.subscription

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.esabook.auzen.App
import com.esabook.auzen.data.api.Api
import com.esabook.auzen.data.db.entity.RssEntity
import com.esabook.auzen.databinding.RssAddDialogBinding
import com.esabook.auzen.extentions.OpmlParseUtils
import com.esabook.auzen.extentions.post2
import com.esabook.auzen.extentions.removeNewLine
import com.esabook.auzen.extentions.toStringWithPattern
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class RssAddDialog(context: Context) : AlertDialog(context) {

    var binding: RssAddDialogBinding? = null

    private fun initView() {
        if (binding != null) return
        binding = RssAddDialogBinding.inflate(LayoutInflater.from(context))
        binding?.run {
            fetchRss.setOnClickListener {
                val url = rssUrl.text.toString()
                fetchRss(url)
            }
            btCancel.setOnClickListener {
                job?.cancel()
                job = null
            }
        }
        setContentView(binding!!.root)
    }

    override fun show() {
        super.show()
        lifecycleScope.launch {
            initView()
        }
    }


    var job: Job? = null
    @Suppress("BlockingMethodInNonBlockingContext")
    private fun fetchRss(url: String) {
       job = lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    setCancelable(false)
                    val xmlStream = Api.inputStream(url=url)
                    val feed: SyndFeed = SyndFeedInput().build(XmlReader(xmlStream))
                    if (feed.title != null && feed.title.isNotBlank()) {
                        val rssEntity = RssEntity(
                            url,
                            feed.title?.removeNewLine(),
                            url,
                            feed.description,
                            (feed.publishedDate ?: Date()).toStringWithPattern(),
                            feed.copyright,
                            feed.categories?.joinToString(";"),
                            feed.icon?.link ?: feed.icon?.url,
                            feed.entries?.size ?: 0
                        )
                        App.db.rssDao().insertAll(rssEntity)
                        OpmlParseUtils.saveArticle(rssEntity, feed.entries)
                    }
                    setCancelable(true)
                } catch (e: Exception) {
                    setCancelable(true)
                    binding?.tvLog?.post2 {
                        text = e.message
                    }
                }
            }

        }
    }
}