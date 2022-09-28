package com.esabook.auzen.article.feeds

import android.os.Bundle
import android.view.Gravity
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.esabook.auzen.R
import com.esabook.auzen.article.feeds.pager.FeedPagerFragment
import com.esabook.auzen.article.feeds.pager.FeedPagerFragment.Companion.RESULT_KEY_EMPTY_STATE
import com.esabook.auzen.article.player.PlayerFragment
import com.esabook.auzen.article.readview.ReadFragment
import com.esabook.auzen.article.subscription.RssCollectionFragment
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.databinding.FeedFragmentBinding
import com.esabook.auzen.extentions.fadInAnimation
import com.esabook.auzen.local.KeyConstant
import com.esabook.auzen.ui.Navigation.Companion.findNavigation
import com.esabook.auzen.ui.viewBinding
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import timber.log.Timber

class FeedFragment : Fragment(R.layout.feed_fragment) {

    private val model: FeedVM by activityViewModels()
    private val binding by viewBinding(FeedFragmentBinding::bind)

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        private val backCountToExit = 2
        private var currentBackCount = backCountToExit
        override fun handleOnBackPressed() {

            if (binding.root.isDrawerOpen(Gravity.LEFT)) {
                binding.root.closeDrawers()
                return
            }

            if (isHidden || view == null) {
                isEnabled = false
                requireActivity().onBackPressed()
                isEnabled = true
                return
            }

            currentBackCount--
            if (currentBackCount > 0) {
                Snackbar.make(view!!, "Sekali lagi untuk keluar", Snackbar.LENGTH_SHORT).also {
                    it.addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            currentBackCount = backCountToExit
                        }
                    })
                }.show()

            } else {
                requireActivity().finish()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        Timber.d("start")
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )

        parentFragmentManager.setFragmentResultListener(
            KeyConstant.OPEN_PLAYER,
            viewLifecycleOwner
        ) { _, _ ->
            gotoPlayerScreen()
        }

        childFragmentManager.setFragmentResultListener(
            KeyConstant.OPEN_PLAYER,
            viewLifecycleOwner
        ) { _, _ ->
            gotoPlayerScreen()
        }

        lifecycleScope.launch {
            initHeaderView()
            initDrawerView()
            initToolbarView()
            initSearchBarView()
            initPlayerView()
            initChipView()

            Timber.d("end")
        }

    }

    private fun initChipView() {
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            model.checkedFilter.clear()

            checkedIds.forEach { id ->
                when (id) {
                    R.id.chip_playlist -> FeedFilter.PLAYLIST.let {
                        model.checkedFilter.set(it.ordinal, it)
                    }
                    R.id.chip_unread -> FeedFilter.UNREAD.let {
                        model.checkedFilter.set(it.ordinal, it)
                    }
                    R.id.chip_read -> FeedFilter.READ.let {
                        model.checkedFilter.set(it.ordinal, it)
                    }
                }
            }

            invalidateDataList()

        }
    }

    private fun invalidateDataList() {
        getFeedsFragment().setFilter(model.checkedFilter)
    }

    private fun getFeedsFragment() = binding.feedPagerFragmentContainer
        .getFragment<FeedPagerFragment>()

    private fun initHeaderView() {

        model.totalItemFlowTitle.asLiveData().observe(viewLifecycleOwner) {
            binding.tvHead.text = it
        }

        getFeedsFragment().parentFragmentManager
            .setFragmentResultListener(
                FeedPagerFragment.RESULT_KEY,
                viewLifecycleOwner
            ) { _, result ->
                if (result.getBoolean(RESULT_KEY_EMPTY_STATE, false)) {
                    gotoRssSettingScreen()
                }
            }

    }

    private fun initDrawerView() {

        binding.root.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            val zoomOutSize = resources.getDimensionPixelSize(R.dimen.dp_60)
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                binding.gContent.x = (slideOffset * zoomOutSize)
            }

            override fun onDrawerOpened(drawerView: View) {
            }

            override fun onDrawerClosed(drawerView: View) {
            }

            override fun onDrawerStateChanged(newState: Int) {
            }
        })
    }

    private fun initToolbarView() {
        binding.appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            // eg: 20
            val totalOffset = appBarLayout.totalScrollRange - appBarLayout.totalScrollRange * .5
            // eg: 10, -10/-20 = .5
            val offsetAsAlpha = verticalOffset.toFloat() / -totalOffset.toFloat()
            val alpha = 1f - offsetAsAlpha
            binding.gHeader.alpha = alpha
        }

        binding.toolbar.setNavigationOnClickListener { gotoRssSettingScreen() }
        binding.toolbar.setOnMenuItemClickListener {
            if (it.itemId == R.id.sc_search) {
                binding.searchParent.apply {
                    isVisible = isGone
                    binding.searchBar.isIconified = isGone
                    fadInAnimation()
                }
            }
            true
        }
    }

    private fun initPlayerView() {
        binding.playerFl.setOnClickListener {
            binding.playerFl.playerView?.articleEntity?.let {
                gotoPlayerScreen()
            }
        }
    }

    private fun initSearchBarView() {
        binding.searchBar.setOnQueryTextListener(model.onquery)
        binding.searchBar.setOnCloseListener {
            binding.searchParent.isGone = true
            true
        }

        getFeedsFragment().setQueryDispatcher(model.queryFlow)
    }

    private fun gotoReadingScreen(payload: ArticleEntity) {
        val args = bundleOf(ReadFragment.PAYLOAD_LINK to payload.link)
        findNavigation().submit {
            fragmentManager.beginTransaction()
                .add(containerId, ReadFragment::class.java, args, "")
                .hide(this@FeedFragment)
                .addToBackStack("")
                .commit()
        }
    }

    var rssFragment: RssCollectionFragment? = null
    private fun gotoRssSettingScreen() {
        if (rssFragment == null) {
            rssFragment = binding.rssFragment.getFragment()
            rssFragment?.parentFragmentManager?.setFragmentResultListener(
                RssCollectionFragment.KEY_RESULT,
                viewLifecycleOwner
            ) { _, result ->
                val menuTitle = result.getString(RssCollectionFragment.KEY_SELECTED_MENU_TITLE, "")

                val guids = result.getString(RssCollectionFragment.KEY_SELECTED_RSS_GUID, null)
                    ?.split(RssCollectionFragment.GUID_SEPARATOR)
                    ?.filterNot(String::isNullOrBlank)

                model.totalItemFlowTitle.tryEmit(menuTitle)
                getFeedsFragment().setGuidsWhiteList(guids)
                binding.root.closeDrawers()

            }

        }
        binding.root.openDrawer(Gravity.LEFT, true)
        rssFragment?.invalidateData()
    }

    private fun gotoPlayerScreen() {
        if (binding.root.isDrawerOpen(Gravity.LEFT))
            binding.root.closeDrawers()

        val fragment = PlayerFragment()
        fragment.onPlayerClickListener = View.OnClickListener {
            fragment.player.articleEntity?.let { it1 ->
                gotoReadingScreen(it1)
                fragment.dismissAllowingStateLoss()
            }
        }
        fragment.show(parentFragmentManager, "")
    }
}