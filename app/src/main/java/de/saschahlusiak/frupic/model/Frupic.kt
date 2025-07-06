package de.saschahlusiak.frupic.model

import android.database.Cursor
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.saschahlusiak.frupic.db.FrupicDB
import de.saschahlusiak.frupic.utils.getInt
import de.saschahlusiak.frupic.utils.getString
import java.io.File
import java.io.Serializable
import kotlin.text.split

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

    @ColumnInfo(name = "flags")
    val flags: Int,

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

    val isStarred get() = (flags and FLAG_FAV) != 0

    val filename by lazy { File(fullUrl).name }

    constructor(cursor: Cursor) : this(
        id = cursor.getInt(FrupicDB.ID_ID),
        flags = cursor.getInt(FrupicDB.FLAGS_ID),
        fullUrl = cursor.getString(FrupicDB.FULLURL_ID),
        thumbUrl = cursor.getString(FrupicDB.THUMBURL_ID),
        date = cursor.getString(FrupicDB.DATE_ID),
        username = cursor.getString(FrupicDB.USERNAME_ID),
        tagsString = cursor.getString(FrupicDB.TAGS_ID)
    )

    fun hasFlag(flag: Int) = (flags and flag) != 0

    companion object {
        private const val serialVersionUID = 12345L
        const val FLAG_NEW = 0x01
        const val FLAG_FAV = 0x02
        const val FLAG_UNUSED = 0x04
        const val FLAG_HIDDEN = 0x08
    }
}