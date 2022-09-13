package com.esabook.auzen.article.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.esabook.auzen.App
import com.esabook.auzen.databinding.RssFragmentBinding
import com.esabook.auzen.extentions.doSync
import com.esabook.auzen.extentions.toast
import com.esabook.auzen.ui.ActionStateListener.ActionState.*
import com.esabook.auzen.ui.OnItemClickListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class RssCollectionFragment : Fragment() {
    lateinit var binding: RssFragmentBinding
    val model: RssCollectionVM by viewModels()

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

        initListener()
        lifecycleScope.launch {
            model.fillAdapter()

        }
    }

    private fun initListener() = lifecycleScope.launch(Dispatchers.IO) {

        model.rssAdapter.onItemClickListener = OnItemClickListener { _, _, payload ->
            lifecycleScope.launchWhenResumed {
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
                }

                model.invalidateTotalArticle()
            }
        }

    }

}