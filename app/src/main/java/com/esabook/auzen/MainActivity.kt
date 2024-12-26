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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

class MainActivity : AppCompatActivity(R.layout.splash) {

    var isShouldSkipSplash = false

    override fun onCreate(savedInstanceState: Bundle?) {
        window.statusBarColor = ContextCompat.getColor(this, R.color.black_80)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.black_80)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (isShouldSkipSplash.not()) {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
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

                            window.statusBarColor = Color.WHITE
                            window.navigationBarColor = Color.WHITE
                        }?.start()
                }
            }
        }
    }

    private fun openFeedScreen() {
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
        if (intent == null) return
        val data = intent.data?.toString()
            ?: intent.dataString
            ?: intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.clipData?.getItemAt(0)?.text?.toString()
            ?: return

        try {
            val url = data.toHttpUrlOrNull()
            openReadScreen(url!!.toString())
        } catch (e: Exception) {
            try {
                val url = data
                    .trim(' ')
                    .split(" ")
                    .firstOrNull {
                        it.toHttpUrlOrNull() != null
                    }
                openReadScreen(url!!.toString())
            } catch (e1: Exception) {
                Timber.e(e1)
                toast(getString(R.string.unsupported_received_link))

            }
        }
    }

}