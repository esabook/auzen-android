package com.esabook.auzen.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.esabook.auzen.data.db.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleQueueDao {

    @Query("UPDATE article SET is_playlist_queue = :asPlaylistQueue, playlist_order = :timeUpdate WHERE guid = :guid")
    fun update(guid: String, asPlaylistQueue: Boolean, timeUpdate: Long = System.currentTimeMillis())

    @Query("SELECT * FROM article WHERE is_playlist_queue = 1 ORDER by playlist_order ASC")
    fun getAll(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM article WHERE is_playlist_queue = 1 ORDER by playlist_order ASC")
    fun getAllPaged(): PagingSource<Int, ArticleEntity>

    @Query("UPDATE article SET is_playlist_queue = 0 WHERE is_playlist_queue = 1")
    fun clearPlaylist(): Int
}