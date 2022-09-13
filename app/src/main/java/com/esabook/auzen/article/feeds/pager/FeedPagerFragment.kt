package com.esabook.auzen.article.feeds.pager

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.esabook.auzen.App
import com.esabook.auzen.R
import com.esabook.auzen.article.feeds.FeedFilterType
import com.esabook.auzen.article.player.PlayerFragment
import com.esabook.auzen.article.player.PlayerView
import com.esabook.auzen.article.readview.ReadFragment
import com.esabook.auzen.article.subscription.RssCollectionFragment
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.databinding.FeedPagerFragmentBinding
import com.esabook.auzen.extentions.*
import com.esabook.auzen.extentions.NewsParserUtils.toSpeakable
import com.esabook.auzen.ui.Navigation.Companion.findNavigation
import com.esabook.auzen.ui.OnItemClickListener
import com.esabook.auzen.ui.ProgressDialog
import com.esabook.auzen.ui.StickHeaderItemDecoration
import com.esabook.auzen.ui.viewBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.cancellable
import timber.log.Timber

class FeedPagerFragment : Fragment(R.layout.feed_pager_fragment) {

    private val binding by viewBinding(FeedPagerFragmentBinding::bind)
    private val model: FeedPagerVM by viewModels()
    private lateinit var progressDialog: ProgressDialog

    private lateinit var feedOptionMenu: FeedOptionMenu
    private lateinit var payloadFilterType: FeedFilterType
    private lateinit var player: PlayerView

    private var queryFlow: MutableStateFlow<String?>? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("start")
        payloadFilterType = FeedFilterType.valueOf(arguments?.getString(PAYLOAD_FILTER_TYPE_NAME)!!)

        lifecycleScope.launch {
            progressDialog = ProgressDialog(requireContext())
            feedOptionMenu = FeedOptionMenu(requireContext())
            player = PlayerView(FrameLayout(requireContext()))
        }
        binding.rvData.adapter = model.itemAdapter
        binding.rvData.addItemDecoration(StickHeaderItemDecoration(model.itemAdapter))
        binding.rvData.setHasFixedSize(true)


        model.itemAdapter.addOnPagesUpdatedListener {
            sendFragmentResult()
            maybeEmptyState()
        }

        model.itemAdapter.addLoadStateListener {
            sendFragmentResult()
            val refresh = it.refresh
            if (refresh is LoadState.NotLoading && refresh.endOfPaginationReached) {
               maybeEmptyState()
            }
        }


        binding.empty.tvButton.setOnClickListener {
            gotoRssSettingScreen()
        }

        binding.swipeRefresh.setProgressViewOffset(true, 5, 100)
        binding.swipeRefresh.setOnRefreshListener {
            App.db.launchIo {
                rssDao().getAll().collectLatest2 {
                    it.filter { !it.muteAutoSync }.doSync { binding.swipeRefresh.post2 { isRefreshing = false } }
                }
            }
        }

        if (payloadFilterType == FeedFilterType.PLAYLIST){
            binding.empty.run {
                ivIllustration.setAnimation(R.raw.search_list)
                tvHeader.text = getString(R.string.empty_playlist)
                tvDescription.text = getString(R.string.empty_playlist_shuffle_ok)
                tvButton.text = getString(R.string.shuffle)
                tvButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                tvButton.setOnClickListener {
                    App.db.launchIo {
                        val data = articleDao().loadAllWithUnread(true, 20)
                        var job: Job? = null
                        job = ioScope.launch {
                            data.cancellable().collectLatest2 { list ->
                                list.forEach { art ->
                                    articleQueueDao().update(art.guid, true)
                                }
                                player.speakPlay()
                                gotoPlayerScreen()
                                job?.cancel()
                                job = null
                            }
                        }
                    }
                }
            }
        }


        lifecycleScope.launch {
            initAction()
            model.filterType = payloadFilterType
            model.feeds.collectLatest2 {
                model.adapterSubmitList(it, lifecycle)
            }
        }

