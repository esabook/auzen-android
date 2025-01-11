package com.esabook.auzen.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.esabook.auzen.data.db.dao.ArticleDao
import com.esabook.auzen.data.db.dao.ArticleQueueDao
import com.esabook.auzen.data.db.dao.ParserDictDao
import com.esabook.auzen.data.db.dao.RssDao
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.data.db.entity.ParserDictEntity
import com.esabook.auzen.data.db.entity.RssEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext

@Database(
    entities = [RssEntity::class, ArticleEntity::class, ParserDictEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 3, to = 4)]
)
abstract class AppDatabase : RoomDatabase() {
    val ioScope = MainScope() + Dispatchers.IO

    fun launchIo(run: suspend AppDatabase.() -> Unit) {
        ioScope.launch {
            withContext(Dispatchers.IO) {
                run.invoke(this@AppDatabase)
            }
        }
    }

    abstract fun rssDao(): RssDao
    abstract fun articleDao(): ArticleDao
    abstract fun articleQueueDao(): ArticleQueueDao
    abstract fun parserDictDao(): ParserDictDao
}
