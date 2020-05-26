package de.saschahlusiak.frupic.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import de.saschahlusiak.frupic.model.Frupic
import javax.inject.Inject

class FrupicDB @Inject constructor(context: Context) {
    private var db: SQLiteDatabase? = null
    private val dbHelper = FrupicDBOpenHandler(context)
    
    fun open(): Boolean {
        db = null
        try {
            db = dbHelper.writableDatabase
        } catch (e: Exception) {
            return false
        }

        return db != null
    }

    fun close() {
        dbHelper.close()
    }

    private fun addFrupic(frupic: Frupic): Boolean {
        val db = db ?: return false
        val fields = arrayOf(ID_ID)

        db.query(TABLE, fields, "$ID_ID=${frupic.id}", null, null, null, null).use {
            if (it.count >= 1) return false
        }

        val values = ContentValues().apply {
            put(ID_ID, frupic.id)
            put(FULLURL_ID, frupic.fullUrl)
            put(THUMBURL_ID, frupic.thumbUrl)
            put(DATE_ID, frupic.date)
            put(USERNAME_ID, frupic.username)
            put(FLAGS_ID, frupic.flags)
            put(TAGS_ID, frupic.tagsString)
        }

        db.insert(TABLE, null, values)
        return true
    }

    fun addFrupics(frupics: Collection<Frupic>): Int {
        val db = db ?: return 0
        db.beginTransaction()
        var added = 0
        frupics.forEach {
            if (addFrupic(it)) added ++
        }
        db.setTransactionSuccessful()
        db.endTransaction()
        return added
    }

    fun updateFlags(frupic: Frupic?, flag: Int, value: Boolean): Boolean {
        val db = db ?: return false
        var clearMask = flag.inv()
        val set = if (value) flag else 0
        if (clearMask < 0) clearMask += 65536
        var sql = "UPDATE $TABLE SET flags = (flags & $clearMask) | $set"
        if (frupic != null) sql += " WHERE " + ID_ID + "=" + frupic.id
        db.execSQL(sql)
        return true
    }

    fun getFrupics(username: String?, flagmask: Int): Cursor {
        val db = requireNotNull(db) { "DB is not open" }
        var where = ""
        if (username != null) where += "$USERNAME_ID=$username"
        if (where != "") where += " AND "
        where += "($FLAGS_ID&$flagmask) = $flagmask"

        return db.query(TABLE, null, where, null, null, null, "$ID_ID DESC", null)
    }

    companion object {
        const val TABLE = "frupics"
        const val ID_ID = "_id"
        const val FULLURL_ID = "fullurl"
        const val THUMBURL_ID = "thumburl"
        const val DATE_ID = "date"
        const val USERNAME_ID = "username"
        const val FLAGS_ID = "flags"
        const val TAGS_ID = "tags"

        const val CREATE_TABLE = """
            CREATE TABLE $TABLE (
                $ID_ID INTEGER PRIMARY KEY, 
                $FULLURL_ID TEXT, 
                $THUMBURL_ID TEXT,
                $DATE_ID TEXT, 
                $USERNAME_ID TEXT, 
                $FLAGS_ID INTEGER, 
                $TAGS_ID TEXT
            );
            """

        const val UPGRADE_FROM_1 = "ALTER TABLE $TABLE ADD $TAGS_ID TEXT;"
        const val DROP_TABLE = "DROP TABLE IF EXISTS $TABLE;"
    }

}