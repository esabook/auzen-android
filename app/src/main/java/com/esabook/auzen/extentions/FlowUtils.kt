package com.esabook.auzen.extentions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

suspend fun <T> Flow<T>.collectLatest2(action: suspend (value: T) -> Unit) = collectLatest {
    Timber.tag("collectLatest2").v("%s %s", action.javaClass.name, it.hashCode())
    action.invoke(it)
}