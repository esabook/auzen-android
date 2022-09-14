package com.esabook.auzen.article.player

import android.content.Context
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.view.ViewGroup
import androidx.core.view.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.esabook.auzen.App
import com.esabook.auzen.R
import com.esabook.auzen.audio.tts.TTSManager
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.databinding.ToolbarAudioPlayControlBinding
import com.esabook.auzen.extentions.NewsParserUtils
import com.esabook.auzen.extentions.NewsParserUtils.toSpeakable
import com.esabook.auzen.extentions.collectLatest2
import com.esabook.auzen.extentions.layoutInflater
import com.esabook.auzen.extentions.post2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*

class PlayerView(viewGroup: ViewGroup) {

    init {
        initEngine(viewGroup.context.applicationContext)
    }

    private var mViewGroup = WeakReference(viewGroup)
    private var binding: ToolbarAudioPlayControlBinding? = null


    val playerStateLiveData: LiveData<PlayerState>
        get() = mPlayerStateLiveData

    val progressPercent: LiveData<Int>
        get() = mProgressPercent.asLiveData()

    val speakLink: String?
        get() = mArticleEntity?.link

    val playerState: PlayerState
        get() = mPlayerState

    val articleEntity: ArticleEntity?
        get() = mArticleEntity


    private fun invalidateView() {
        binding?.run {
            root.post2 {
                when (playerState) {
                    PlayerState.PLAYING -> {
                        isVisible = true
                        btPlay.setImageResource(R.drawable.ic_round_pause)
                        progressCircular.isIndeterminate = false
                        progressCircular.show()
                    }
                    PlayerState.PAUSED -> {
                        btPlay.setImageResource(R.drawable.ic_baseline_play_arrow)
                        progressCircular.isIndeterminate = false
                    }
                    PlayerState.STOPPED -> {
                        isGone = true
                        progressCircular.hide()
                    }
                    PlayerState.LOADING -> {
                        progressCircular.isIndeterminate = true
                        progressCircular.show()
                    }
                }
                if (tvTitle.text != mArticleEntity?.title)
                    tvTitle.text = mArticleEntity?.title
            }
        }
    }

    fun setPaddingRight(px: Int) {
        binding?.root?.post2 {
            updatePadding(paddingLeft, paddingTop, px, paddingBottom)
        }
    }


    fun getView(): View? {

        if (mViewGroup.get() == null)
            return null

        if (binding == null) {
            binding = ToolbarAudioPlayControlBinding.inflate(
                mViewGroup.get()!!.layoutInflater(), mViewGroup.get()!!, false
            )

            binding?.run {
                btPlay.setOnClickListener {
                    when (playerState) {
                        PlayerState.PLAYING -> speakPause()
                        PlayerState.PAUSED -> speakPlay()
                        else -> {}
                    }
                }
                btClose.setOnClickListener {
                    speakStopAndReset()
                }
                // start marquee
                tvTitle.isSelected = true

                root.doOnAttach {
                    progressPercent.observeForever(this@PlayerView::invalidateProgress)
                    playerStateLiveData.observeForever(this@PlayerView::invalidateObserver)
                }

                root.doOnDetach {
                    progressPercent.removeObserver(this@PlayerView::invalidateProgress)
                    playerStateLiveData.removeObserver(this@PlayerView::invalidateObserver)
                }
            }

        }

        return binding?.root
    }

    private fun invalidateProgress(int: Int) {
        binding?.progressCircular?.progress = int
    }

    private fun invalidateObserver(state: PlayerState) {
        invalidateView()
    }

    fun speakPlay(articleEntity: ArticleEntity) {
        if (articleEntity.link == speakLink)
            return

        internalPlay(articleEntity)
    }

    fun setWords(articleEntity: ArticleEntity?, words: List<String>) {
        if (articleEntity?.link == speakLink)
            return

        speakStopAndReset()
        mArticleEntity = articleEntity
        speakQueue = words
    }


    fun speakPlay() {
        speakNextParagraph()
    }

    fun speakPause() {
        mPlayerState = PlayerState.PAUSED
        TTSManager.stop()
    }

