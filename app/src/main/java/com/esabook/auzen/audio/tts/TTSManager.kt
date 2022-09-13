package com.esabook.auzen.audio.tts

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.EngineInfo
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import timber.log.Timber
import java.util.*


object TTSManager {
    enum class TtsState {
        PLAY, STOP
    }

    var utteranceProgressListener: UtteranceProgressListener? = null

    var audioAttrs = AudioAttributes.Builder().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE)
        }
        setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
    }

    var currentPitch = 1F
        set(value) {
            field = value
            textToSpeech?.setPitch(field)
        }

    var currentSpeed = 1F
        set(value) {
            field = value
            textToSpeech?.setSpeechRate(field)
        }

    var currentVoice: Voice? = null
    set(value) {
        field = value
        if (field != null){
            textToSpeech?.voice = field
        }
    }


    private var textToSpeech: TextToSpeech? = null

    var language = Locale.forLanguageTag("ID")

    private val mTtsState = MutableLiveData(TtsState.STOP)
    private val mUtteranceProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {
            utteranceProgressListener?.onStart(utteranceId)
            mTtsState.postValue(TtsState.PLAY)
        }

        override fun onDone(utteranceId: String) {
            mTtsState.postValue(TtsState.STOP)
            utteranceProgressListener?.onDone(utteranceId)

        }

        @Suppress("DEPRECATION")
        @Deprecated(
            "Deprecated in Java", ReplaceWith(
                "utteranceProgressListener?.onError(utteranceId)",
                "com.esabook.auzen.audio.tts.TTSManager.utteranceProgressListener"
            )
        )
        override fun onError(utteranceId: String) {
            mTtsState.postValue(TtsState.STOP)
            utteranceProgressListener?.onError(utteranceId)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            mTtsState.postValue(TtsState.STOP)
            utteranceProgressListener?.onError(utteranceId, errorCode)
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            mTtsState.postValue(TtsState.STOP)
            utteranceProgressListener?.onStop(utteranceId, interrupted)
        }

        @RequiresApi(Build.VERSION_CODES.N)
        override fun onBeginSynthesis(
            utteranceId: String?,
            sampleRateInHz: Int,
            audioFormat: Int,
            channelCount: Int
        ) {
            mTtsState.postValue(TtsState.PLAY)
            utteranceProgressListener?.onBeginSynthesis(
                utteranceId,
                sampleRateInHz,
                audioFormat,
                channelCount
            )
        }

        @RequiresApi(Build.VERSION_CODES.N)
        override fun onAudioAvailable(utteranceId: String?, audio: ByteArray?) {
            utteranceProgressListener?.onAudioAvailable(utteranceId, audio)
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            utteranceProgressListener?.onRangeStart(utteranceId, start, end, frame)
        }
    }

    fun initSpeech(context: Context) {
        if (textToSpeech != null) {
            throw IllegalStateException("Please call stop before init")
        }

        textToSpeech = TextToSpeech(context) { status ->
            Timber.i("TextToSpeech onInit status = $status")
            if (status == TextToSpeech.SUCCESS) {
                getSpeechParams()

                val result = textToSpeech?.setLanguage(language)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED ){
                    val installIntent = Intent()
                    installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                    context.startActivity(installIntent)
                }

                textToSpeech?.setAudioAttributes(audioAttrs.build())
                textToSpeech?.setPitch(currentPitch)
                textToSpeech?.setSpeechRate(currentSpeed)
            }
        }
    }

    fun getTtsEngineVoiceList(filterName: String = "id-ID") =
        textToSpeech?.voices?.filter { it.name.contains(filterName, true) }

    private fun getSpeechParams() = textToSpeech?.run {
        val engineInfoList: List<EngineInfo> = getEngines()
        val builderEngineInfo = StringBuilder("")
        for (engineInfo in engineInfoList) {
            builderEngineInfo.append(" ").append(engineInfo.name).append("&")
                .append(engineInfo.label).append(" |")
        }
        Timber.i(" engineInfoList = $builderEngineInfo")
        val defaultEngine: String = defaultEngine
        Timber.i(" defaultEngine = $defaultEngine")
        val builderVoice = StringBuilder("")
        val voiceList: Set<Voice> = voices
        for (vioce in voiceList) {
            builderVoice.append(" ").append(vioce.name).append(" |")
        }
        Timber.i(" voiceList = $builderVoice")

        val defaultVoice: Voice? = defaultVoice
        if (defaultVoice != null) {
            Timber.i(" defaultVoice = $defaultVoice")
            currentVoice = defaultVoice
        }
    }

    fun playSpeechToSpeaker(inputText: String, playId: String = Random().nextInt().toString()) {
        val result = textToSpeech!!.setLanguage(language)
        Timber.i(" textToSpeech.setLanguage result = $result")
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Timber.i("TextToSpeech.LANG_MISSING_DATA || TextToSpeech.LANG_NOT_SUPPORTED; result = $result")

        } else {
            val text = inputText
            val state = textToSpeech!!.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                playId
            )
            Timber.i("speak state : $state")
            textToSpeech!!.setOnUtteranceProgressListener(mUtteranceProgressListener)
        }
    }

//    fun saveSpeechToFile(inputText: String, playId: String = Random().nextInt().toString()) {
//        val result = textToSpeech!!.setLanguage(language)
//        Timber.i(" textToSpeech.setLanguage result = $result")
//        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//            Timber.i("TextToSpeech.LANG_MISSING_DATA || TextToSpeech.LANG_NOT_SUPPORTED; result = $result")
//
//        } else {
//            val text = inputText
//            val filename = "tts.mp3"
//            val destDir = Environment.getExternalStorageDirectory().path + "/audio"
//            val ttsFile = File(destDir, filename)
//            // 3.调用synthesizeToFile
//            val state: Int = textToSpeech?.synthesizeToFile(
//                text,
//                null,
//                ttsFile,
//                playId
//            ) ?: 0
//            Timber.i("synthesizeToFile state : $state")
//            textToSpeech!!.setOnUtteranceProgressListener(mUtteranceProgressListener)
//        }
//    }


    fun stop() {
        textToSpeech?.stop()
        mTtsState.postValue(TtsState.STOP)
    }

    fun deinit() {
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}