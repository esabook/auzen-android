package com.esabook.auzen.article.readview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.forEach
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.esabook.auzen.App
import com.esabook.auzen.R
import com.esabook.auzen.article.player.PlayerFragment
import com.esabook.auzen.article.player.PlayerView
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.databinding.ReadFragmentBinding
import com.esabook.auzen.extentions.NewsParserUtils
import com.esabook.auzen.extentions.NewsParserUtils.toSpeakable
import com.esabook.auzen.extentions.copyToClipboard
import com.esabook.auzen.extentions.getDrw
import com.esabook.auzen.extentions.openLinkInExternalBrowser
import com.esabook.auzen.extentions.shareTextToExternal
import com.esabook.auzen.extentions.toast
import com.esabook.auzen.extentions.tooltip
import com.esabook.auzen.ui.Navigation.Companion.findNavigation
import com.esabook.auzen.ui.ProgressDialog
import com.esabook.auzen.ui.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Article
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber


class ReadFragment : Fragment(R.layout.read_fragment) {

    companion object {
        const val PAYLOAD_LINK = "payload_link"
        const val FONT_NAME_KEY = "font_name_key"
    }

    private val binding by viewBinding(ReadFragmentBinding::bind)
    private val model: ReadVM by viewModels()
    private lateinit var progressDialog: ProgressDialog

    private val pref by lazy {
        context?.getSharedPreferences("ReadFragment", Context.MODE_PRIVATE)
    }


