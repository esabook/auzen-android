package com.esabook.auzen.article.player

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.esabook.auzen.App
import com.esabook.auzen.R
import com.esabook.auzen.article.player.PlayerView.PlayerState.LOADING
import com.esabook.auzen.article.player.PlayerView.PlayerState.PLAYING
import com.esabook.auzen.audio.tts.TTSManager
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.databinding.PlayerFragmentBinding
import com.esabook.auzen.extentions.collectLatest2
import com.esabook.auzen.extentions.post2
import com.esabook.auzen.ui.OnItemClickListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import timber.log.Timber
import java.util.*

class PlayerFragment : BottomSheetDialogFragment() {
    private var binding: PlayerFragmentBinding? = null
    private val model: PlayerVM by viewModels()

    lateinit var player: PlayerView
    private lateinit var dragHelper: ItemTouchHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = PlayerFragmentBinding.inflate(layoutInflater, container, false)
        player = PlayerView(binding!!.root)
        binding?.searchBar?.setOnQueryTextListener(model.onQuery)

        return binding?.root
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(v: View, newState: Int) {
            binding?.bottomSpace?.minimumHeight = v.y.toInt()
        }

        override fun onSlide(v: View, offset: Float) {
            binding?.bottomSpace?.minimumHeight = v.y.toInt()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.rvData?.adapter = model.itemAdapter
        model.itemAdapter.onItemClickListener = OnItemClickListener { _, _, payload ->
            if (payload is ArticleEntity && payload.link != null) {
                player.speakPlay(payload)

            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.feeds.collectLatest2 { model.itemAdapter.submitList(it) }
            }
        }

        binding?.root?.parent?.let {
            val behavior = BottomSheetBehavior.from(it as View)
            behavior.addBottomSheetCallback(bottomSheetCallback)
            behavior.isFitToContents = false

            val treeObserver = object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    binding?.bottomSpace?.minimumHeight = it.y.toInt()
                    it.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
            it.viewTreeObserver.addOnGlobalLayoutListener(treeObserver)
        }

        val changeSpeedFlow = MutableStateFlow(TTSManager.currentSpeed)
        lifecycleScope.launch(Dispatchers.IO) {
            changeSpeedFlow.debounce(500).collectLatest2 { value ->
                if (TTSManager.currentSpeed == value)
                    return@collectLatest2

                TTSManager.currentSpeed = value
                if (player.playerState == PLAYING) {
                    player.speakPause()
                    player.speakPlay()
                }
            }
        }


        binding?.slideReaderSpeed?.value = TTSManager.currentSpeed
        "${TTSManager.currentSpeed}x".also { binding?.tvSpeechSpeedRate?.text = it }


        binding?.slideReaderSpeed?.post2 {
            addOnChangeListener { _, value, _ ->
                changeSpeedFlow.tryEmit(value)
                "${value}x".also { binding?.tvSpeechSpeedRate?.text = it }
            }
        }


        binding?.gBottom?.setOnClickListener {
            binding?.slideReaderSpeed?.isGone = true
        }

        binding?.tvSpeechSpeedRate?.setOnClickListener {
            binding?.slideReaderSpeed?.isGone = binding?.slideReaderSpeed?.isGone?.not() ?: true
        }


        player.playerStateLiveData.observe(viewLifecycleOwner) {

            binding?.tvTitle?.post2 {
                if (player.articleEntity?.title != text.toString()) {
                    val title = player.articleEntity?.title
                    text = title
                    isGone = title.isNullOrBlank() == true
                }
            }


            when (it) {
                PLAYING -> {
//                    progressDialog.setRefreshing(false)
                    binding?.btPlay?.setImageResource(R.drawable.ic_round_pause)
                    binding?.progressCircular?.isIndeterminate = false

                }
                LOADING -> {
                    binding?.progressCircular?.isIndeterminate = true
                }
                else -> {
                    binding?.progressCircular?.isIndeterminate = false
                    binding?.btPlay?.setImageResource(R.drawable.ic_baseline_play_arrow)
                }
            }
        }

        player.progressPercent.observe(viewLifecycleOwner) {
            binding?.progressCircular?.progress = it
        }

        binding?.btPlay?.setOnClickListener {

            if (player.playerState == PLAYING)
                player.speakPause()
            else {
                if (PlayerView.linkQueue.isEmpty() && model.itemAdapter.itemCount == 0) {
                    binding?.run {
                        Snackbar.make(
                            gBottom,
                            getString(R.string.empty_playlist_shuffle_ok),
                            Snackbar.LENGTH_SHORT
                        ).let {
                            it.anchorView = gBottom
                            it.setAction(getString(R.string.shuffle)) {
                                App.db.launchIo {
                                    val data = articleDao().loadAllWithUnread(true, 20)
                                    var job: Job? = null
                                    job = ioScope.launch {
                                        data.collectLatest2 { list ->
                                            list.forEach { art ->
                                                articleQueueDao().update(art.guid, true)
                                            }
                                            player.speakPlay()
                                            job?.cancel()
                                            job = null
                                        }
                                    }
                                }
                            }
                        }.show()
                    }
                } else {
                    player.speakPlay()
                }
            }
        }
        binding?.tvTitle?.setOnClickListener(onPlayerClickListener)

        binding?.btSetting?.setOnClickListener {
            try {
                val ttsIntent = Intent("com.android.settings.TTS_SETTINGS")
                startActivity(ttsIntent)
            } catch (e: Exception) {
            }
        }

        dragHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                lifecycleScope.launch(Dispatchers.IO) {
                    viewHolder.itemView.elevation = 16F

                    val from = viewHolder.bindingAdapterPosition
                    val to = target.bindingAdapterPosition

                    val oldList = model.itemAdapter.currentList
                    val newList: ArrayList<ArticleEntity> = ArrayList(oldList)
                    Collections.swap(newList, from, to)
                    withContext(Dispatchers.Main) {
                        model.itemAdapter.submitList(newList)
                    }

                    startUpdateDB(newList)

                }

                return true
            }

            var job: Job? = null
            var debounceTime = 500L
            fun startUpdateDB(newList: ArrayList<ArticleEntity>) {
                job?.cancel()
                job = lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Default) {
                        delay(debounceTime)
                        newList.forEachIndexed { index, it ->
                            App.db.articleQueueDao()
                                .update(it.guid, it.isPlayListQueue, index.toLong())
                            Timber.d("startUpdateDB: %s", index)
                        }
                    }
                }

            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                viewHolder?.itemView?.elevation = 0F
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        })

        dragHelper.attachToRecyclerView(binding!!.rvData)
        model.itemAdapter.dragHelper = dragHelper
    }

    var onPlayerClickListener: View.OnClickListener? = null
        set(value) {
            field = value
            binding?.tvTitle?.setOnClickListener(field)
        }

}