        Timber.d("end")
    }

    private fun maybeEmptyState(){
        val isEmpty = model.itemAdapter.itemCount < 1
        binding.rvData.isGone = isEmpty
        binding.empty.root.isVisible = isEmpty
    }

    private fun sendFragmentResult() {
        try {
            val bundleKey = getItemCountKey(payloadFilterType)
            val result = bundleOf(
                RESULT_KEY to bundleKey,
                bundleKey to model.itemAdapter.itemCount
            )
            parentFragmentManager.setFragmentResult(RESULT_KEY, result)
        } catch (e: Exception) {
            Timber.e(e)
        }

    }

    override fun onResume() {
        queryFlow?.value?.let {
            model.searchQueryAction.trySend(it)
        }
        queryFlow?.asLiveData()?.observe(viewLifecycleOwner, this::onQueryObserver)
        super.onResume()
    }

    override fun onPause() {
        queryFlow?.asLiveData()?.removeObserver(this::onQueryObserver)
        super.onPause()

    }

    fun setQueryDispatcher(queryFlow: MutableStateFlow<String?>) {
        this.queryFlow = queryFlow
    }

    private fun onQueryObserver(q: String?) {
        model.searchQueryAction.trySend(q ?: "")
    }

    private suspend fun initAction() {
        withContext(Dispatchers.Default) {
            delay(500)

            model.itemAdapter.onItemClickListener = OnItemClickListener { _, _, payload ->
                if (payload is ArticleEntity) {
                    gotoReadingScreen(payload)
                }
            }

            model.itemAdapter.onLongItemClickListener =
                OnItemClickListener { holder, pos, payload ->
                    if (payload !is ArticleEntity || holder !is FeedItemViewHolder)
                        return@OnItemClickListener

                    feedOptionMenu.showWithPayload(holder, pos, payload)
                }

            feedOptionMenu.onshow {
                lifecycleScope.launch {
                    getMenuBinding(R.id.sc_mark_as_read)?.let { m ->
                        if (payload?.isUnread == true) {
                            m.title.setText(R.string.mark_as_read)
                            m.icon.setImageResource(R.drawable.ic_read_check)
                        } else {
                            m.title.setText(R.string.mark_as_unread)
                            m.icon.setImageResource(R.drawable.ic_read_uncheck)
                        }
                    }

                    getMenuBinding(R.id.sc_add_to_playlist)?.let { m ->
                        if (payload?.isPlayListQueue == true) {
                            m.title.setText(R.string.remove_from_speech_queue)
                            m.icon.setImageResource(R.drawable.ic_baseline_bookmark_remove)
                        } else {
                            m.title.setText(R.string.add_to_speech_queue)
                            m.icon.setImageResource(R.drawable.ic_baseline_bookmark_add)
                        }
                    }
                }
            }


            feedOptionMenu.setOnMenuClickListener { _, pos, payload, view ->
                when (view.id) {
                    R.id.sc_mark_as_read -> {
                        progressDialog.setRefreshing(true)
                        App.db.launchIo {
                            val inverseCurrentUnread = payload.isUnread.not()
                            articleDao().markAsRead(payload.guid, inverseCurrentUnread)

                            model.invalidateItem(
                                pos,
                                payload.copy(isUnread = inverseCurrentUnread)
                            ) {
                                progressDialog.setRefreshing(false)
                            }
                        }
                    }

                    R.id.sc_add_to_playlist -> {
                        progressDialog.setRefreshing(true)
                        App.db.launchIo {
                            val inverseCurrentIsInPlaylist = payload.isPlayListQueue.not()
                            articleQueueDao().update(
                                payload.guid,
                                inverseCurrentIsInPlaylist
                            )

                            model.invalidateItem(
                                pos,
                                payload.copy(isPlayListQueue = inverseCurrentIsInPlaylist)
                            ) {
                                progressDialog.setRefreshing(false)
                            }

                        }
                    }

                    R.id.sc_read_in_speech -> {
                        var job: Job? = null
                        progressDialog.onShow {
                            job = lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    App.db.articleQueueDao().update(payload.guid, true)
                                    payload.link?.let {
                                        val words = NewsParserUtils.getArticle(it)
                                            ?.toSpeakable() ?: listOf()
                                        if (isActive) {
                                            player.setWords(payload, words)
                                            player.speakPlay()
                                        }
                                    }
                                    setRefreshing(false)
                                }


                            }
                        }.onDismiss {
                            job?.cancel()
                        }.setRefreshing(true)


                    }

                    R.id.sc_open_browser -> {
                        context?.openLinkInExternalBrowser(payload.link)
                    }
                    R.id.sc_share_link_external -> {
                        context?.shareTextToExternal(payload.link)
                    }
                    R.id.sc_copy_link -> {
                        context?.copyToClipboard(payload.link)
                    }
                    R.id.sc_delete -> {
                        model.delete(payload)
                    }
                }
            }
        }

    }

    private fun gotoReadingScreen(payload: ArticleEntity) {
        val args = bundleOf(ReadFragment.PAYLOAD_LINK to payload.link)
        findNavigation().submit {
            fragmentManager.beginTransaction()
                .add(containerId, ReadFragment::class.java, args, "")
                .hide(getLastVisibleFragment())
                .addToBackStack("")
                .commit()
        }
    }


    private fun gotoRssSettingScreen() {
        findNavigation().submit {
            fragmentManager.beginTransaction()
                .add(containerId, RssCollectionFragment::class.java, null, "")
                .hide(getLastVisibleFragment())
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

    companion object {
        const val PAYLOAD_FILTER_TYPE_NAME = "payload_filter_type"
        const val RESULT_KEY = "feed_pager"

        fun getItemCountKey(type: FeedFilterType) = type.name + "itemCount"
    }
}