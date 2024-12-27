package com.esabook.auzen.article.feeds.pager

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.esabook.auzen.R
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.ui.OnItemClickListener
import com.esabook.auzen.ui.StickHeaderItemDecoration
import timber.log.Timber

class FeedItemAdapter : PagingDataAdapter<FeedListItem, RecyclerView.ViewHolder>(DIFF_CALLBACK),
    OnItemClickListener, StickHeaderItemDecoration.StickyHeaderInterface {
    var onItemClickListener: OnItemClickListener? = null
    var onLongItemClickListener: OnItemClickListener? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 1)
            FeedItemHeaderViewHolder(parent)
        else
            FeedItemViewHolder(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val payload = getItem(position)
        if (holder is FeedItemViewHolder && payload is FeedListItem.Item) {
            holder.setData(payload.articleEntity)

            holder.itemView.setOnLongClickListener { onLongClick(holder, position, payload) }
            holder.binding.ivMore.setOnClickListener { holder.itemView.performLongClick() }
            holder.itemView.setOnClickListener { onClick(holder, position, payload.articleEntity) }
        } else if (holder is FeedItemHeaderViewHolder && payload is FeedListItem.Separator) {
            holder.setData(payload.letter)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is FeedItemViewHolder) {
            holder.notifyRecycled()
        }
    }

    private fun onLongClick(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payload: FeedListItem.Item
    ): Boolean {
        onLongItemClickListener?.onClick(
            holder,
            position,
            payload.articleEntity
        )
        return false
    }

    override fun onClick(holder: RecyclerView.ViewHolder, position: Int, payload: Any?) {
        (payload as? ArticleEntity)?.let {
            Timber.v(payload.toString())
        }
        onItemClickListener?.onClick(holder, position, payload)
    }

    override fun getItemViewType(position: Int): Int {
        return if (isHeader(position)) 1 else 0
    }

    override fun getHeaderLayout(headerPosition: Int): Int {
        return R.layout.feed_sticky_header
    }

    override fun getHeaderPositionForItem(itemPosition: Int): Int {
        var mItemPos = itemPosition
        var headerPosition = 0
        do {
            if (isHeader(mItemPos)) {
                headerPosition = mItemPos
                break
            }
            mItemPos -= 1
        } while (mItemPos >= 0)
        return headerPosition
    }

    override fun bindHeaderData(header: View?, headerPosition: Int) {
        if (headerPosition < 0) return
        val item = getItem(headerPosition)
        if (item is FeedListItem.Separator) {
            header?.findViewById<AppCompatTextView>(R.id.tv_date_indicator)?.text = item.letter
            header?.isVisible = true
        }

    }

    override fun isHeader(itemPosition: Int): Boolean {
        return try {
            getItem(itemPosition) is FeedListItem.Separator
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<FeedListItem>() {
            override fun areItemsTheSame(oldItem: FeedListItem, newItem: FeedListItem): Boolean {
                return if (oldItem is FeedListItem.Item && newItem is FeedListItem.Item) {
                    oldItem.articleEntity.guid == newItem.articleEntity.guid

                } else if (oldItem is FeedListItem.Separator && newItem is FeedListItem.Separator) {
                    oldItem.letter == newItem.letter

                } else {
                    false
                }
            }

            override fun areContentsTheSame(
                oldItem: FeedListItem,
                newItem: FeedListItem
            ): Boolean {
                return if (oldItem is FeedListItem.Item && newItem is FeedListItem.Item)
                    isArticleSame(oldItem.articleEntity, newItem.articleEntity)
                else
                    true
            }

            private fun isArticleSame(old: ArticleEntity, new: ArticleEntity): Boolean {
                return old.isUnread == new.isUnread
                        && old.isPlayListQueue == new.isPlayListQueue
                        && old.playListOrder == new.playListOrder
            }

        }
    }

}