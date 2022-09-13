package com.esabook.auzen.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rss")
data class RssEntity(
    @PrimaryKey val guid: String,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "link")
    val link: String?,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "last_build_date")
    val lastBuildDate: String?,

    @ColumnInfo(name = "copyright")
    val copyright: String?,

    @ColumnInfo(name = "category")
    val category: String?,

    @ColumnInfo(name = "favicon")
    val favicon: String?,

    @ColumnInfo(name = "total_entry")
    val totalEntry: Int = 0,

    @ColumnInfo(name = "total_entry_unread")
    val totalEntryUnread: Int = 0,

    @ColumnInfo(name = "mute_auto_sync")
    var muteAutoSync: Boolean = false,

)

