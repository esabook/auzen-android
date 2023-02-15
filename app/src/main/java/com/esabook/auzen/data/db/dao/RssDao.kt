package com.esabook.auzen.data.db.dao

import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.REPLACE
import com.esabook.auzen.data.db.entity.RssEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RssDao {
    @Query("SELECT * FROM rss")
    fun getAll(): Flow<List<RssEntity>>

    @Query("SELECT * FROM rss WHERE guid IN (:guids)")
    fun loadAllByIds(guids: Array<String>): List<RssEntity>

    @Query("SELECT * FROM rss WHERE title LIKE :first AND " +
            "link LIKE :last LIMIT 1")
    fun findByTitleAndLink(first: String, last: String): RssEntity?

    @Query("SELECT * FROM rss WHERE title LIKE :guid LIMIT 1")
    fun findByGuid(guid: String): RssEntity?

    @Insert(onConflict = REPLACE)
    fun insertAll(vararg rssEntities: RssEntity)

    @Update(entity = RssEntity::class)
    fun update(vararg rssEntity: RssEntity)

    @Delete
    fun delete(rssEntity: RssEntity)
}
