package com.esabook.auzen.article.feeds

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.esabook.auzen.R
import com.esabook.auzen.article.feeds.pager.FeedPagerAdapter
import com.esabook.auzen.article.feeds.pager.FeedPagerFragment
import com.esabook.auzen.article.player.PlayerFragment
import com.esabook.auzen.article.readview.ReadFragment
import com.esabook.auzen.article.subscription.RssAddDialog
import com.esabook.auzen.article.subscription.RssCollectionFragment
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.databinding.FeedFragmentBinding
import com.esabook.auzen.extentions.getDrw
import com.esabook.auzen.extentions.setTextAnimation
import com.esabook.auzen.extentions.waitUntilExpanded
import com.esabook.auzen.ui.Navigation.Companion.findNavigation
import com.esabook.auzen.ui.viewBinding
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class FeedFragment : Fragment(R.layout.feed_fragment) {

    private val model: FeedVM by activityViewModels()
    private val binding by viewBinding(FeedFragmentBinding::bind)

    private lateinit var pagerAdapter: FeedPagerAdapter

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        private val backCountToExit = 2
        private var currentBackCount = backCountToExit
        override fun handleOnBackPressed() {

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
        requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallback)

        pagerAdapter = FeedPagerAdapter(childFragmentManager, lifecycle, model.queryFlow)
        lifecycleScope.launch {

            binding.viewpager.adapter = pagerAdapter

            val bundle = bundleOf()
            pagerAdapter.tabs.forEachIndexed { index, tabObj ->
                val tab = binding.tab.newTab()
                tab.text = tabObj.title
                tab.contentDescription = tabObj.desc
                tab.icon = tabObj.iconId?.let { resources.getDrw(it) }

                if (tabObj.selected && model.tabPosition == -1) {
                    model.tabPosition = index
                    tab.select()
                }
                binding.tab.addTab(tab)
            }


            binding.tab.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    binding.viewpager.setCurrentItem(tab.position, true)

                    val tabs = pagerAdapter.tabs[tab.position]
                    if (tabs.id == FeedFilterType.DUMMY) {
                        RssAddDialog(requireContext()).show()
                        binding.tab.selectTab(binding.tab.getTabAt(model.tabPosition))
                        return
                    }


                    binding.tvHead.setTextAnimation(tabs.desc)
                    model.tabPosition = tab.position

                    val bundleKey = FeedPagerFragment.getItemCountKey(tabs.id)
                    val itemCount = bundle.getInt(bundleKey)
                    model.totalItemFlow.tryEmit(itemCount)
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {

                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                }
            })

            binding.viewpager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    binding.tab.selectTab(binding.tab.getTabAt(position), true)
//                    val allowSlideH = position == 0
//                    binding.viewpager.isUserInputEnabled = allowSlideH
                }

            })


            binding.tab.selectTab(binding.tab.getTabAt(model.tabPosition), true)

            childFragmentManager.setFragmentResultListener(
                FeedPagerFragment.RESULT_KEY,
                viewLifecycleOwner
            ) { _, result ->
                bundle.putAll(result)
                val key = result.getString(FeedPagerFragment.RESULT_KEY)
                val itemCount = result.getInt(key, 0)
                model.totalItemFlow.tryEmit(itemCount)
            }



            binding.appbar.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
                // eg: 20
                val totalOffset = appBarLayout.totalScrollRange - appBarLayout.totalScrollRange * .5
                // eg: 10, -10/-20 = .5
                val offsetAsAlpha = verticalOffset.toFloat() / -totalOffset.toFloat()
                val alpha = 1f - offsetAsAlpha
                binding.gHeader.alpha = alpha

                if (binding.searchBar.isVisible && alpha < .5F) {
                    binding.searchParent.isGone = true
                    binding.searchBar.isIconified = true
                }
            }

            binding.toolbar.setNavigationOnClickListener { gotoRssSettingScreen() }
            binding.toolbar.setOnMenuItemClickListener {
                if (it.itemId == R.id.sc_search) {
                    binding.searchParent.apply {
                        lifecycleScope.launch {
                            withContext(Dispatchers.Main) {
                                binding.appbar.setExpanded(true, true)
                                binding.appbar.waitUntilExpanded()
                                isVisible = true
                                binding.searchBar.isIconified = false
                            }
                        }

                    }
                }
                true
            }
            binding.searchBar.setOnQueryTextListener(model.onquery)
            binding.searchBar.setOnCloseListener {
                binding.searchParent.isGone = true
                true
            }

//            binding.btPlaylist.setOnClickListener { gotoPlayerScreen() }

            model.totalItemFlow.asLiveData().observe(viewLifecycleOwner) {
                "$it Berita".also { t -> binding.tvHeadTotal.text = t }
            }

            binding.playerFl.setOnClickListener {
                binding.playerFl.playerView?.articleEntity?.let {
                    gotoPlayerScreen()
                }
            }

        }

        Timber.d("end")
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

    private fun gotoRssSettingScreen() {
        findNavigation().submit {
            fragmentManager.beginTransaction()
                .add(containerId, RssCollectionFragment::class.java, null, "")
                .hide(this@FeedFragment)
                .addToBackStack("")
                .commit()
        }
    }

    private fun gotoPlayerScreen() {
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