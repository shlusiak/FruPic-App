package de.saschahlusiak.frupic.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class FrupicDBOpenHandler(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(FrupicDB.CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            if (oldVersion < 2) {
                db.execSQL(FrupicDB.UPGRADE_FROM_1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            db.execSQL(FrupicDB.DROP_TABLE)
            onCreate(db)
        }
    }

    companion object {
        private const val DATABASE_NAME = "frupic.db"
        private const val DATABASE_VERSION = 2
        private val tag = FrupicDBOpenHandler::class.java.simpleName
    }
}