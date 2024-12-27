package com.esabook.auzen.parser

import com.esabook.auzen.data.api.Api
import com.google.gson.Gson
import com.google.gson.JsonParseException
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URL
import java.net.URLEncoder

object GoogleNewsArticleDecoder {

    private fun getBase64Str(sourceUrl: String): Map<String, Any> {
        return try {
            val url = URL(sourceUrl)
            val path = url.path.split("/")
            if (url.host == "news.google.com" && path.size > 1 && (path[path.size - 2] == "articles" || path[path.size - 2] == "read")) {
                mapOf("status" to true, "base64_str" to path.last())
            } else {
                mapOf("status" to false, "message" to "Invalid Google News URL format.")
            }
        } catch (e: Exception) {
            mapOf("status" to false, "message" to "Error in getBase64Str: ${e.message}")
        }
    }

    private fun getDecodingParams(base64Str: String): Map<String, Any> {
        val client = Api.okHttpClient
        val firstUrl = "https://news.google.com/articles/$base64Str"

        return try {
            val request = Request.Builder().url(firstUrl).build()
            val response: Response = client.newCall(request).execute()
            response.use {
                if (!it.isSuccessful) throw IOException("Unexpected code $it")

                val document = Jsoup.parse(it.body.string())
                val dataElement = document.selectFirst("c-wiz > div[jscontroller]")
                    ?: return mapOf(
                        "status" to false,
                        "message" to "Failed to fetch data attributes from Google News with the articles URL."
                    )

                mapOf(
                    "status" to true,
                    "signature" to dataElement.attr("data-n-a-sg"),
                    "timestamp" to dataElement.attr("data-n-a-ts"),
                    "base64_str" to base64Str
                )
            }
        } catch (reqErr: Exception) {
            val fallbackUrl = "https://news.google.com/rss/articles/$base64Str"
            return try {
                val request = Request.Builder().url(fallbackUrl).build()
                val response: Response = client.newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) throw IOException("Unexpected code $it")

                    val document = Jsoup.parse(it.body.string())
                    val dataElement = document.selectFirst("c-wiz > div[jscontroller]")
                        ?: return mapOf(
                            "status" to false,
                            "message" to "Failed to fetch data attributes from Google News with the RSS URL."
                        )

                    mapOf(
                        "status" to true,
                        "signature" to dataElement.attr("data-n-a-sg"),
                        "timestamp" to dataElement.attr("data-n-a-ts"),
                        "base64_str" to base64Str
                    )
                }
            } catch (rssReqErr: Exception) {
                mapOf(
                    "status" to false,
                    "message" to "Request error in getDecodingParams with RSS URL: ${rssReqErr.message}"
                )
            }
        } catch (e: Exception) {
            mapOf(
                "status" to false,
                "message" to "Unexpected error in getDecodingParams: ${e.message}"
            )
        }
    }


    private fun decodeUrl(
        signature: String,
        timestamp: String,
        base64Str: String
    ): Map<String, Any> {
        val client = Api.okHttpClient
        val url = "https://news.google.com/_/DotsSplashUi/data/batchexecute"
        val payload = listOf(
            "Fbv4je",
            """["garturlreq",[["X","X",["X","X"],null,null,1,1,"US:en",null,1,null,null,null,null,null,0,1],"X","X",1,[1,1,1],1,1,null,0,0,null,0],"$base64Str",$timestamp,"$signature"]"""
        )
        val headers = Headers.Builder()
            .add("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
            .add(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"
            )
            .build()

        val requestBody =
            "f.req=${URLEncoder.encode(Gson().toJson(listOf(listOf(payload))), "UTF-8")}"
                .toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .headers(headers)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val responseData = response.body.string().split("\n\n")?.get(1)?.dropLast(2)
            val startIndex = responseData!!.indexOf("http", ignoreCase = true)
            val endIndex = responseData.indexOf("\\\"", startIndex)
            val decodedUrl = responseData.substring(startIndex, endIndex)

            mapOf("status" to true, "decoded_url" to decodedUrl)
        } catch (reqErr: IOException) {
            mapOf("status" to false, "message" to "Request IOException: ${reqErr.message}")
        } catch (parseErr: JsonParseException) {
            mapOf("status" to false, "message" to "Parsing JsonParseException: ${parseErr.message}")
        } catch (e: Exception) {
            mapOf("status" to false, "message" to "Error in decodeUrl: ${e.message}")
        }
    }

    fun decodeGoogleNewsUrl(sourceUrl: String): Map<String, Any> {
        return try {
            val base64Response = getBase64Str(sourceUrl)
            if (!(base64Response["status"] as Boolean)) {
                return base64Response
            }

            val decodingParamsResponse = getDecodingParams(base64Response["base64_str"] as String)
            if (!(decodingParamsResponse["status"] as Boolean)) {
                return decodingParamsResponse
            }

            val decodedUrlResponse = decodeUrl(
                decodingParamsResponse["signature"] as String,
                decodingParamsResponse["timestamp"] as String,
                decodingParamsResponse["base64_str"] as String
            )
            decodedUrlResponse
        } catch (e: Exception) {
            mapOf(
                "status" to false,
                "message" to "Error in decodeGoogleNewsUrl: ${e.message}"
            )
        }
    }

    fun decodeGoogleAlertUrl(sourceUrl: String): Map<String, Any> {
        return try {
            val url: String =
                sourceUrl.toHttpUrlOrNull()?.queryParameterValues("url")?.firstOrNull()!!
            mapOf("status" to true, "decoded_url" to url)
        } catch (e: Exception) {
            mapOf(
                "status" to false,
                "message" to "Error in decodeGoogleNewsUrl: ${e.message}"
            )
        }
    }

}