package com.esabook.auzen.extentions

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.esabook.auzen.R

object SoundPool {
    private val soundPool by lazy {
        val audioAttr = AudioAttributes.Builder()
            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
            .build()
        SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttr)
            .build()
    }

    private var tickSoundID: Int? = null

    fun Context.playTickSound() {
        if (tickSoundID == null) {
            tickSoundID = try {
                soundPool.load(this, R.raw.page_back_chime, 1)
            } catch (exception: Exception) {
                null
            }
        }

        tickSoundID?.let {
            soundPool.play(it, 1f, 1f, 1, 0, 1f)
        }
    }
}