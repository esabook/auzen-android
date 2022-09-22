package com.esabook.auzen.article.subscription

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esabook.auzen.article.subscription.RssCollectionAction.*
import com.esabook.auzen.data.db.entity.RssEntity
import com.esabook.auzen.extentions.copyToClipboard
import com.esabook.auzen.extentions.post2
import com.esabook.auzen.extentions.shareTextToExternal
import com.esabook.auzen.ui.ActionStateListener
import com.esabook.auzen.ui.ActionStateListener.ActionState.*
import com.esabook.auzen.ui.OnItemClickListener

class RssCollectionItemAdapter : ListAdapter<RssEntity, RssCollectionItemViewHolder>(DIFF_CALLBACK),
    OnItemClickListener {
    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RssCollectionItemViewHolder {
        return RssCollectionItemViewHolder(parent)
    }

    override fun onBindViewHolder(holder: RssCollectionItemViewHolder, position: Int) {
        val payload = getItem(position)
        holder.setData(payload)
        holder.getBinding().run {
            val state = ActionStateListener {
                when (it) {
                    START -> progressCircular.post2 { show() }
                    FAIL -> progressCircular.post2 { hide() }
                    SUCCESS -> progressCircular.post2 { hide() }
                }

                btSync.post2 {
                    isEnabled = it != START //it == FAIL || it == SUCCESS
                }
            }

            btSync.setOnClickListener {

                onClick(holder, position, Sync(payload, state))
            }

            btMute.setOnClickListener {
                val stateMute = ActionStateListener {
                    if (it == SUCCESS) {
                        btMute.post2 { holder.setData(payload) }
                    }
                }
                onClick(holder, position, MuteAutoSync(payload, stateMute))
            }

            btDelete.setOnClickListener {
                onClick(holder, position, Delete(payload))
            }

            btClean.setOnClickListener {
                onClick(holder, position, PurgeAndRebuild(payload, state))
            }

            btShare.setOnClickListener {
                payload.link?.let { it1 -> it.context.shareTextToExternal(it1) }
            }

            btShare.setOnLongClickListener {
                payload.link?.let { it1 -> it.context.copyToClipboard(it1) }
                false
            }

            btEdit.setOnClickListener {
                onClick(holder, position, Edit(payload))
            }

        }

        holder.itemView.setOnClickListener {
            onClick(holder, position, Filter(payload))
        }
    }


    override fun onClick(holder: RecyclerView.ViewHolder, position: Int, payload: Any?) {
        onItemClickListener?.onClick(holder, position, payload)
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RssEntity>() {
            override fun areItemsTheSame(oldItem: RssEntity, newItem: RssEntity): Boolean {
                return oldItem.guid == newItem.guid
            }

            override fun areContentsTheSame(oldItem: RssEntity, newItem: RssEntity): Boolean {
                return oldItem.title == newItem.title
                        && oldItem.muteAutoSync == oldItem.muteAutoSync
                        && oldItem.totalEntryUnread == newItem.totalEntryUnread
                        && oldItem.totalEntry == newItem.totalEntry
            }

        }
    }
}