package com.esabook.auzen.article.player

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.ui.OnItemClickListener
import timber.log.Timber

class PlayerQueueAdapter : ListAdapter<ArticleEntity, PlayerQueueItemViewHolder>(DIFF_CALLBACK),
    OnItemClickListener {
    var onItemClickListener: OnItemClickListener? = null
    var dragHelper: ItemTouchHelper? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerQueueItemViewHolder {
        return PlayerQueueItemViewHolder(parent)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PlayerQueueItemViewHolder, position: Int) {
        val payload = getItem(position)
        holder.setData(payload)
        holder.itemView.setOnClickListener { onClick(holder, position, payload) }
        holder.binding.tvHintReorder.setOnTouchListener { _, event ->
            return@setOnTouchListener if (event.action == KeyEvent.ACTION_DOWN)
                holder.itemView.performLongClick()
            else
                false
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