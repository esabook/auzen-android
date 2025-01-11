package com.esabook.auzen.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "article",
    indices = [Index("guid", "link", unique = true)]
)
data class ArticleEntity(
    @PrimaryKey
    val guid: String,

    @ColumnInfo(name = "rss_guid")
    val rssGuid: String,

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "link")
    val link: String?,

    @ColumnInfo(name = "description")
    var description: String?,

    @ColumnInfo(name = "author")
    val author: String?,

    @ColumnInfo(name = "pub_date")
    val pubDate: String?,

    @ColumnInfo(name = "pub_date_timestamp")
    val pubDateTimeStamp: Long?,

    @ColumnInfo(name = "enclosure")
    val enclosure: String?,

    @ColumnInfo(name = "source_link")
    val sourceLink: String?,

    @ColumnInfo(name = "source_title")
    val sourceTitle: String?,

    @ColumnInfo(name = "is_unread")
    var isUnread: Boolean = true,

    @ColumnInfo(name = "is_playlist_queue")
    var isPlayListQueue: Boolean = false,

    @ColumnInfo(name = "playlist_order", defaultValue = "0")
    var playListOrder: Long = 0L,

    @ColumnInfo(name = "last_modified_time")
    var lastModifiedTime: String? = null

)

