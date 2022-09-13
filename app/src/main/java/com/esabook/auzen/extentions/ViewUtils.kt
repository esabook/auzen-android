package com.esabook.auzen.extentions

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.view.doOnDetach
import com.google.android.material.appbar.AppBarLayout
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs


fun <T: View> T.post2(delay: Long = 0L, runnable: T.() -> Unit) {
    if (delay == 0L) {
        post { runnable.invoke(this) }
    } else {
        postDelayed({ runnable.invoke(this) }, delay)
    }
}

fun TextView.setTextAnimation(text: CharSequence?, duration: Long = 200, completion: (() -> Unit)? = null) {
    fadOutAnimation(duration) {
        this.text = text
        fadInAnimation(duration) {
            completion?.invoke()
        }
    }
}

fun View.fadOutAnimation(duration: Long = 300, visibility: Int = View.INVISIBLE, completion: (() -> Unit)? = null) {
    animate()
        .alpha(0f)
        .setDuration(duration)
        .withEndAction {
            this.visibility = visibility
            completion?.invoke()
        }
}

fun View.fadInAnimation(duration: Long = 300, completion: (() -> Unit)? = null) {
    alpha = 0f
    visibility = View.VISIBLE
    animate()
        .alpha(1f)
        .setDuration(duration)
        .withEndAction {
            completion?.invoke()
        }
}

suspend fun View.awaitNextLayout() = suspendCancellableCoroutine { cont ->
    // This lambda is invoked immediately, allowing us to create
    // a callback/listener

    val listener = object : View.OnLayoutChangeListener{
        override fun onLayoutChange(
            v: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            // The next layout has happened!
            // First remove the listener to not leak the coroutine
            v?.removeOnLayoutChangeListener(this)
            // Finally resume the continuation, and
            // wake the coroutine up
            cont.resume(Unit)


        }

    }

    doOnDetach {
        removeOnLayoutChangeListener(listener)
    }

    // If the coroutine is cancelled, remove the listener
    cont.invokeOnCancellation { removeOnLayoutChangeListener(listener) }
    // And finally add the listener to view
    addOnLayoutChangeListener(listener)


    // The coroutine will now be suspended. It will only be resumed
    // when calling cont.resume() in the listener above
}

suspend fun AppBarLayout.waitUntilExpanded() = suspendCancellableCoroutine { cont ->
    val listener = object : AppBarLayout.OnOffsetChangedListener{
        override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
            if(abs(verticalOffset) - appBarLayout.totalScrollRange == 0) {
                // collapse
            } else {
                // expanded
                appBarLayout.removeOnOffsetChangedListener(this)
                cont.resume(Unit)
            }
        }
    }

    doOnDetach {
        removeOnOffsetChangedListener(listener)
    }
    // If the coroutine is cancelled, remove the listener
    cont.invokeOnCancellation { removeOnOffsetChangedListener(listener) }
    // And finally add the listener to view
    addOnOffsetChangedListener(listener)
}

fun View.showInputMethod() {
    val imm: InputMethodManager? = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    imm?.showSoftInput(this, 0)
}