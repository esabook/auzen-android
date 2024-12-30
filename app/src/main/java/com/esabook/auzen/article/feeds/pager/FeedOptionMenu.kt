package com.esabook.auzen.article.feeds.pager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.text.parseAsHtml
import androidx.core.view.forEach
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.esabook.auzen.R
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.databinding.FeedOptionMenuBinding
import com.esabook.auzen.databinding.FeedOptionMenuItemBinding
import com.esabook.auzen.extentions.loadImageWithGlide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import timber.log.Timber

class FeedOptionMenu : BottomSheetDialogFragment() {

    private val menuId = R.menu.feed_item_menu
    private var binding: FeedOptionMenuBinding? = null

    private var onMenuClickListener: View.OnClickListener? = null

    private var holder: FeedItemViewHolder? = null
    private var position: Int? = null
    private var onShowAction: (FeedOptionMenu.() -> Unit)? = null

    var payload: ArticleEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FeedOptionMenuBinding.inflate(LayoutInflater.from(context), container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.run {
            if (gContent.childCount > 0) {
                Timber.w("Skip reinflate")
                return
            }

            val popupMenu = PopupMenu(requireContext(), FrameLayout(requireContext()))
            popupMenu.menuInflater.inflate(menuId, popupMenu.menu)

            popupMenu.menu.forEach {
                val itemView = FeedOptionMenuItemBinding
                    .inflate(LayoutInflater.from(context), gContent, false)
                itemView.icon.setImageDrawable(it.icon)
                itemView.title.text = it.title
                itemView.root.id = it.itemId
                itemView.root.setOnClickListener(this@FeedOptionMenu::onViewClick)
                gContent.addView(itemView.root)
            }
        }

    }

    private fun onViewClick(view: View) {
        onMenuClickListener?.onClick(view)
        dismiss()
    }

    fun showWithPayload(
        fgManager: FragmentManager,
        holder: FeedItemViewHolder,
        pos: Int,
        payload: ArticleEntity
    ) {
        this.holder = holder
        this.position = pos
        this.payload = payload
        show(fgManager, "menu")
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            onShowAction?.invoke(this@FeedOptionMenu)
        }

        binding?.run {
            tvTitle.text = payload?.title?.parseAsHtml()
            ivThumbnail.loadImageWithGlide(payload?.enclosure)
        }
    }

    fun setOnMenuClickListener(action: (holder: FeedItemViewHolder, pos: Int, payload: ArticleEntity, view: View) -> Unit) {
        onMenuClickListener = View.OnClickListener {
            action.invoke(holder!!, position!!, payload!!, it)

        }
    }

    fun getMenuBinding(@IdRes id: Int) =
        binding?.gContent?.findViewById<View>(id)?.let { FeedOptionMenuItemBinding.bind(it) }

    fun onShow(action: FeedOptionMenu.() -> Unit) {
        onShowAction = action
    }
}