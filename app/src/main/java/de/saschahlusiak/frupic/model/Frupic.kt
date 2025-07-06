package de.saschahlusiak.frupic.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File
import java.io.Serializable

/**
 * Returns a URL with frupic.frubar.net replaced with a cloudfront URL
 */
val String.cloudfront get() = this
    .replace("frupic.frubar.net", "d1ofuc5rnolp9w.cloudfront.net")
    .replace("http://", "https://")

/**
 * Model object of a Frupic definition, as read from the database or the API.
 */
@Immutable
@Stable
@Entity(tableName = "frupics")
data class Frupic(
    @PrimaryKey
    @ColumnInfo(name = "_id")
    val id: Int,

    @ColumnInfo(name = "starred")
    val isStarred: Boolean,

    @ColumnInfo(name = "new")
    val isNew: Boolean,

    @ColumnInfo(name = "fullurl")
    val fullUrl: String,

    @ColumnInfo(name = "thumburl")
    val thumbUrl: String,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "username")
    val username: String?,

    @ColumnInfo(name = "tags")
    @Stable
    val tagsString: String
) : Serializable {
    @Transient
    val url = "https://frupic.frubar.net/$id"

    val isAnimated get() = fullUrl.endsWith(".gif") || fullUrl.endsWith(".GIF")

    val tags by lazy { tagsString.split(", ") }

    val filename: String by lazy { File(fullUrl).name }
}