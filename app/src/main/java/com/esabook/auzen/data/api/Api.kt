package com.esabook.auzen.data.api

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.executeAsync
import java.io.InputStream
import java.util.concurrent.TimeUnit


object Api {
    val headers = Headers.Builder()
        .add(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:105.0) Gecko/20100101 Firefox/105.0"
        )
        .build()

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
    ) = client.newCall(
        Request.Builder()
            .url(url)
            .headers(headers)
            .build()
    ).executeAsync()
}