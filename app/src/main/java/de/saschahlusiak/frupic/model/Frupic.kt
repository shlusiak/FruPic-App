package de.saschahlusiak.frupic.model

import android.database.Cursor
import de.saschahlusiak.frupic.db.FrupicDB
import de.saschahlusiak.frupic.utils.getInt
import de.saschahlusiak.frupic.utils.getString
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
class Frupic(
    val id: Int,
    var flags: Int,
    val fullUrl: String,
    val thumbUrl: String,
    val date: String?,
    val username: String?,
    val tags: List<String>
) : Serializable {

    val url = "https://frupic.frubar.net/$id"
    val isAnimated = fullUrl.endsWith(".gif") || fullUrl.endsWith(".GIF")
    val tagsString = tags.joinToString(", ")

    val isStarred get() = (flags and FLAG_FAV) != 0

    val filename by lazy { File(fullUrl).name }

    constructor(cursor: Cursor) : this(
        id = cursor.getInt(FrupicDB.ID_ID),
        flags = cursor.getInt(FrupicDB.FLAGS_ID),
        fullUrl = cursor.getString(FrupicDB.FULLURL_ID),
        thumbUrl = cursor.getString(FrupicDB.THUMBURL_ID),
        date = cursor.getString(FrupicDB.DATE_ID),
        username = cursor.getString(FrupicDB.USERNAME_ID),
        tags = cursor.getString(FrupicDB.TAGS_ID)?.split(", ") ?: emptyList()
    )

    fun hasFlag(flag: Int) = (flags and flag) != 0

    override fun equals(other: Any?) = (other is Frupic) && other.id == id

    override fun hashCode() = id

    companion object {
        private const val serialVersionUID = 12345L
        const val FLAG_NEW = 0x01
        const val FLAG_FAV = 0x02
        const val FLAG_UNUSED = 0x04
    }
}