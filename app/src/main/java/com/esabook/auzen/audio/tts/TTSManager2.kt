package com.esabook.auzen.audio.tts

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import kotlin.concurrent.Volatile


class TTSManager2 {
    private var mWorker: InputWorker? = null

    fun init(context: Context) {
        ThreadPoolManager.getInstance().getSingleExecutor("init").execute {
            try {
                val fastspeech = copyFile(context, FASTSPEECH2_MODULE)
                val vocoder = copyFile(context, MELGAN_MODULE)
                mWorker = InputWorker(context, fastspeech, vocoder)
            } catch (e: Exception) {
                Timber.e(e, "mWorker init failed")
            }
            TtsStateDispatcher.instance?.onTtsReady()
        }
    }

    private fun copyFile(context: Context, strOutFileName: String): String {
        Timber.d("start copy file " + strOutFileName)
        val file: File = context.filesDir

        val tmpFile = file.absolutePath + "/" + strOutFileName
        val f = File(tmpFile)
        if (f.exists()) {
            Timber.d("file exists " + strOutFileName)
            return f.absolutePath
        }

        try {
            FileOutputStream(f).use { myOutput ->
                context.assets.open(strOutFileName).use { myInput ->
                    val buffer = ByteArray(1024)
                    var length: Int = myInput.read(buffer)
                    while (length > 0) {
                        myOutput.write(buffer, 0, length)
                        length = myInput.read(buffer)
                    }
                    myOutput.flush()
                    Timber.d("Copy task successful")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "copyFile: Failed to copy")
        } finally {
            Timber.d("end copy file " + strOutFileName)
        }
        return f.absolutePath
    }

    fun stopTts() {
        mWorker?.interrupt()
    }

    fun speak(inputText: String, speed: Float, interrupt: Boolean, playId: String) {
        if (interrupt) {
            stopTts()
        }

        ThreadPoolManager.getInstance().execute { mWorker?.processInput(inputText, speed, playId) }
    }

    companion object {

        private val INSTANCE_WRITE_LOCK = Any()

        @Volatile
        var instance: TTSManager2? = null
            get() {
                if (field == null) {
                    synchronized(INSTANCE_WRITE_LOCK) {
                        if (field == null) {
                            field = TTSManager2()
                        }
                    }
                }
                return field
            }
            private set

        private const val FASTSPEECH2_MODULE = "fastspeech2_quant.tflite"
        private const val MELGAN_MODULE = "mbmelgan.tflite"
    }
}