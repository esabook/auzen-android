package com.esabook.auzen

import android.app.Application
import androidx.room.Room
import com.esabook.auzen.data.db.AppDatabase
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
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

            if (BuildConfig.DEBUG)
                Timber.plant(Timber.DebugTree())

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


    }


}