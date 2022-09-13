package com.esabook.auzen

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.esabook.auzen.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

class App : Application() {
    companion object {
        lateinit var db: AppDatabase
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        xCrash.init(app = this)
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

        }
    }


}