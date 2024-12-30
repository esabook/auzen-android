package com.esabook.auzen.audio.tts

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber
import java.io.File
import java.nio.FloatBuffer


class MBMelGan(context: Context, modulePath: String) : AbstractModule() {
    private lateinit var mModule: InterpreterApi

//    private val initializeTask: Task<Void> by lazy { TfLite.initialize(context) }

    init {
//        initializeTask.addOnSuccessListener {
        try {
            mModule = Interpreter(File(modulePath), option)
            val input: Int = mModule.inputTensorCount
            for (i in 0 until input) {
                val inputTensor: Tensor = mModule.getInputTensor(i)
                Timber.d(
                    "input:" + i + " name:" + inputTensor.name() + " shape:" + inputTensor.shape()
                        .contentToString() + " dtype:" + inputTensor.dataType()
                )
            }

            val output: Int = mModule.getOutputTensorCount()
            for (i in 0 until output) {
                val outputTensor: Tensor = mModule.getOutputTensor(i)
                Timber.d(
                    "output:" + i + " name:" + outputTensor.name() + " shape:" + outputTensor.shape()
                        .contentToString() + " dtype:" + outputTensor.dataType()
                )
            }
            Timber.d("successfully init")
        } catch (e: Exception) {
            e.printStackTrace()
        }
//        }
    }

    fun getAudio(input: TensorBuffer): FloatArray {
        mModule.resizeInput(0, input.shape)
        mModule.allocateTensors()

        val outputBuffer = FloatBuffer.allocate(350000)

        val time = System.currentTimeMillis()
        mModule.run(input.buffer, outputBuffer)
        Timber.d("time cost: " + (System.currentTimeMillis() - time))

        val audioArray = FloatArray(outputBuffer.position())
        outputBuffer.rewind()
        outputBuffer[audioArray]
        return audioArray
    }

}