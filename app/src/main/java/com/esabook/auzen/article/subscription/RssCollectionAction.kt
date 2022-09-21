package com.esabook.auzen.article.subscription

import com.esabook.auzen.data.db.entity.RssEntity
import com.esabook.auzen.ui.ActionStateListener

sealed class RssCollectionAction {
    data class Sync(val rss: RssEntity, val actionState: ActionStateListener? = null)
    data class MuteAutoSync(val rss: RssEntity, val actionState: ActionStateListener? = null)
    data class PurgeAndRebuild(val rss: RssEntity, val actionState: ActionStateListener? = null)
    data class Delete(val rss: RssEntity)
    data class Filter(val rss: RssEntity)

}
