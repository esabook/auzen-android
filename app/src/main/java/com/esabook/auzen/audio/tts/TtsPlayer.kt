package com.esabook.auzen.audio.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import timber.log.Timber
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.min


internal class TtsPlayer {
    private val mAudioTrack: AudioTrack = AudioTrack(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build(),
        AudioFormat.Builder()
            .setSampleRate(22050)
            .setEncoding(FORMAT)
            .setChannelMask(CHANNEL)
            .build(),
        BUFFER_SIZE,
        AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE
    )

    private val mAudioQueue = LinkedBlockingQueue<AudioData>()
    private var mCurrentAudioData: AudioData? = null

    init {
        mAudioTrack.play()

        ThreadPoolManager.getInstance().getSingleExecutor("audio").execute {
            while (true) {
                try {
                    mCurrentAudioData = mAudioQueue.take()
                    Timber.d("playing: " + mCurrentAudioData!!.text)
                    var index = 0
                    while (index < mCurrentAudioData!!.audio.size && !mCurrentAudioData!!.isInterrupt) {
                        val buffer = min(
                            BUFFER_SIZE.toDouble(),
                            (mCurrentAudioData!!.audio.size - index).toDouble()
                        )
                            .toInt()
                        mAudioTrack.write(
                            mCurrentAudioData!!.audio,
                            index,
                            buffer,
                            AudioTrack.WRITE_BLOCKING
                        )
                        index += BUFFER_SIZE
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Exception: ")
                } finally {
                    TtsStateDispatcher.instance?.onTtsStop(mCurrentAudioData?.playId ?: "0")
                }
            }
        }
    }

    fun play(audioData: AudioData) {
        Timber.d("add audio data to queue: " + audioData.text)
        mAudioQueue.offer(audioData)
    }

    fun interrupt() {
        mAudioQueue.clear()
        mCurrentAudioData?.interrupt()
    }

    internal class AudioData(val text: String, val audio: FloatArray, val playId: String) {
        var isInterrupt: Boolean = false

        fun interrupt() {
            isInterrupt = true
        }
    }

    companion object {

        private const val FORMAT = AudioFormat.ENCODING_PCM_FLOAT
        private const val SAMPLERATE = 22050
        private const val CHANNEL = AudioFormat.CHANNEL_OUT_MONO
        private val BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLERATE, CHANNEL, FORMAT)
    }
}