    private var selectedFontName: String? = null
        get() = pref?.getString(FONT_NAME_KEY, null)
        set(value) {
            field = value
            pref?.edit { putString(FONT_NAME_KEY, field) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                withContext(Dispatchers.IO) {
                    model.articleLink = arguments?.getString(PAYLOAD_LINK) ?: ""
                    App.db.articleDao().markAsReadByGuidOrLink(model.articleLink, model.articleLink)
                }
            }
        }
    }

    private var renderJob: Job? = null
    private fun renderArticle() = binding.let { b ->
        b.web.stopLoading()
        b.progressHorizontal.show()
        renderJob?.cancel()


        renderJob = lifecycleScope.launch {

            if (b.web.url == model.articleLink) {
                if (b.web.canGoBack())
                    b.web.goBack()
                else
                    b.web.clearHistory()
            }
            withContext(Dispatchers.IO) {
                try {
                    if (model.readibilityModeOn.value == true) {
                        val it = model.article.value
                        if (it?.uri != model.articleLink) {
                            model.generateArticle(false)
                            return@withContext
                        }

                        loadArticleToBrowser(it, model.articleEntity)

                    } else {
                        withContext(Dispatchers.Main) {
                            val url = b.web.url ?: model.articleLink
                            b.web.loadUrl(url)
                        }
                    }

                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    private suspend fun loadArticleToBrowser(article: Article, articleEntity: ArticleEntity?) =
        withContext(Dispatchers.IO) {
            val content = model.getHtmlContent(article, articleEntity)
            withContext(Dispatchers.Main) {
                binding.web.loadDataWithBaseURL(
                    article.uri,
                    content,
                    "text/html",
                    "utf-8",
                    article.uri
                )
            }
        }


    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        progressDialog = ProgressDialog(requireContext())

        lifecycleScope.launch {
            selectedFontName?.let { name ->
                model.selectedFont = ReadVM.fontFamilies.asList().first { it.name == name }
            }
            model.generateArticle()
            initView()
        }
    }

    @SuppressLint("RestrictedApi")
    private fun initView() = binding.let { b ->

        model.article.observe(viewLifecycleOwner) {
            if (it != null)
                renderArticle()
            else {
                b.progressHorizontal.hide()
                b.swipeRefresh.isRefreshing = false
            }

            renderToolbar(model.articleEntity?.sourceLink)
        }

        model.readibilityModeOn.observe(viewLifecycleOwner) { bool ->
            b.toolbar.menu.forEach {
                if (it.itemId == R.id.sc_readibility_mode) {
                    if (bool) {
                        it.icon = resources.getDrw(R.drawable.ic_baseline_book)
                    } else {
                        it.icon = resources.getDrw(R.drawable.ic_baseline_book_off)
                    }
                }
            }
            renderArticle()
        }

        b.swipeRefresh.setProgressViewOffset(true, 5, 100)
        b.swipeRefresh.setOnRefreshListener {
            renderArticle()
        }

        b.web.settings.javaScriptEnabled = true
        b.web.settings.domStorageEnabled = true
        b.web.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        b.web.addOnScrollChanged { _, y, _, oldY ->
            b.progressScroll.apply {
                if (max == 0) {
                    max = b.web.getTotalContentHeight() - b.web.height
                }
                progress = y
            }
        }

        b.web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                b.progressHorizontal.show()

                if (url != model.articleEntity?.link) renderToolbar(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                b.progressHorizontal.hide()
                b.progressScroll.max = b.web.getTotalContentHeight() - view!!.height
                b.swipeRefresh.isRefreshing = false

            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                b.progressScroll.max = b.web.getTotalContentHeight() - view!!.height
                request?.url?.toString()?.let {
                    model.articleLink = it
                    if (model.readibilityModeOn.value == true) {
                        model.generateArticle()
                        return true
                    }
                }
                return false
            }
        }

        model.isUnread.observe(viewLifecycleOwner) {
            val readIcon = if (it) R.drawable.ic_read_uncheck
            else R.drawable.ic_read_check
            b.btMarkRead.setImageResource(readIcon)

            val tooltipMsg = if (it) R.string.mark_as_unread
            else R.string.mark_as_read
            b.btMarkRead.tooltip(tooltipMsg)
        }

        model.isInPlaylist.observe(viewLifecycleOwner) {
            val playlistIcon = if (it) R.drawable.ic_baseline_bookmark_remove
            else R.drawable.ic_baseline_bookmark_add
            b.btAddToQueue.setImageResource(playlistIcon)


            val tooltipMsg = if (it) R.string.remove_from_speech_queue
            else R.string.add_to_speech_queue
            b.btAddToQueue.tooltip(tooltipMsg)
        }

        val player = b.playerFl.playerView
        b.btPlay2.setOnClickListener {
            if (model.readibilityModeOn.value?.not() == true) {
                context?.toast("Perlu ganti mode baca untuk memulai")
                return@setOnClickListener
            }

            if (player?.speakLink != model.articleLink) {
                player?.speakStopAndReset()
            }

            if (player != null) {
                when (player.playerState) {
                    PlayerView.PlayerState.STOPPED -> {
                        var job: Job? = null
                        progressDialog.onShow {
                            job = lifecycleScope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Default) {
                                    val text = model.article.value?.toSpeakable() ?: listOf()
                                    player.setWords(model.articleEntity, text)
                                    player.speakPlay()
                                    progressDialog.setRefreshing(false)
                                }
                            }
                        }.onDismiss {
                            job?.cancel()

                        }.setRefreshing(true)


                    }
                    PlayerView.PlayerState.PAUSED -> {
                        player.speakPlay()
                    }
                    PlayerView.PlayerState.PLAYING -> {
                        player.speakPause()
                    }
                    PlayerView.PlayerState.LOADING -> {
                        //
                    }
                }
            }

        }

        b.btMarkRead.setOnClickListener {
            model.markUnRead(model.isUnread.value?.not() ?: false)
        }

        b.btAddToQueue.setOnClickListener {
            model.markInPlaylist(model.isInPlaylist.value?.not() ?: true)
        }

        b.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }


        (b.toolbar.menu as? MenuBuilder)?.setOptionalIconsVisible(true)

        b.toolbar.setOnMenuItemClickListener {
            val url = model.articleEntity?.link ?: return@setOnMenuItemClickListener false

            when (it.itemId) {
                R.id.sc_reader_theme -> {
                    model.selectedFont = ReadVM.fontFamilies.asList()
                        .first { f -> f.name != model.selectedFont.name }

                    selectedFontName = model.selectedFont.name

                    lifecycleScope.launch {
                        model.article.value?.let { it1 ->
                            loadArticleToBrowser(it1, model.articleEntity)
                        }
                    }

                }
                R.id.sc_readibility_mode -> {
                    model.readibilityModeOn.apply { postValue(value?.not() ?: true) }
                }
                R.id.sc_open_browser -> {
                    context?.openLinkInExternalBrowser(url)
                }
                R.id.sc_copy_link -> {
                    context?.copyToClipboard(url)
                }
                R.id.sc_share_link_external -> {
                    context?.shareTextToExternal(url)
                }
            }
            true
        }

        b.playerFl.setOnClickListener {
            player?.articleEntity?.let {
                gotoPlayerScreen()
            }
        }

        player?.playerStateLiveData?.observe(viewLifecycleOwner) {
            if (player.speakLink != model.articleLink) {
                b.btPlay2.setImageResource(R.drawable.ic_baseline_play_arrow)
                return@observe
            }

            if (it == PlayerView.PlayerState.PLAYING)
                b.btPlay2.setImageResource(R.drawable.ic_round_pause)
            else
                b.btPlay2.setImageResource(R.drawable.ic_baseline_play_arrow)
        }

        binding.web.addOnScrollChanged(this::onWebScrollListener)
    }

    private fun renderToolbar(url: String?) = binding.let { b ->
        val uri = url?.toHttpUrlOrNull()
        val mUrl = NewsParserUtils.getFaviconUrl(url ?: "")

        Glide.with(b.toolbar)
            .load(mUrl)
            .override(resources.getDimensionPixelSize(R.dimen.dp_18))
            .fitCenter()
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    b.toolbar.logo = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    b.toolbar.logo = null
                }
            })
        b.toolbar.title = uri?.host


    }

    override fun onDestroyView() {
        try {
            binding.web.stopLoading()
            binding.web.loadUrl("about:blank")
            binding.web.removeAllViews()
            binding.web.destroy()
        } catch (e: Exception) {
            Timber.e(e)
        }

        super.onDestroyView()
    }

    private fun onWebScrollListener(x: Int, y: Int, oldx: Int, oldy: Int) {
        try {
            if (binding.web.canScrollVertically(1).not())
                model.markUnRead(false)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun gotoReadingScreen(payload: ArticleEntity) {
        val args = bundleOf(PAYLOAD_LINK to payload.link)
        findNavigation().submit {
            fragmentManager.beginTransaction()
                .add(containerId, ReadFragment::class.java, args, "")
                .hide(this@ReadFragment)
                .addToBackStack("")
                .commit()
        }
    }


    private fun gotoPlayerScreen() {
        val fragment = PlayerFragment()
        fragment.onPlayerClickListener = View.OnClickListener {
            fragment.player.articleEntity?.let { it1 ->
                if (it1.link == model.articleLink) {
                    fragment.dismissAllowingStateLoss()
                    return@OnClickListener
                }

                gotoReadingScreen(it1)
                fragment.dismissAllowingStateLoss()
            }
        }
        fragment.lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event.targetState) {
                Lifecycle.State.RESUMED -> {
                    binding.playerFl.isGone = true
                }
                Lifecycle.State.DESTROYED -> {
                    val fl = binding.playerFl.playerView
                    if (fl?.playerState != PlayerView.PlayerState.STOPPED) {
                        binding.playerFl.isVisible = true
                    }
                }
                else -> {}
            }
        }
        fragment.show(parentFragmentManager, "")
    }

}