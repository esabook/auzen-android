package com.esabook.auzen.extentions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Debouncer(private val scope: CoroutineScope) {

    private var debounceJob: Job? = null

    fun create(interval: Long, action: () -> Unit): () -> Unit {
        return {
            debounce(interval, action)
        }
    }

    fun <T> create(interval: Long, action: suspend (T) -> Unit): (T) -> Unit {
        return { input ->
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(interval)
                action(input)
            }
        }
    }

    fun debounce(interval: Long, action: () -> Unit) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(interval)
            action()
        }
    }
}
