package com.esabook.auzen.audio.tts

import org.tensorflow.lite.Interpreter


abstract class AbstractModule {
    val option: Interpreter.Options
        get() {
            val options: Interpreter.Options = Interpreter.Options()
            options.setNumThreads(5)
            return options
        }
}