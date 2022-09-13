package com.esabook.auzen.article.feeds.pager

import com.esabook.auzen.data.db.entity.ArticleEntity

sealed class FeedListItem(val payload: Any?) {
    data class Item(val articleEntity: ArticleEntity) : FeedListItem(articleEntity)
    data class Separator(val letter: String?) : FeedListItem(letter)
}