    fun speakStopAndReset() {
        selfSpeakStopAndReset()
    }


    enum class PlayerState {
        PLAYING, PAUSED, STOPPED, LOADING
    }

    companion object {


        val linkQueue: Deque<ArticleEntity> = LinkedList()

        private fun initEngine(ctx: Context) {
            if (inited) return
            inited = true
            TTSManager.initSpeech(ctx)
            TTSManager.utteranceProgressListener = utteranceProgressListener
        }

        private fun selfSpeakStopAndReset() {
            TTSManager.stop()
            mPlayerState = PlayerState.STOPPED
            speakQueue = listOf()
            mArticleEntity = null
            lastDoneID = -1
            mProgressPercent.tryEmit(0)
        }

        private var inited = false

        private val mPlayerStateLiveData = MutableLiveData<PlayerState>()

        private var speakQueue = listOf<String>()

        private var lastDoneID = -1

        private var mArticleEntity: ArticleEntity? = null

        private var mPlayerState = PlayerState.STOPPED
            private set(value) {
                field = value
                mPlayerStateLiveData.postValue(field)
            }

        private val mProgressPercent = MutableStateFlow(0)

        private var utteranceProgressListener = object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {

            }

            override fun onDone(utteranceId: String) {
                lastDoneID = utteranceId.toInt()
                if (mPlayerState == PlayerState.PLAYING)
                    speakNextParagraph()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {

            }
        }


        private fun speakNextParagraph() {
            mProgressPercent.tryEmit(getProgress())

            val nextId = lastDoneID + 1
            val nextQueue = speakQueue.getOrNull(nextId)
            if (nextQueue == null) {
                markArticleAsRead()
                removeFromPlaylistQueue()
                selfSpeakStopAndReset()
                speakNextQueue()
                return
            }

            TTSManager.playSpeechToSpeaker(nextQueue, nextId.toString())
            mPlayerState = PlayerState.PLAYING
        }

        private fun markArticleAsRead() {
            mArticleEntity?.link?.let {
                App.db.launchIo { articleDao().markAsReadByGuidOrLink(mArticleEntity!!.guid, it) }
            }
        }

        private fun removeFromPlaylistQueue() {
            mArticleEntity?.guid?.let {
                App.db.launchIo { articleQueueDao().update(it, false) }
            }
        }

        private fun getProgress(): Int {
            val totalParagraph = speakQueue.size
            val currentParagraphIndex = lastDoneID

            //2 from 10 is 20%
            //2 from 8 is
            return try {
                currentParagraphIndex * 100 / totalParagraph
            } catch (e: Exception) {
                0
            }
        }

        private val dbPlaylist by lazy { App.db.articleQueueDao().getAll() }

        private fun speakNextQueue() {
            if (linkQueue.isEmpty()) {
                Timber.d("forceLoadDb && linkQueue.isEmpty()")
                App.db.launchIo {
                    var job: Job? = null
                    job = ioScope.launch {
                        dbPlaylist.collectLatest2 {
                            val sortUnplayedLinkToFirst = it
                            linkQueue.clear()
                            linkQueue.addAll(sortUnplayedLinkToFirst)

                            if (linkQueue.isEmpty().not()) {
                                Timber.d("New list available in playedList, try to play article")
                                speakNextQueue()
                            }
                            Timber.d("forceLoadDb && linkQueueSize = ${linkQueue.size}")
                            job?.cancel()
                            job = null
                        }
                    }
                }

                return
            }

            try {
                linkQueue.removeFirst()?.let {
                    internalPlay(it)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

        }

        private fun internalPlay(articleEntity: ArticleEntity) {
            App.db.ioScope.launch {
                withContext(Dispatchers.IO) {
                    mArticleEntity = articleEntity
                    mPlayerState = PlayerState.LOADING
                    try {
                        val link = articleEntity.link!!
                        val words = NewsParserUtils.getArticle(link)?.toSpeakable()
                        speakQueue = words!!
                        speakNextParagraph()

                    } catch (e: Exception) {
                        selfSpeakStopAndReset()
                    }
                }
            }
        }
    }
}