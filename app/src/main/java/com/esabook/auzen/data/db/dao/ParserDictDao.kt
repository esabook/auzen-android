package com.esabook.auzen.data.db.dao

import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.REPLACE
import com.esabook.auzen.data.db.entity.ParserDictEntity

@Dao
interface ParserDictDao {
    @Query("SELECT * FROM parser_dict")
    fun getAll(): List<ParserDictEntity>

    @Insert(onConflict = REPLACE)
    fun insertAll(vararg parser: ParserDictEntity)

    @Update(entity = ParserDictEntity::class)
    fun update(vararg parsers: ParserDictEntity)

    @Delete
    fun delete(parser: ParserDictEntity)

    @Query("DELETE FROM parser_dict WHERE 1")
    fun delete()
}