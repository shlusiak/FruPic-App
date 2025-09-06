package de.saschahlusiak.frupic.utils

import androidx.compose.runtime.Stable
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

@Stable
fun Long.sizeString(): String {
    val bytes = this
    if (bytes < 50000) return "$bytes bytes"
    val kb = bytes / 1024
    if (kb < 3000) return "$kb KB"
    val mb = kb / 1024
    return "$mb MB"
}
