package de.saschahlusiak.frupic.utils

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONArray

fun <T> JSONArray.toSequence() = sequence {
    @Suppress("UNCHECKED_CAST")
    for (i in 0 until length()) yield(get(i) as T)
}

fun <T> JSONArray.toList(): List<T> = toSequence<T>().toList()

fun <T> Deferred<T>.asFlow(): Flow<T> = flow { emit(await()) }