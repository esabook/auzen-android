package com.esabook.auzen.audio.tts

import android.annotation.SuppressLint
import android.content.Context
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import timber.log.Timber
import java.io.File
import java.nio.FloatBuffer


class FastSpeech2(context: Context, modulePath: String) : AbstractModule() {
    private lateinit var mModule: InterpreterApi

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

            val output: Int = mModule.outputTensorCount
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

    fun getMelSpectrogram(inputIds: IntArray, speed: Float): TensorBuffer {
        Timber.d("input id length: " + inputIds.size)
        mModule.resizeInput(0, intArrayOf(1, inputIds.size))
        mModule.allocateTensors()

        @SuppressLint("UseSparseArrays") val outputMap: MutableMap<Int, Any> = HashMap()

        val outputBuffer = FloatBuffer.allocate(350000)
        outputMap[0] = outputBuffer

        val inputs = Array(1) { IntArray(inputIds.size) }
        inputs[0] = inputIds

        val time = System.currentTimeMillis()
        mModule.runForMultipleInputsOutputs(
            arrayOf<Any>(
                inputs,
                Array(1) { IntArray(1) },
                intArrayOf(0),
                floatArrayOf(speed),
                floatArrayOf(1f),
                floatArrayOf(1f)
            ),
            outputMap
        )
        Timber.d("time cost: " + (System.currentTimeMillis() - time))

        val size: Int = mModule.getOutputTensor(0)?.shape()?.get(2)!!
        val shape = intArrayOf(1, outputBuffer.position() / size, size)
        val spectrogram = TensorBuffer.createFixedSize(shape, DataType.FLOAT32)
        val outputArray = FloatArray(outputBuffer.position())
        outputBuffer.rewind()
        outputBuffer[outputArray]
        spectrogram.loadArray(outputArray)

        return spectrogram
    }

}