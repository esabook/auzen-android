package com.esabook.auzen.data.api

import okhttp3.*
import java.io.InputStream
import java.util.concurrent.TimeUnit

object Api {
    private val headers = Headers.Builder()
        .add(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:105.0) Gecko/20100101 Firefox/105.0"
        )
        .build()

    private val interceptor = Interceptor { chain ->
        var req = chain.request()
        var reqUrl = req.url
        if (reqUrl.isHttps.not()) {
            reqUrl = reqUrl.newBuilder().scheme("https").build()
            req = req.newBuilder().url(reqUrl).build()
        }

        val res = chain.proceed(req)
        res
    }

    val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
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