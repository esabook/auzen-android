package com.esabook.auzen.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parser_dict")
data class ParserDictEntity(
    /**
     * used for link matcher
     * eg. |*.news.tv|*news.tv|
     */
    @ColumnInfo(name = "domain_matcher")
    @PrimaryKey
    val domainMatcher: String,

    /**
     * used for jsoup selector
     * https://jsoup.org/cookbook/extracting-data/selector-syntax
     * eg. "div.paragraph, div#paragraph"
     */
    @ColumnInfo(name = "xpath_content_matcher")
    val xpathContentMatcher: String,

    @ColumnInfo(name = "all_page_param")
    val allPageParam: String
)