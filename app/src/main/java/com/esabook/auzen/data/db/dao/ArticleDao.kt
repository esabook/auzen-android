package com.esabook.auzen.data.db.dao

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.*
import androidx.room.OnConflictStrategy.IGNORE
import com.esabook.auzen.data.db.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM article ORDER by pub_date_timestamp DESC")
    fun getAll(): PagingSource<Int, ArticleEntity>

    @Query("SELECT * FROM article WHERE guid IN (:guids)")
    fun loadAllByIds(guids: IntArray): LiveData<List<ArticleEntity>>

    @Query("SELECT * FROM article WHERE title LIKE :q")
    fun loadAllByKeyword(q: String): PagingSource<Int, ArticleEntity>

    @Query("SELECT * FROM article WHERE guid LIKE :key OR link LIKE :key LIMIT 1")
    fun findByGuidOrLink(key: String): Flow<ArticleEntity?>

    @Query("SELECT is_unread FROM article WHERE rss_guid = :second")
    fun getIsUnreadStatusByRssGuid(second: String): List<Boolean>

    @Insert(onConflict = IGNORE)
    fun insertAll(vararg article: ArticleEntity)

    @Update(entity = ArticleEntity::class, onConflict = IGNORE)
    fun update(vararg article: ArticleEntity)

    @Delete
    fun delete(article: ArticleEntity)

    @Query("DELETE from article WHERE rss_guid = :rssGuid")
    fun deleteAllByRssGuid(vararg rssGuid: String): Int

    @Query("UPDATE article SET is_unread = :isUnRead WHERE guid = :guid")
    fun markAsRead(guid: String, isUnRead: Boolean): Int

    @Query("UPDATE article SET is_unread = 0 WHERE guid == :guid OR link LIKE :link")
    fun markAsReadByGuidOrLink(guid: String, link: String): Int


    @Query("SELECT * FROM article WHERE is_unread = :isUnRead ORDER by pub_date_timestamp DESC")
    fun loadAllWithUnread(isUnRead: Boolean): PagingSource<Int, ArticleEntity>

    @Query("SELECT * FROM article WHERE is_unread = :isUnRead ORDER by pub_date_timestamp DESC LIMIT :limit")
    fun loadAllWithUnread(isUnRead: Boolean, limit: Int): Flow<List<ArticleEntity>>
}
