package com.esabook.auzen.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.esabook.auzen.data.db.dao.ArticleDao
import com.esabook.auzen.data.db.dao.ArticleQueueDao
import com.esabook.auzen.data.db.dao.RssDao
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.data.db.entity.RssEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

@Database(
    entities = [RssEntity::class, ArticleEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
abstract class AppDatabase : RoomDatabase() {
    val ioScope = MainScope() + Dispatchers.IO

    fun launchIo(run: suspend AppDatabase.() -> Unit) {
        ioScope.launch(Dispatchers.IO) {
            run.invoke(this@AppDatabase)
        }
    }

    abstract fun rssDao(): RssDao
    abstract fun articleDao(): ArticleDao
    abstract fun articleQueueDao(): ArticleQueueDao
}
