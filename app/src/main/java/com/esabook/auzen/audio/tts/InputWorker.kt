package com.esabook.auzen.audio.tts

import android.content.Context
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber
import java.util.concurrent.LinkedBlockingQueue


/**
 * @author []" "Xuefeng Ding"">&quot;mailto:xuefeng.ding@outlook.com&quot; &quot;Xuefeng Ding&quot;
 * Created 2020-07-28 14:25
 */
internal class InputWorker(context: Context, fastspeech: String, vocoder: String) {
    private val mInputQueue = LinkedBlockingQueue<InputText>()
    private var mCurrentInputText: InputText? = null
    private val mFastSpeech2: FastSpeech2 = FastSpeech2(context, fastspeech)
    private val mMBMelGan: MBMelGan = MBMelGan(context, vocoder)
    private val mProcessor: ProcessorIndo = ProcessorIndo(context)
    private val mTtsPlayer: TtsPlayer = TtsPlayer()

    init {

        ThreadPoolManager.getInstance().getSingleExecutor("worker").execute {
            while (true) {
                try {
                    mCurrentInputText = mInputQueue.take()
                    Timber.d("processing: " + mCurrentInputText?.INPUT_TEXT)
                    TtsStateDispatcher.instance?.onTtsStart(
                        mCurrentInputText?.INPUT_TEXT,
                        mCurrentInputText?.playId!!
                    )
                    mCurrentInputText?.proceed()
                } catch (e: Exception) {
                    Timber.e(e, "Exception: ")
                }
            }
        }
    }

    fun processInput(inputText: String, speed: Float, playId: String) {
        Timber.d("add to queue: $inputText")
        mInputQueue.offer(InputText(inputText, speed, playId))
    }

    fun interrupt() {
        mInputQueue.clear()
        mCurrentInputText?.interrupt()
        mTtsPlayer.interrupt()
    }


    private inner class InputText(
        val INPUT_TEXT: String,
        private val SPEED: Float,
        val playId: String
    ) {
        private var isInterrupt = false

        fun proceed() {
            val sentences = INPUT_TEXT.split("[.,]".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            Timber.d("speak: " + sentences.contentToString())

            for (sentence in sentences) {
                val time = System.currentTimeMillis()

                val inputIds: IntArray = mProcessor.textToIds(sentence)

                val output: TensorBuffer = mFastSpeech2.getMelSpectrogram(inputIds, SPEED)

                if (isInterrupt) {
                    Timber.d("proceed: interrupt")
                    return
                }

                val encoderTime = System.currentTimeMillis()

                val audioData: FloatArray = mMBMelGan.getAudio(output)

                if (isInterrupt) {
                    Timber.d("proceed: interrupt")
                    return
                }

                val vocoderTime = System.currentTimeMillis()

                Timber.d("Time cost: " + (encoderTime - time) + "+" + (vocoderTime - encoderTime) + "=" + (vocoderTime - time))

                mTtsPlayer.play(TtsPlayer.AudioData(sentence, audioData, playId))
            }
        }

        fun interrupt() {
            this.isInterrupt = true
        }
    }
}