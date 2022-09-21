package com.esabook.auzen.article.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.esabook.auzen.App
import com.esabook.auzen.R
import com.esabook.auzen.databinding.RssFragmentBinding
import com.esabook.auzen.extentions.collectLatest2
import com.esabook.auzen.extentions.doSync
import com.esabook.auzen.extentions.post2
import com.esabook.auzen.extentions.toast
import com.esabook.auzen.ui.ActionStateListener.ActionState.*
import com.esabook.auzen.ui.OnItemClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


class RssCollectionFragment : Fragment() {
    lateinit var binding: RssFragmentBinding
    val model: RssCollectionVM by viewModels()

    private val onItemClickListener = OnItemClickListener { _, _, payload ->
        viewLifecycleOwner.lifecycleScope.launch {
            when (payload) {
                is RssCollectionAction.Sync -> {
                    try {
                        payload.actionState?.updateState(START)
                        listOf(payload.rss).doSync {
                            payload.actionState?.updateState(SUCCESS)
                        }

                    } catch (e: Exception) {
                        context?.toast(e.message)
                        payload.actionState?.updateState(FAIL)
                    }

                }
                is RssCollectionAction.Delete -> {
                    withContext(Dispatchers.IO) {
                        App.db.rssDao().delete(payload.rss)
                        App.db.articleDao().deleteAllByRssGuid(payload.rss.guid)
                    }

                }
                is RssCollectionAction.MuteAutoSync -> {
                    withContext(Dispatchers.IO) {
                        payload.actionState?.updateState(START)
                        val isMutedInverse = payload.rss.muteAutoSync.not()
                        val newRss = payload.rss.copy(muteAutoSync = isMutedInverse)
                        payload.rss.muteAutoSync = isMutedInverse
                        App.db.rssDao().update(newRss)
                        payload.actionState?.updateState(SUCCESS)
                    }
                }

                is RssCollectionAction.PurgeAndRebuild -> {
                    withContext(Dispatchers.IO) {
                        payload.actionState?.updateState(START)
                        App.db.articleDao().deleteAllByRssGuid(payload.rss.guid)
                        payload.actionState?.updateState(SUCCESS)
                    }
                }

                is RssCollectionAction.Filter -> {
                    sendFragmentResult(
                        payload.rss.title ?: payload.rss.link ?: "",
                        payload.rss.guid
                    )
                }
            }

            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.invalidateTotalArticle()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = RssFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rvData.adapter = model.rssAdapter
        model.rssAdapter.onItemClickListener = onItemClickListener

        initListener()
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                lifecycleScope.launch {
                    model.fillAdapter()

                }
            }
        }
    }

    private fun initListener() = lifecycleScope.launch(Dispatchers.IO) {

        binding.toolbar.setOnMenuItemClickListener {
            return@setOnMenuItemClickListener when (it.itemId) {
                R.id.sc_sync_all -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        binding.progressHorizontal.post2 { show() }
                        App.db.rssDao().getAll().collectLatest2 { list ->
                            list.filter { r -> !r.muteAutoSync }.doSync {
                                binding.progressHorizontal.post2 { hide() }
                            }
                        }
                    }
                    true
                }
                R.id.sc_add_rss -> {
                    RssAddDialog(requireContext()).show()
                    true
                }
                else -> false
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            it.context?.toast("TODO")
        }

        binding.cardAll.setOnClickListener {
            sendFragmentResult("Semua", "")
        }

        model.totalArticleEntries.collectLatest2 {
            binding.tvSubtitle.text = "$it Berita"
        }

        model.rssAdapter.onItemClickListener = onItemClickListener

    }

    private fun sendFragmentResult(title: String, vararg guids: String) {
        try {
            val guidBundle = bundleOf(
                KEY_SELECTED_MENU_TITLE to title,
                KEY_SELECTED_RSS_GUID to guids.joinToString(GUID_SEPARATOR)
            )
            parentFragmentManager.setFragmentResult(
                KEY_RESULT,
                guidBundle
            )
        } catch (e: Exception) {
            Timber.e(e)
            context?.toast(e.message)
        }

    }

    companion object {
        const val KEY_RESULT = "rss_collection"
        const val KEY_SELECTED_MENU_TITLE = "selected_menu_title"
        const val KEY_SELECTED_RSS_GUID = "selected_rss_guid"
        const val GUID_SEPARATOR = ";|"
    }

}