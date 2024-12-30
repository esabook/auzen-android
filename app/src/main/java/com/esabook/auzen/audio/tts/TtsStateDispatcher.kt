package com.esabook.auzen.audio.tts

import android.os.Handler
import android.os.Looper
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.Volatile


/**
 * @author []" "Xuefeng Ding"">&quot;mailto:xuefeng.ding@outlook.com&quot; &quot;Xuefeng Ding&quot;
 * Created 2020-07-28 14:25
 */
class TtsStateDispatcher {
    private val handler: Handler = Handler(Looper.getMainLooper())

    private val mListeners: CopyOnWriteArrayList<OnTtsStateListener> =
        CopyOnWriteArrayList<OnTtsStateListener>()

    fun release() {
        Timber.d("release: ")
        mListeners.clear()
    }

    fun addListener(listener: OnTtsStateListener) {
        if (mListeners.contains(listener)) {
            return
        }
        Timber.d("addListener: " + listener.javaClass)
        mListeners.add(listener)
    }

    fun removeListener(listener: OnTtsStateListener) {
        if (mListeners.contains(listener)) {
            Timber.d("removeListener: " + listener.javaClass)
            mListeners.remove(listener)
        }
    }

    fun onTtsStart(text: String?, playId: String) {
        Timber.d("onTtsStart: ")
        if (!mListeners.isEmpty()) {
            for (listener in mListeners) {
                handler.post { listener.onTtsStart(text, playId) }
            }
        }
    }

    fun onTtsStop(playId: String) {
        Timber.d("onTtsStop: ")
        if (!mListeners.isEmpty()) {
            for (listener in mListeners) {
                handler.post { listener.onTtsStop(playId) }
            }
        }
    }

    fun onTtsReady() {
        Timber.d("onTtsReady: ")
        if (!mListeners.isEmpty()) {
            for (listener in mListeners) {
                handler.post(listener::onTtsReady)
            }
        }
    }

    companion object {

        @Volatile
        var instance: TtsStateDispatcher? = null
            get() {
                if (field == null) {
                    synchronized(INSTANCE_WRITE_LOCK) {
                        if (field == null) {
                            field = TtsStateDispatcher()
                        }
                    }
                }
                return field
            }
            private set
        private val INSTANCE_WRITE_LOCK = Any()
    }
}