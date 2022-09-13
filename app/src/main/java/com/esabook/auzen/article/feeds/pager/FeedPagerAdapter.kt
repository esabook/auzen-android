package com.esabook.auzen.article.feeds.pager

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.esabook.auzen.R
import com.esabook.auzen.article.feeds.FeedFilterType
import com.esabook.auzen.article.feeds.FeedFilterType.*
import com.esabook.auzen.article.feeds.pager.FeedPagerFragment.Companion.PAYLOAD_FILTER_TYPE_NAME
import kotlinx.coroutines.flow.MutableStateFlow

class FeedPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val queryFlow: MutableStateFlow<String?>
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    val tabs = listOf(
        Tab(PLAYLIST, title = null, R.drawable.ic_round_headset, "Daftar putar"),
        Tab(ALL, "Semua", null, selected = true),
        Tab(UNREAD, "Belum Baca", null),
        Tab(READ, "Terbaca", null),
        Tab(DUMMY, "Tambah", R.drawable.ic_round_add)
    )

    override fun getItemCount(): Int {
        return tabs.filter { it.id != DUMMY }.size
    }

    override fun createFragment(position: Int): Fragment {
        val selectedTab = tabs[position]
        return when (selectedTab.id) {
            PLAYLIST -> FeedPagerFragment().apply {
                arguments = bundleOf(PAYLOAD_FILTER_TYPE_NAME to PLAYLIST.name)
                setQueryDispatcher(queryFlow)
            }
            ALL -> FeedPagerFragment().apply {
                arguments = bundleOf(PAYLOAD_FILTER_TYPE_NAME to ALL.name)
                setQueryDispatcher(queryFlow)
            }
            UNREAD -> FeedPagerFragment().apply {
                arguments = bundleOf(PAYLOAD_FILTER_TYPE_NAME to UNREAD.name)
                setQueryDispatcher(queryFlow)
            }
            READ -> FeedPagerFragment().apply {
                arguments = bundleOf(PAYLOAD_FILTER_TYPE_NAME to READ.name)
                setQueryDispatcher(queryFlow)
            }
            else -> {
                TODO()
            }
        }
    }


    inner class Tab(
        val id: FeedFilterType,
        val title: String?,
        val iconId: Int?,
        val desc : String? = title,
        val selected: Boolean = false
    )
}