package com.esabook.auzen.data.api

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.executeAsync
import java.io.InputStream
import java.util.concurrent.TimeUnit


object Api {
    val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun inputStream(
        client: OkHttpClient = okHttpClient,
        url: String
    ): InputStream = response(client, url).body.byteStream()

    suspend fun response(
        client: OkHttpClient = okHttpClient,
        url: String
    ) = client.newCall(Request.Builder().url(url).build()).executeAsync()
}