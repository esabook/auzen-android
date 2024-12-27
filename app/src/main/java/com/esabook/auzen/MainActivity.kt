package com.esabook.auzen

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.esabook.auzen.article.feeds.FeedFragment
import com.esabook.auzen.article.readview.ReadFragment
import com.esabook.auzen.extentions.toast
import com.esabook.auzen.ui.Navigation.Companion.findNavigation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private var isShouldSkipSplash = false

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = ContextCompat.getColor(this, R.color.black_80)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black_80)
        super.onCreate(savedInstanceState)

        isShouldSkipSplash = intent.getUrl() != null

        if (isShouldSkipSplash) {
            setContentView(R.layout.activity_main)
            openFeedScreen()
        } else {
            setContentView(R.layout.splash)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isShouldSkipSplash) {
            lifecycleScope.launch {
                delay(2000)
                findViewById<View?>(R.id.iv_logo)
                    ?.animate()
                    ?.setDuration(700)
                    ?.alpha(0f)
                    ?.withEndAction {
                        // After the fade out, remove the
                        // splash and set content view
                        isShouldSkipSplash = true
                        setContentView(R.layout.activity_main)
                        openFeedScreen()
                    }?.start()
            }
        }
    }

    private fun openFeedScreen() {
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        findNavigation().submit {
            fragmentManager.beginTransaction()
                .replace(containerId, FeedFragment::class.java, null)
                .commitNowAllowingStateLoss()
        }
        resolveReceivedIntent(intent)
    }

    private fun openReadScreen(link: String) {
        val args = bundleOf(ReadFragment.PAYLOAD_LINK to link)
        findNavigation().submit {
            fragmentManager.beginTransaction()
                .add(containerId, ReadFragment::class.java, args, "")
                .hide(getLastVisibleFragment())
                .addToBackStack("")
                .commit()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        resolveReceivedIntent(intent)
    }

    private fun resolveReceivedIntent(intent: Intent?) {

        var url: String = intent.getUrl() ?: return
        try {
            openReadScreen(url)
        } catch (e: Exception) {
            try {
                url = url
                    .trim(' ')
                    .split(" ")
                    .firstOrNull {
                        it.toHttpUrlOrNull() != null
                    } ?: return
                openReadScreen(url)
            } catch (e1: Exception) {
                Timber.e(e1)
                toast(getString(R.string.unsupported_received_link))

            }
        }
    }

    private fun Intent?.getUrl(): String? {
        if (this == null) return null

        val data = this.data?.toString()
            ?: this.dataString
            ?: this.getStringExtra(Intent.EXTRA_TEXT)
            ?: this.clipData?.getItemAt(0)?.text?.toString()
            ?: return null

        return data.toHttpUrlOrNull()?.toString()
    }

}