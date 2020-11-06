package de.saschahlusiak.frupic.utils

import android.database.Cursor
import org.json.JSONArray

fun <T> JSONArray.toSequence() = sequence {
    @Suppress("UNCHECKED_CAST")
    for (i in 0 until length()) yield(get(i) as T)
}

fun <T> JSONArray.toList(): List<T> = toSequence<T>().toList()

fun Cursor.getInt(columnName: String) = getInt(getColumnIndexOrThrow(columnName))
fun Cursor.getString(columnName: String): String = getString(getColumnIndexOrThrow(columnName))