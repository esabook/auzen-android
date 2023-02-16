package com.esabook.auzen.article.feeds.pager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.forEach
import com.esabook.auzen.R
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.databinding.FeedOptionMenuBinding
import com.esabook.auzen.databinding.FeedOptionMenuItemBinding
import com.esabook.auzen.extentions.loadImageWithGlide
import com.google.android.material.bottomsheet.BottomSheetDialog
import timber.log.Timber

class FeedOptionMenu(context: Context) : BottomSheetDialog(context) {

    private val menuId = R.menu.feed_item_menu
    private val binding = FeedOptionMenuBinding.inflate(LayoutInflater.from(context))

    private var onMenuClickListener: View.OnClickListener? = null

    var holder: FeedItemViewHolder? = null
    var position: Int? = null
    var payload: ArticleEntity? = null

    init {
        binding.gContent.removeAllViews()
        initView()
    }

    private fun initView() {
        if (binding.gContent.childCount > 0) {
            Timber.w("Skip reinflate")
            return
        }

        val popupMenu = PopupMenu(context, FrameLayout(context))
        popupMenu.menuInflater.inflate(menuId, popupMenu.menu)

        val layoutInflater = LayoutInflater.from(context)
        popupMenu.menu.forEach {
            val itemView = FeedOptionMenuItemBinding
                .inflate(layoutInflater, binding.gContent, false)
            itemView.icon.setImageDrawable(it.icon)
            itemView.title.text = it.title
            itemView.root.id = it.itemId
            itemView.root.setOnClickListener(this::onViewClick)
            binding.gContent.addView(itemView.root)
        }

        setContentView(binding.root)
    }

    private fun onViewClick(view: View) {
        onMenuClickListener?.onClick(view)
        dismiss()
    }

    fun showWithPayload(
        holder: FeedItemViewHolder,
        pos: Int,
        payload: ArticleEntity
    ) {
        this.holder = holder
        this.position = pos
        this.payload = payload
        show()
    }

    override fun show() {
        super.show()
        binding.tvTitle.text = payload?.title
        binding.ivThumbnail.loadImageWithGlide(payload?.enclosure)
    }

    fun setOnMenuClickListener(action: (holder: FeedItemViewHolder, pos: Int, payload: ArticleEntity, view: View) -> Unit) {
        onMenuClickListener = View.OnClickListener {
            action.invoke(holder!!, position!!, payload!!, it)

        }
    }

    fun getMenuBinding(@IdRes id: Int) =
        binding.gContent.findViewById<View>(id)?.let { FeedOptionMenuItemBinding.bind(it) }

    fun onshow(action: FeedOptionMenu.()-> Unit){
        setOnShowListener {
            action.invoke(this)
        }
    }
}