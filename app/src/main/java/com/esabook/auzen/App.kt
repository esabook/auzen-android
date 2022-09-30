package com.esabook.auzen

import android.app.Application
import androidx.room.Room
import com.esabook.auzen.data.api.Api
import com.esabook.auzen.data.db.AppDatabase
import com.esabook.auzen.data.db.entity.ParserDictEntity
import com.esabook.auzen.parser.ParserConfig
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber


class App : Application() {
    companion object {
        lateinit var db: AppDatabase
    }

    override fun onCreate() {
        super.onCreate()

        MainScope().launch(Dispatchers.IO) {
            db = Room.databaseBuilder(
                this@App,
                AppDatabase::class.java,
                "auzen-db"
            )
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()

            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())

            } else {
                SentryAndroid.init(
                    this@App
                ) { options: SentryAndroidOptions ->
                    options.dsn =
                        "https://9e67d9ee5eda41a0965053e620edb8f0@o1411097.ingest.sentry.io/6749448"
                    options.tracesSampleRate = 1.0
                    options.isEnableUserInteractionTracing = true
                    options.environment = BuildConfig.BUILD_TYPE
                }
            }

            updateParserDict()
        }

    }

    suspend fun updateParserDict() = withContext(Dispatchers.IO) {
        try {
            Api.response(url = ParserConfig.parserRemoteUrl).body.string().let { str ->
                Timber.d(str)
                val json = JSONObject(str)
                ParserConfig.unlikelyCandidate = json.getString("unlikelyCandidate")
                ParserConfig.positiveCandidate = json.getString("positiveCandidate")
                ParserConfig.byLine = json.getString(
                    "byLine"
                )

                db.parserDictDao().delete()
                json.getJSONArray("parser_dict").let {
                    val len = it.length()
                    for (i in 0 until len) {
                        val jsonObj = it.getJSONObject(i)
                        val domain = jsonObj.getString("domain_matcher")
                        val xpath = jsonObj.getString("xpath_content_matcher")
                        val allPage = jsonObj.getString("all_page_param")
                        val parserDictEntity = ParserDictEntity(domain, xpath, allPage)
                        db.parserDictDao().insertAll(parserDictEntity)
                    }
                }

                ParserConfig.parserDicts.clear()
                ParserConfig.parserDicts.addAll(db.parserDictDao().getAll())
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}