package de.saschahlusiak.frupic.model

import android.database.Cursor
import de.saschahlusiak.frupic.db.FrupicDB
import java.io.File
import java.io.Serializable

/**
 * Model object of a Frupic definition, as read from the database or the API.
 */
class Frupic(
    @JvmField val id: Int,
    @JvmField var flags: Int,
    val fullUrl: String,
    val thumbUrl: String,
    val date: String?,
    val username: String?,
    val tags: List<String>
) : Serializable {

    @JvmField
    @Deprecated("Don't use this")
    var tag: Any? = null

    val url = "https://frupic.frubar.net/$id"
    val isAnimated = fullUrl.endsWith(".gif") || fullUrl.endsWith(".GIF")
    val tagsString = tags.joinToString(", ")

    constructor(cursor: Cursor) : this(
        id = cursor.getInt(FrupicDB.ID_INDEX),
        flags = cursor.getInt(FrupicDB.FLAGS_INDEX),
        fullUrl = cursor.getString(FrupicDB.FULLURL_INDEX),
        thumbUrl = cursor.getString(FrupicDB.THUMBURL_INDEX),
        date = cursor.getString(FrupicDB.DATE_INDEX),
        username = cursor.getString(FrupicDB.USERNAME_INDEX),
        tags = cursor.getString(FrupicDB.TAGS_INDEX)?.split(", ") ?: emptyList()
    )

//    val fullUrl: String
//        get() = full_url
//            .replace("frupic.frubar.net".toRegex(), "d1ofuc5rnolp9w.cloudfront.net")

//    val thumbUrl: String
//        get() = thumb_url
//            .replace("frupic.frubar.net".toRegex(), "d1ofuc5rnolp9w.cloudfront.net")

    @Deprecated("Don't use this")
    fun getFileName(thumb: Boolean): String {
        return if (!thumb) File(fullUrl).name else "frupic_" + id + if (thumb) "_thumb" else ""
    }

    fun hasFlag(flag: Int) = (flags and flag) != 0

    override fun equals(other: Any?) = (other is Frupic) && other.id == id

    override fun hashCode() = id

    companion object {
        private const val serialVersionUID = 12345L
        const val FLAG_NEW = 0x01
        const val FLAG_FAV = 0x02
        const val FLAG_UNSEEN = 0x04
    }
}