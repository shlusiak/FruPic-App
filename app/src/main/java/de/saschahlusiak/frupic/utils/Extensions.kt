package de.saschahlusiak.frupic.utils

import org.json.JSONArray

fun <T> JSONArray.toSequence() = sequence<T> {
    @Suppress("UNCHECKED_CAST")
    for (i in 0 until length()) yield(get(i) as T)
}

fun <T> JSONArray.toList(): List<T> = toSequence<T>().toList()
