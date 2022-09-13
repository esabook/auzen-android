package com.esabook.auzen.article.player

import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esabook.auzen.R
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.extentions.getClr
import com.esabook.auzen.extentions.post2
import com.esabook.auzen.ui.OnItemClickListener
import timber.log.Timber

class PlayerQueueAdapter : ListAdapter<ArticleEntity, PlayerQueueItemViewHolder>(DIFF_CALLBACK),
    OnItemClickListener {
    var onItemClickListener: OnItemClickListener? = null
    var dragHelper: ItemTouchHelper? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerQueueItemViewHolder {
        return PlayerQueueItemViewHolder(parent)
    }

    override fun onBindViewHolder(holder: PlayerQueueItemViewHolder, position: Int) {
        val payload = getItem(position) ?: return
        holder.setData(payload)
        holder.itemView.setOnClickListener { onClick(holder, position, payload) }
        holder.itemView.setOnLongClickListener {
            holder.v.tvHintReorder.apply {
                isVisible = true
                alpha = 0F
                animate()
                    .alpha(1F)
                    .setDuration(300)
                    .start()
            }

            val lastColor = holder.v.root.cardBackgroundColor
            holder.v.root.setCardBackgroundColor(it.resources.getClr(R.color.purple_100))
            holder.v.tvHintReorder.post2(1000) {
                isGone = true
                holder.v.root.setCardBackgroundColor(lastColor)
            }
            return@setOnLongClickListener true
        }


    }


    override fun onClick(holder: RecyclerView.ViewHolder, position: Int, payload: Any?) {
        (payload as? ArticleEntity)?.let {
            Timber.v(payload.toString().replace(", ", "\n"))
        }
        onItemClickListener?.onClick(holder, position, payload)
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ArticleEntity>() {
            override fun areItemsTheSame(oldItem: ArticleEntity, newItem: ArticleEntity): Boolean {
                return oldItem.guid == newItem.guid
            }

            override fun areContentsTheSame(
                oldItem: ArticleEntity,
                newItem: ArticleEntity
            ): Boolean {
                return oldItem.playListOrder == newItem.playListOrder
            }

        }
    }
}